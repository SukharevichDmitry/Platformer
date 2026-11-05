package com.cursedarchie.platformer.actors.enemies.group;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.Array;
import com.cursedarchie.platformer.actors.enemies.Enemy;
import com.cursedarchie.platformer.actors.hero.Hero;
import com.cursedarchie.platformer.physics.PhysicsCategories;
import com.cursedarchie.platformer.world.GameWorld;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Owns the combat slots around the hero and hands them to the enemies that are fighting. Nothing here moves a body
 * directly (except the leap over the hero and the capped separation push): the manager decides where each enemy
 * should be, the enemy's own steering walks it there, so positions change smoothly and never teleport.
 *
 * <p>One pass per fixed step: refresh slot geometry, take in / drop enemies, release slots that stopped being
 * valid, hand out slots (melee roles first, then archers, then support) and publish each enemy's slot state and
 * target. The pass is single-threaded and claims a slot the moment it is chosen, so two enemies can never pick the
 * same one.</p>
 */
public final class CombatSlotManager {
    /** Upward speed of the arc over the hero. */
    private static final float LEAP_VY = 19f;
    /** An enemy this much off the hero's floor cannot strike (feet vs floor, metres). */
    private static final float REACH_HEIGHT = 1.0f;

    private final GameWorld gameWorld;
    private final GroupPositioningConfig config;
    // java.util lists on purpose: libGDX Array iterators cannot be nested, and the slot helpers nest loops.
    private final List<CombatSlot> slots = new ArrayList<>();
    private final List<Enemy> members = new ArrayList<>();
    private final List<Enemy> pass = new ArrayList<>();
    private final List<Enemy> allEnemies = new ArrayList<>();
    private float floorY;
    private float heroX, heroY;
    private boolean heroFacingLeft;
    private int lastRoomId = Integer.MIN_VALUE;
    private final boolean[] hit = new boolean[1];
    private float[] push = new float[0];

    public CombatSlotManager(GameWorld gameWorld) {
        this(gameWorld, GroupPositioningConfig.get());
    }

    public CombatSlotManager(GameWorld gameWorld, GroupPositioningConfig config) {
        this.gameWorld = gameWorld;
        this.config = config;
        buildSlots();
    }

    /*======================================================SLOTS=====================================================*/

    private void buildSlots() {
        int id = 0;
        int[] sides = {-1, 1};
        for (int side : sides) {
            String s = side < 0 ? "LEFT" : "RIGHT";
            String l = side < 0 ? "L" : "R";
            slots.add(new CombatSlot(id++, "FRONT_" + s, CombatSlot.Type.MELEE, side, config.frontOffset, 0));
            slots.add(new CombatSlot(id++, "SIDE_" + s, CombatSlot.Type.MELEE, side, config.sideOffset, 1));
            slots.add(new CombatSlot(id++, "REAR_" + s, CombatSlot.Type.MELEE, side, config.rearOffset, 2));
            for (int i = 0; i < config.reserveCount; i++)
                slots.add(new CombatSlot(id++, "RESERVE_" + l + (i + 1), CombatSlot.Type.RESERVE, side,
                    config.reserveStart + i * config.reserveStep, i));
            for (int i = 0; i < config.rangedCount; i++)
                slots.add(new CombatSlot(id++, "RANGED_" + l + (i + 1), CombatSlot.Type.RANGED, side,
                    config.rangedFirst + i * config.rangedStep, i));
            for (int i = 0; i < config.supportCount; i++)
                slots.add(new CombatSlot(id++, "SUPPORT_" + l + (i + 1), CombatSlot.Type.SUPPORT, side,
                    config.supportFirst + i * config.supportStep, i));
        }
    }

    public List<CombatSlot> getSlots() { return slots; }
    public GroupPositioningConfig getConfig() { return config; }

    public CombatSlot findSlot(String name) {
        for (CombatSlot slot : slots) if (slot.name.equals(name)) return slot;
        return null;
    }

    public int countOccupied(CombatSlot.Type type) {
        int count = 0;
        for (CombatSlot slot : slots) if (slot.type == type && slot.isOccupied()) count++;
        return count;
    }

