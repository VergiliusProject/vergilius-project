package com.vergiliusproject.repos;

import com.vergiliusproject.entities.Os;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface OsRepository extends CrudRepository<Os, Integer> {   
    @Query("select u from Os u where u.arch = :arch")
    List<Os> findByArch(@Param("arch") String arch);
    
    @Query("select u from Os u where u.oldFamilyName IS NOT NULL")
    List<Os> findByOldFamilyNameNotNull();

    @Query("select u from Os u where u.arch = :arch and u.familySlug = :familySlug")
    List<Os> findByArchAndFamilySlug(@Param("arch") String arch, @Param("familySlug") String familySlug);
    
    @Query("select u from Os u where u.arch = :arch and u.oldFamilyName = :oldFamilyName")
    List<Os> findByArchAndOldFamilyName(@Param("arch") String arch, @Param("oldFamilyName") String oldFamilyName);

    @Query("select u from Os u where u.arch = :arch and u.familySlug = :familySlug and u.osSlug = :osSlug")
    Os findByArchAndFamilySlugAndOsSlug(@Param("arch") String arch, @Param("familySlug") String familySlug, @Param("osSlug") String osSlug);
    
    @Query("select u from Os u where u.arch = :arch and u.oldFamilyName = :oldFamilyName and u.oldOsName = :oldOsName")
    Os findByArchAndOldFamilyNameAndOldOsName(@Param("arch") String arch, @Param("oldFamilyName") String oldFamilyName, @Param("oldOsName") String oldOsName);
}