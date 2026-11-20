import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Reproducible visual example, not part of the game's runtime asset pipeline. */
public final class RoadModuleExample {
    private static final int WIDTH = 512;
    private static final int HEIGHT = 448;
    private static final int CROP_X = 68;
    private static final int CROP_Y = 493;
    private static final int SEAM_X = CROP_X + WIDTH;
    private static final int EDGE = 64;
    private static final int BLEND = 64;

    public static void main(String[] args) throws Exception {
        File folder = new File(args.length == 0 ? "." : args[0]);
        BufferedImage source = ImageIO.read(new File(folder, "source-red-road.png"));
        if (source.getWidth() < CROP_X + WIDTH * 3 || source.getHeight() < CROP_Y + HEIGHT)
            throw new IllegalArgumentException("Unexpected source size");

        BufferedImage[] modules = new BufferedImage[3];
        for (int module = 0; module < modules.length; module++) {
            BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    int original = source.getRGB(CROP_X + module * WIDTH + x, CROP_Y + y);
                    int pixel = original;
                    if (x < EDGE + BLEND) {
                        int shared = source.getRGB(SEAM_X + x, CROP_Y + y);
                        float weight = x < EDGE ? 1f : (EDGE + BLEND - x) / (float) BLEND;
                        pixel = mix(original, shared, weight);
                    } else if (x >= WIDTH - EDGE - BLEND) {
                        int shared = source.getRGB(SEAM_X - WIDTH + x, CROP_Y + y);
                        float weight = x >= WIDTH - EDGE ? 1f :
                            (x - (WIDTH - EDGE - BLEND)) / (float) BLEND;
                        pixel = mix(original, shared, weight);
                    }
                    image.setRGB(x, y, pixel);
                }
            }
            modules[module] = image;
            ImageIO.write(image, "png", new File(folder, "road-" + (char) ('A' + module) + ".png"));
        }
        verifyEdges(source, modules);
        writeSheet(folder, modules);
        writeAssembly(folder, modules);
    }

    private static int mix(int base, int edge, float edgeWeight) {
        int result = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int a = (base >>> shift) & 255;
            int b = (edge >>> shift) & 255;
            result |= Math.round(a * (1f - edgeWeight) + b * edgeWeight) << shift;
        }
        return result;
    }

    private static void verifyEdges(BufferedImage source, BufferedImage[] modules) {
        for (BufferedImage image : modules) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < EDGE; x++) {
                    if (image.getRGB(x, y) != source.getRGB(SEAM_X + x, CROP_Y + y) ||
                        image.getRGB(WIDTH - EDGE + x, y) !=
                            source.getRGB(SEAM_X - EDGE + x, CROP_Y + y))
                        throw new AssertionError("The shared seam contract was not preserved");
                }
            }
        }
    }

    private static void writeSheet(File folder, BufferedImage[] modules) throws Exception {
        BufferedImage sheet = new BufferedImage(3 * WIDTH + 4 * 24,
            HEIGHT + 76, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(new Color(25, 29, 37));
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setColor(new Color(233, 215, 176));
        g.setFont(new Font("Monospaced", Font.BOLD, 22));
        String[] labels = {"A  intact", "B  repaired", "C  cracked"};
        for (int i = 0; i < modules.length; i++) {
            int x = 24 + i * (WIDTH + 24);
            g.drawString(labels[i], x, 30);
            g.drawImage(modules[i], x, 52, null);
        }
        g.dispose();
        ImageIO.write(sheet, "png", new File(folder, "module-sheet.png"));
    }

    private static void writeAssembly(File folder, BufferedImage[] modules) throws Exception {
        int[] order = {0, 2, 1, 0, 2};
        BufferedImage assembled = new BufferedImage(order.length * WIDTH,
            HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = assembled.createGraphics();
        g.setColor(new Color(25, 29, 37));
        g.fillRect(0, 0, assembled.getWidth(), assembled.getHeight());
        for (int i = 0; i < order.length; i++)
            g.drawImage(modules[order[i]], i * WIDTH, 0, null);
        g.dispose();
        ImageIO.write(assembled, "png", new File(folder, "assembled-A-C-B-A-C.png"));
    }
}