    /** Frees every slot and tells every enemy to forget it (room change, location change). */
    public void clear() {
        for (CombatSlot slot : slots) slot.setOwner(null);
        Array<Enemy> enemies = gameWorld.getEnemies();
        for (int i = 0; i < enemies.size; i++) {
            Enemy enemy = enemies.get(i);
            enemy.clearSlotAssignment();
            enemy.endLeap();
        }
        members.clear();
    }

    /** One enemy leaves the formation for good (it died, or its room unloaded). */
    public void forget(Enemy enemy) {
        release(enemy);
        enemy.clearSlotAssignment();
        members.remove(enemy);
    }

    private void release(Enemy enemy) {
        CombatSlot slot = enemy.getCurrentSlot();
        if (slot != null && slot.getOwner() == enemy) slot.setOwner(null);
        enemy.setCurrentSlot(null);
        enemy.setDesiredSlot(null);
    }

    /*=====================================================UPDATE=====================================================*/

    public void update(float delta) {
        Hero hero = gameWorld.getHero();
        int room = gameWorld.getCurrentRoomId();
        if (room != lastRoomId) {
            clear();
            lastRoomId = room;
        }
        heroX = hero.getPosition().x;
        heroY = hero.getPosition().y;
        heroFacingLeft = hero.isFacingLeft();
        allEnemies.clear();
        Array<Enemy> world = gameWorld.getEnemies();
        for (int i = 0; i < world.size; i++) allEnemies.add(world.get(i));

        refreshGeometry(hero);
        collectMembers();
        releaseInvalid();
        trackAttackCompletion(delta);
        assign(hero);
        publish(hero, delta);
        endSettledLeaps(hero);
    }

    /** The floor under the hero (not the hero's body, so a jump does not collapse the formation). */
    private void refreshGeometry(Hero hero) {
        Vector2 h = hero.getPosition();
        floorY = h.y - .5f;
        final float[] best = {1f};
        gameWorld.getWorld().rayCast((fixture, point, normal, fraction) -> {
            if (fixture.isSensor() || !"level".equals(fixture.getBody().getUserData())) return -1f;
            if (fraction < best[0]) {
                best[0] = fraction;
                floorY = point.y;
            }
            return fraction;
        }, h.x, h.y, h.x, h.y - 12f);

        for (CombatSlot slot : slots) {
            slot.x = heroX + slot.side * slot.offset;
            slot.y = floorY + .5f;
            slot.valid = !wallBetween(h.x, h.y, slot.x) && groundAt(slot.x);
            slot.attackCapable = slot.type == CombatSlot.Type.MELEE && slot.valid;
        }
    }

    private boolean wallBetween(float fromX, float y, float toX) {
        hit[0] = false;
        gameWorld.getWorld().rayCast((fixture, point, normal, fraction) -> {
            if (fixture.isSensor()) return -1f;
            if (fixture.getFilterData().categoryBits == PhysicsCategories.WALL) {
                hit[0] = true;
                return 0f;
            }
            return -1f;
        }, fromX, y, toX, y);
        return hit[0];
    }

    private boolean groundAt(float x) {
        hit[0] = false;
        gameWorld.getWorld().rayCast((fixture, point, normal, fraction) -> {
            if (!fixture.isSensor() && "level".equals(fixture.getBody().getUserData())) {
                hit[0] = true;
                return 0f;
            }
            return -1f;
        }, x, floorY + .6f, x, floorY - 1.5f);
        return hit[0];
    }

    /** Alive, awake, on their feet and aware of the hero: those are in the formation. */
    private void collectMembers() {
        members.clear();
        for (int i = 0; i < allEnemies.size(); i++) {
            Enemy e = allEnemies.get(i);
            boolean out = !e.isAlive() || !e.isActive() || e.isKnockedDown() || e.isStaggered() ||
                Math.abs(e.getPosition().x - heroX) > config.engageRange ||
                !(e.isCanSeeHero() || e.isAttacking());
            if (out) {
                release(e);
                // A staggered or fallen enemy keeps its "managed" flag (it cannot attack anyway); one that simply lost
                // the hero goes back to ordinary patrol / search behaviour.
                if (!e.isAlive() || !e.isActive() || !(e.isKnockedDown() || e.isStaggered()))
                    e.clearSlotAssignment();
                else {
                    e.setSlotState(SlotState.NONE);
                    e.setSlotTargetX(Float.NaN);
                    e.setSlotCrossing(false);
                }
                continue;
            }
            e.setSlotManaged(true);
            members.add(e);
        }
    }

