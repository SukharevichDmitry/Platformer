package com.cursedarchie.platformer.run;

/** Six bag cells and two separate quick-access pockets. */
public final class Inventory {
    public static final int COLUMNS = 3;
    public static final int ROWS = 2;
    public static final int CAPACITY = COLUMNS * ROWS;
    public static final int POCKETS = 2;
    public static final int TOTAL_SLOTS = CAPACITY + POCKETS;
    private final String[] ids = new String[TOTAL_SLOTS];
    private final int[] counts = new int[TOTAL_SLOTS];

    public String itemId(int slot) { checkSlot(slot); return ids[slot]; }
    public int count(int slot) { checkSlot(slot); return counts[slot]; }

    public int count(String id) {
        int total = 0;
        for (int i = 0; i < TOTAL_SLOTS; i++) if (id.equals(ids[i])) total += counts[i];
        return total;
    }

    public boolean add(String id) {
        if (!ItemCatalog.supports(id)) throw new IllegalArgumentException("Unknown item: " + id);
        if (ItemCatalog.SEALED_NOTE.equals(id) && count(id) > 0) return false;
        int max = ItemCatalog.maxStack(id);
        for (int i = 0; i < CAPACITY; i++) {
            if (id.equals(ids[i]) && counts[i] < max) { counts[i]++; return true; }
        }
        for (int i = 0; i < CAPACITY; i++) {
            if (ids[i] == null) { ids[i] = id; counts[i] = 1; return true; }
        }
        return false;
    }

    public boolean removeOne(int slot) {
        checkSlot(slot);
        if (ids[slot] == null) return false;
        if (--counts[slot] == 0) ids[slot] = null;
        return true;
    }

    public int firstSlot(String id) {
        for (int i = CAPACITY; i < TOTAL_SLOTS; i++) if (id.equals(ids[i])) return i;
        return -1;
    }

    public boolean swapWithPocket(int bagSlot, int pocket) {
        if (bagSlot < 0 || bagSlot >= CAPACITY || pocket < 0 || pocket >= POCKETS)
            throw new IndexOutOfBoundsException("Bag or pocket slot");
        int pocketSlot = CAPACITY + pocket;
        if (ids[bagSlot] == null && ids[pocketSlot] == null) return false;
        String id = ids[bagSlot];
        int count = counts[bagSlot];
        ids[bagSlot] = ids[pocketSlot];
        counts[bagSlot] = counts[pocketSlot];
        ids[pocketSlot] = id;
        counts[pocketSlot] = count;
        return true;
    }

    public void setSlot(int slot, String id, int count) {
        checkSlot(slot);
        if (id == null && count == 0) { ids[slot] = null; counts[slot] = 0; return; }
        if (!ItemCatalog.supports(id) || count < 1 || count > ItemCatalog.maxStack(id))
            throw new IllegalArgumentException("Invalid inventory slot");
        ids[slot] = id;
        counts[slot] = count;
    }

    private static void checkSlot(int slot) {
        if (slot < 0 || slot >= TOTAL_SLOTS) throw new IndexOutOfBoundsException("Inventory slot " + slot);
    }
}
