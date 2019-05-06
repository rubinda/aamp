package si.rubin;

import com.google.code.externalsorting.ExternalSort;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * DataSort je razred namenjen preobdelavi testnih podatkov, tako da jih uredi
 * Testni podatki so zaradi velikost (~230MB) iz repozitorija izpusceni. Za primer
 * datoteke je podanih prvih 10000 vrstic znotraj Korte_Vegetation_10k.txt
 *
 * @author David Rubin
 */
public class DataSort {
    private static List<ReflectionPoint> points;
    private static Pattern spacePattern = Pattern.compile(" ");
    private static final String DATA_SET = "Korte_Vegetation_10k.txt";

    public static void main(String[] args) {
        // Check if the user has given 2 program arguments
        if (args.length < 2) {
            System.out.println("Please rerun the program with a given input and output file.\n");
            System.out.println("Sample usage:\n  java DataSort <input_file> <output_file>\n");
            System.out.println("  <input_file> ... must respect the format as in Korte_Vegetation_10k.txt");
            System.out.println("  <output_file> ... will contain sorted data");
            System.exit(1);
        }
        try {
            // Read the file names for input and output
            File inputFile = new File(args[0]);
            File outputFile = new File(args[1]);
            PointComparator pc = new PointComparator();
            File sortedInput = new File(args[0] + ".out");

            System.out.print("Sorting the data ... ");
            long startTime = System.currentTimeMillis();
            // Fastest method I've tested. Probably in-memory.
            ExternalSort.sort(inputFile, sortedInput, pc);

            // True merge sort with temporary files
            //int linesSorted = (int) ExternalSort.mergeSortedFiles(ExternalSort.sortInBatch(inputFile, pc), sortedInput);

            // Using Java Collections
            //List<String> lines = Files.readAllLines(inputFile.toPath());
            //Collections.sort(lines, lc);
            float sortTime = (float) (System.currentTimeMillis() - startTime) / 1000;
            System.out.println(String.format("took %.2fs", sortTime));

            // Preberi sortirano datoteko vrstico za vrstico in jo prepisi v binarno
            System.out.print("Rewriting the file to binary ... ");
            startTime = System.currentTimeMillis();

            // Read file into memory and convert to binary (uses more memory)
            List<String> lines = Files.readAllLines(sortedInput.toPath());
            int linesSorted = lines.size();
            // linesSorted vrstic po 14B + zacetek, kjer je podana dolzina datoteke
            ByteBuffer bb = ByteBuffer.allocate(linesSorted * 14 + 4);
            bb.putInt(linesSorted);
            for (String line : lines) {
                // Read a sorted line and convert it into custom bytes
                // Each line will now have float|float|float|short or exactly 14 bytes (4+4+4+2)
                String[] numbers = spacePattern.split(line);
                bb.putFloat(Float.parseFloat(numbers[0]));
                bb.putFloat(Float.parseFloat(numbers[1]));
                bb.putFloat(Float.parseFloat(numbers[2]));
                bb.putShort(Short.parseShort(numbers[3]));
            }
            FileOutputStream fos = new FileOutputStream(outputFile);
            fos.write(bb.array());
            fos.close();
            // Read file from disk and rewrite as binary (should use less memory)
            /*BufferedReader reader = new BufferedReader(new FileReader(sortedInput));
            FileOutputStream fos = new FileOutputStream(outputFile);
            ByteBuffer bb = ByteBuffer.allocate(linesSorted * 14);
            String line;
            while ((line = reader.readLine()) != null) {
                // Read a sorted line and convert it into custom bytes
                // Each line will now have float|float|float|short or exactly 14 bytes (4+4+4+2)
                String[] numbers = line.split(" ");
                bb.putFloat(Float.parseFloat(numbers[0]));
                bb.putFloat(Float.parseFloat(numbers[1]));
                bb.putFloat(Float.parseFloat(numbers[2]));
                bb.putShort(Short.parseShort(numbers[3]));
            }
            fos.write(bb.array());
            fos.close();
            reader.close();*/
            sortTime = (float) (System.currentTimeMillis() - startTime) / 1000;
            System.out.println(String.format("took %.2fs", sortTime));
            binToTxt(outputFile, new File("Korte_Vege_10k_binToTxt.txt"), linesSorted);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void binToTxt(File binaryFile, File txtFile, int lines) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(txtFile));

            ByteBuffer bb = ByteBuffer.allocate(lines * 14 + 4);
            FileInputStream fis = new FileInputStream(binaryFile);
            FileChannel fChan = fis.getChannel();
            fChan.read(bb);
            bb.position(0);
            int nLines = bb.getInt();
            while (bb.hasRemaining()) {
                float x = bb.getFloat();
                float y = bb.getFloat();
                float z = bb.getFloat();
                short i = bb.getShort();
                bw.write(String.format("%.2f %.2f %.2f %d\n", x, y, z, i));
            }
            bw.close();
            fChan.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *  Custom Comparator za primerjanje nasih tock
     */
    public static class PointComparator implements Comparator<String> {

        @Override
        public int compare(String line1, String line2) {
            ReflectionPoint o1 = new ReflectionPoint(line1);
            ReflectionPoint o2 = new ReflectionPoint(line2);
            // Ce je X koordinata manjsa, je prej
            if (o1.getX() < o2.getX()) return -1;

            // Ce je X koordinata vecja, je kasneje
            if (o1.getX() > o2.getX()) return 1;

            // Ce je X koordinata ista pri obeh pogledaj Y
            if (o1.getX() == o2.getX()) {
                // Ce je Y manjsi, je o1 prej
                if (o1.getY() < o2.getY()) return -1;

                // Ce je Y vecji, je o1 kasneje
                if (o1.getY() > o2.getY()) return 1;

                // Ce sta poleg X tudi Y enaka, poglej Z
                if (o1.getY() == o2.getY()) {
                    // Ce je Z manjsi, je o1 prej
                    if (o1.getZ() < o2.getZ()) return -1;

                    // Ce je Z vecji, je o1 kasneje
                    if (o1.getZ() > o2.getZ()) return 1;
                }
            }
            // V kolikor so vse 3 koordinate enake pademo sem
            return 0;
        }
    }

    /**
     * ReflectionPoint je struktura vhodnih podatkov
     * Znotraj vsake vrstice so koordinate v 3D prostoru
     * in intenziteta odboja podane loceno s presledki.
     */
    static class ReflectionPoint {
        private double x;
        private double y;
        private double z;
        private int reflection;

        public ReflectionPoint(double x, double y, double z, int i)  {
            this.x = x;
            this.y = y;
            this.z = z;
            this.reflection = i;
        }

        /*
            Predvideva, da so podane vrstice iz datoteke, kjer so vrednosti:
            x y z i
         */
        ReflectionPoint(String line) {
            String[] values = line.split(" ");
            this.x = Double.parseDouble(values[0]);
            this.y = Double.parseDouble(values[1]);
            this.z = Double.parseDouble(values[2]);
            this.reflection = Integer.parseInt(values[3]);
        }

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }

        public double getZ() {
            return z;
        }

        public void setZ(double z) {
            this.z = z;
        }

        public int getReflection() {
            return reflection;
        }

        public void setReflection(int reflection) {
            this.reflection = reflection;
        }
    }
}