    private void releaseInvalid() {
        for (CombatSlot slot : slots) {
            Enemy owner = slot.getOwner();
            if (owner == null) continue;
            if (!members.contains(owner)) { release(owner); continue; }
            if (slot.isReserved()) continue;                       // an attack in progress keeps its slot
            if (owner.isLeapAirborne()) continue;
            boolean melee = owner.getCombatRole().isMelee();
            if (!slot.valid || (melee && slot.type == CombatSlot.Type.MELEE && !heroReachable(owner)) ||
                (melee && owner.isRetreating())) {
                release(owner);
            }
        }
    }

    /** After an attack ends the attacker may step away so a waiting ally can use the slot. */
    private void trackAttackCompletion(float delta) {
        int waiting = 0;
        for (Enemy e : members)
            if (e.getCombatRole().isMelee() && !e.hasAttackSlot() && e.getSlotLeaveRemaining() <= 0f) waiting++;
        for (Enemy e : members) {
            boolean attacking = e.isAttacking();
            e.setSlotLeaveRemaining(e.getSlotLeaveRemaining() - delta);
            if (e.hasAttackSlot() && !attacking) e.setSlotHoldTime(e.getSlotHoldTime() + delta);
            if (e.wasSlotAttacking() && !attacking && e.getCombatRole().isMelee() && e.hasAttackSlot()) {
                CombatRole role = e.getCombatRole();
                boolean step = false;
                float duration = config.leaveDuration;
                if (role == CombatRole.FAST) { step = true; duration = config.fastLeaveDuration; }
                else if (role == CombatRole.STANDARD) step = waiting > 0;
                else if (role == CombatRole.HEAVY) step = waiting > 0 && e.getSlotHoldTime() >= config.heavyHoldTime;
                if (step) {
                    release(e);
                    e.setSlotLeaveRemaining(duration);
                    e.setSlotHoldTime(0f);
                }
            }
            e.setSlotWasAttacking(attacking);
        }
    }

    /*====================================================ASSIGN======================================================*/

    private void assign(Hero hero) {
        // Everyone who is not locked into an attack, a leap or a step-away is re-solved: nearest first.
        pass.clear();
        for (Enemy e : members) {
            if (e.isAttacking() || e.isDashPhase() || e.isLeapAirborne()) continue;
            if (e.getSlotLeaveRemaining() > 0f) { release(e); continue; }
            if (e.hasAttackSlot() && e.getSlotState() == SlotState.ATTACK_POSITION && e.getCombatRole().isMelee()) {
                continue;                                              // settled attackers keep their place
            }
            pass.add(e);
        }
        for (Enemy e : pass) release(e);                                 // last step's slot lives on in `previous`
        final Comparator<Enemy> byDistance = (a, b) -> {
            int c = Float.compare(Math.abs(a.getPosition().x - heroX), Math.abs(b.getPosition().x - heroX));
            return c != 0 ? c : Integer.compare(allEnemies.indexOf(a), allEnemies.indexOf(b));
        };
        solveGroup(hero, byDistance, true, false, false);
        solveGroup(hero, byDistance, false, true, false);
        solveGroup(hero, byDistance, false, false, true);
    }

    private final java.util.IdentityHashMap<Enemy, CombatSlot> previous = new java.util.IdentityHashMap<>();

