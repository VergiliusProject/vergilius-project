package vergilius.repos;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import vergilius.Os;
import vergilius.Ttype;

import java.util.List;


public interface TtypeRepository extends CrudRepository<Ttype, Integer> {

    List<Ttype> findByOpersysAndIsConstFalseAndIsVolatileFalse(@Param("opersys") Os opersys);

    //Ttype findByNameAndOpersysAndIsConstFalseAndIsVolatileFalse(@Param("name") String name, @Param("opersys") Os opersys);
    Ttype findByNameAndOpersys(@Param("name") String name, @Param("opersys") Os opersys);

    @Query("select distinct t.ttype from Tdata t  where (t.id = :id or t.id in(select u.ttype from Tdata  u where u.id= :id) or (t.id in(select s.ttype from Tdata  s where s.id in(select m.ttype from Tdata  m where m.id= :id))) and t.ttype.kind='POINTER')and t.ttype.kind in ('STRUCT', 'ENUM', 'UNION') and t.ttype.name <> '<unnamed-tag>'")
    List<Ttype> findById( @Param("id") Integer id);

}