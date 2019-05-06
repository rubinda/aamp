package si.rubin;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Racuna statistiko s pomocjo histograma
 *
 * TODO: popravi racunanje v kateri bin spada (@line 175)
 * TODO: namesto 1 bloka uporabljaj MAX_MEMORY
 * TODO: preveri kaj se zgodi v kolikor je candidateLines > linesInBlock
 * @author David Rubin
 */
public class Statistics {
    private static String DATA_FILE;        // The file which holds our sorted data (@see si.rubin.DataSort)
    private static int DISK_READS;          // How many times we read the file
    private static double MAX_MEMORY;       // How much memory is available (heap?)
    private static double BLOCK_SIZE;       // How much we can read at once
    private static double MIN_X, MAX_X;     // X Coordinate limits
    private static double MIN_Y, MAX_Y;     // Y Coordinate limits
    private static float[] xs, ys, zs;      // Store coordinate values
    private static short[] is;              // Stores reflection intensity values
    private static ByteBuffer blockBuffer;  // A buffer for storing the block read
    private static int BIN_SIZE;            // Size of the histogram bin
    private static int DATA_SIZE;           // Number of rows in the file
    private static char TARGET_VAR;         // The target variable (either Z or I, check README)
    private static int[] BIN_COUNTS;        // The histogram (number of values in bins)
    private static double[] BIN_VALUES;      // Values for each bin
    private static int VALUES_SIZE;         // How many points there are in the histogram

    public static void main(String[] args) {
        if (args.length < 9) {
            printHelp();
            System.exit(1);
        }
        //long beforeUsedMem = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
        initParams(args);
        try {
            FileInputStream fis = new FileInputStream(DATA_FILE);
            FileChannel fChan = fis.getChannel();

            // Read the number of lines (First 4 bytes inside the file)
            ByteBuffer intBuff = ByteBuffer.allocate(4);
            fChan.read(intBuff);
            intBuff.flip();
            DATA_SIZE = intBuff.getInt();

            // A line from the original data has been compressed to 14 bytes (3 floats + 1 short)
            int linesInBlock = (int) Math.floor(BLOCK_SIZE / 0.000014);
            // V pomnilniku lahko imamo M/B blokov, torej M/B * linesInBlock vrstic:
            int readableLines = (int) Math.floor(MAX_MEMORY / BLOCK_SIZE) * linesInBlock;
            xs = new float[linesInBlock];
            ys = new float[linesInBlock];
            zs = new float[linesInBlock];
            is = new short[linesInBlock];
            // Allocate enough to fit a block worth of lines into the buffer
            blockBuffer = ByteBuffer.allocate(linesInBlock * 14);
            //System.out.println("File has " + dataSize + " lines");
            //System.out.println("Block holds " + linesInBlock + " lines");

            int loBorder = findBound(fChan, 0, DATA_SIZE, MIN_X);
            //System.out.println("From line " + loBorder + " every X is larger than " + MIN_X);
            int upBorder = findBound(fChan,0, DATA_SIZE, MAX_X);
            //System.out.println("From line " + upBorder + " down every X is smaller than " + MAX_X);
            int candidateLines = upBorder - loBorder;

            if (candidateLines > linesInBlock) {
                System.out.println("More lines to filter than in a single Block, proceed with caution.");
            }
            // We are targeting short values
            int readBytes = readBlock(fChan, loBorder * 14 + 4);
            ArrayList<Float> targets = findTargets(readBytes, candidateLines);
            buildHistogram(targets);
            double avg = average();
            double stdv = standardDeviation(avg);
            double skew = skewness(avg);
            double kurt = kurtosis(avg);

            System.out.println("Points: " + VALUES_SIZE);
            System.out.println("Average: " + avg);
            System.out.println("Standard deviation: " + stdv);
            System.out.println("Skewness: " + skew);
            System.out.println("Kurtosis: " + kurt);
            System.out.println("Disk reads: " + DISK_READS);
        } catch (FileNotFoundException e) {
            System.out.println("File not found.");
        } catch (Exception e) {
            e.printStackTrace();
        }
        //long afterUsedMem=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
        //System.out.println(String.format("Memory usage: %.2f MB", ((float)(afterUsedMem - beforeUsedMem) / 1000000)));
    }

