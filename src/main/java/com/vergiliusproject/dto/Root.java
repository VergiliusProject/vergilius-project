package com.vergiliusproject.dto;

import com.vergiliusproject.entities.Ttype;
import java.util.List;
import lombok.Data;

@Data
public class Root {
    private String family;
    private String osname;
    private String oldfamily;
    private String oldosname;
    private String buildnumber;
    private String arch;
    private long timestamp;
    private List<Ttype> types;
}