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

public class KarbonDietzFinalProj {
        //Creates a byte array named terminator that acts as a marker to signal the end of the secret message.
    public static byte[] terminator = "END-OF-SECRET-DATA".getBytes(StandardCharsets.UTF_8);
    //This method getContainerImage prompts the user for the name of the PNG that they want to hide secret data in.
    //It then reads this file and stores it in the variable containerImg and returns that.
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
    /*Method getBitsPerByte prompts the user for a number between 8 and 1 inclusive that is a power of 2.
     * This number is the amount of bits per byte that the user would like to encode the file with or the bits per byte that were used in the file when decoding.
     * Returned as the variable bitPerByte.
     */  
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
                //Case statements ensure that values are powers of 2.
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
    /*Byte array method getSecretFile is called if the user selects that they would like to write a file rather than text onto the image.
     * Prompts the user for the path to the file and then reads the bytes for that file onto byte array secretData and returns secretData.
     */
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
        boolean dataFits = false;
        byte[] secretData = new byte[0];
        byte[] inputData = new byte[0];
        //While loops prompts the user if they would like to hide a file or text in the PNG and doesn't break the loop until they give a valid answer f or t. 
        while(!dataFits) {
            System.out.println("Would you like to encode a file or text into the PNG?");
            char choice = '!';
            while(choice == '!') {
                System.out.print("Enter \"f\" for file or \"t\" for text:");
                choice = scnr.nextLine().toLowerCase().charAt(0);
                //If the user selects to encode text then it prompts them for a string and converts that to a byte array called inputData
                if(choice == 't') {
                    System.out.print("Please enter data to encode:");
                    inputData = scnr.nextLine().getBytes(Charset.forName("UTF-8"));
                //Calls the method getSecretFile if the user selects to encode a file and stores it in variable inputData.
                } else if(choice == 'f') {
                    inputData = getSecretFile(scnr);
                } else {
                    choice = '!';
                }
            }
            if(inputData.length<=(bytesOfStorage-terminator.length)) {
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
    //Writes the encoded image onto a new file with a user specified path.
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
    //Goes to method stegEncode if the user selects that they would like to encode an image rather than decode an image.
    public static void stegEncode(Scanner scnr) {
            //Prompts the user for the path of the PNG they would like to store the secret data in and stores that in variable containerImg.
        System.out.println("What PNG would you like to store your secret data in?");
        BufferedImage containerImg = getContainerImage(scnr);
        System.out.println("How many bits per byte would you like to encode? (Must be a power of 2)");
        //Uses method getBitsPerByte to prompt user for the bits per byte and stores the result in integer variable bitsPerByte
        int bitsPerByte = getBitsPerByte(scnr);
        /*Declares integer variable bytesPerPixel and sets it based on if the image has an alpha channel. 
         * If the image does have an alpha channel it determines that each pixel has four color components and makes bytesPerPixel = 4.
         * If there is no alpha channel then it makes bytePerPixel = 3 as it has 3 colors components.
         */
        int bytesPerPixel = (containerImg.getAlphaRaster() != null) ? 4 : 3;
        //Calculates the total number of bytes avaliable for storing secret data within the image.
        long bytesOfStorage = (containerImg.getWidth() * containerImg.getHeight() * bytesPerPixel * bitsPerByte)/8;
        //Empty scanner statement to clear up bugs due to unexpected results without it.
        scnr.nextLine(); // Mention that this is due to weird scanner behavior.
        //Calls method getSecretData to get the data that the user would like to encode.
        byte[] secretData = getSecretData(scnr, bytesOfStorage);
        int currentPixelColor, newPixelColor;
        //Isolates the color components of a pixel by calculating a bitmask
        int pixelColorMask = Integer.valueOf((1 << (bytesPerPixel*8)) - 1).byteValue();
        boolean stillData = true;
        int secretSliceIndex = 0;
        //Nested for loops that are responsible for iterating through each pixel in the image and implanting the secret data based on the pixels least significant bit.         
        for(int x = 0;x<containerImg.getWidth() && stillData;x++) {
                //Iterates through each pixel, going row by row.
            for(int y = 0;y<containerImg.getHeight() && stillData;y++) {
                int currentPixelOffset = (x*containerImg.getHeight())+y;
                int containerImgByteOffset = currentPixelOffset*bytesPerPixel;
                //currentPixelColor stores the pixel's color value.
                currentPixelColor = containerImg.getRGB(x,y);
                newPixelColor = currentPixelColor & pixelColorMask;
                                //Iterates through each color component within that pixel.
                for(int colorComponentIndex = 0;colorComponentIndex<bytesPerPixel && stillData;colorComponentIndex++) {
                    int colorComponentIndexShift = colorComponentIndex * 8;
                    byte colorComponent = Integer.valueOf(currentPixelColor >> (colorComponentIndex*8)).byteValue(); // This gets the color component by shifting the currentPixelColor integer 8*colorComponentIndex bits and then converting to a byte therebye cutting off the top 24 bits.
                    int containerImgByteIndex = (containerImgByteOffset+colorComponentIndex);
                    int secretDataByteIndex = (containerImgByteIndex*bitsPerByte)/8;
                    //This breaks the loop early if all secret data has been embedded.
                    if(secretDataByteIndex>=secretData.length) {
                        stillData = false;
                        break;
                    }
                    //These lines isolate the bits to be modified and the secret data.
                    byte secretDataMask = Integer.valueOf((1 << bitsPerByte) - 1).byteValue();
                    byte colorComponentMask = Integer.valueOf(~secretDataMask).byteValue();
                    byte secretDataSlice = Integer.valueOf((secretData[secretDataByteIndex] >> (secretSliceIndex*bitsPerByte)) & secretDataMask).byteValue();
                    byte colorComponentSlice = Integer.valueOf(colorComponent & colorComponentMask).byteValue();
                    byte newColorComponent = Integer.valueOf(colorComponentSlice | secretDataSlice).byteValue();
                    // Now we must convert these color components back into a 32 bit integer which will actually be a 24 bit integer....
                    int newColorComponentMask = (0xff << (colorComponentIndexShift));
                    int newPixelColorMask = ~newColorComponentMask;
                    //The selected bits from the secret data are combined with the original color component in this line.
                    newPixelColor = (newPixelColor & newPixelColorMask) | ((newColorComponent << colorComponentIndexShift) & newColorComponentMask);
                    secretSliceIndex++;
                    //Checks if secretSliceIndex has met or exceededthe number of bits per byte.
                    if(secretSliceIndex >= (8/bitsPerByte)) {
                        secretSliceIndex=0;
                    }
                }
                //Modified pixel color is set back into the image.
                containerImg.setRGB(x,y,newPixelColor);
            }
        }
        //Prompts the user for the path of the file that they would like to save the newly encoded PNG onto.
        System.out.println("What file would you like to save this encoded PNG to?");
        writeEncodedImage(scnr, containerImg);
    }
    //Method getOutputFile manages this prompt and returns the new OutputFileStream
    public static FileOutputStream getOutputFile(Scanner scnr) throws IOException {
        String path;
        System.out.print("Path:");
        path = scnr.nextLine();
        FileOutputStream fos = new FileOutputStream(path);
        return fos;
    }
    //This is the decoding method if the user chooses to decode a message that already containss an image with a message written onto it with steganography.
    public static void stegDecode(Scanner scnr) {
            //Prompts the user for the name of the PNG that they would like to decode.
        System.out.println("What PNG would you like to decode?");
        BufferedImage containerImg = getContainerImage(scnr);
        //Asks for the number of bits per byte that were used while encoding and uses method getBitsPerByte to verify this.
        System.out.println("How many bits per byte were used during encoding?");
        int bitsPerByte = getBitsPerByte(scnr);
        //Checks for an alpha channel using the same linein the earlier method
        int bytesPerPixel = (containerImg.getAlphaRaster() != null) ? 4 : 3;
        int bytesOfStorage = (containerImg.getWidth() * containerImg.getHeight() * bytesPerPixel * bitsPerByte)/8; // Possibly add printing how much storage is available.
        scnr.nextLine();
        byte[] secretData = new byte[bytesOfStorage];
        int currentPixelColor;
        int secretDataShift = 0;
        byte lastByte = 0x00;
        boolean decoded = false;
        int size = 0;
        //Nested for loop that iterates through each pixel and extracts the secret data.
        for(int x = 0;x<containerImg.getWidth() && !decoded;x++) {
                //Iterates through each pixel going row-by-row just like in the encoding method.  The decoded flag is used to break out of the loop early if all information has been extracted.
            for(int y = 0;y<containerImg.getHeight() && !decoded;y++) {
                int currentPixelOffset = (x*containerImg.getHeight())+y;
                int containerImgByteOffset = currentPixelOffset*bytesPerPixel;
                //Calculates the offset of the current pixel within the image.
                currentPixelColor = containerImg.getRGB(x,y);
                //Iterates over each color component within the pixel
                for(int colorComponentIndex = 0;colorComponentIndex<bytesPerPixel && !decoded;colorComponentIndex++) {
                    int colorComponentIndexShift = colorComponentIndex * 8;
                    byte colorComponent = Integer.valueOf(currentPixelColor >> (colorComponentIndex*8)).byteValue(); // This gets the color component by shifting the currentPixelColor integer 8*colorComponentIndex bits and then converting to a byte therebye cutting off the top 24 bits.
                    int containerImgByteIndex = (containerImgByteOffset+colorComponentIndex);
                    int secretDataByteIndex = (containerImgByteIndex*bitsPerByte)/8;
                    byte secretDataMask = Integer.valueOf((1<<bitsPerByte)-1).byteValue();
                    secretData[secretDataByteIndex] = Integer.valueOf(secretData[secretDataByteIndex] | ((colorComponent & secretDataMask)<<secretDataShift)).byteValue();
                    secretDataShift+=bitsPerByte;
                    //Terminator is used to find when all the data has been found and set decoded to true if it does so.
                    if(secretDataShift>7) {
                        secretDataShift = 0;
                        if(size>terminator.length) {
                            byte[] newArray = Arrays.copyOfRange(secretData,(size-terminator.length),size);
                            if(Arrays.equals(newArray,terminator)) {
                                decoded = true;
                                size = size-terminator.length;
                                break;
                            }
                        }
                        size++;
                        lastByte = secretData[secretDataByteIndex];
                    }
                }
            }
        }
        //Prompts the user if the data they want to decode is a file or text and uses a while loop to continuously prompt for a valid response.
        System.out.println("Would you like to decode to a file or text?");
        char choice = '!';
        while(choice == '!') {
            System.out.print("Enter \"f\" for file or \"t\" for text:");
            choice = scnr.nextLine().toLowerCase().charAt(0);
            if(choice == 't') {
                    //Prints the decoded information
                System.out.println("Decoded data:");
                for(int i = 0;i<size;i++) {
                    System.out.print(new String(new byte[]{secretData[i]}, StandardCharsets.US_ASCII));
                }
            } else if(choice == 'f') {
                        //Prints the decoded file to a new file.
                try {
                    FileOutputStream outputFileStream = getOutputFile(scnr);
                    for(int i = 0;i<size;i++) {
                        outputFileStream.write(secretData[i]);
                    }
                    outputFileStream.close();
                    System.out.println("Wrote decoded data to file!");
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            } else {
                choice = '!';
            }
        }
    }
    //Main method prompts the user if they would like to encode or decode PNG and forwards to relevent methods based on response.
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
