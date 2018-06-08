package vergilius.repos;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import vergilius.Os;
import vergilius.Ttype;

import java.util.List;


public interface TtypeRepository extends CrudRepository<Ttype, Integer> {

    //@Query("select u from Ttype u where u.opersys = :opersys")
    List<Ttype> findByOpersysAndIsConstFalseAndIsVolatileFalse(@Param("opersys")Os opersys);

    //@Query("select u from Ttype u where u.name = :name and u.opersys = :opersys")
    List<Ttype> findByNameAndOpersys(@Param("name") String name, @Param("opersys")Os opersys);

    //List<Ttype> findByIsConstAndIsVolatileAndIdtypeIn(boolean isConst, boolean isVolatile, List<Integer> ids);
}