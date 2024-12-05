import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.lang.Object;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;

public class CS160FinalDietzKarbon {
	public static void main(String args[]) {
		/* Notes:
		 * Ask the user what file they want to store data in
		 * Ask the user how many bits they want to use in that storage
		 * Tell the user how much data that will allow to store in the image
		 * Make sure the user is okay with that information
		 *
		 */
		String path = "test.png";
		try {
			BufferedImage containerImg = ImageIO.read(new File(path));
			int bitsPerColor = 2;//Must be less than 8 if your doing 24 bit color and storing for even/odd!!!! higher the number more distortion!!!!
			boolean hasAlpha = (containerImg.getAlphaRaster() != null);
			int pixelByteCount = 3;
			if(hasAlpha) {
				pixelByteCount = 4;
			}
			//int containerImgWidth = containerImg.getWidth();
			//int containerImgHeight = containerImg.getHeight();
			//System.out.println(pixelByteCount);
			for(int x = 0;x<(containerImg.getWidth());x++) {
				for(int y = 0;y<(containerImg.getHeight());y++) {
					System.out.print(" ");
					for(int c = 0;c<pixelByteCount;c++) {
						int colorInt = containerImg.getRGB(x,y);
						int[] pixelColor = new int[pixelByteCount];
						for(int i = 0;i<pixelByteCount;i++) {
							pixelColor[i]=(colorInt & (0xff << (i*8))) >> (i * 8);
						}
						System.out.print(pixelColor[0] + ",");
					}
				}
				System.out.println("");
			}
			
		} catch (IOException e) {
			System.out.println("test");
		}
	}
}
