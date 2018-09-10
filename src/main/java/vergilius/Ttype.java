package vergilius;

import javax.persistence.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

//EXAMPLE:
/*@Table(name    = "company__activity",
        indexes = {@Index(name = "i_company_activity", columnList = "activity_id,company_id")})*/
@Entity
public class Ttype {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Integer idtype;

    @Column(name="tname")
    private String name;

    private Integer id;

    @Column(name="size")
    private Integer sizeof;

    public enum Kind {
        STRUCT, ENUM, UNION, ARRAY, BASE, POINTER, FUNCTION;

        public String toString() {
            String lowercase = name().toLowerCase(java.util.Locale.US);
            return lowercase;
        }
    }
    @Column(name="kind")
    @Enumerated(EnumType.STRING)
    private Kind kind;

    @Column(name="const")
    private Boolean isConst = false;

    @Column(name="volatile")
    private Boolean isVolatile = false;

    //relationship with Os
    @ManyToOne
    @JoinColumn(name = "Operating_system_idopersys")
    private Os opersys;

    //relationship with Tdata
    @OneToMany(mappedBy="ttype", cascade = CascadeType.ALL)
    private Set<Tdata> data;

    public Integer getIdtype() {
        return idtype;
    }

    public void setIdtype(Integer idtype) {
        this.idtype = idtype;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSizeof() {
        return sizeof;
    }

    public void setSizeof(Integer sizeof) {
        this.sizeof = sizeof;
    }

    public Kind getKind()
    {
       return kind;
    }

    public void setKind(Kind kind) {

        this.kind = kind;
    }

    public Os getOpersys() {
        return opersys;
    }

    public void setOpersys(Os opersys) {
        this.opersys = opersys;
    }

    public void setData(Set<Tdata> data) {
        this.data = data;
    }

    public Set<Tdata> getData() {
        return data;
    }

    public boolean isIsConst() {
        return (isConst == null || isConst == false)? false: true;
    }


    public boolean isIsVolatile() {
       return (isVolatile == null || isVolatile == false)? false: true;
    }


    public static List<Ttype>FilterByTypes(List<Ttype> list, Kind param)
    {
        List<Ttype> retVal = new ArrayList<>();
        for(Ttype i: list)
        {
            String name = i.getName();
            if(name != null && !name.equals("<unnamed-tag>") && i.getKind() == param)
            {
                retVal.add(i);
            }
        }
        return retVal;
    }
}


