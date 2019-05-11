package si.rubin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.DoubleStream;

import org.apache.commons.math3.linear.MatrixUtils;

/**
 * LinearRegression on Space separated values. The points can compose a polynomial, linear or multilinear function.
 * Based on a parameter the given file is treated as one of the previous options and coefficients are calculated.
 *
 * Sample usage:
 *  ./LinearRegression <file> <type> [degree]
 *  file ...    path to a file which holds the points (lines with: X Y or X1 X2 ... XN Y)
 *  type ...    [li|po|ml] the type of the function the points are describing (check PointPlot/points_plot.py)
 *              (li=linear, po=polynomial, ml=multilinear)
 *  degree ...  polynomial degree to fit the data. Required if type equals polynomial
 *
 * @author David Rubin
 * Uporabljeno pri predmetu Algoritmi in analiza masovnih podatkov, FERI 2019
 * TODO: calculate b0 with given b1-bN
 */

public class LinearRegression {
    private static final Pattern SPACE_PATTERN = Pattern.compile(" ");
    private static double[][] X_VALUES;     // The X values (in case of multilinear/poly more Xs are given)
    private static double[] Y_VALUES;       // The Y values

    public static void main(String[] args) {
        // Get the parameters
        if (args.length < 2) {
            System.out.println("Please include the file and function type as program parameters");
            System.exit(1);
        }
        String fileName = args[0];
        String functionType = args[1];
        int degree = 1;
        if (functionType.equals("po")) {
            if (args.length < 3) {
                System.out.println("Please include a 3rd parameter for the polynomial degree");
                System.exit(1);
            }
            degree = Integer.parseInt(args[2]);
        }
        readFile(fileName, functionType, degree);
        centerData();
        double[] b = calculateCoefficients(X_VALUES, Y_VALUES);
    }

    /**
     * Prints out a 2D array
     * @param A 2D array
     */
    private static void printMatrix(double[][] A) {
        for (int i = 0; i < A.length; i++) {
            for (int j = 0; j < A[i].length; j++) {
                System.out.print(A[i][j] + " ");
            }
            System.out.println();
        }
    }

    /**
     * Initializes the lists for reading the file into them
     * @param fileLength number of lines inside the data file
     * @param xSize number of Xs, >1 if multilin or poly
     */
    private static void initPointLists(int fileLength, int xSize) {
        X_VALUES = new double[fileLength][xSize];
        Y_VALUES = new double[fileLength];
    }

    /**
     * ReadFile reads the points given in fileName into X_VALUES and Y_VALUES
     * @param fileName the filename of the data
     * @param functionType linear, polynomial or multilinear
     * @param degree polynomial degree if functionType == "po"
     */
    private static void readFile(String fileName, String functionType, int degree) {
        List<String> lines = null;
        try {
            // Reads all of the data
            lines = Files.readAllLines(new File(fileName).toPath());
            // Calculate the number of Xs in a row (split the header and take away 1 for the Y)
            // Also discards the header
            int xSize = lines.remove(0).split(" ").length - 1;

            // Init the arrays for point data
            initPointLists(lines.size(), degree > 1 ? degree : xSize);
            int lineCount = 0;
            for (String line : lines) {
                // Parse the values into doubles
                double[] values = SPACE_PATTERN.splitAsStream(line).mapToDouble(Double::parseDouble).toArray();
                // The last value is Y, all before are Xs
                Y_VALUES[lineCount] = values[values.length - 1];
                for (int i = 0; i < values.length-1; i++) {
                    X_VALUES[lineCount][i] = values[i];
                }
                lineCount++;
            }
            // String lines are no longer needed
            lines.clear();
            // Preprocess the lines if we have a polynomial function
            if (functionType.equals("po")) polynomialPreprocess(degree);
            //centerData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Calculates b via OLS
     * b = (X' * X)^-1 * X' * y
     * @param X input variables
     * @param y target variable
     * @return coefficients b
     */
    private static double[] calculateCoefficients(double[][] X, double[] y) {
        double[][] transposeX = transpose(X);
        double[][] xTx =  matMul(transposeX, X);
        double[][] inverted = MatrixUtils.inverse(MatrixUtils.createRealMatrix(xTx)).getData();
        double[][] res = matMul(inverted, transposeX);
        return matMul(res, y);
    }

    /**
     * PolynomialPreprocess edits the data, so that we have Xj = X1^j,
     * where j > 1
     * @param guessedDegree the (approx.) degree of the polynomial
     */
    private static void polynomialPreprocess(int guessedDegree) {
        // Number of points inside the datafile
        int dataSize = Y_VALUES.length;

        // Populate the new lists with data based on X1
        for (int j = 0; j < dataSize; j++) {
            double x1 = X_VALUES[0][j];
            // Calculate every Xj, where j > 1
            // List indices start with 0, so Xi = X1^(i+1)
            int i=1;
            while (i<guessedDegree) {
                X_VALUES[i][j] = Math.pow(x1, i+1);
                i++;
            }
        }
    }

    /**
     * CenterData centers the data (deducts the average for each column)
     */
    private static void centerData() {
        // Center the Y_VALUES (deduct the average)
        double ySum = DoubleStream.of(Y_VALUES).sum();
        double yAvg = ySum / Y_VALUES.length;
        for (int i = 0; i < Y_VALUES.length; i++) {
            Y_VALUES[i] -= yAvg;
        }

        // Center the X_VALUES for each column
        // Calculate the average of each column
        double[] colAvg = new double[X_VALUES[0].length];
        for (int i = 0; i < X_VALUES.length; i++) {
            for (int j = 0; j < X_VALUES[0].length; j++) {
                colAvg[j] += X_VALUES[i][j];
            }
        }
        for (int i = 0; i < colAvg.length; i++) {
            colAvg[i] = colAvg[i] / X_VALUES.length;
        }
        // Deduct the average from each value
        for (int i = 0; i < X_VALUES.length; i++) {
            for (int j = 0; j < X_VALUES[0].length; j++) {
                X_VALUES[i][j] -= colAvg[j];
            }
        }
    }

    /**
     * The simplest matrix multiplication
     * @param A matrix with size n x m
     * @param B matrix with size m x p
     * @return AB matrix with size n x p
     */
    private static double[][] matMul(double[][] A, double[][] B) {
        // Result has A rows and B columns
        int n = A.length;
        int m = B.length;
        int p = B[0].length;
        double[][] result = new double[n][p];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < p; j++) {
                for (int k = 0; k < m; k++) {
                    result[i][j] += A[i][k] * B[k][j];
                }
            }
        }
        return result;
    }

    /**
     * Multiplies the matrix A with vector b
     * @param A matrix with size m x n
     * @param b vector with size n x 1
     * @return product Ab with size m x 1
     */
    private static double[] matMul(double[][] A, double[] b) {
        double[] result = new double[A.length];
        for (int i = 0; i < A.length; i++) {
            for (int j = 0; j < A[0].length; j++) {
                result[i] += A[i][j] * b[j];
            }
        }
        return result;
    }

    /**
     * Transposes the given matrix
     * @param A 2D matrix
     * @return A'
     */
    private static double[][] transpose(double [][] A) {
        int newRows = A[0].length;
        int newCols = A.length;
        double[][] result = new double[newRows][newCols];
        for (int i = 0; i < newRows; i++) {
            for (int j = 0; j < newCols; j++) {
                result[i][j] = A[j][i];
            }
        }
        return result;
    }
}
