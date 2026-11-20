import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Preserves the approved paving and blends a drawn soil continuation below it. */
public final class RoadDepthComposer {
    private static final int WIDTH = 512;
    private static final int ROAD_HEIGHT = 448;
    private static final int DEPTH_HEIGHT = 576;
    private static final int EDGE_BLEND = 64;
    private static final int JOIN_BLEND = 96;

    public static void main(String[] args) throws Exception {
        File assets = new File(args.length == 0 ? "assets/terrain" : args[0]);
        File referenceFile = new File(args.length < 2
            ? "documents/road-texture-examples/soil-reference-portrait.png" : args[1]);
        BufferedImage reference = ImageIO.read(referenceFile);
        if (reference == null) throw new IllegalArgumentException("Missing soil reference");
        int requiredSourceHeight = (JOIN_BLEND + DEPTH_HEIGHT) * reference.getWidth() / WIDTH;
        if (reference.getHeight() < requiredSourceHeight)
            throw new IllegalArgumentException("Soil reference is too short for uniform scaling");
        for (char variant = 'A'; variant <= 'C'; variant++) {
            File target = new File(assets, "ruins-road-" + variant + ".png");
            BufferedImage road = ImageIO.read(new File(referenceFile.getParentFile(),
                "road-" + variant + ".png"));
            if (road.getWidth() != WIDTH || road.getHeight() != ROAD_HEIGHT)
                throw new IllegalArgumentException("Expected original 512x448 road: " + target);
            BufferedImage extended = new BufferedImage(WIDTH, ROAD_HEIGHT + DEPTH_HEIGHT,
                BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < ROAD_HEIGHT - JOIN_BLEND; y++)
                for (int x = 0; x < WIDTH; x++) extended.setRGB(x, y, road.getRGB(x, y));
            for (int y = ROAD_HEIGHT - JOIN_BLEND; y < ROAD_HEIGHT + DEPTH_HEIGHT; y++) {
                int depth = y - (ROAD_HEIGHT - JOIN_BLEND);
                float join = smooth(Math.min(1f, depth / (float) JOIN_BLEND));
                for (int x = 0; x < WIDTH; x++) {
                    int soil = tileableSoil(reference, x, depth);
                    int original = y < ROAD_HEIGHT ? road.getRGB(x, y) : soil;
                    extended.setRGB(x, y, mix(original, soil, join));
                }
            }
            ImageIO.write(extended, "png", target);
        }
    }

    private static int tileableSoil(BufferedImage image, int x, int y) {
        int pixel = sample(image, x, y);
        int seam = mix(sample(image, 0, y), sample(image, WIDTH - 1, y), .5f);
        if (x < EDGE_BLEND)
            return mix(pixel, seam, smooth((EDGE_BLEND - x) / (float) EDGE_BLEND));
        if (x >= WIDTH - EDGE_BLEND)
            return mix(pixel, seam, smooth((x - (WIDTH - EDGE_BLEND) + 1) / (float) EDGE_BLEND));
        return pixel;
    }

    private static int sample(BufferedImage image, int x, int y) {
        int sourceX = x * image.getWidth() / WIDTH;
        // Keep the source's pixel aspect ratio: X and Y use the same scale.
        int sourceHeight = (JOIN_BLEND + DEPTH_HEIGHT) * image.getWidth() / WIDTH;
        int sourceY = image.getHeight() - sourceHeight + y * image.getWidth() / WIDTH;
        return image.getRGB(Math.min(image.getWidth() - 1, sourceX),
            Math.min(image.getHeight() - 1, sourceY));
    }

    private static float smooth(float t) { return t * t * (3f - 2f * t); }

    private static int mix(int a, int b, float t) {
        int result = 0xff000000;
        for (int shift : new int[] {16, 8, 0}) {
            int aa = (a >>> shift) & 255;
            int bb = (b >>> shift) & 255;
            result |= Math.round(aa + (bb - aa) * t) << shift;
        }
        return result;
    }
}
