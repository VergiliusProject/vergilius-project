package vergilius;

import javax.persistence.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

@Entity
public class Os{
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    //@Column(updatable = false, nullable = false, unique = true)
    private int idopersys;

    private String osname;

    @OneToMany(mappedBy = "opersys", cascade = CascadeType.ALL)
    private Set<Ttype> ttypes;

    public int getIdopersys() {
        return idopersys;
    }

    public void setIdopersys(Integer idopersys) {
        this.idopersys = idopersys;
    }

    public String getOsname() {
        return osname;
    }

    public void setOsname(String osname) {
        this.osname = osname;
    }


    public Set<Ttype> getTypes() {
        return ttypes;
    }

    public void setTypes(Set<Ttype> ttypes) {
        this.ttypes = ttypes;
    }


    private String family;

    private long timestamp;

    private String buildnumber;

    private int ordinal;

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

    public String convertTimestamptoDate(long timestamp)
    {
        //convert seconds to milliseconds
        Date date = new Date(timestamp*1000L);
        // format of the date
        //SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd");
        return jdf.format(date);
    }

    public String getFamily() {

        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}