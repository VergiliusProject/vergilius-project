package com.vergiliusproject.repos;

import com.vergiliusproject.entities.Os;
import jakarta.persistence.QueryHint;
import java.util.List;
import static org.hibernate.jpa.HibernateHints.HINT_CACHEABLE;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface OsRepository extends CrudRepository<Os, Integer> {   
    @QueryHints(@QueryHint(name = HINT_CACHEABLE, value = "true"))
    @Query("select u from Os u where u.arch = :arch")
    List<Os> findByArch(@Param("arch") String arch);
    
    @QueryHints(@QueryHint(name = HINT_CACHEABLE, value = "true"))
    @Query("select u from Os u where u.oldFamilyName IS NOT NULL")
    List<Os> findByOldFamilyNameNotNull();

    @QueryHints(@QueryHint(name = HINT_CACHEABLE, value = "true"))
    @Query("select u from Os u where u.arch = :arch and u.familySlug = :familySlug")
    List<Os> findByArchAndFamilySlug(@Param("arch") String arch, @Param("familySlug") String familySlug);
    
    @QueryHints(@QueryHint(name = HINT_CACHEABLE, value = "true"))
    @Query("select u from Os u where u.arch = :arch and u.oldFamilyName = :oldFamilyName")
    List<Os> findByArchAndOldFamilyName(@Param("arch") String arch, @Param("oldFamilyName") String oldFamilyName);

    @QueryHints(@QueryHint(name = HINT_CACHEABLE, value = "true"))
    @Query("select u from Os u where u.arch = :arch and u.familySlug = :familySlug and u.osSlug = :osSlug")
    Os findByArchAndFamilySlugAndOsSlug(@Param("arch") String arch, @Param("familySlug") String familySlug, @Param("osSlug") String osSlug);
    
    @QueryHints(@QueryHint(name = HINT_CACHEABLE, value = "true"))
    @Query("select u from Os u where u.arch = :arch and u.oldFamilyName = :oldFamilyName and u.oldOsName = :oldOsName")
    Os findByArchAndOldFamilyNameAndOldOsName(@Param("arch") String arch, @Param("oldFamilyName") String oldFamilyName, @Param("oldOsName") String oldOsName);
}