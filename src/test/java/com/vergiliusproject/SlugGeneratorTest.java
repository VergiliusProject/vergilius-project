package com.vergiliusproject;

import com.vergiliusproject.utils.SlugGenerator;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class SlugGeneratorTest {
    @ParameterizedTest
    @CsvSource(value = { 
        "Windows 10:windows-10", 
        "Windows XP:windows-xp",
        "Windows 8.1 | Server 2012R2:windows-8.1",
        "Windows 8 | Server 2012:windows-8"
    }, delimiter = ':')
    public void testFamily(String input, String expected) {
        assertEquals(expected, SlugGenerator.create(input));
    }
    
    @ParameterizedTest
    @CsvSource(value = { 
        "RTM:rtm", 
        "SP3:sp3",
        "Update 1:update-1",
        "1809 | Server 2019 (October 2018 Update, Redstone 5):1809",
        "1909 (November 2019 Update, Titanium R2):1909",
        "23H2 (2023 Update, Nickel R2):23h2",
        "Insider Preview (Jun 2021):insider-preview-jun-2021"
    }, delimiter = ':')
    public void testOsName(String input, String expected) {
        assertEquals(expected, SlugGenerator.create(input));
    }
}
