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

public class Stego {
    //Creates a byte array named terminator that acts as a marker to signal the end of the secret message.
    public static byte[] terminator = "END-OF-SECRET-DATA".getBytes(StandardCharsets.UTF_8);
    /*This method getContainerImage prompts the user for the name of the PNG that they want to hide secret data in.
     * It then reads this file and stores it in the variable containerImg and returns that.
     * It gets a path string from the user and then attempts to get a BufferedImage out of the specified path.
     * It continue until successful then returns said BufferedImage.
     */ 
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
     * Once successful, returns int bitsPerByte.
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
     * Prompts the user for the path to the file and then reads the bytes for that file into byte array secretData and returns secretData.
     */
    public static byte[] getSecretFile(Scanner scnr) {
        boolean fileAttained = false;
        byte[] secretData = new byte[0];
        String path;
        while(!fileAttained) {//Again, this will continue until successful.
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
    /*getSecretData is called in order to get byte array for storage inside of image.
     *It prompts the user for either a file or a piece of text, then appends the terminator marker.
     */
    public static byte[] getSecretData(Scanner scnr) {
        byte[] inputData = new byte[0];
        //While loop prompts the user if they would like to hide a file or text in the PNG and doesn't break the loop until they give a valid answer f or t.
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
        byte[] secretData = new byte[inputData.length+terminator.length];
        for(int i = 0;i<inputData.length;i++) {
            secretData[i]=inputData[i];
        }
        for(int i = 0;i<terminator.length;i++) {
            secretData[(secretData.length-1)-i] = terminator[(terminator.length-1)-i];
        }
        return secretData;
    }
    /*getStorageCapacity returns the total number of bytes that can be hidden in containerImg at the given
     * bitsPerByte, including the space that will be consumed by the terminator marker.
     */
    public static long getStorageCapacity(BufferedImage containerImg, int bitsPerByte) {
        int bytesPerPixel = (containerImg.getAlphaRaster() != null) ? 4 : 3;
        return ((long)containerImg.getWidth() * containerImg.getHeight() * bytesPerPixel * bitsPerByte)/8;
    }
    /*getLowestBitsPerByte determines the smallest power-of-2 bitsPerByte (1, 2, 4, or 8) that is large
     * enough to fit secretData within containerImg. Returns -1 if the data doesn't fit even at 8 bits per byte.
     */
    public static int getLowestBitsPerByte(BufferedImage containerImg, byte[] secretData) {
        for(int bitsPerByte = 1;bitsPerByte<=8;bitsPerByte*=2) {
            if(secretData.length<=getStorageCapacity(containerImg, bitsPerByte)) {
                return bitsPerByte;
            }
        }
        return -1;
    }
    //showAvailableSpace prompts for an image and prints how many bytes it can hold at each valid bitsPerByte.
    public static void showAvailableSpace(Scanner scnr) {
        System.out.println("What image (PNG, JPG, or other common format) would you like to check available space for?");
        BufferedImage containerImg = getContainerImage(scnr);
        System.out.println("Available storage by bits per byte (usable bytes account for the "+terminator.length+"-byte terminator marker every encode reserves):");
        for(int bitsPerByte = 1;bitsPerByte<=8;bitsPerByte*=2) {
            long bytesOfStorage = getStorageCapacity(containerImg, bitsPerByte);
            long usableBytes = Math.max(0, bytesOfStorage-terminator.length);
            System.out.println(bitsPerByte+" bits per byte: "+bytesOfStorage+" bytes total, "+usableBytes+" bytes usable");
        }
    }
    //Standard Euclidean gcd, used to find a step that visits every slot exactly once.
    private static long gcd(long a, long b) {
        while(b != 0) {
            long t = b;
            b = a % b;
            a = t;
        }
        return a;
    }
    /*computeSpreadStep picks a step size coprime with totalSlots so that the sequence (i*step) % totalSlots,
     * for i = 0..totalSlots-1, visits every slot exactly once (a permutation) while landing far apart on
     * consecutive visits. Starting near the golden ratio conjugate of totalSlots spreads the visited slots
     * evenly across the whole image instead of clustering them in one area. Since it only depends on
     * totalSlots, decode can recompute the exact same step without any extra data being stored.
     */
    public static long computeSpreadStep(long totalSlots) {
        if(totalSlots<=1) {
            return 1;
        }
        long step = Math.round(totalSlots*0.6180339887498949);
        if(step<1) {
            step = 1;
        }
        while(gcd(step,totalSlots)!=1) {
            step++;
            if(step>=totalSlots) {
                step = 1;
            }
        }
        return step;
    }
    /*slotToPixelCoords converts a container byte index (0..width*height*bytesPerPixel-1) back into the
     * pixel coordinates and color component it refers to, matching the (x*height)+y pixel offset convention
     * used throughout this class.
     */
    public static int[] slotToPixelCoords(long containerByteIndex, int imgHeight, int bytesPerPixel) {
        int colorComponentIndex = (int)(containerByteIndex%bytesPerPixel);
        long pixelIndex = containerByteIndex/bytesPerPixel;
        int x = (int)(pixelIndex/imgHeight);
        int y = (int)(pixelIndex%imgHeight);
        return new int[]{x,y,colorComponentIndex};
    }
    //writeEncodedImage method is for writing encoded image onto a new file with a user specified path.
    public static void writeEncodedImage(Scanner scnr, BufferedImage containerImg) {
        boolean wroteImage = false;
        String path;
        while(!wroteImage) {
            System.out.print("Path:");
            path = scnr.nextLine();
            //The encoded data only survives a lossless format, so the output is always a PNG regardless of what
            //the cover image was. If the given path doesn't already end in ".png", append it rather than write
            //PNG bytes into a file named e.g. ".jpg", which would be misleading and could invite recompression.
            if(!path.toLowerCase().endsWith(".png")) {
                path = path+".png";
            }
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
        //Prompts the user for the path of the image they would like to store the secret data in and stores that in variable containerImg.
        System.out.println("What image (PNG, JPG, or other common format) would you like to store your secret data in?");
        BufferedImage containerImg = getContainerImage(scnr);
        /*Declares integer variable bytesPerPixel and sets it based on if the image has an alpha channel.
         * If the image does have an alpha channel it determines that each pixel has four color components and makes bytesPerPixel = 4.
         * If there is no alpha channel then it makes bytePerPixel = 3 as it has 3 colors components.
         */
        int bytesPerPixel = (containerImg.getAlphaRaster() != null) ? 4 : 3;
        //Calls method getSecretData to get the data that the user would like to encode, then automatically picks the
        //smallest bitsPerByte (1, 2, 4, or 8) that fits it into the image, re-prompting for data if it never fits.
        byte[] secretData;
        int bitsPerByte;
        while(true) {
            secretData = getSecretData(scnr);
            bitsPerByte = getLowestBitsPerByte(containerImg, secretData);
            if(bitsPerByte != -1) {
                break;
            }
            System.out.println("Data does not fit within the available storage, even at 8 bits per byte! Please choose a smaller file or shorter text.");
        }
        System.out.println("Using "+bitsPerByte+" bits per byte to encode this data. Remember this value - you will need it to decode the image!");
        int imgHeight = containerImg.getHeight();
        //totalSlots is every color component byte in the image; step is a golden-ratio-based stride that's
        //coprime with totalSlots, so visiting slots at (i*step)%totalSlots hits every slot exactly once while
        //spreading consecutive writes evenly across the whole image instead of clustering them at the start.
        long totalSlots = (long)containerImg.getWidth()*containerImg.getHeight()*bytesPerPixel;
        long step = computeSpreadStep(totalSlots);
        int secretSliceIndex = 0;
        //Loop counter i is the sequential position of the secret data bitstream; containerByteIndex is where that
        //slice of data actually gets written, so the mapping between the two is what spreads the data out.
        for(long i = 0;i<totalSlots;i++) {
            int secretDataByteIndex = (int)((i*bitsPerByte)/8);
            //This breaks the loop early once all secret data has been embedded.
            if(secretDataByteIndex>=secretData.length) {
                break;
            }
            long containerByteIndex = (i*step)%totalSlots;
            int[] coords = slotToPixelCoords(containerByteIndex, imgHeight, bytesPerPixel);
            int x = coords[0], y = coords[1], colorComponentIndex = coords[2];
            int colorComponentIndexShift = colorComponentIndex * 8;
            int currentPixelColor = containerImg.getRGB(x,y); //Returned is a 32bit int even if only 24 bits (4 bytes) are used.
            byte colorComponent = Integer.valueOf(currentPixelColor >> colorComponentIndexShift).byteValue(); // This gets the color component by shifting the currentPixelColor integer 8*colorComponentIndex bits and then converting to a byte therebye cutting off the top 24 bits.
            //These lines isolate the bits to be modified and the secret data.
            byte secretDataMask = Integer.valueOf((1 << bitsPerByte) - 1).byteValue();
            byte colorComponentMask = Integer.valueOf(~secretDataMask).byteValue();
            byte secretDataSlice = Integer.valueOf((secretData[secretDataByteIndex] >> (secretSliceIndex*bitsPerByte)) & secretDataMask).byteValue();
            byte colorComponentSlice = Integer.valueOf(colorComponent & colorComponentMask).byteValue();
            byte newColorComponent = Integer.valueOf(colorComponentSlice | secretDataSlice).byteValue();
            // Now we must convert this color component back into the pixel's 32 bit integer, leaving every other component untouched.
            int newColorComponentMask = (0xff << (colorComponentIndexShift));
            int newPixelColorMask = ~newColorComponentMask;
            int newPixelColor = (currentPixelColor & newPixelColorMask) | ((newColorComponent << colorComponentIndexShift) & newColorComponentMask);
            containerImg.setRGB(x,y,newPixelColor); //Modified pixel color is set back into the BufferedImage which we will write later.
            secretSliceIndex++;
            //Checks if secretSliceIndex has met or exceededthe number of bits per byte.
            if(secretSliceIndex >= (8/bitsPerByte)) {
                secretSliceIndex=0;
            }
        }
        //Prompts the user for the path of the file that they would like to save the newly encoded PNG onto.
        System.out.println("What file would you like to save this encoded PNG to? (Always saved as a lossless PNG, even if the cover image was a JPG.)");
        writeEncodedImage(scnr, containerImg); //Calls writeEncodedImage which will handle the rest.
    }
    //Method getOutputFile manages this prompt and returns the new OutputFileStream
    public static FileOutputStream getOutputFile(Scanner scnr) throws IOException { //Passes any exceptions onto the caller due to FileOutputStream constructor constraints.
        String path;
        System.out.print("Path:");
        path = scnr.nextLine();
        FileOutputStream fos = new FileOutputStream(path);
        return fos;
    }
    //This is the decoding method if the user chooses to decode a message that already containss an image with a message written onto it with steganography.
    //Much of the following method is like stegEncode, but due to some different inter loop implementation we have created a seperate method.
    public static void stegDecode(Scanner scnr) {
        //Prompts the user for the name of the encoded image that they would like to decode. This must be the
        //lossless PNG that stegEncode produced - a re-compressed JPG would have destroyed the hidden bits.
        System.out.println("What encoded PNG would you like to decode?");
        BufferedImage containerImg = getContainerImage(scnr); //Gets the image we are decoding from.
        //Asks for the number of bits per byte that were used while encoding through calling getBitsPerByte method.
        System.out.println("How many bits per byte were used during encoding?");
        int bitsPerByte = getBitsPerByte(scnr);
        //Checks for an alpha channel using the same linein the earlier method
        int bytesPerPixel = (containerImg.getAlphaRaster() != null) ? 4 : 3;
        scnr.nextLine(); //Clear the input buffer after getBitsPerByte calls nextInt() method.
        int imgHeight = containerImg.getHeight();
        //Same totalSlots/step derivation as stegEncode, so the exact same spread-out visiting order is reproduced here.
        long totalSlots = (long)containerImg.getWidth()*imgHeight*bytesPerPixel;
        long step = computeSpreadStep(totalSlots);
        byte[] secretData = new byte[(int)((totalSlots*bitsPerByte)/8)]; //Allocate heap space for byte array of bytesOfStorage size.
	//Variable setup for following decoding.
        int secretDataShift = 0; //Counter for bit shift within secret byte.
        boolean decoded = false; //Decode flag used to brake out of loops early.
        int size = 0; //Counter for bytes of decoded message.
        //Walks the same spread-out slot order used during encoding, decoding sequentially until the terminator is found.
        for(long i = 0;i<totalSlots && !decoded;i++) {
            long containerByteIndex = (i*step)%totalSlots;
            int[] coords = slotToPixelCoords(containerByteIndex, imgHeight, bytesPerPixel);
            int x = coords[0], y = coords[1], colorComponentIndex = coords[2];
            int colorComponentIndexShift = colorComponentIndex * 8;
            int currentPixelColor = containerImg.getRGB(x,y);
            byte colorComponent = Integer.valueOf(currentPixelColor >> colorComponentIndexShift).byteValue(); // This gets the color component by shifting the currentPixelColor integer 8*colorComponentIndex bits and then converting to a byte therebye cutting off the top 24 bits.
            int secretDataByteIndex = (int)((i*bitsPerByte)/8);
            byte secretDataMask = Integer.valueOf((1<<bitsPerByte)-1).byteValue();
            secretData[secretDataByteIndex] = Integer.valueOf(secretData[secretDataByteIndex] | ((colorComponent & secretDataMask)<<secretDataShift)).byteValue();
            secretDataShift+=bitsPerByte;
            if(secretDataShift>7) {//Once the shift is 8 (or more) we know were on the next byte.
                secretDataShift = 0;//Reset counter.
                size++;//The byte at index size-1 was just completed.
                if(size>=terminator.length) {//Make sure we've decoded at least terminator.length bytes so we know we can compare the terminator.
                    //Terminator is used to find when all the data has been found and set decoded to true if it does so.
                    //Includes the byte just completed above, otherwise a terminator ending on the very last
                    //available byte (e.g. a payload that maxes out the image's capacity) would never be caught.
                    byte[] newArray = Arrays.copyOfRange(secretData,(size-terminator.length),size); //this byte array is a subsection of the last terminator.length bytes of the decoded byte array (secretData). This is what we will be comparing to in order to determine if we have reached the end of the decode.
                    if(Arrays.equals(newArray,terminator)) { //If the terminator is found, subtract terminator from final size, set decoded flag, and exit loop.
                        decoded = true;
                        size = size-terminator.length;
                        break;
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
                    System.out.print(new String(new byte[]{secretData[i]}, StandardCharsets.US_ASCII)); //Prints current byte of secretData byte array decoded to UTF-8 using StandardCharsets.
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
        System.out.println("Would you like to encode a PNG, decode a PNG, or check a PNG's available space?");
        char choice = '!';
        while(choice == '!') { //Continues until valid response is recieved. In this case d for decode, e for encode, or c for capacity.
            System.out.print("Enter \"e\" for encode, \"d\" for decode, or \"c\" for available space:");
            choice = scnr.nextLine().toLowerCase().charAt(0);
            if(choice == 'e') {
                stegEncode(scnr);
            } else if(choice == 'd') {
                stegDecode(scnr);
            } else if(choice == 'c') {
                showAvailableSpace(scnr);
            } else {
                choice = '!';
            }
        }
    }
}
