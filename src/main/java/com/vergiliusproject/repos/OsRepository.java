package com.vergiliusproject.repos;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import com.vergiliusproject.Os;

import java.util.List;

public interface OsRepository extends CrudRepository<Os, Integer> {
    //operating systems search by arch !!!
    @Query("select u from Os u where u.arch = :arch")
    List<Os> findOsByArch(@Param("arch") String arch);

    @Query("select u from Os u where u.arch = :arch and u.family = :famname")
    List<Os> findByArchAndFamily(@Param("arch") String arch, @Param("famname") String famname);

    @Query("select u from Os u where u.arch = :arch and u.family = :famname and u.osname = :osname")
    Os findByArchAndFamilyAndOsname(@Param("arch") String arch, @Param("famname") String famname, @Param("osname") String osname);
}