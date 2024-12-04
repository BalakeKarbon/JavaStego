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
		String path = "test.png";
		try {
			BufferedImage containerImg = ImageIO.read(new File(path));
			//Testing... Will all be re wrote!
			int bitsPerColor = 2;//Must be less than 8 if your doing 24 bit color and storing for even/odd!!!! higher the number more distortion!!!!
			System.out.println(containerImg.getColorModel().toString());
			boolean hasAlpha = (containerImg.getAlphaRaster() != null);
			int pixelByteCount = 3;
			if(hasAlpha) {
				pixelByteCount = 4;
			}
			int containerImgWidth = containerImg.getWidth();
			int containerImgHeight = containerImg.getHeight();
			/*for(int x = 0;x<(containerImgWidth);x++) {
				for(int y = 0;y<(containerImgHeight);y++) {
					System.out.print(" ");
					for(int c = 0;c<pixelByteCount;c++) {
						System.out.print(Integer.toHexString(pixels[x*y])+",");
					}
				}
				System.out.println("");
			}*/
			
		} catch (IOException e) {
			System.out.println("test");
		}
	}
}
