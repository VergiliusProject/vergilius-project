package vergilius.repos;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import vergilius.Os;
import vergilius.Ttype;

import java.util.List;


public interface TtypeRepository extends CrudRepository<Ttype, Integer> {

    //@Query("select u from Ttype u where u.opersys = :opersys")
    List<Ttype> findByOpersysAndIsConstFalseAndIsVolatileFalse(@Param("opersys") Os opersys);

    //List<Ttype> findByNameAndOpersys(@Param("name") String name, @Param("opersys") Os opersys);
    Ttype findByNameAndOpersys(@Param("name") String name, @Param("opersys") Os opersys);

    //String query = "select distinct t.name from Ttype t, Tdata d where t.idtype = d.id and t.kind = :kind and d.ttype = :id";

    //@Query("select distinct t.name from Ttype t, Tdata d where t.idtype = d.id and t.kind = :kind and d.ttype = :id")
    //List<String> findByKindAndId(@Param("kind") Ttype.Kind kind, @Param("id") Ttype id);

    @Query("select distinct t.ttype from Tdata t  where t.id = :id") //list of owners, who has fields of that type (by id)
    List<Ttype> findById( @Param("id") Integer id); //only in current os

}