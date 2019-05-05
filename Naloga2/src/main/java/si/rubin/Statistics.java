package si.rubin;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Racuna statistiko s pomocjo histograma
 *
 * @author David Rubin
 */
public class Statistics {

    private static float[] xs;
    private static float[] ys;
    private static float[] zs;
    private static short[] is;
    private static ByteBuffer blockBuffer;

    public static void main(String[] args) {
        if (args.length < 9) {
            printHelp();
            System.exit(1);
        }
        //long beforeUsedMem = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
        String fileName = args[0]; // File with sorted data
        int dataSize = -1; // Number of rows in the file (read below)
        int maxMemory = Integer.parseInt(args[1]); // Maximum memory allowed by ouy application
        int blockSize = Integer.parseInt(args[2]); // How much data is read at once
        double minX = Double.parseDouble(args[3]); // X coordinate limits
        double maxX = Double.parseDouble(args[4]);
        double minY = Double.parseDouble(args[5]); // Y coordinate limits
        double maxY = Double.parseDouble(args[6]);
        int binSize = Integer.parseInt(args[7]); // Histogram bin size
        String targetVar = args[8]; // Which value is used to calculate statistics (z coordinate or intensity i)

        int nBins = (int) Math.ceil((maxX - minX) / binSize); // Number of bins
        int[] histogram = new int[nBins];
        try {
            FileInputStream fis = new FileInputStream(fileName);
            FileChannel fChan = fis.getChannel();

            // Read the number of lines (dataSize)
            ByteBuffer intBuff = ByteBuffer.allocate(8);
            fChan.read(intBuff);
            intBuff.flip();
            dataSize = intBuff.getInt();

            // Ena vrstica znotraj datoteke je velika 14 byteov (3x float + 1x short)
            int linesInBlock = (int) Math.floor(blockSize / 0.000014);
            // V pomnilniku lahko imamo M/B blokov, torej M/B * linesInBlock vrstic:
            int readableLines = (int) Math.floor(maxMemory / blockSize) * linesInBlock;
            xs = new float[linesInBlock];
            ys = new float[linesInBlock];
            zs = new float[linesInBlock];
            is = new short[linesInBlock];
            blockBuffer = ByteBuffer.allocate(linesInBlock);
            //System.out.println("File has " + dataSize + " lines");
            //System.out.println("Block holds " + linesInBlock + " lines");

            int line = findStartingLine(minX, minY, fChan, dataSize, 0);

        } catch (FileNotFoundException e) {
            System.out.println("File not found.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //long afterUsedMem=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
        //System.out.println(String.format("Memory usage: %.2f", (afterUsedMem - beforeUsedMem) / 1000000));
    }

    /**
     * Prebere blok iz datoteke in shrani podatke v podane sezname
     * @param fChan FileChannel nad datoteko
     * @param offset koliko je zamaknjeno branje
     * @return stevilo prebranih byteov
     */
    public static int readBlock(FileChannel fChan, long offset) {
        blockBuffer.position(0);
        int bytesRead = 0;
        int i = 0;
        try {
            if ((bytesRead = fChan.read(blockBuffer, offset)) > 0) {
                blockBuffer.flip();
                populateValues(i, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytesRead;
    }

    /**
     * Preberi 1 blok podatkov iz datoteke
     * @param fChan deskriptor datoteke
     * @return stevilo prebranih byteov
     */
    public static int readBlock(FileChannel fChan) {
        blockBuffer.position(0);
        int bytesRead = 0;
        int i = 0;
        try {
            if ((bytesRead = fChan.read(blockBuffer)) > 0) {
                blockBuffer.flip();
                populateValues(i, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytesRead;
    }

    /**
     * Napolni globalne sezname vrednosti s podanimi iz bloka
     * @param offset kje v seznamu pricne polniti
     * @param bytesRead koliko vrednosti je prebranih v bufferju
     */
    public static void populateValues(int offset, int bytesRead) {
        while (blockBuffer.hasRemaining() && offset < (bytesRead/14)) {
            xs[offset] = blockBuffer.getFloat();
            ys[offset] = blockBuffer.getFloat();
            zs[offset] = blockBuffer.getFloat();
            is[offset] = blockBuffer.getShort();
            offset++;
        }
    }

    /**
     * Najde vrstico v kateri se nahaja vrednost minX in minY
     * @param minX vrednost X koordinate
     * @param minY vrednost Y koordinate
     * @param fChan datoteka, kjer so podatki
     * @param upperBound zgornja meja vrstic ko iscemo
     * @param lowerBound spodnja meja vrstic ko iscemo
     * @return stevilka vrstice v datoteki, kjer se zacne interval za statistiko
     */
    public static int findStartingLine(double minX, double minY, FileChannel fChan, int upperBound, int lowerBound) {
        // Binary search the first X value that is equal to minX
        int middleLine = (int) (lowerBound + upperBound) / 2;
        // Read the block in the middle -> middleLine * 14 bytes of offset
        int bytesRead = readBlock(fChan, middleLine*14);
        if (bytesRead < 1) return -1;

        // Check if minX is inside the block or partially inside or outside
        if (minX >= xs[0] && minX <= xs[bytesRead/14]) {
            //Prebrani X je znotraj bloka, YAY
            System.out.println("X is inside this block (line " + middleLine + ")");
            return middleLine;
        } else if (minX < xs[0]) {
            // Search for the X in the lower half
            return findStartingLine(minX, minY, fChan, middleLine, lowerBound);
        } else if (minX > xs[bytesRead/14]) {
            // Search for the X in the upper half
            return findStartingLine(minX, minY, fChan, upperBound, middleLine);
        }
        System.out.println("Returning -1, none of above happened");
        return -1;
    }

    public static void printHelp() {
        System.out.println("Please provide all the arguments.");
        System.out.println("\nSample usage:\n  Statistics <obdelani_podatki> <M> <B> <minX> <maxX> <minY> <maxY> <velikost_kosa> <opcija>");
        System.out.println("\n  <obdelani_podatki>  - datoteka s preobdelanimi podatki\n" +
                "  N                   - velikost vhodnih podatkov (to velikost preberete sami in ne bo podana kot argument)\n" +
                "  <M>                 - velikost pomnilnika (to velikost doloci uporabnik in doloca koliko vhodnih podatkov lahko imamo naenkrat v \"buffer-ju\")\n" +
                "  <B>                 - velikost bloka (to velikost doloci uporabnik, ter nam doloca koliko vhodnih podatkov lahko preberemo naenkrat v aplikacijo)\n" +
                "  <minX>              - vrednost X, ki nam bo definiral zacetek obmocja, kjer bomo iskali tocke\n" +
                "  <maxX>              - vrednost X, ki nam bo definiral konec obmocja, kjer bomo iskali tocke\n" +
                "  <minY>              - vrednost Y, ki nam bo definiral zacetek obmocja, kjer bomo iskali tocke\n" +
                "  <minY>              - vrednost Y, ki nam bo definiral konec obmocja, kjer bomo iskali tocke\n" +
                "  <velikost kosa>     - velikost kosa v histogramu\n" +
                "  <opcija>            - moznost izbire uporabnika preko argumenta, ali se za izracun statistike uporabila vrednosti intenzitete (i) ali visine (z)");
    }
}
