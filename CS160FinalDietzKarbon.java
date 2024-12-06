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
import java.nio.charset.Charset;

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
			int bitsPerColor = 1;//Must be less than 8 if your doing 24 bit color and storing for even/odd!!!! higher the number more distortion!!!!
			boolean hasAlpha = (containerImg.getAlphaRaster() != null);
			int pixelByteCount = 3;
			if(hasAlpha) {
				pixelByteCount = 4;
			}
			int bytesOfStorage = (containerImg.getWidth() * containerImg.getHeight() * pixelByteCount * bitsPerColor)/8; //Will have issues as not long? Test with large image!?
			byte[] secretData = "Test".getBytes(Charset.forName("UTF-8"));
			if(bytesOfStorage >= secretData.length) {
			//int containerImgWidth = containerImg.getWidth();
			//int containerImgHeight = containerImg.getHeight();
			//System.out.println(pixelByteCount);
				int secretIndex = 0;
				for(int x = 0;x<(containerImg.getWidth());x++) {
					for(int y = 0;y<(containerImg.getHeight());y++) {
						System.out.print(" ");
						int colorInt = containerImg.getRGB(x,y);
						int[] pixelColor = new int[pixelByteCount];
						for(int c = 0;c<pixelByteCount;c++) {
							secretIndex=(((x*y)+y)+c)/bitsPerColor; //THIS IS BAD
							pixelColor[c]=(colorInt & (0xff << (c*8))) >> (c * 8);
							System.out.printf("(%d %02X)",secretIndex,pixelColor[c]);
							//Modify here and re write to new BufferedImage in order to save for later!
						}
					}
					System.out.println("");
				}
			} else {
				System.out.println("Here we have to go back through loop and let homie know it wont fit.");
			}
			
		} catch (IOException e) {
			System.out.println("test");
		}
	}
}
