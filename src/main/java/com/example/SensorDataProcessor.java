package com.example;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class SensorDataProcessor {

    // Senson data and limits.
    public double[][][] data;
    public double[][] limit;

    // constructor
    public SensorDataProcessor(double[][][] data, double[][] limit) {
        this.data = data;
        this.limit = limit;
    }

    // calculates average of sensor data
    private double average(double[] array) {
        int i = 0;
        double val = 0;
        for (i = 0; i < array.length; i++) {
            val += array[i];
        }

        return val / array.length;
    }

    // calculate data

    /*
     * manual improvements to the original
     * 
     * 1. put iLength, jLength, kLength as variables to reduce the amount of times
     * .length is called
     * 
     * 2. took away math.pow statements in the math.abs comparisons, comparisons
     * would be the same if they were removed
     * 
     * 3. average(data2[i][j]) is called multiple times, put it into a variable to
     * reduce times it is called.
     * 
     * 4. Else continue is redundant, removed it.
     * 
     * 5. Math.max(data[i][j][k], data2[i][j][k]) > data[i][j][k] can be simplified
     * to data2[i][j][k] > data[i][j][k]:
     * 
     * | Math.max(x, y) > x | y > x
     * -------------------------------------------------------------------
     * y < x | false | false
     * | |
     * y == x | false | false
     * | |
     * y > x | true | true
     * 
     */
    public void calculate(double d) {

        int i, j, k = 0;
        int iLength = data.length;
        int jLength = data[0].length;
        int kLength = data[0][0].length;

        double[][][] data2 = new double[iLength][jLength][kLength];

        BufferedWriter out;

        // Write racing stats data into a file
        try {
            out = new BufferedWriter(new FileWriter("RacingStatsData.txt"));

            for (i = 0; i < iLength; i++) {
                for (j = 0; j < jLength; j++) {
                    for (k = 0; k < kLength; k++) {
                        data2[i][j][k] = data[i][j][k] / d - Math.pow(limit[i][j], 2.0);

                        double averageData2 = average(data2[i][j]);

                        if (averageData2 > 10 && averageData2 < 50)
                            break;
                        else if (data2[i][j][k] > data[i][j][k])
                            break;
                        else if (Math.abs(data[i][j][k]) < Math.abs(data2[i][j][k])
                                && average(data[i][j]) < data2[i][j][k] && (i + 1) * (j + 1) > 0)
                            data2[i][j][k] *= 2;
                    }
                }
            }

            for (i = 0; i < data2.length; i++) {
                for (j = 0; j < data2[0].length; j++) {
                    out.write(data2[i][j] + "\t");
                }
            }

            out.close();

        } catch (Exception e) {
            System.out.println("Error= " + e);
        }
    }

}
