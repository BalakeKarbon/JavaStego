import java.util.Scanner;
import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.charset.Charset;

public class CS160FinalDietzKarbonWriter {
	public static BufferedImage getContainerImage(Scanner scnr) {
		boolean fileAttained = false;
		String path;
		BufferedImage containerImg = new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
		while(!fileAttained) {
			System.out.print("Path:");
			path = scnr.nextLine();
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
	public static int getBitsPerByte(Scanner scnr) {
		int bitsPerByte = 0;
		boolean foundBits = false;
		while(!foundBits) {
			System.out.print("Bits:");
			bitsPerByte = scnr.nextInt();
			if(!(bitsPerByte <= 8 && bitsPerByte > 0)) {
				System.out.println("Only 8 bits per byte! Encoded bits must be value between 1 and 8 inclusive.");
			} else {
				foundBits = true;
			}
		}
		return bitsPerByte;
	}
	public static byte[] getSecretData(Scanner scnr,long bytesOfStorage) {
		// TO-DO: Add the ability to specify a end of message modifier for recognition by the reader.
		boolean dataFits = false;
		byte[] secretData = new byte[0];
		while(!dataFits) {
			System.out.print("Please enter data to encode:");
			secretData = scnr.nextLine().getBytes(Charset.forName("UTF-8"));
			if(secretData.length<=bytesOfStorage) {
				dataFits = true;
			} else {
				System.out.println("Data does not fit withing the available storage!");
			}
		}
		return secretData;
	}
	public static void writeEncodedImage(Scanner scnr, BufferedImage containerImg) {
		boolean wroteImage = false;
		String path;
		while(!wroteImage) {
			System.out.print("Path:");
			path = scnr.nextLine();
			try {
				File outputFile = new File(path);
				ImageIO.write(containerImg, "png", outputFile);
				System.out.println("Encoded PNG saved at \""+path+"\"!");
				wroteImage = true;
			} catch (IOException e) {
				System.out.println(e.getMessage());
				continue;
			}
		}
	}
	public static void stegEncode(Scanner scnr) {
		System.out.println("What PNG would you like to store your secred data in?");
		BufferedImage containerImg = getContainerImage(scnr);
		System.out.println("How many bits per byte would you like to encode?");
		int bitsPerByte = getBitsPerByte(scnr);
		int bytesPerPixel = (containerImg.getAlphaRaster() != null) ? 4 : 3;
		long bytesOfStorage = (containerImg.getWidth() * containerImg.getHeight() * bytesPerPixel * bitsPerByte)/8; // Possibly add printing how much storage is available.
		scnr.nextLine(); // Mention that this is due to weird scanner behavior.
		byte[] secretData = getSecretData(scnr, bytesOfStorage);
		//Encode data
		int currentPixelColor, newPixelColor;
		int pixelColorMask = Integer.valueOf((1 << (bytesPerPixel*8)) - 1).byteValue();
		boolean stillData = true;
		//System.out.println(""); // TEMPORARY
		for(int x = 0;x<containerImg.getWidth() && stillData;x++) {
			for(int y = 0;y<containerImg.getHeight() && stillData;y++) {
				int currentPixelOffset = (x*containerImg.getWidth())+y;
				int containerImgByteOffset = currentPixelOffset*bytesPerPixel;
				currentPixelColor = containerImg.getRGB(x,y);
				newPixelColor = currentPixelColor & pixelColorMask;
				for(int colorComponentIndex = 0;colorComponentIndex<bytesPerPixel && stillData;colorComponentIndex++) {
					int colorComponentIndexShift = colorComponentIndex * 8;
					byte colorComponent = Integer.valueOf(currentPixelColor >> (colorComponentIndex*8)).byteValue(); // This gets the color component by shifting the currentPixelColor integer 8*colorComponentIndex bits and then converting to a byte therebye cutting off the top 24 bits.
					int containerImgByteIndex = (containerImgByteOffset+colorComponentIndex);
					int secretDataByteIndex = (containerImgByteIndex*bitsPerByte)/8;
					if(secretDataByteIndex>=secretData.length) {
						stillData = false;
						break; // Possibly continue depending on what happens next.
					}
					
					//System.out.print("("+ containerImgByteIndex +","+ secretDataByteIndex +")");
					//System.out.printf("%02X",colorComponent);
					//System.out.print(Integer.toBinaryString(colorComponent) + ",");
					// Now time to encode data
					// (everything above x bits of color component OR'ed with the x bits?)
					byte secretDataMask = Integer.valueOf((1 << bitsPerByte) - 1).byteValue();
					byte colorComponentMask = Integer.valueOf(~secretDataMask).byteValue();
					byte secretDataSlice = Integer.valueOf((secretData[secretDataByteIndex] >> (colorComponentIndex*bitsPerByte)) & secretDataMask).byteValue();
					byte colorComponentSlice = Integer.valueOf(colorComponent & colorComponentMask).byteValue();
					byte newColorComponent = Integer.valueOf(colorComponentSlice | secretDataSlice).byteValue();
					//System.out.print(Integer.toBinaryString()+"");
					//System.out.print("("+Integer.toBinaryString(colorComponent)+"-");
					//System.out.print(Integer.toBinaryString(newColorComponent)+")");
					// Now we must convert these color components back into a 32 bit integer which will actually be a 24 bit integer....
					int newColorComponentMask = (0xff << (colorComponentIndexShift));
					int newPixelColorMask = ~newColorComponentMask;
					newPixelColor = (newPixelColor & newPixelColorMask) | ((newColorComponent << colorComponentIndexShift) & newColorComponentMask);
				}
				//System.out.printf("%02X->%02X",currentPixelColor,newPixelColor);
				//System.out.print(Integer.toBinaryString(currentPixelColor)+"-");
				//System.out.print(Integer.toBinaryString(newPixelColor));
				//System.out.print(","); // TEMPORARY
				containerImg.setRGB(x,y,newPixelColor);
			}
			//System.out.println(""); // TEMPORARY
		}
		// THEN write to output file.
		System.out.println("What file would you like to save this encoded PNG to?");
		writeEncodedImage(scnr, containerImg);
	}
	public static void stegDecode(Scanner scnr) {
		System.out.println("What PNG would you like to decode?");
		BufferedImage containerImg = getContainerImage(scnr);
		System.out.println("How many bits per byte were used during encodeing?");
		int bitsPerByte = getBitsPerByte(scnr);
		int bytesPerPixel = (containerImg.getAlphaRaster() != null) ? 4 : 3;
		int bytesOfStorage = (containerImg.getWidth() * containerImg.getHeight() * bytesPerPixel * bitsPerByte)/8; // Possibly add printing how much storage is available.
		byte[] secretData = new byte[bytesOfStorage];
		//Decode data
		int currentPixelColor;
		int secretDataShift = 0;
		for(int x = 0;x<containerImg.getWidth();x++) {
			for(int y = 0;y<containerImg.getHeight();y++) {
				int currentPixelOffset = (x*containerImg.getWidth())+y;
				int containerImgByteOffset = currentPixelOffset*bytesPerPixel;
				currentPixelColor = containerImg.getRGB(x,y);
				for(int colorComponentIndex = 0;colorComponentIndex<bytesPerPixel;colorComponentIndex++) {
					int colorComponentIndexShift = colorComponentIndex * 8;
					byte colorComponent = Integer.valueOf(currentPixelColor >> (colorComponentIndex*8)).byteValue(); // This gets the color component by shifting the currentPixelColor integer 8*colorComponentIndex bits and then converting to a byte therebye cutting off the top 24 bits.
					int containerImgByteIndex = (containerImgByteOffset+colorComponentIndex);
					int secretDataByteIndex = (containerImgByteIndex*bitsPerByte)/8;
					byte secretDataMask = Integer.valueOf((1<<bitsPerByte)-1).byteValue();
					int containerImgIndex = containerImgByteOffset+colorComponentIndex;
					//System.out.print("C:"+Integer.toBinaryString(secretData[secretDataByteIndex])+",S:"+Integer.toBinaryString((colorComponent & secretDataMask) << secretDataShift)+"\n");
					secretData[secretDataByteIndex] = Integer.valueOf(secretData[secretDataByteIndex] | ((colorComponent & secretDataMask)<<secretDataShift)).byteValue();
					secretDataShift+=bitsPerByte;
					if(secretDataShift>7) {
						secretDataShift = 0;
						System.out.print((char)(secretData[secretDataByteIndex] & 0xFF));
					}
					//System.out.print("("+ containerImgByteIndex +","+ secretDataByteIndex +")");
					//System.out.printf("%02X",colorComponent);
					//System.out.print(Integer.toBinaryString(colorComponent) + ",");
					// Now time to encode data
					// (everything above x bits of color component OR'ed with the x bits?)
					/*byte secretDataMask = Integer.valueOf((1 << bitsPerByte) - 1).byteValue();
					byte colorComponentMask = Integer.valueOf(~secretDataMask).byteValue();
					byte secretDataSlice = Integer.valueOf((secretData[secretDataByteIndex] >> (colorComponentIndex*bitsPerByte)) & secretDataMask).byteValue();
					byte colorComponentSlice = Integer.valueOf(colorComponent & colorComponentMask).byteValue();
					byte newColorComponent = Integer.valueOf(colorComponentSlice | secretDataSlice).byteValue();*/
					//System.out.print(Integer.toBinaryString()+"");
					//System.out.print("("+Integer.toBinaryString(colorComponent)+"-");
					//System.out.print(Integer.toBinaryString(newColorComponent)+")");
					// Now we must convert these color components back into a 32 bit integer which will actually be a 24 bit integer....
					/*int newColorComponentMask = (0xff << (colorComponentIndexShift));
					int newPixelColorMask = ~newColorComponentMask;*/
				}
				//System.out.printf("%02X->%02X",currentPixelColor,newPixelColor);
				//System.out.print(Integer.toBinaryString(currentPixelColor)+"-");
				//System.out.print(Integer.toBinaryString(newPixelColor));
				//System.out.print(","); // TEMPORARY
			}
			//System.out.println(""); // TEMPORARY
		}
		// THEN write to output file.
	}
	public static void main(String args[]) {
		Scanner scnr = new Scanner(System.in);
		System.out.println("Would you like to encode or decode a PNG?");
		char choice = '!';
		while(choice == '!') {
			System.out.print("Enter \"e\" for encode or \"d\" for decode:");
			choice = scnr.nextLine().toLowerCase().charAt(0);
			if(choice == 'e') {
				stegEncode(scnr);
			} else if(choice == 'd') {
				stegDecode(scnr);
			} else {
				choice = '!';
			}
		}
	}
}
