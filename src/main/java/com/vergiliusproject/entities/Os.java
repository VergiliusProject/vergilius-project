package com.vergiliusproject.entities;

import com.vergiliusproject.utils.SlugGenerator;
import jakarta.persistence.*;
import java.text.SimpleDateFormat;
import java.util.*;
import lombok.Data;
import org.apache.commons.lang3.builder.CompareToBuilder;

@Entity
@Data
public class Os implements Comparable<Os> {
    @Id
    @Column(length=16)
    private UUID id;

    @Column(length=32)
    private String familyName;
    
    @Column(length=64)
    private String osName;
    
    @Column(length=32)
    private String oldFamilyName;
    
    @Column(length=64)
    private String oldOsName;
    
    @Column(length=32)
    private String familySlug;
    
    @Column(length=64)
    private String osSlug;
    
    @Column(length=32)
    private String buildnumber;
    
    @Column(length=3)
    private String arch; // name and slug are the same
    
    @Column(name="n_timestamp") // timestamp is an SQL reserver keyword, so rename the column
    private long timestamp;
    
    @OneToMany(mappedBy = "opersys", cascade = CascadeType.ALL)
    private Set<Ttype> ttypes;
    
    @PrePersist
    protected void onCreation() {
        if (id == null) {
            id = UUID.nameUUIDFromBytes((buildnumber + arch).getBytes());
        }
        
        if (familySlug == null) {
            familySlug = SlugGenerator.create(familyName);
        }
        
        if (osSlug == null) {
            osSlug = SlugGenerator.create(osName);
        }
        
        if (oldFamilyName != null && oldOsName == null ||
            oldFamilyName == null && oldOsName != null) {
            throw new IllegalStateException("oldFamilyName and oldOsName should be both null or not null!");
        }
    }

    public String convertTimestampToDate(long timestamp) {
        Date date = new Date(timestamp*1000L);
        SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd");
        return jdf.format(date);
    }

    @Override
    public int compareTo(Os that) {
        var thisNumbers = Arrays.stream(this.getBuildnumber().split("\\.")).map(Integer::parseInt).toArray();
        var thatNumbers = Arrays.stream(that.getBuildnumber().split("\\.")).map(Integer::parseInt).toArray();

        return new CompareToBuilder()
            .append(thisNumbers, thatNumbers)
            .toComparison();
    }
}