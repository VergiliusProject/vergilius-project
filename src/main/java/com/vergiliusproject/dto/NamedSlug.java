package com.vergiliusproject.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NamedSlug {
    private final String name;
    private final String slug;
}