    private void solveGroup(Hero hero, Comparator<Enemy> order, boolean melee, boolean ranged, boolean support) {
        List<Enemy> group = new ArrayList<>();
        for (Enemy e : pass) {
            CombatRole role = e.getCombatRole();
            if (melee && role.isMelee() || ranged && role == CombatRole.RANGED || support && role == CombatRole.SUPPORT)
                group.add(e);
        }
        group.sort(order);
        for (Enemy e : group) {
            CombatSlot pick = null;
            CombatRole role = e.getCombatRole();
            if (role.isMelee()) {
                pick = bestMeleeSlot(e, hero);
                if (pick == null) pick = bestReserveSlot(e);
            } else if (role == CombatRole.RANGED) pick = bestRangedSlot(e);
            else pick = bestSupportSlot(e);
            if (pick != null) {
                if (previous.get(e) != pick) e.setSlotHoldTime(0f);
                pick.setOwner(e);
                e.setCurrentSlot(pick);
                e.setDesiredSlot(pick);
            } else {
                e.setCurrentSlot(null);
                e.setDesiredSlot(null);
            }
        }
    }

    private int sideOf(Enemy e) {
        float dx = e.getPosition().x - heroX;
        if (Math.abs(dx) < .05f) return e.isFacingLeft() ? 1 : -1;   // facing the hero: left means it stands right
        return dx > 0f ? 1 : -1;
    }

    /** How far an enemy that is wider than a standard one stands outside the tier distance. */
    private static float extra(Enemy e) { return Math.max(0f, (e.getSize() - 1f) * .5f); }

    private float effectiveOffset(CombatSlot slot, Enemy e) {
        return slot.type == CombatSlot.Type.MELEE || slot.type == CombatSlot.Type.RESERVE ?
            slot.offset + extra(e) : slot.offset;
    }

    public float targetXFor(CombatSlot slot, Enemy e) {
        return heroX + slot.side * effectiveOffset(slot, e);
    }

    /** Minimum centre distance between two allies of the given kinds. */
    public float minDistance(Enemy a, Enemy b) {
        CombatRole ra = a.getCombatRole(), rb = b.getCombatRole();
        float widest = (a.getSize() + b.getSize()) * .3f;
        float base;
        if (ra == CombatRole.HEAVY && rb == CombatRole.HEAVY) base = config.heavyHeavyMinDistance;
        else if (ra == CombatRole.HEAVY || rb == CombatRole.HEAVY) base = config.heavyStandardMinDistance;
        else if (ra == CombatRole.SUPPORT || rb == CombatRole.SUPPORT) base = config.supportAllyMinDistance;
        else if (ra == CombatRole.RANGED || rb == CombatRole.RANGED) base = config.rangedAllyMinDistance;
        else base = config.standardMinDistance;
        return Math.max(base, widest);
    }

    private boolean canCoexist(CombatSlot slot, Enemy e) {
        float mine = effectiveOffset(slot, e);
        for (CombatSlot other : slots) {
            Enemy owner = other.getOwner();
            if (owner == null || owner == e || other.side != slot.side) continue;
            if (Math.abs(mine - effectiveOffset(other, owner)) < minDistance(e, owner) - .001f) return false;
        }
        return true;
    }

    /** An enemy on a different level than the hero's floor cannot strike (one still falling keeps what it has). */
    private boolean heroReachable(Enemy e) {
        return !e.isGrounded() || Math.abs(e.getPosition().y - e.getSize() / 2f - floorY) < REACH_HEIGHT;
    }

    private boolean canCross(Enemy e) {
        return e.getCombatRole() != CombatRole.HEAVY && e.getCombatRole() != CombatRole.SUPPORT &&
            e.getSize() < 2f && e.isGrounded();
    }

    /** True when an enemy at {@code e}'s side could, standing in this melee slot, reach the hero with its weapon. */
    public boolean attackSuitable(CombatSlot slot, Enemy e) {
        if (slot.type != CombatSlot.Type.MELEE || !slot.valid) return false;
        float eff = effectiveOffset(slot, e);
        float contact = Hero.BODY_SIZE / 2f + e.getSize() / 2f;      // bodies just touching
        return eff >= contact && eff <= e.getAttackApproachDistance(gameWorld.getHero()) - .1f;
    }

