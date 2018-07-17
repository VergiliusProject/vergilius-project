package vergilius;

import java.util.List;

public class Root {

    private String osname;
    private String family;
    private long timestamp;

    private String buildnumber;

    private int ordinal;

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

    public int getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
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
