package vergilius;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Set;

@Entity
public class Structure {
    @Id
    private Integer idstruct;

    private String structname;

    private Integer strsize;

    public enum Kind {
        STRUCT, ENUM, UNION;

        public String toString() {
            String lowercase = name().toLowerCase(java.util.Locale.US);
            return lowercase;
        }
    }
    @Column(name="strkind")
    @Enumerated(EnumType.STRING)
    private Kind strkind;

    //relationship with Os
    @ManyToOne
    @JoinColumn(name = "Operating_system_idopersys")
    private Os opersys;

    //relationship with Field
    @OneToMany(mappedBy="structure", cascade = CascadeType.ALL)
    private Set<Field> fields;

    public Integer getIdstruct() {
        return idstruct;
    }

    public void setIdstruct(Integer idstruct) {
        this.idstruct = idstruct;
    }

    public String getStructname() {
        return structname;
    }

    public void setStructname(String structname) {
        this.structname = structname;
    }

    public Integer getStrsize() {
        return strsize;
    }

    public void setStrsize(Integer strsize) {
        this.strsize = strsize;
    }

    public Kind getStrkind()
    {
       return strkind;
    }

    public void setStrkind(Kind strkind) {
        this.strkind = strkind;
    }

    public Os getOpersys() {
        return opersys;
    }

    public void setOpersys(Os opersys) {
        this.opersys = opersys;
    }

    public void setFields(Set<Field> fields) {
        this.fields = fields;
    }

    public Set<Field> getFields() {
        return fields;
    }

    public String toString()
    {
        String s ="";
        return s+ getIdstruct();
    }
}


