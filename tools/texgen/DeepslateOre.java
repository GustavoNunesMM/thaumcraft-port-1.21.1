import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Makes a deepslate variant of a 1.12 stone ore texture: pixels that don't look like plain stone
 * (saturated, or much brighter/darker than stone grey) are copied onto the vanilla deepslate texture.
 * Usage: java DeepslateOre.java <ore.png> <deepslate.png> <out.png>
 */
public class DeepslateOre {
   public static void main(String[] a) throws Exception {
      BufferedImage ore = ImageIO.read(new File(a[0]));
      BufferedImage deep = ImageIO.read(new File(a[1]));
      BufferedImage out = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
      for (int y = 0; y < 16; y++) {
         StringBuilder row = new StringBuilder();
         for (int x = 0; x < 16; x++) {
            int p = ore.getRGB(x, y);
            int r = p >> 16 & 255, g = p >> 8 & 255, b = p & 255;
            int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
            float sat = max == 0 ? 0 : (max - min) / (float) max;
            boolean isOre = sat > 0.18f || max > 0xA8 || max < 0x50;
            out.setRGB(x, y, isOre ? p : deep.getRGB(x, y));
            row.append(isOre ? '#' : '.');
         }
         System.out.println(row);
      }
      ImageIO.write(out, "png", new File(a[2]));
   }
}
