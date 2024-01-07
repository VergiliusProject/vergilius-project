package com.vergiliusproject;

import java.util.List;

public class Root {
    private String osname;
    private String family;
    private long timestamp;

    private String buildnumber;

    private String arch;

    public String getOsname() {
        return osname;
    }

    public void setOsname(String osname) {
        this.osname = osname;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }


    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public void setArch(String arch) {
        this.arch = arch;
    }

    public String getArch() {
        return arch;
    }

    public String getBuildnumber() {
        return buildnumber;
    }

    public void setBuildnumber(String buildnumber) {
        this.buildnumber = buildnumber;
    }

    private List<Ttype> types;

    public List<Ttype> getTypes() {
        return types;
    }

    public void setTypes(List<Ttype> types) {
        this.types = types;
    }
}