    /**
     * Izracuna povprecno vrednost histograma (glej enacbe.pdf)
     * @return povprecna vrednost
     */
    private static double average() {
        double sum = 0.0;
        for (int k = 0; k < BIN_COUNTS.length; k++) {
            sum += (double) BIN_COUNTS[k] * BIN_VALUES[k];
        }
        return sum / VALUES_SIZE;
    }

    /**
     * Izracuna standardni odklon nad histogramom (glej enacbe.pdf)
     * @param average povprecna vrednost histograma
     * @return standardni odklon
     */
    private static double standardDeviation(double average) {
        double sum = 0.0;
        for (int k = 0; k < BIN_COUNTS.length; k++) {
            sum += BIN_COUNTS[k] * Math.pow((BIN_VALUES[k] - average), 2);
        }
        return Math.sqrt(sum / VALUES_SIZE);
    }

    /**
     * Izracuna asimetrijo za histogram (glej enacbe.pdf)
     * @param average povprecna vrednost
     * @return skewness od histograma
     */
    private static double skewness(double average) {
        double upper = 0.0;
        double lower = 0.0;
        for (int k = 0; k < BIN_COUNTS.length; k++) {
            upper += BIN_COUNTS[k] * Math.pow((BIN_VALUES[k] - average), 3);
            lower += BIN_COUNTS[k] * Math.pow((BIN_VALUES[k] - average), 2);
        }
        upper /= VALUES_SIZE;
        lower /= (VALUES_SIZE - 1);
        return upper / Math.pow(lower, 1.5);
    }

    /**
     * Poracuna sploscenost (glej enacbe.pdf)
     * @param average povprecna vrednost
     * @return kurtosis od histograma
     */
    private static double kurtosis(double average) {
        double upper = .0;
        double lower = .0;
        for (int k = 0; k < BIN_COUNTS.length; k++) {
            upper += BIN_COUNTS[k] * Math.pow((BIN_VALUES[k] - average), 4);
            lower += BIN_COUNTS[k] * Math.pow((BIN_VALUES[k] - average), 2);
        }
        lower = Math.pow(lower, 2);
        return VALUES_SIZE * (upper / lower) - 3;
    }

    /**
     * Izgradi histogram: BIN_COUNTS drzi koliko vrednosti je v posameznem kosu,
     * BIN_VALUES pa vrednost za posamezen kos
     * @param values Vrednosti iz katerih gradimo histogram
     */
    private static void buildHistogram(ArrayList<Float> values) {
        // Create a new histogram
        float maxValue = Collections.max(values);
        float minValue = Collections.min(values);
        int nBins = (int) Math.ceil((maxValue - minValue) / BIN_SIZE); // Number of bins
        VALUES_SIZE = values.size();
        BIN_COUNTS = new int[nBins];
        BIN_VALUES = new double[nBins];
        // Calculate the values of bins
        for (int k = 0; k < nBins; k++) {
            BIN_VALUES[k] = minValue + k*BIN_SIZE + BIN_SIZE/2.;
        }
        // Count the values into the bins
        for (float value : values) {
            // Which bin the value belongs to
            int bin = (int) (value - minValue) / BIN_SIZE;
            BIN_COUNTS[bin]++;
        }
    }

    /**
     * Prebere vrednosti iz ByteBuffer (do vrstice bodisi bytesRead ali pa lineLimit)
     * in preveri katere Y vrednosti spadajo v nas interval
     * @param bytesRead koliko bytes je bilo prebranih v blockBuffer (globalen)
     * @param lineLimit koliko vrstic lahko pregledamo (ce jih je manj kot pa pase v blok)
     * @return seznam ciljnih spremenljivk
     * @throws Exception ce je vmes kaksna vrstica ki ne sodi v interval za X
     */
    private static ArrayList<Float> findTargets(int bytesRead, int lineLimit) throws Exception {
        populateValues(0, bytesRead);
        ArrayList<Float> targets = new ArrayList<>();
        for (int i = 0; i < lineLimit; i++) {
            if (ys[i] >= MIN_Y && ys[i] < MAX_Y) {
                if (TARGET_VAR == 'i')
                    targets.add((float) is[i]);
                else
                    targets.add(zs[i]);
            }
            if (xs[i] < MIN_X || xs[i] > MAX_X)
                throw new Exception("Some Xs are not inside the interval");
        }
        return targets;
    }

