import java.util.Scanner;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.FileOutputStream;
import java.io.IOException;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.InputMismatchException;
import java.util.Arrays;

public class CS160FinalDietzKarbon {
	public static byte[] terminator = "END-OF-SECRET-DATA".getBytes(StandardCharsets.UTF_8);
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
				switch(bitsPerByte) {
					case 1:
						break;
					case 2:
						break;
					case 4:
						break;
					case 8:
						break;
					default:
						System.out.println("Value must be a power of 2!");
						foundBits = false;
				}
			}
		}
		return bitsPerByte;
	}
	public static byte[] getSecretFile(Scanner scnr) {
		boolean fileAttained = false;
		byte[] secretData = new byte[0];
		String path;
		while(!fileAttained) {
			System.out.print("Path:");
			path = scnr.nextLine();
			try {
				secretData = Files.readAllBytes(Paths.get(path));
			} catch (IOException e) {
				System.out.println(e.getMessage());
				continue;
			}
			fileAttained = true;
		}
		return secretData;
	}
	public static byte[] getSecretData(Scanner scnr,long bytesOfStorage) {
		// TO-DO: Add the ability to specify a end of message modifier for recognition by the reader.
		//bytesOfStorage = (int)((double)bytesOfStorage * .8); // Error where 6/7 of data is encoded in image? Not sure why so for now this is the work around.
		boolean dataFits = false;
		byte[] secretData = new byte[0];
		byte[] inputData = new byte[0];
		while(!dataFits) {
			System.out.println("Would you like to encode a file or text into the PNG?");
			char choice = '!';
			while(choice == '!') {
				System.out.print("Enter \"f\" for file or \"t\" for text:");
				choice = scnr.nextLine().toLowerCase().charAt(0);
				if(choice == 't') {
					System.out.print("Please enter data to encode:");
					inputData = scnr.nextLine().getBytes(Charset.forName("UTF-8"));
				} else if(choice == 'f') {
					inputData = getSecretFile(scnr);
				} else {
					choice = '!';
				}
			}
			if(inputData.length<=(bytesOfStorage-terminator.length)) { // 2 bytes needed for delimeter!
				dataFits = true;
				secretData = new byte[inputData.length+terminator.length];
				for(int i = 0;i<inputData.length;i++) {
					secretData[i]=inputData[i];
				}
				for(int i = 0;i<terminator.length;i++) {
					secretData[(secretData.length-1)-i] = terminator[(terminator.length-1)-i];
				}
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
	public static void printBytes(byte[] bytes) {
		for(int i = 0;i<bytes.length;i++) {
			System.out.print(Integer.toBinaryString(bytes[i]));
			if(i!=bytes.length-1) {
				System.out.print(",");
			}
		}
		System.out.println();
	}
	public static void stegEncode(Scanner scnr) {
		System.out.println("What PNG would you like to store your secred data in?");
		BufferedImage containerImg = getContainerImage(scnr);
		System.out.println("How many bits per byte would you like to encode (Must be a power of 2)?");
		int bitsPerByte = getBitsPerByte(scnr);
		int bytesPerPixel = (containerImg.getAlphaRaster() != null) ? 4 : 3;
		long bytesOfStorage = (containerImg.getWidth() * containerImg.getHeight() * bytesPerPixel * bitsPerByte)/8; // Possibly add printing how much storage is available.
		scnr.nextLine(); // Mention that this is due to weird scanner behavior.
		byte[] secretData = getSecretData(scnr, bytesOfStorage);
		//System.out.println("DEBUG: "+new String(secretData, StandardCharsets.US_ASCII));
		int currentPixelColor, newPixelColor;
		int pixelColorMask = Integer.valueOf((1 << (bytesPerPixel*8)) - 1).byteValue();
		boolean stillData = true;
		//System.out.println();
		//printBytes(secretData);
		int secretSliceIndex = 0;
		for(int x = 0;x<containerImg.getWidth() && stillData;x++) {
			for(int y = 0;y<containerImg.getHeight() && stillData;y++) {
				int currentPixelOffset = (x*containerImg.getHeight())+y;
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
					byte secretDataMask = Integer.valueOf((1 << bitsPerByte) - 1).byteValue();
					byte colorComponentMask = Integer.valueOf(~secretDataMask).byteValue();
					byte secretDataSlice = Integer.valueOf((secretData[secretDataByteIndex] >> (secretSliceIndex*bitsPerByte)) & secretDataMask).byteValue();// wrong?
					byte colorComponentSlice = Integer.valueOf(colorComponent & colorComponentMask).byteValue();
					byte newColorComponent = Integer.valueOf(colorComponentSlice | secretDataSlice).byteValue();
					// Now we must convert these color components back into a 32 bit integer which will actually be a 24 bit integer....
					int newColorComponentMask = (0xff << (colorComponentIndexShift));
					int newPixelColorMask = ~newColorComponentMask;
					newPixelColor = (newPixelColor & newPixelColorMask) | ((newColorComponent << colorComponentIndexShift) & newColorComponentMask);
					secretSliceIndex++;
					if(secretSliceIndex >= (8/bitsPerByte)) { //Can optimize this by having calc earlier.
						secretSliceIndex=0;
					}
					/*System.out.print(containerImgByteIndex + "-" + secretDataByteIndex + " ");
					System.out.print("Byte Were Encoding: " + Integer.toBinaryString(secretData[secretDataByteIndex] & 0xff)+"");
					System.out.print("n" + Integer.toBinaryString(newColorComponent & 0xff)+"");
					System.out.print("p" + Integer.toBinaryString(secretDataSlice & 0xff)+" ");
					System.out.print(new String(new byte[]{Integer.valueOf(secretData[secretDataByteIndex] & 0xff).byteValue()}, StandardCharsets.US_ASCII)+" ");
					System.out.print("Shift: " + Integer.toBinaryString(newColorComponentMask & 0xffffff) + " " + colorComponentIndexShift);
					System.out.println();*/
					//System.out.println(Integer.toBinaryString(newColorComponent & 0xff));
				}
				containerImg.setRGB(x,y,newPixelColor);
				//System.out.println(Integer.toBinaryString(newPixelColor&0xffffff));
			}
		}
		System.out.println("What file would you like to save this encoded PNG to?");
		writeEncodedImage(scnr, containerImg);
	}
	public static FileOutputStream getOutputFile(Scanner scnr) throws IOException {
		String path;
		System.out.print("Path:");
		path = scnr.nextLine();
		FileOutputStream fos = new FileOutputStream(path);
		return fos;
	}
	public static void stegDecode(Scanner scnr) {
		System.out.println("What PNG would you like to decode?");
		BufferedImage containerImg = getContainerImage(scnr);
		System.out.println("How many bits per byte were used during encodeing?");
		int bitsPerByte = getBitsPerByte(scnr);
		int bytesPerPixel = (containerImg.getAlphaRaster() != null) ? 4 : 3;
		int bytesOfStorage = (containerImg.getWidth() * containerImg.getHeight() * bytesPerPixel * bitsPerByte)/8; // Possibly add printing how much storage is available.
		scnr.nextLine();
		//System.out.println("Bytes of storage!:" + bytesOfStorage);
		byte[] secretData = new byte[bytesOfStorage];
		//Decode data
		int currentPixelColor;
		int secretDataShift = 0;
		byte lastByte = 0x00;
		boolean decoded = false;
		int size = 0;
		for(int x = 0;x<containerImg.getWidth() && !decoded;x++) {
			for(int y = 0;y<containerImg.getHeight() && !decoded;y++) {
				int currentPixelOffset = (x*containerImg.getHeight())+y;
				int containerImgByteOffset = currentPixelOffset*bytesPerPixel;
				currentPixelColor = containerImg.getRGB(x,y);
				for(int colorComponentIndex = 0;colorComponentIndex<bytesPerPixel && !decoded;colorComponentIndex++) {
					int colorComponentIndexShift = colorComponentIndex * 8;
					byte colorComponent = Integer.valueOf(currentPixelColor >> (colorComponentIndex*8)).byteValue(); // This gets the color component by shifting the currentPixelColor integer 8*colorComponentIndex bits and then converting to a byte therebye cutting off the top 24 bits.
					int containerImgByteIndex = (containerImgByteOffset+colorComponentIndex);
					int secretDataByteIndex = (containerImgByteIndex*bitsPerByte)/8; // This should be the biggest number of the set and it is still not very close to overflow.
					byte secretDataMask = Integer.valueOf((1<<bitsPerByte)-1).byteValue();
					secretData[secretDataByteIndex] = Integer.valueOf(secretData[secretDataByteIndex] | ((colorComponent & secretDataMask)<<secretDataShift)).byteValue();
					secretDataShift+=bitsPerByte;
					if(secretDataShift>7) {
						secretDataShift = 0;
						// TO-DO: Fix terminator!
						if(size>terminator.length) {
							byte[] newArray = Arrays.copyOfRange(secretData,(size-terminator.length),size);
							//System.out.println(new String(newArray, StandardCharsets.US_ASCII)); // Better to use the one above because does not write entire array stops when desired.
							//System.out.println(newArray.length + " vs " + terminator.length);
							if(Arrays.equals(newArray,terminator)) {
								decoded = true;
								size = size-terminator.length;
								break;
							}
						}
						size++;
						lastByte = secretData[secretDataByteIndex];
						// TO-DO: When writing to file or screen delay output by number of bytes required for terminator because then you wont end up writing the terminator itself to the file or screen.
						//System.out.print(new String(new byte[]{secretData[secretDataByteIndex-2]}, StandardCharsets.US_ASCII));
					}
					//System.out.print(secretDataByteIndex + " " + containerImgByteIndex + " "); //DEBUG
					//System.out.print(x + " " + y + " " + colorComponentIndex + " ");
					//System.out.print(Integer.toBinaryString(secretData[secretDataByteIndex]) + "\n");
				}
			}
		}
		System.out.println("Would you like to decode to a file or text?");
		char choice = '!';
		while(choice == '!') {
			System.out.print("Enter \"f\" for file or \"t\" for text:");
			choice = scnr.nextLine().toLowerCase().charAt(0);
			if(choice == 't') {
				for(int i = 0;i<size;i++) {
					System.out.print(new String(new byte[]{secretData[i]}, StandardCharsets.US_ASCII));
				}
			} else if(choice == 'f') {
				try {
					FileOutputStream outputFileStream = getOutputFile(scnr);
					for(int i = 0;i<size;i++) {
						outputFileStream.write(secretData[i]);
					}
					outputFileStream.close();
				} catch (IOException e) {
					System.out.println(e.getMessage());
				}
			} else {
				choice = '!';
			}
		}
		//System.out.println("DEBUG:" + size);
		//System.out.println(new String(secretData, StandardCharsets.US_ASCII)); // Better to use the one above because does not write entire array stops when desired.
		//System.out.println(decoded);
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
