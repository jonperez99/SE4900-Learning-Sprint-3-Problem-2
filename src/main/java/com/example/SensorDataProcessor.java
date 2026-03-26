package com.example;

import java.io.BufferedWriter;
import java.io.FileWriter;


public class SensorDataProcessor{

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
    public void calculate(double d) {

        int i, j, k;
        int iLen = data.length;
        int jLen = data[0].length;
        int kLen = data[0][0].length;
        
        double inverseD = 1.0 / d;
        double inverseKLen = 1.0 / kLen;
        
        double lowBound = 10.0 * kLen;
        double highBound = 50.0 * kLen;

        BufferedWriter out;

        // Write racing stats data into a file
        try {
            out = new BufferedWriter(new FileWriter("RacingStatsData.txt"), 65536);

            double[] currentData2 = new double[kLen];
            String currentData2Str = currentData2.toString();

            for (i = 0; i < iLen; i++) {
                double[][] dataI = data[i];
                double[] limitI = limit[i];

                for (j = 0; j < jLen; j++) {
                    double[] currentData = dataI[j];
                    double limitIJ = limitI[j];
                    double limitSquare = limitIJ * limitIJ;
                    
                    double avgData = Double.NaN;
                    double sum2 = 0.0;

                    for (k = 0; k < kLen; k++) {
                        double val = currentData[k];
                        double val2 = val * inverseD - limitSquare;
                        currentData2[k] = val2;
                        sum2 += val2;

                        if (val2 > val || (sum2 > lowBound && sum2 < highBound)) {
                            break;
                        } else if (Math.abs(val) < Math.abs(val2)) {
                            if (avgData != avgData) {
                                double tempSum = 0.0;
                                for (double cv : currentData) {
                                    tempSum += cv;
                                }
                                avgData = tempSum * inverseKLen;
                            }
                            if (avgData < val2) {
                                currentData2[k] = val2 * 2.0;
                                sum2 += val2;
                            }
                        }
                    }

                    out.write(currentData2Str);
                    out.write('\t');

                    int maxClean = (k < kLen) ? k : kLen - 1;
                    for (int z = 0; z <= maxClean; z++) {
                        currentData2[z] = 0.0;
                    }
                }
            }

            out.close();

        } catch (Exception e) {
            System.out.println("Error= " + e);
        }
    }
    
}