    /**
     * Prebere blok iz datoteke in shrani podatke v podane sezname
     * @param fChan FileChannel nad datoteko
     * @param byteOffset koliko byteov je zamaknjeno branje
     * @return stevilo prebranih byteov
     */
    private static int readBlock(FileChannel fChan, long byteOffset) {
        DISK_READS++;
        blockBuffer.position(0);
        int bytesRead = 0;
        int i = 0;
        try {
            if ((bytesRead = fChan.read(blockBuffer, byteOffset)) > 0) {
                blockBuffer.position(0);
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
    private static int readBlock(FileChannel fChan) {
        DISK_READS++;
        blockBuffer.position(0);
        int bytesRead = 0;
        int i = 0;
        try {
            if ((bytesRead = fChan.read(blockBuffer)) > 0) {
                blockBuffer.position(0);
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
     * @param bytesRead koliko byteov je prebranih v bufferju
     */
    private static void populateValues(int offset, int bytesRead) {
        int linesRead = bytesRead / 14;
        while (blockBuffer.hasRemaining() && offset < linesRead) {
            xs[offset] = blockBuffer.getFloat();
            ys[offset] = blockBuffer.getFloat();
            zs[offset] = blockBuffer.getFloat();
            is[offset] = blockBuffer.getShort();
            offset++;
        }
    }

    /**
     * Najde vrstico datoteke od katere naprej so vsi X > (ce wantMin=True, drugace <) x
     * @param fChan datoteka, kjer so podatki
     * @param upperBound zgornja meja vrstic ko iscemo
     * @param lowerBound spodnja meja vrstic ko iscemo
     * @param x vrednost katero iscemo
     * @return stevilka vrstice v datoteki, kjer se zacne interval za statistiko
     */
    private static int findBound(FileChannel fChan, int lowerBound, int upperBound, double x) throws Exception {
        int middleLine = (int) (lowerBound + upperBound) / 2;
        // Read the block in the middle -> 4 + middleLine * 14 bytes of offset
        // (first INT for length and a row has 14 bytes)
        int byteOffset = 4 + (middleLine * 14);
        int bytesRead = readBlock(fChan, byteOffset);
        int linesRead = bytesRead / 14;
        if (bytesRead < 1) return -1;
        // Check if x is inside the block
        if (x > xs[0] && x < xs[linesRead - 1]) {
            // x is inside our read block, find the first line so that
            // every next X is >= x (including the line)
            //System.out.println("x is inside this block (line " + lowerBound + " till " + upperBound + ")");
            // Find the limit where every next X is bigger than our x
            int i = 0;
            while (x > xs[i]) i++;
            //System.out.println("Lowest: " + xs[0] + "\nLimit: " + xs[i] + "\nLimit-1:" + xs[i-1] + "\nLimit+1: " + xs[i+1]);
            return middleLine + i;
        } else if (x <= xs[0]) {
            // Search for the x in the lower half
            //System.out.println("x is lower than the bound " + xs[0]);
            return findBound(fChan, lowerBound, middleLine, x);
        } else if (x >= xs[linesRead - 1]) {
            // Search for the x in the upper half
            //System.out.println("x is bigger than the bound: " + xs[bytesRead/14 - 1]);
            return findBound(fChan, middleLine, upperBound, x);
        }
        // If x hasn't fallen into any of the intervals, raise an Exception
        throw new Exception("x is neither inside or outside the given bounds!");
    }

    /**
     * Izpise kratko pomoc (izsek iz README)
     */
    private static void printHelp() {
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

    /**
     * Preveri vhodne parametre (ali so meje zgresene)
     */
    private static void checkParams() {
        if (MIN_X > MAX_X) {
            System.out.println("Min X bound is higher than Max X");
            System.exit(1);
        }
        if (MIN_Y > MAX_Y) {
            System.out.println("Min Y bound is higher than Max Y");
            System.exit(1);
        }
    }

    /**
     * Inicializira vhodne podatke
     * @param args iz metode main (argumenti pri zagonu)
     */
    private static void initParams(String[] args) {
        DISK_READS = 0;
        DATA_FILE = args[0];
        MAX_MEMORY = Double.parseDouble(args[1]);
        BLOCK_SIZE = Double.parseDouble(args[2]);
        MIN_X = Double.parseDouble(args[3]);
        MAX_X = Double.parseDouble(args[4]);
        MIN_Y = Double.parseDouble(args[5]);
        MAX_Y = Double.parseDouble(args[6]);
        BIN_SIZE = Integer.parseInt(args[7]);
        TARGET_VAR = args[8].charAt(0);
        checkParams();
    }
}
