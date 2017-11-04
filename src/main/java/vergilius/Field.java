package vergilius;


import javax.persistence.*;


@Entity
public class Field {
    @Id
    private Integer idfield;

    private Integer offset;

    private Integer idtype;

    private String fname;

    private boolean ispointer;

    private boolean isconst;

    private boolean isvolatile;


    @ManyToOne
    @JoinColumn(name="Structure_idstruct")
    private Structure structure;

    public Integer getIdfield() {
        return idfield;
    }

    public void setIdfield(Integer idfield) {
        this.idfield = idfield;
    }

    public String getFname() {
        return fname;
    }

    public void setFname(String fname) {
        this.fname = fname;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Integer getIdtype() {
        return idtype;
    }

    public void setIdtype(Integer idtype) {
        this.idtype = idtype;
    }

    public Structure getStructure() {
        return structure;
    }

    public void setStructure(Structure structure) {
        this.structure = structure;
    }

    public boolean isIsconst() {
        return isconst;
    }

    public void setIsconst(boolean isconst) {
        this.isconst = isconst;
    }

    public boolean isIspointer() {
        return ispointer;
    }

    public void setIspointer(boolean ispointer) {
        this.ispointer = ispointer;
    }

    public boolean isIsvolatile() {
        return isvolatile;
    }

    public void setIsvolatile(boolean isvolatile) {
        this.isvolatile = isvolatile;
    }
}