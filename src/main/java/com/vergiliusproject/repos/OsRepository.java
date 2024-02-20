package com.vergiliusproject.repos;

import com.vergiliusproject.entities.Os;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface OsRepository extends CrudRepository<Os, Integer> {   
    @Query("select u from Os u where u.arch = :arch")
    List<Os> findOsByArch(@Param("arch") String arch);
    
    @Query("select u from Os u where u.oldfamily IS NOT NULL")
    List<Os> findWithOldFamilyNotNull();

    @Query("select u from Os u where u.arch = :arch and u.family = :family")
    List<Os> findByArchAndFamily(@Param("arch") String arch, @Param("family") String family);
    
    @Query("select u from Os u where u.arch = :arch and u.oldfamily = :oldfamily")
    List<Os> findByArchAndOldFamily(@Param("arch") String arch, @Param("oldfamily") String oldfamily);

    @Query("select u from Os u where u.arch = :arch and u.family = :family and u.osname = :osname")
    Os findByArchAndFamilyAndOsname(@Param("arch") String arch, @Param("family") String famname, @Param("osname") String osname);
    
    @Query("select u from Os u where u.arch = :arch and u.oldfamily = :oldfamily and u.oldosname = :oldosname")
    Os findByArchAndOldFamilyAndOldOsname(@Param("arch") String arch, @Param("oldfamily") String oldfamily, @Param("oldosname") String oldosname);
}