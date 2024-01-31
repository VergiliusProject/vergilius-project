package com.vergiliusproject.entities;

import org.apache.commons.lang3.builder.CompareToBuilder;
import javax.persistence.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Entity
public class Os implements Comparator<Os> {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)

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

    public String convertTimestampToDate(long timestamp) {
        Date date = new Date(timestamp*1000L);
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
    public int compare(Os obj1, Os obj2) {
        List<Integer> arrObj1 = Arrays.stream(obj1.getBuildnumber().split("\\.")).map(Integer::parseInt).collect(Collectors.toList());
        List<Integer> arrObj2 = Arrays.stream(obj2.getBuildnumber().split("\\.")).map(Integer::parseInt).collect(Collectors.toList());

        return new CompareToBuilder()
                .append(arrObj1.get(0), arrObj2.get(0))
                .append(arrObj1.get(1), arrObj2.get(1))
                .append(arrObj1.get(2), arrObj2.get(2))
                .append(arrObj1.get(3), arrObj2.get(3))
                .toComparison();
    }
}