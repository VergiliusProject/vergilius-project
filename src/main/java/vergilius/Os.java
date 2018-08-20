package vergilius;

import javax.persistence.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Entity
public class Os implements Comparator<Os>{
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

    private String arch;

    public String getBuildnumber() {
        return buildnumber;
    }

    public void setBuildnumber(String buildnumber) {
        this.buildnumber = buildnumber;
    }

    public String getArch() {
        return arch;
    }

    public void setArch(String arch) {
        this.arch = arch;
    }

    public String convertTimestampToDate(long timestamp)
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

    @Override
    public int compare(Os obj1, Os obj2)
    {
        List<Integer> arrObj1 = Arrays.stream(obj1.getBuildnumber().split("\\.")).map(Integer::parseInt).collect(Collectors.toList());
        List<Integer> arrObj2 = Arrays.stream(obj2.getBuildnumber().split("\\.")).map(Integer::parseInt).collect(Collectors.toList());

        //does bildnumber always look like X.X.X.X ?
        if(arrObj1.get(0) > arrObj2.get(0))
            return 1;
        else if(arrObj1.get(0) < arrObj2.get(0))
            return -1;
        else
        {
            if(arrObj1.get(1) > arrObj2.get(1))
                return 1;
            else if(arrObj1.get(1) < arrObj2.get(1))
                return -1;
            else
            {
                if(arrObj1.get(2) > arrObj2.get(2))
                    return 1;
                else if(arrObj1.get(2) < arrObj2.get(2))
                    return -1;
                else
                {
                    if(arrObj1.get(3) > arrObj2.get(3))
                        return 1;
                    else if(arrObj1.get(3) < arrObj2.get(3))
                        return -1;
                    else return 0;
                }
            }
        }
    }
}