import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.lang.Object;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;

public class CS160FinalDietzKarbon {
	public static void main(String args[]) {
		String path = "test.png";
		try {
			BufferedImage containerImg = ImageIO.read(new File(path));
			//Testing... Will all be re wrote!
			int bitsPerColor = 2;//Must be less than 8 if your doing 24 bit color and storing for even/odd!!!! higher the number more distortion!!!!
			System.out.println(containerImg.getColorModel().toString());
			Raster imgData = containerImg.getData();
			//System.out.println(imgData.getMinX() + " " + imgData.getMinY() + " " + imgData.getWidth() + " " + imgData.getHeight());
			
		} catch (IOException e) {
			System.out.println("error");
		}
	}
}