    private CombatSlot bestMeleeSlot(Enemy e, Hero hero) {
        CombatRole role = e.getCombatRole();
        int own = sideOf(e);
        int facing = heroFacingLeft ? -1 : 1;
        CombatSlot best = null;
        float bestCost = Float.MAX_VALUE;
        for (CombatSlot slot : slots) {
            if (slot.type != CombatSlot.Type.MELEE || slot.isOccupied() || !attackSuitable(slot, e)) continue;
            boolean crossing = slot.side != own;
            if (crossing && !canCross(e)) continue;
            if (role == CombatRole.FAST && slot.priority == 0 && slot.side == facing) continue;   // not head-on
            if (!canCoexist(slot, e)) continue;
            float cost = Math.abs(e.getPosition().x - targetXFor(slot, e)) + slot.priority * .6f;
            if (crossing) cost += role == CombatRole.FAST ? .5f : config.crossingPenalty;
            if (role == CombatRole.FAST && slot.side == -facing) cost -= 2.5f;
            if (previousSlot(e) == slot) cost -= config.stickiness;
            if (cost < bestCost) { bestCost = cost; best = slot; }
        }
        return best;
    }

    private CombatSlot bestReserveSlot(Enemy e) {
        int own = sideOf(e);
        CombatSlot best = null;
        float bestCost = Float.MAX_VALUE;
        for (CombatSlot slot : slots) {
            if (slot.type != CombatSlot.Type.RESERVE || slot.isOccupied() || !slot.valid || slot.side != own) continue;
            if (!canCoexist(slot, e)) continue;
            float cost = Math.abs(e.getPosition().x - targetXFor(slot, e)) + slot.priority * .3f;
            if (previousSlot(e) == slot) cost -= config.stickiness;
            if (cost < bestCost) { bestCost = cost; best = slot; }
        }
        return best;
    }

    private CombatSlot bestRangedSlot(Enemy e) {
        int own = sideOf(e);
        CombatSlot best = null;
        float bestCost = Float.MAX_VALUE;
        for (CombatSlot slot : slots) {
            if (slot.type != CombatSlot.Type.RANGED || slot.isOccupied() || !slot.valid) continue;
            boolean crossing = slot.side != own;
            if (crossing && !canCross(e)) continue;
            if (!canCoexist(slot, e)) continue;
            float cost = Math.abs(e.getPosition().x - slot.x) + slot.priority * .5f;
            if (allyBetween(e, slot.x)) cost += 25f;     // never shoot through an ally: find a clear side instead
            if (crossing) cost += 4f;
            if (previousSlot(e) == slot) cost -= config.stickiness;
            if (cost < bestCost) { bestCost = cost; best = slot; }
        }
        return best;
    }

    private CombatSlot bestSupportSlot(Enemy e) {
        int own = sideOf(e);
        CombatSlot best = null;
        float bestCost = Float.MAX_VALUE;
        for (CombatSlot slot : slots) {
            if (slot.type != CombatSlot.Type.SUPPORT || slot.isOccupied() || !slot.valid || slot.side != own) continue;
            if (!canCoexist(slot, e)) continue;
            // Behind the front line: farther out than every melee ally on that side.
            if (slot.offset < frontLine(slot.side, e) + 1.0f) continue;
            float cost = Math.abs(e.getPosition().x - slot.x) + slot.priority * .5f - combatAllies(slot.side, e) * 1.5f;
            if (previousSlot(e) == slot) cost -= config.stickiness;
            if (cost < bestCost) { bestCost = cost; best = slot; }
        }
        return best;
    }

    private float frontLine(int side, Enemy self) {
        float line = 0f;
        for (Enemy m : members) {
            if (m == self || !m.getCombatRole().isMelee()) continue;
            if (sideOf(m) != side) continue;
            line = Math.max(line, Math.abs(m.getPosition().x - heroX));
        }
        return line;
    }

    private int combatAllies(int side, Enemy self) {
        int count = 0;
        for (Enemy m : members)
            if (m != self && m.getCombatRole().isMelee() && sideOf(m) == side) count++;
        return count;
    }

