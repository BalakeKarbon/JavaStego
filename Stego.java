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
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;

public class Stego {
    //Creates a byte array named terminator that acts as a marker to signal the end of the secret message.
    public static byte[] terminator = "END-OF-SECRET-DATA".getBytes(StandardCharsets.UTF_8);
    /*ImageInput is what getImageInput resolves a user-supplied path to: either a single loaded image, or a
     * whole directory's worth of readable images (with their backing files, filename-sorted) to operate on
     * as a group. Every mode - encode, decode, capacity - branches on isDirectory to decide which path to take.
     */
    public static class ImageInput {
        public boolean isDirectory;
        public BufferedImage image;
        public List<BufferedImage> images;
        public List<File> files;
    }
    /*getImageInput prompts for a single path that can point at either an image file or a directory of images,
     * and resolves it accordingly - loading one BufferedImage, or scanning the directory for every file
     * ImageIO can decode (via listReadableImages). Re-prompts until it gets a readable image, a directory
     * containing at least one, or an unresolvable path. This is what lets encode/decode/capacity all support
     * bulk mode transparently based on what kind of path the user gives them.
     */
    public static ImageInput getImageInput(Scanner scnr) {
        while(true) {
            System.out.print("Path:");
            String path = scnr.nextLine();
            File f = new File(path);
            if(f.isDirectory()) {
                List<File> files = new ArrayList<>();
                List<BufferedImage> images = listReadableImages(path, files);
                if(images.isEmpty()) {
                    System.out.println("No readable images were found in \""+path+"\".");
                    continue;
                }
                System.out.println("Found "+images.size()+" readable image(s) in \""+path+"\".");
                ImageInput result = new ImageInput();
                result.isDirectory = true;
                result.images = images;
                result.files = files;
                return result;
            }
            BufferedImage img;
            try {
                img = ImageIO.read(f);
            } catch (IOException e) {
                System.out.println(e.getMessage());
                continue;
            }
            if(img == null) {
                System.out.println("\""+path+"\" is not a readable image or directory.");
                continue;
            }
            ImageInput result = new ImageInput();
            result.isDirectory = false;
            result.image = img;
            return result;
        }
    }
    /*getOutputDirectoryPath prompts the user for a directory to write bulk-encoded images into, creating it
     * (including parent directories) if it doesn't already exist.
     */
    public static String getOutputDirectoryPath(Scanner scnr) {
        boolean dirAttained = false;
        String path = "";
        while(!dirAttained) {
            System.out.print("Directory:");
            path = scnr.nextLine();
            File dir = new File(path);
            if(dir.exists() && !dir.isDirectory()) {
                System.out.println("That path exists and is not a directory: \""+path+"\"");
                continue;
            }
            if(!dir.exists() && !dir.mkdirs()) {
                System.out.println("Could not create directory: \""+path+"\"");
                continue;
            }
            dirAttained = true;
        }
        return path;
    }
    /*listReadableImages scans a directory (non-recursively) for files that ImageIO can actually decode,
     * sorted by filename so that encode and decode walk the group of images in the same, reproducible order.
     * Files that aren't readable images are skipped with a warning rather than aborting the whole operation.
     */
    public static List<BufferedImage> listReadableImages(String dirPath, List<File> outFiles) {
        List<BufferedImage> images = new ArrayList<>();
        File dir = new File(dirPath);
        File[] entries = dir.listFiles();
        if(entries == null) {
            return images;
        }
        Arrays.sort(entries, (a,b) -> a.getName().compareTo(b.getName()));
        for(File f : entries) {
            if(!f.isFile()) {
                continue;
            }
            BufferedImage img;
            try {
                img = ImageIO.read(f);
            } catch (IOException e) {
                System.out.println("Skipping \""+f.getName()+"\": "+e.getMessage());
                continue;
            }
            if(img == null) {
                System.out.println("Skipping \""+f.getName()+"\": not a readable image format.");
                continue;
            }
            images.add(img);
            outFiles.add(f);
        }
        return images;
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
    /*getLowestBitsPerByteForImages is the bulk-mode analogue of getLowestBitsPerByte: it finds the smallest
     * power-of-2 bitsPerByte that, applied uniformly across every image in the group, gives them enough
     * combined capacity to hold secretData. A single shared bitsPerByte keeps encode/decode capacity math
     * (and the CLI prompt) identical to single-image mode, just summed over the whole directory.
     */
    public static int getLowestBitsPerByteForImages(List<BufferedImage> images, byte[] secretData) {
        for(int bitsPerByte = 1;bitsPerByte<=8;bitsPerByte*=2) {
            long total = 0;
            for(BufferedImage img : images) {
                total += getStorageCapacity(img, bitsPerByte);
            }
            if(secretData.length<=total) {
                return bitsPerByte;
            }
        }
        return -1;
    }
    /*formatBytes renders a byte count in the largest unit (B/KB/MB/GB/TB/PB, binary/1024-based) that keeps
     * it at least 1 in magnitude, so capacity output reads naturally regardless of image size - "582 B" for
     * a handful of pixels, "3.82 MB" for a large photo, without the caller having to pick a unit.
     */
    public static String formatBytes(long bytes) {
        String[] units = {"B","KB","MB","GB","TB","PB"};
        double value = bytes;
        int unitIndex = 0;
        while(value >= 1024 && unitIndex < units.length-1) {
            value /= 1024;
            unitIndex++;
        }
        if(unitIndex == 0) {
            return (long)value+" "+units[0];
        }
        return String.format("%.2f %s", value, units[unitIndex]);
    }
    /*printTable prints a left-aligned, pipe-delimited table: a header row, a rule beneath it, then the data
     * rows, with every column padded to the width of its longest cell (header included) so values line up
     * vertically in a monospaced terminal. Each row array must be the same length as headers.
     */
    public static void printTable(String[] headers, List<String[]> rows) {
        int cols = headers.length;
        int[] widths = new int[cols];
        for(int c = 0;c<cols;c++) {
            widths[c] = headers[c].length();
        }
        for(String[] row : rows) {
            for(int c = 0;c<cols;c++) {
                widths[c] = Math.max(widths[c], row[c].length());
            }
        }
        System.out.println(formatTableRow(headers, widths));
        StringBuilder rule = new StringBuilder();
        for(int c = 0;c<cols;c++) {
            if(c>0) {
                rule.append("-|-");
            }
            for(int i = 0;i<widths[c];i++) {
                rule.append('-');
            }
        }
        System.out.println(rule);
        for(String[] row : rows) {
            System.out.println(formatTableRow(row, widths));
        }
    }
    //Pads each cell in a row out to its column's width and joins them with " | " separators, per printTable.
    private static String formatTableRow(String[] cells, int[] widths) {
        StringBuilder line = new StringBuilder();
        for(int c = 0;c<cells.length;c++) {
            if(c>0) {
                line.append(" | ");
            }
            line.append(cells[c]);
            for(int i = cells[c].length();i<widths[c];i++) {
                line.append(' ');
            }
        }
        return line.toString();
    }
    /*showAvailableSpace prompts for an image or a directory of images and prints how many bytes it (or the
     * whole group, combined) can hold at each valid bitsPerByte, as aligned tables.
     */
    public static void showAvailableSpace(Scanner scnr) {
        System.out.println("What image (PNG, JPG, or other common format) or directory of images would you like to check available space for? (A directory reports the combined capacity across every image inside it.)");
        ImageInput input = getImageInput(scnr);
        if(input.isDirectory) {
            System.out.println("Per-image storage by bits per byte:");
            List<String[]> perImageRows = new ArrayList<>();
            for(int i = 0;i<input.images.size();i++) {
                String[] row = new String[5];
                row[0] = input.files.get(i).getName();
                int col = 1;
                for(int bitsPerByte = 1;bitsPerByte<=8;bitsPerByte*=2) {
                    row[col++] = formatBytes(getStorageCapacity(input.images.get(i), bitsPerByte));
                }
                perImageRows.add(row);
            }
            printTable(new String[]{"Image","1 bpb","2 bpb","4 bpb","8 bpb"}, perImageRows);
            System.out.println("Combined storage across "+input.images.size()+" image(s), usable accounting for the "+terminator.length+"-byte terminator marker every encode reserves:");
            List<String[]> combinedRows = new ArrayList<>();
            for(int bitsPerByte = 1;bitsPerByte<=8;bitsPerByte*=2) {
                long bytesOfStorage = 0;
                for(BufferedImage img : input.images) {
                    bytesOfStorage += getStorageCapacity(img, bitsPerByte);
                }
                long usableBytes = Math.max(0, bytesOfStorage-terminator.length);
                combinedRows.add(new String[]{String.valueOf(bitsPerByte), formatBytes(usableBytes)});
            }
            printTable(new String[]{"Bits/Byte","Usable"}, combinedRows);
        } else {
            System.out.println("Available storage, usable bytes accounting for the "+terminator.length+"-byte terminator marker every encode reserves:");
            List<String[]> rows = new ArrayList<>();
            for(int bitsPerByte = 1;bitsPerByte<=8;bitsPerByte*=2) {
                long bytesOfStorage = getStorageCapacity(input.image, bitsPerByte);
                long usableBytes = Math.max(0, bytesOfStorage-terminator.length);
                rows.add(new String[]{String.valueOf(bitsPerByte), String.valueOf(bytesOfStorage), formatBytes(bytesOfStorage), String.valueOf(usableBytes), formatBytes(usableBytes)});
            }
            printTable(new String[]{"Bits/Byte","Bytes Total","Human Total","Bytes Usable","Human Usable"}, rows);
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
    /*encodeIntoImage embeds as much of secretData (starting at startIndex) as containerImg has room for at
     * bitsPerByte, using the same golden-ratio spread order as single-image encode. It never writes a partial
     * logical byte: the amount it takes is capped at containerImg's whole-byte capacity, so a byte is never
     * split across two images in bulk mode. Returns the new index into secretData (startIndex plus however
     * many bytes this image absorbed) so the caller can hand the remainder to the next image.
     */
    public static int encodeIntoImage(BufferedImage containerImg, byte[] secretData, int startIndex, int bitsPerByte) {
        int bytesPerPixel = (containerImg.getAlphaRaster() != null) ? 4 : 3;
        int imgHeight = containerImg.getHeight();
        long totalSlots = (long)containerImg.getWidth()*imgHeight*bytesPerPixel;
        long step = computeSpreadStep(totalSlots);
        long capacity = getStorageCapacity(containerImg, bitsPerByte);
        int bytesToWrite = (int)Math.min(capacity, secretData.length-startIndex);
        int secretSliceIndex = 0;
        for(long i = 0;i<totalSlots;i++) {
            int localByteIndex = (int)((i*bitsPerByte)/8);
            //This breaks the loop early once this image's share of the secret data has been embedded.
            if(localByteIndex>=bytesToWrite) {
                break;
            }
            int secretDataByteIndex = startIndex+localByteIndex;
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
        return startIndex+bytesToWrite;
    }
    /*decodeFromImage reads as many bytes as containerImg holds at bitsPerByte into secretData (starting at
     * startIndex), stopping the instant the terminator marker shows up in the trailing bytes decoded so far.
     * Mirrors encodeIntoImage's per-image byte cap, so a logical byte is never split across two images and
     * this can be called once per image in filename order to reassemble data spread by bulk encode.
     * Returns {newIndex, terminatorFound(0/1)}.
     */
    public static int[] decodeFromImage(BufferedImage containerImg, int bitsPerByte, byte[] secretData, int startIndex) {
        int bytesPerPixel = (containerImg.getAlphaRaster() != null) ? 4 : 3;
        int imgHeight = containerImg.getHeight();
        long totalSlots = (long)containerImg.getWidth()*imgHeight*bytesPerPixel;
        long step = computeSpreadStep(totalSlots);
        long capacity = getStorageCapacity(containerImg, bitsPerByte);
        int maxBytesThisImage = (int)Math.min(capacity, secretData.length-startIndex);
        int secretDataShift = 0; //Counter for bit shift within secret byte.
        boolean decoded = false; //Decode flag used to break out of the loop early.
        int size = 0; //Counter for bytes of decoded message completed within this image.
        for(long i = 0;i<totalSlots && !decoded;i++) {
            int localByteIndex = (int)((i*bitsPerByte)/8);
            if(localByteIndex>=maxBytesThisImage) {
                break;
            }
            int secretDataByteIndex = startIndex+localByteIndex;
            long containerByteIndex = (i*step)%totalSlots;
            int[] coords = slotToPixelCoords(containerByteIndex, imgHeight, bytesPerPixel);
            int x = coords[0], y = coords[1], colorComponentIndex = coords[2];
            int colorComponentIndexShift = colorComponentIndex * 8;
            int currentPixelColor = containerImg.getRGB(x,y);
            byte colorComponent = Integer.valueOf(currentPixelColor >> colorComponentIndexShift).byteValue(); // This gets the color component by shifting the currentPixelColor integer 8*colorComponentIndex bits and then converting to a byte therebye cutting off the top 24 bits.
            byte secretDataMask = Integer.valueOf((1<<bitsPerByte)-1).byteValue();
            secretData[secretDataByteIndex] = Integer.valueOf(secretData[secretDataByteIndex] | ((colorComponent & secretDataMask)<<secretDataShift)).byteValue();
            secretDataShift+=bitsPerByte;
            if(secretDataShift>7) {//Once the shift is 8 (or more) we know were on the next byte.
                secretDataShift = 0;//Reset counter.
                size++;//The byte at index size-1 was just completed.
                int completedGlobally = startIndex+size;
                if(completedGlobally>=terminator.length) {//Make sure we've decoded at least terminator.length bytes so we know we can compare the terminator.
                    //Terminator is used to find when all the data has been found and set decoded to true if it does so.
                    byte[] tail = Arrays.copyOfRange(secretData,(completedGlobally-terminator.length),completedGlobally); //this byte array is a subsection of the last terminator.length bytes of the decoded byte array (secretData). This is what we will be comparing to in order to determine if we have reached the end of the decode.
                    if(Arrays.equals(tail,terminator)) { //If the terminator is found, set decoded flag and exit loop.
                        decoded = true;
                    }
                }
            }
        }
        return new int[]{startIndex+size, decoded ? 1 : 0};
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
    /*stegEncode is entered if the user selects that they would like to encode rather than decode. It resolves
     * the given path to either a single image or a directory of images and dispatches accordingly - the same
     * prompt supports both single-image and bulk encoding based on what kind of path was given.
     */
    public static void stegEncode(Scanner scnr) {
        System.out.println("What image (PNG, JPG, or other common format) or directory of images would you like to store your secret data in? (A directory spreads the data across every image inside it.)");
        ImageInput input = getImageInput(scnr);
        if(input.isDirectory) {
            bulkStegEncode(scnr, input.images, input.files);
            return;
        }
        BufferedImage containerImg = input.image;
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
        encodeIntoImage(containerImg, secretData, 0, bitsPerByte);
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
    /*outputSecretData prompts the user for whether the first size bytes of secretData should be printed as
     * text or written out to a file, and does so. Shared tail end of both single-image and bulk decode.
     */
    public static void outputSecretData(Scanner scnr, byte[] secretData, int size) {
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
                System.out.println();
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
    /*stegDecode is entered if the user chooses to decode a message from an image (or group of images) that
     * already contains data written onto it with steganography. Resolves the given path to either a single
     * image or a directory of images and dispatches accordingly, same as stegEncode.
     */
    public static void stegDecode(Scanner scnr) {
        //Prompts the user for the encoded image or directory of encoded images they would like to decode. This
        //must be the lossless PNG(s) that stegEncode produced - a re-compressed JPG would have destroyed the
        //hidden bits.
        System.out.println("What encoded image or directory of encoded images would you like to decode?");
        ImageInput input = getImageInput(scnr);
        //Asks for the number of bits per byte that were used while encoding through calling getBitsPerByte method.
        System.out.println("How many bits per byte were used during encoding?");
        int bitsPerByte = getBitsPerByte(scnr);
        scnr.nextLine(); //Clear the input buffer after getBitsPerByte calls nextInt() method.
        if(input.isDirectory) {
            bulkStegDecode(scnr, input.images, bitsPerByte);
            return;
        }
        BufferedImage containerImg = input.image;
        byte[] secretData = new byte[(int)getStorageCapacity(containerImg, bitsPerByte)]; //Allocate heap space for byte array of bytesOfStorage size.
        int[] result = decodeFromImage(containerImg, bitsPerByte, secretData, 0);
        int size = result[0];
        boolean decoded = result[1] == 1;
        if(!decoded) {
            System.out.println("Warning: terminator marker was never found. This image may not contain hidden data, or the wrong bits-per-byte value was given. Showing everything decoded anyway.");
        } else {
            size = size-terminator.length;
        }
        outputSecretData(scnr, secretData, size);
    }
    /*stripExtension removes a trailing ".ext" from a filename, if present, so bulk encode can rename cover
     * files to ".png" without ending up with things like "photo.jpg.png".
     */
    public static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return (dot>0) ? name.substring(0,dot) : name;
    }
    /*uniqueName appends a numeric suffix to baseName until it no longer collides with a name already in
     * usedNames, so that two source files that only differ by extension (e.g. "photo.jpg" and "photo.png")
     * don't clobber each other once both are renamed to ".png" in the output directory.
     */
    public static String uniqueName(String baseName, HashSet<String> usedNames) {
        if(usedNames.add(baseName)) {
            return baseName;
        }
        String stem = stripExtension(baseName);
        int n = 2;
        String candidate;
        do {
            candidate = stem+"_"+n+".png";
            n++;
        } while(!usedNames.add(candidate));
        return candidate;
    }
    /*bulkStegEncode spreads a single file or piece of text across every image in images (already resolved by
     * getImageInput), filling each one to its whole-byte capacity (at one shared bitsPerByte) before moving on
     * to the next, in the same filename-sorted order they were found in. Only images that actually end up
     * holding data are written to the output directory - decode later walks whatever images are present there
     * in the same sorted order.
     */
    public static void bulkStegEncode(Scanner scnr, List<BufferedImage> images, List<File> files) {
        byte[] secretData;
        int bitsPerByte;
        while(true) {
            secretData = getSecretData(scnr);
            bitsPerByte = getLowestBitsPerByteForImages(images, secretData);
            if(bitsPerByte != -1) {
                break;
            }
            System.out.println("Data does not fit within the combined available storage of all "+images.size()+" image(s), even at 8 bits per byte! Please choose a smaller file or shorter text.");
        }
        System.out.println("Using "+bitsPerByte+" bits per byte to encode this data across the image group. Remember this value - you will need it to decode!");
        System.out.println("Where would you like to save the encoded images?");
        String outputDir = getOutputDirectoryPath(scnr);
        HashSet<String> usedNames = new HashSet<>();
        int startIndex = 0;
        int imagesUsed = 0;
        for(int i = 0;i<images.size() && startIndex<secretData.length;i++) {
            BufferedImage img = images.get(i);
            int newIndex = encodeIntoImage(img, secretData, startIndex, bitsPerByte);
            if(newIndex==startIndex) {
                continue; //This image has no capacity left to contribute at this bitsPerByte; leave it out of the output directory.
            }
            startIndex = newIndex;
            String outName = uniqueName(stripExtension(files.get(i).getName())+".png", usedNames);
            File outFile = new File(outputDir, outName);
            try {
                ImageIO.write(img, "png", outFile);
                System.out.println("Wrote \""+outFile.getPath()+"\"");
                imagesUsed++;
            } catch (IOException e) {
                System.out.println("Failed to write \""+outFile.getPath()+"\": "+e.getMessage());
            }
        }
        if(startIndex<secretData.length) {
            System.out.println("Warning: ran out of images before all data could be written. The image group's capacity may have been miscalculated.");
        }
        System.out.println("Encoded data across "+imagesUsed+" image(s) into \""+outputDir+"\". Decode all images in that directory together, in filename order, at "+bitsPerByte+" bits per byte.");
    }
    /*bulkStegDecode is the counterpart to bulkStegEncode: it walks every image in images (already resolved by
     * getImageInput), in the same filename-sorted order encode used, reassembling the byte stream until the
     * terminator marker turns up (possibly spanning several images, since it doesn't know in advance how many
     * were used).
     */
    public static void bulkStegDecode(Scanner scnr, List<BufferedImage> images, int bitsPerByte) {
        long totalCapacity = 0;
        for(BufferedImage img : images) {
            totalCapacity += getStorageCapacity(img, bitsPerByte);
        }
        byte[] secretData = new byte[(int)totalCapacity];
        int startIndex = 0;
        boolean decoded = false;
        for(BufferedImage img : images) {
            int[] result = decodeFromImage(img, bitsPerByte, secretData, startIndex);
            startIndex = result[0];
            if(result[1]==1) {
                decoded = true;
                break;
            }
        }
        int size = startIndex;
        if(!decoded) {
            System.out.println("Warning: terminator marker was never found across all "+images.size()+" image(s) in that directory. Wrong bits-per-byte value, an incomplete image set, or these images don't contain bulk-encoded data. Showing everything decoded anyway.");
        } else {
            size = size-terminator.length;
        }
        outputSecretData(scnr, secretData, size);
    }
    //Main method prompts the user if they would like to encode, decode, or check available space, then forwards
    //to the relevant method based on response. Each of those methods accepts either a single image file or a
    //directory of images (spreading/reassembling the data across the whole group), so there's no separate mode.
    public static void main(String args[]) {
        Scanner scnr = new Scanner(System.in);
        System.out.println("Would you like to encode, decode, or check available space? Point at a single image file, or a directory of images to spread/read data across the whole group.");
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
