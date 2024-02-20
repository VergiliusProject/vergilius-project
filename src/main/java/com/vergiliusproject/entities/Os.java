package com.vergiliusproject.entities;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import javax.persistence.*;
import lombok.Data;
import org.apache.commons.lang3.builder.CompareToBuilder;

@Entity
@Data
public class Os implements Comparator<Os> {
    @Id
    @Column(length=16)
    private UUID idopersys;

    @Column(length=32)
    private String family;
    
    @Column(length=64)
    private String osname;
    
    @Column(length=32)
    private String oldfamily;
    
    @Column(length=64)
    private String oldosname;
    
    @Column(length=32)
    private String buildnumber;
    
    @Column(length=3)
    private String arch;
    
    @Column(name="n_timestamp") // timestamp is an SQL reserver keyword, so rename the column
    private long timestamp;
    
    @OneToMany(mappedBy = "opersys", cascade = CascadeType.ALL)
    private Set<Ttype> ttypes;
    
    @PrePersist
    protected void onCreation() {
        if (idopersys == null) {
            idopersys = UUID.nameUUIDFromBytes((buildnumber + arch).getBytes());
        }
        
        if (oldfamily != null && oldosname == null ||
            oldfamily == null && oldosname != null) {
            throw new IllegalStateException("oldfamily and oldosname should be both null or not null!");
        }
    }

    public String convertTimestampToDate(long timestamp) {
        Date date = new Date(timestamp*1000L);
        SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd");
        return jdf.format(date);
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