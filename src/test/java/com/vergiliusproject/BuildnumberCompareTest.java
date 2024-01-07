package com.vergiliusproject;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BuildnumberCompareTest {

    @Test
    public void firstIsMoreThanSecond() {
        assertTrue(1 == Buildnumber.compareBuildnumbers("7.0.6002.18005", "6.0.6002.18005"));

        assertTrue(1 == Buildnumber.compareBuildnumbers("6.1.6002.18005", "6.0.6002.18005"));

        assertTrue(1 == Buildnumber.compareBuildnumbers("6.0.6003.18005", "6.0.6002.18005"));
        assertTrue(1 == Buildnumber.compareBuildnumbers("6.0.6012.18005", "6.0.6002.18005"));
        assertTrue(1 == Buildnumber.compareBuildnumbers("6.0.6102.18005", "6.0.6002.18005"));
        assertTrue(1 == Buildnumber.compareBuildnumbers("6.0.7002.18005", "6.0.6002.18005"));

        assertTrue(1 == Buildnumber.compareBuildnumbers("6.0.6002.18006", "6.0.6002.18005"));
        assertTrue(1 == Buildnumber.compareBuildnumbers("6.0.6002.18015", "6.0.6002.18005"));
        assertTrue(1 == Buildnumber.compareBuildnumbers("6.0.6002.18105", "6.0.6002.18005"));
        assertTrue(1 == Buildnumber.compareBuildnumbers("6.0.6002.19005", "6.0.6002.18005"));
        assertTrue(1 == Buildnumber.compareBuildnumbers("6.0.6002.28005", "6.0.6002.18005"));

        assertTrue(1 == Buildnumber.compareBuildnumbers("1.0.0000.00000", "0.0.0000.00000"));
        assertTrue(1 == Buildnumber.compareBuildnumbers("0.2.0000.00000", "0.0.0000.00000"));
        assertTrue(1 == Buildnumber.compareBuildnumbers("0.0.0001.00000", "0.0.0000.00000"));
        assertTrue(1 == Buildnumber.compareBuildnumbers("0.0.0010.00000", "0.0.0000.00000"));
        assertTrue(1 == Buildnumber.compareBuildnumbers("0.0.0100.00000", "0.0.0000.00000"));
        assertTrue(1 == Buildnumber.compareBuildnumbers("0.0.1000.00000", "0.0.0000.00000"));

        assertTrue(1 == Buildnumber.compareBuildnumbers("0.0.0000.00001", "0.0.0000.00000"));
        assertTrue(1 == Buildnumber.compareBuildnumbers("0.0.0000.00010", "0.0.0000.00000"));
        assertTrue(1 == Buildnumber.compareBuildnumbers("0.0.0000.00100", "0.0.0000.00000"));
        assertTrue(1 == Buildnumber.compareBuildnumbers("0.0.0000.01000", "0.0.0000.00000"));
        assertTrue(1 == Buildnumber.compareBuildnumbers("0.0.0000.10000", "0.0.0000.00000"));

        //compares integers - not strings
        assertTrue(1 == Buildnumber.compareBuildnumbers("10.0.0000.10000", "9.0.0000.00000"));

        assertTrue(1 == Buildnumber.compareBuildnumbers("10.1.6000.40000", "10.1.3000.50000"));
    }

    @Test
    public void firstIsLessThanSecond()
    {
        assertTrue(-1 == Buildnumber.compareBuildnumbers("6.0.6002.18005", "7.0.6002.18005"));

        assertTrue(-1 == Buildnumber.compareBuildnumbers("6.0.6002.18005", "6.1.6002.18005"));

        assertTrue(-1 == Buildnumber.compareBuildnumbers("6.0.6002.18005", "6.0.6003.18005"));
        assertTrue(-1 == Buildnumber.compareBuildnumbers("6.0.6002.18005", "6.0.6012.18005"));
        assertTrue(-1 == Buildnumber.compareBuildnumbers("6.0.6002.18005", "6.0.6102.18005"));
        assertTrue(-1 == Buildnumber.compareBuildnumbers("6.0.6002.18005", "6.0.7002.18005"));

        assertTrue(-1 == Buildnumber.compareBuildnumbers("6.0.6002.18005", "6.0.6002.18006"));
        assertTrue(-1 == Buildnumber.compareBuildnumbers("6.0.6002.18005", "6.0.6002.18015"));
        assertTrue(-1 == Buildnumber.compareBuildnumbers("6.0.6002.18005", "6.0.6002.18105"));
        assertTrue(-1 == Buildnumber.compareBuildnumbers("6.0.6002.18005", "6.0.6002.19005"));
        assertTrue(-1 == Buildnumber.compareBuildnumbers("6.0.6002.18005", "6.0.6002.28005"));

        assertTrue(-1 == Buildnumber.compareBuildnumbers("0.0.0000.00000", "1.0.0000.00000"));
        assertTrue(-1 == Buildnumber.compareBuildnumbers("0.0.0000.00000", "0.2.0000.00000"));
        assertTrue(-1 == Buildnumber.compareBuildnumbers("0.0.0000.00000", "0.0.0001.00000"));
        assertTrue(-1 == Buildnumber.compareBuildnumbers("0.0.0000.00000", "0.0.0010.00000"));
        assertTrue(-1 == Buildnumber.compareBuildnumbers("0.0.0000.00000", "0.0.0100.00000"));
        assertTrue(-1 == Buildnumber.compareBuildnumbers("0.0.0000.00000", "0.0.1000.00000"));

        assertTrue(-1 == Buildnumber.compareBuildnumbers("0.0.0000.00000", "0.0.0000.00001"));
        assertTrue(-1 == Buildnumber.compareBuildnumbers("0.0.0000.00000", "0.0.0000.00010"));
        assertTrue(-1 == Buildnumber.compareBuildnumbers("0.0.0000.00000", "0.0.0000.00100"));
        assertTrue(-1 == Buildnumber.compareBuildnumbers("0.0.0000.00000", "0.0.0000.01000"));
        assertTrue(-1 == Buildnumber.compareBuildnumbers("0.0.0000.00000", "0.0.0000.10000"));

        //compares integers - not strings
        assertTrue(-1 == Buildnumber.compareBuildnumbers("9.0.0000.10000", "10.0.0000.00000"));

        assertTrue(-1 == Buildnumber.compareBuildnumbers("9.2.8000.10000", "10.2.9000.00000"));
    }

    @Test
    public void buildnumbersEqual()
    {
        assertEquals(0, Buildnumber.compareBuildnumbers("6.0.6002.18005", "6.0.6002.18005"));
        assertEquals(0, Buildnumber.compareBuildnumbers("1.1.1111.11111", "1.1.1111.11111"));
        assertEquals(0, Buildnumber.compareBuildnumbers("0.0.0000.00000", "0.0.0000.00000"));
    }
}
