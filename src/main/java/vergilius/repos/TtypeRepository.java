package vergilius.repos;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import vergilius.Os;
import vergilius.Ttype;

import java.util.List;


public interface TtypeRepository extends CrudRepository<Ttype, Integer> {
    List<Ttype> findByOpersys(Os opersys);

    List<Ttype> findByNameAndOpersys(String name, Os opersys);

    //@Query("select u from Ttype u where u.isConst = ?1 and u.isVolatile = ?2")
    //List<Ttype> findByIsConstAndIsVolatile(Boolean isConst, Boolean isVolatile);

    //@Query("select u from Ttype u where u.name = :name")
    //List<Ttype> findByName(@Param("name") String name);


}