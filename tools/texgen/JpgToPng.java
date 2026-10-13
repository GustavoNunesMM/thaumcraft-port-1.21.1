import java.io.File;
import javax.imageio.ImageIO;

/** Converts JPG textures to PNG (1.21 only loads PNG textures). Usage: java JpgToPng.java <file.jpg>... */
public class JpgToPng {
   public static void main(String[] args) throws Exception {
      for (String a : args) {
         File in = new File(a);
         File out = new File(a.substring(0, a.length() - 4) + ".png");
         ImageIO.write(ImageIO.read(in), "png", out);
         System.out.println(out);
      }
   }
}
