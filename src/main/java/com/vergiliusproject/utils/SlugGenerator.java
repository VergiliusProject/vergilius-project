package com.vergiliusproject.utils;

public class SlugGenerator {
    public static String create(String input) {
        // Take the first words until '|' or '(', replace spaces and make lowercase
        return input.split("[\\|(]")[0].trim().replace(' ', '-').toLowerCase();
    }
}
