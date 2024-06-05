package com.vergiliusproject.utils;

import org.apache.commons.lang3.StringUtils;

public class SlugGenerator {
    public static String create(String input) {
        // Special case: insider preview versions
        if (StringUtils.containsIgnoreCase(input, "preview")) {
            // Remove '(' and ')', replace spaces and make lowercase
            return input.replaceAll("[()]", "").trim().replace(' ', '-').toLowerCase();
        }
        
        // Take the first words until '|' or '(', replace spaces and make lowercase
        return input.split("[\\|(]")[0].trim().replace(' ', '-').toLowerCase();
    }
}
