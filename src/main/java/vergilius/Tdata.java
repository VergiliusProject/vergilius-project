package vergilius;


import javax.persistence.*;

@Entity
public class Tdata {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Integer iddata;

    private Integer offset;

    private Integer id;

    @Column(name="dname")
    private String name;

    @Column(name="idordinal")
    private Integer ordinal;

    @ManyToOne
    @JoinColumn(name="Ttype_idtype")
    private Ttype ttype;

    public Integer getIddata() {
        return iddata;
    }

    public void setIddata(Integer iddata) {
        this.iddata = iddata;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(Integer ordinal) {
        this.ordinal = ordinal;
    }

    public Ttype getTtype() {
        return ttype;
    }

    public void setTtype(Ttype ttype) {
        this.ttype = ttype;
    }

    public String toString()
    {
        //return "" + getId();
        return "" + getName() + " " + getOffset()+ " " + getId();
    }
}