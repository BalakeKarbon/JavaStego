import java.util.Scanner;
import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class CS160FinalDietzKarbonWriter {
	public static BufferedImage getContainerImage(Scanner scnr) {
		boolean fileAttained = false;
		String path;
		BufferedImage containerImg = new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
		while(!fileAttained) {
			System.out.print("Path:");
			path = scnr.next(); //Next Line?
			try {
				containerImg = ImageIO.read(new File(path));
			} catch (IOException e) {
				System.out.println(e.getMessage());
				continue;
			}
			fileAttained = true;
		}
		return containerImg;
	}
	public static void main(String args[]) {
		Scanner scnr = new Scanner(System.in);
		System.out.println("What PNG would you like to store your secred data in?");
		BufferedImage containerImg = getContainerImage(scnr);

	}
}
