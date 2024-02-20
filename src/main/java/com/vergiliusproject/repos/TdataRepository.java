package com.vergiliusproject.repos;

import com.vergiliusproject.entities.Tdata;
import com.vergiliusproject.entities.Ttype;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface TdataRepository extends CrudRepository<Tdata, Integer> {
    List<Tdata> findByTtype(Ttype ttype);
}