    /** An ally stands between the point x and the hero, close to the hero's floor level. */
    private boolean allyBetween(Enemy self, float x) {
        float lo = Math.min(x, heroX) + .3f, hi = Math.max(x, heroX) - .3f;
        for (int i = 0; i < allEnemies.size(); i++) {
            Enemy o = allEnemies.get(i);
            if (o == self || !o.isAlive()) continue;
            float ox = o.getPosition().x;
            if (ox > lo && ox < hi && Math.abs(o.getPosition().y - o.getSize() / 2f - floorY) < .6f) return true;
        }
        return false;
    }

    private CombatSlot previousSlot(Enemy e) {
        return previous.get(e);
    }

    /*====================================================PUBLISH=====================================================*/

    private void publish(Hero hero, float delta) {
        previous.clear();
        for (Enemy e : members) {
            if (e.isLeapAirborne()) continue;
            CombatRole role = e.getCombatRole();
            int own = sideOf(e);
            CombatSlot slot = e.getCurrentSlot();
            e.setSlotCrossing(false);

            if (e.getSlotLeaveRemaining() > 0f) {
                e.setSlotState(SlotState.LEAVING_SLOT);
                e.setSlotTargetX(heroX + own * config.reserveStart);
                continue;
            }
            if (slot == null) {
                // Nothing free: wait on the outer edge, combat-ready, until something opens up.
                float far = role == CombatRole.RANGED ? config.rangedMaxDistance :
                    role == CombatRole.SUPPORT ? Math.max(config.supportMinDistance + 1f, frontLine(own, e) + 1.5f) :
                    config.reserveStart + config.reserveCount * config.reserveStep;
                e.setSlotTargetX(heroX + own * clampBeforeWall(own, far));
                e.setSlotState(role == CombatRole.RANGED ? SlotState.RANGED_POSITION :
                    role == CombatRole.SUPPORT ? SlotState.SUPPORT_POSITION : SlotState.SURROUNDING);
                continue;
            }
            previous.put(e, slot);
            float target = targetXFor(slot, e);
            boolean crossing = slot.side != own;
            if (crossing) {
                e.setSlotCrossing(true);
                target = heroX + own * config.launchDistance;
                tryLeap(e, slot, own);
            }
            e.setSlotTargetX(target);
            switch (slot.type) {
                case MELEE: {
                    float off = Math.abs(e.getPosition().x - target);
                    boolean wasIn = e.getSlotState() == SlotState.ATTACK_POSITION;
                    boolean arrived = !crossing && (e.isAttacking() || off <= (wasIn ? config.restartTolerance : config.arriveTolerance));
                    e.setSlotState(arrived ? SlotState.ATTACK_POSITION : SlotState.SEEKING_ATTACK_SLOT);
                    break;
                }
                case RESERVE: e.setSlotState(SlotState.SURROUNDING); break;
                case RANGED: e.setSlotState(SlotState.RANGED_POSITION); break;
                default: e.setSlotState(SlotState.SUPPORT_POSITION); break;
            }
        }
    }

    /** A waiting spot behind a wall cannot be reached: stop short of the first wall between it and the hero. */
    private float clampBeforeWall(int side, float offset) {
        final float[] nearest = {offset};
        gameWorld.getWorld().rayCast((fixture, point, normal, fraction) -> {
            if (fixture.isSensor() || fixture.getFilterData().categoryBits != PhysicsCategories.WALL) return -1f;
            nearest[0] = Math.min(nearest[0], Math.abs(point.x - heroX) - .7f);
            return fraction;
        }, heroX, heroY, heroX + side * offset, heroY);
        return Math.max(1.2f, nearest[0]);
    }

    private void tryLeap(Enemy e, CombatSlot slot, int own) {
        if (Math.abs(e.getPosition().x - heroX) > config.leapMaxStartDistance) return;
        if (e.getLeapCooldownRemaining() > 0f || !e.isGrounded() || e.isLeaping()) return;
        float flight = 2f * LEAP_VY / Math.abs(gameWorld.getWorld().getGravity().y == 0f ? 80f :
            gameWorld.getWorld().getGravity().y);
        float destX = targetXFor(slot, e);
        float vx = (destX - e.getPosition().x) / flight;
        float cap = Math.min(config.leapSpeedCap, e.getVelocity() * 1.5f);
        vx = Math.max(-cap, Math.min(cap, vx));
        e.beginLeap(vx, LEAP_VY);
    }

