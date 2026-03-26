package com.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SensorDataProcessor targeting 100% statement and branch coverage.
 *
 * Branches inside calculate() per inner-k-loop iteration:
 *   B1:      average(data2[i][j]) > 10 && average(data2[i][j]) < 50  → break
 *   B1-L:    average > 10 but >= 50  (left true, right false)         → falls to B2
 *   B2:      Math.max(data[k], data2[k]) > data[k]  (data2 > data)   → break
 *   B3:      |data|³ < |data2|³ && avg(data) < data2 && (i+1)(j+1)>0 → data2 *= 2
 *   B3-L:    |data|³ < |data2|³ but avg(data) >= data2               → falls to B4
 *   B4:      else → continue
 *   EX:      IOException catch block
 */
public class SensorDataProcessorTest {

    private static final Path OUT = Paths.get("RacingStatsData.txt");

    @BeforeEach
    void cleanupBefore() throws Exception {
        Files.deleteIfExists(OUT);
    }

    @AfterEach
    void cleanupAfter() throws Exception {
        // If the test left a directory instead of a file, delete it too
        if (Files.exists(OUT) && Files.isDirectory(OUT)) {
            Files.delete(OUT);
        } else {
            Files.deleteIfExists(OUT);
        }
    }

    // ------------------------------------------------------------------
    // B1 TRUE  –  average(data2[i][j]) in (10, 50)  →  break
    // data=[[[30,30]]], limit=[[0]], d=1
    //   data2[0][0][0] = 30/1 - 0 = 30
    //   average(data2[0][0]) = (30 + 0) / 2 = 15  →  15 > 10 && 15 < 50  → TRUE
    // ------------------------------------------------------------------
    @Test
    void testB1BothTrue_BreakOnFirstCondition() throws Exception {
        double[][][] data  = {{{30.0, 30.0}}};
        double[][]   limit = {{0.0}};
        SensorDataProcessor p = new SensorDataProcessor(data, limit);
        p.calculate(1.0);
        assertTrue(Files.exists(OUT), "Output file should be created");
        assertFalse(Files.readString(OUT).isEmpty(), "Output file should not be empty");
    }

    // ------------------------------------------------------------------
    // B1 LEFT=TRUE, RIGHT=FALSE  (average >= 50)  →  falls through to B2 TRUE
    // data=[[[200,200]]], limit=[[0]], d=0.5
    //   data2[0][0][0] = 200/0.5 - 0 = 400
    //   average(data2[0][0]) = (400 + 0) / 2 = 200  →  >10 TRUE, <50 FALSE
    //   B2: max(data=200, data2=400) = 400 > 200  →  break
    // ------------------------------------------------------------------
    @Test
    void testB1LeftTrueRightFalse_ThenB2True() throws Exception {
        double[][][] data  = {{{200.0, 200.0}}};
        double[][]   limit = {{0.0}};
        SensorDataProcessor p = new SensorDataProcessor(data, limit);
        p.calculate(0.5);
        assertTrue(Files.exists(OUT), "Output file should be created");
    }

    // ------------------------------------------------------------------
    // B2 TRUE  (average <= 10, but data2 > data)  →  break
    // data=[[[5,5]]], limit=[[0]], d=0.5
    //   data2[0][0][0] = 5/0.5 - 0 = 10
    //   average(data2[0][0]) = (10 + 0) / 2 = 5   →  5 > 10 FALSE  (B1 false)
    //   B2: max(data=5, data2=10) = 10 > 5  →  break
    // ------------------------------------------------------------------
    @Test
    void testB2True_B1False_BreakOnSecondCondition() throws Exception {
        double[][][] data  = {{{5.0, 5.0}}};
        double[][]   limit = {{0.0}};
        SensorDataProcessor p = new SensorDataProcessor(data, limit);
        p.calculate(0.5);
        assertTrue(Files.exists(OUT), "Output file should be created");
    }

