package vergilius;

import javax.persistence.*;
import java.util.Set;

@Entity
public class Os{
    @Id
    private Integer idopersys;

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

    public String toString()
    {
        return "" + getIdopersys();

    }
}