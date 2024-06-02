package com.vergiliusproject;

import com.vergiliusproject.entities.Os;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class OsCompareToTest {
    @ParameterizedTest
    @CsvSource(value = { 
        "2.2.2.2:2.2.2.10",
        "2.2.2.2:2.2.10.2",
        "2.2.2.2:2.10.2.2",
        "2.2.2.2:10.2.2.2"
    }, delimiter = ':')
    public void testLess(String leftBuildNumber, String rightBuildNumber) {
        var left = new Os();
        left.setBuildnumber(leftBuildNumber);
        
        var right = new Os();
        right.setBuildnumber(rightBuildNumber);
        
        assertEquals(-1, left.compareTo(right));
    }
    
    @ParameterizedTest
    @CsvSource(value = { 
        "2.2.2.2:2.2.2.2",
        "0.0.0.0:0.0.0.0"
    }, delimiter = ':')
    public void testEqual(String leftBuildNumber, String rightBuildNumber) {
        var left = new Os();
        left.setBuildnumber(leftBuildNumber);
        
        var right = new Os();
        right.setBuildnumber(rightBuildNumber);
        
        assertEquals(0, left.compareTo(right));
    }
    
    @ParameterizedTest
    @CsvSource(value = { 
        "2.2.2.10:2.2.2.2",
        "2.2.10.2:2.2.2.2",
        "2.10.2.2:2.2.2.2",
        "10.2.2.2:2.2.2.2"
    }, delimiter = ':')
    public void testGreater(String leftBuildNumber, String rightBuildNumber) {
        var left = new Os();
        left.setBuildnumber(leftBuildNumber);
        
        var right = new Os();
        right.setBuildnumber(rightBuildNumber);
        
        assertEquals(1, left.compareTo(right));
    }
}
