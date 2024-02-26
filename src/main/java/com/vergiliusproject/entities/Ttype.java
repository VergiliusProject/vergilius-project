package com.vergiliusproject.entities;

import jakarta.persistence.*;
import java.util.List;
import java.util.Set;

@Table(name = "ttype",
       indexes = {@Index(name = "indexTtype", columnList = "id, Operating_system_idopersys")})
@Entity
public class Ttype {
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="ttype_seq")
    @SequenceGenerator(name="ttype_seq", allocationSize=500)
    private Integer idtype;

    @Column(name="tname")
    private String name;

    private Integer id;

    @Column(name="size")
    private Integer sizeof;

    public enum Kind {
        STRUCT, ENUM, UNION, ARRAY, BASE, POINTER, FUNCTION;

        @Override
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

    public Kind getKind() {
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
        return (isConst != null && isConst != false);
    }

    public boolean isIsVolatile() {
       return (isVolatile != null && isVolatile != false);
    }

    public static List<Ttype> filterByTypes(List<Ttype> list, Kind param) {
        return list.stream().filter(x -> { 
            String name = x.getName();
            return name != null && !name.equals("<unnamed-tag>") && !name.equals("__unnamed") && x.getKind() == param;
        }).toList();
    }
}