    // ------------------------------------------------------------------
    // B3 TRUE  –  all three sub-conditions true  →  data2[k] *= 2
    // data=[[[−1, −200]]], limit=[[0]], d=0.01
    //   data2[0][0][0] = -1/0.01 - 0 = -100
    //   average(data2[0][0]) = (-100 + 0)/2 = -50   →  -50>10 FALSE (B1 false)
    //   B2: max(data=-1, data2=-100) = -1 = data     →  NOT > data (B2 false)
    //   B3 part1: |-1|³=1 < |-100|³=1e6             →  TRUE
    //   B3 part2: avg(data[0][0]) = (-1+-200)/2=-100.5 < data2=-100  →  TRUE
    //   B3 part3: (0+1)*(0+1)=1 > 0                 →  TRUE
    //   → data2[0][0][0] *= 2
    // ------------------------------------------------------------------
    @Test
    void testB3True_MultiplyByTwo() throws Exception {
        double[][][] data  = {{{-1.0, -200.0}}};
        double[][]   limit = {{0.0}};
        SensorDataProcessor p = new SensorDataProcessor(data, limit);
        p.calculate(0.01);
        assertTrue(Files.exists(OUT), "Output file should be created");
    }

    // ------------------------------------------------------------------
    // B3 PARTIAL (left=true, right=false)  –  |data|³ < |data2|³ but avg(data) >= data2  → B4/else
    // data=[[[−100, −100]]], limit=[[0]], d=0.01
    //   data2[0][0][0] = -100/0.01 = -10000
    //   average(data2[0][0]) = -5000   →  B1: -5000>10 FALSE
    //   B2: max(-100, -10000) = -100 = data  →  NOT > data (B2 false)
    //   B3 part1: |-100|³=1e6 < |-10000|³=1e12   →  TRUE
    //   B3 part2: avg(data[0][0]) = -100 < data2=-10000 ?  -100 < -10000 FALSE → short-circuit
    //   → else (continue)
    // ------------------------------------------------------------------
    @Test
    void testB3Part1TruePart2False_FallsToElse() throws Exception {
        double[][][] data  = {{{-100.0, -100.0}}};
        double[][]   limit = {{0.0}};
        SensorDataProcessor p = new SensorDataProcessor(data, limit);
        p.calculate(0.01);
        assertTrue(Files.exists(OUT), "Output file should be created");
    }

    // ------------------------------------------------------------------
    // B4 / else (continue)  –  all conditions false, simple continue
    // data=[[[1,1]]], limit=[[0]], d=10
    //   data2[0][0][0] = 1/10 - 0 = 0.1
    //   average(data2[0][0]) = 0.05   →  B1: 0.05>10 FALSE
    //   B2: max(data=1, data2=0.1)=1 = data  →  NOT > data (false)
    //   B3 part1: |1|³=1 < |0.1|³=0.001 ?  FALSE  →  else (continue)
    // ------------------------------------------------------------------
    @Test
    void testB4Else_Continue() throws Exception {
        double[][][] data  = {{{1.0, 1.0}}};
        double[][]   limit = {{0.0}};
        SensorDataProcessor p = new SensorDataProcessor(data, limit);
        p.calculate(10.0);
        assertTrue(Files.exists(OUT), "Output file should be created");
    }

    // ------------------------------------------------------------------
    // EXCEPTION BRANCH  –  IOException caught inside calculate()
    // Creating a directory named "RacingStatsData.txt" prevents FileWriter from
    // opening a file with that name, causing an IOException caught internally.
    // ------------------------------------------------------------------
    @Test
    void testIOExceptionCaught() throws Exception {
        // Create a directory with the same name as the output file to trigger IOException
        Files.createDirectories(OUT);
        assertTrue(Files.isDirectory(OUT), "Pre-condition: path is a directory");

        double[][][] data  = {{{1.0, 1.0}}};
        double[][]   limit = {{0.0}};
        SensorDataProcessor p = new SensorDataProcessor(data, limit);

        // calculate() must not throw – the IOException is swallowed internally
        assertDoesNotThrow(() -> p.calculate(1.0));
        assertTrue(Files.isDirectory(OUT), "Directory should still exist (no file was created)");
    }

    // ------------------------------------------------------------------
    // MULTIPLE i,j ITERATIONS  –  ensures outer loops are also covered
    // Uses a [2][2][2] tensor so the i and j loops iterate more than once.
    // ------------------------------------------------------------------
    @Test
    void testMultipleSensorsAndReadings() throws Exception {
        double[][][] data = {
            {{30.0, 30.0}, {5.0, 5.0}},
            {{1.0,  1.0},  {-1.0, -200.0}}
        };
        double[][] limit = {{0.0, 0.0}, {0.0, 0.0}};
        SensorDataProcessor p = new SensorDataProcessor(data, limit);
        p.calculate(1.0);
        assertTrue(Files.exists(OUT), "Output file should be created for multi-sensor run");
    }
}