    /** A leap ends once the enemy is clear of the hero's body again (contacts are off until then). */
    private void endSettledLeaps(Hero hero) {
        for (int i = 0; i < allEnemies.size(); i++) {
            Enemy e = allEnemies.get(i);
            if (e.getLeapPhase() != Enemy.LeapPhase.SETTLING) continue;
            float gap = Hero.BODY_SIZE / 2f + e.getSize() / 2f - .05f;
            float dx = e.getPosition().x - hero.getPosition().x;
            if (Math.abs(dx) >= gap) { e.endLeap(); continue; }
            if (e.getLeapTime() > .8f) {
                // Still wedged against the hero after the leap: walk out the way the arc was heading.
                float dir = dx == 0f ? (e.isFacingLeft() ? -1f : 1f) : Math.signum(dx);
                e.getBody().setLinearVelocity(dir * 3f, e.getBody().getLinearVelocity().y);
            }
        }
    }

    /*=================================================SOFT PUSHING===================================================*/

    /**
     * Allies are not solid to each other. Instead, once two of them are closer than their minimum distance, each is
     * given a small sideways speed away from the other: {@code direction * overlap * separationStrength}, capped.
     * The push is shared by weight: an enemy mid-attack is never moved, one standing in its slot is hard to shift,
     * one walking to its slot is nudged gently (it keeps walking), everyone else yields fully.
     */
    public void applySoftSeparation(Array<Enemy> enemies) {
        int n = enemies.size;
        if (push.length < n) push = new float[Math.max(n, push.length * 2)];
        for (int i = 0; i < n; i++) push[i] = 0f;
        for (int i = 0; i < n; i++) {
            Enemy a = enemies.get(i);
            if (!pushable(a)) continue;
            for (int j = i + 1; j < n; j++) {
                Enemy b = enemies.get(j);
                if (!pushable(b)) continue;
                if (Math.abs(a.getPosition().y - b.getPosition().y) > .9f) continue;
                float dx = b.getPosition().x - a.getPosition().x;
                float overlap = minDistance(a, b) - Math.abs(dx);
                if (overlap <= 0f) continue;
                float wa = weight(a), wb = weight(b);
                if (wa + wb <= 0f) continue;
                float dir = dx > 0f ? 1f : dx < 0f ? -1f : 1f;       // exact tie: the later enemy goes right
                float force = Math.min(overlap * config.separationStrength, config.maxSeparationSpeed);
                push[i] -= dir * force * (wa / (wa + wb));
                push[j] += dir * force * (wb / (wa + wb));
            }
        }
        for (int i = 0; i < n; i++) {
            if (push[i] == 0f) continue;
            Enemy e = enemies.get(i);
            float v = Math.max(-config.maxSeparationSpeed, Math.min(config.maxSeparationSpeed, push[i]));
            Vector2 lv = e.getBody().getLinearVelocity();
            e.getBody().setLinearVelocity(lv.x + v, lv.y);
        }
    }

    private boolean pushable(Enemy e) {
        return e.isAlive() && e.isActive() && !e.isKnockedDown() && !e.isStaggered() && !e.isDodging() &&
            !e.isDashing() && !e.isLeapAirborne() && e.getBody().getType() == BodyDef.BodyType.DynamicBody;
    }

    private float weight(Enemy e) {
        if (e.isAttacking() || e.isDashPhase() || e.isRangedWindingUp() || e.isCasting()) return 0f;
        SlotState state = e.getSlotState();
        if (state == SlotState.ATTACK_POSITION) return .25f;
        if (state == SlotState.SEEKING_ATTACK_SLOT || state == SlotState.RANGED_POSITION ||
            state == SlotState.SUPPORT_POSITION) return .6f;
        return 1f;
    }
}
