package vergilius;

import javax.persistence.*;
import java.util.Set;

@Entity
public class Os{
    @Id
    private Integer idopersys;

    private String osname;

    @OneToMany(mappedBy = "opersys", cascade = CascadeType.ALL)
    private Set<Structure> structures;

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

    public Set<Structure> getStructures() {
        return structures;
    }

    public void setStructures(Set<Structure> structures) {
        this.structures = structures;
    }

    public String toString()
    {
        String s ="";
        return s + getIdopersys();
    }
}