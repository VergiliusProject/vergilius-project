package com.vergiliusproject.entities;

import jakarta.persistence.*;

@Table(indexes = {@Index(columnList = "id, Ttype_idtype")})
@Entity
public class Tdata {
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="tdata_seq")
    @SequenceGenerator(name="tdata_seq", allocationSize=500)
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

    @Override
    public String toString() {
        return "" + getName() + " " + getOffset()+ " " + getId();
    }
}