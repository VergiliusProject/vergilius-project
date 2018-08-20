package vergilius.repos;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import vergilius.Os;
import vergilius.Ttype;

import java.util.List;


public interface TtypeRepository extends CrudRepository<Ttype, Integer> {

    List<Ttype> findByOpersysAndIsConstFalseAndIsVolatileFalse(@Param("opersys") Os opersys);

    Ttype findByIdAndOpersys(@Param("id") int id, @Param("opersys") Os opersys);

    List<Ttype> findByNameAndOpersys(@Param("name") String name, @Param("opersys") Os opersys);

    @Query("SELECT DISTINCT u.ttype FROM Tdata u"
            + " WHERE u.id= :id"
            + " AND u.ttype.name <> '<unnamed-tag>' AND u.ttype.name IS NOT NULL")
    List<Ttype> findById1(@Param("id") Integer id);

    @Query("SELECT DISTINCT s.ttype FROM Tdata s"
            + " WHERE s.id in(SELECT m.ttype.id FROM Tdata m"
            + " WHERE m.id= :id)"
            + " AND s.ttype.name <> '<unnamed-tag>' AND s.ttype.name IS NOT NULL")
    List<Ttype> findById2(@Param("id") Integer id);

    @Query("SELECT DISTINCT y.ttype FROM Tdata y"
            +" WHERE y.id IN(SELECT q.ttype.id FROM Tdata q"
            + " WHERE q.id IN(SELECT z.ttype.id FROM Tdata z"
            + " WHERE z.id= :id))"
            + " AND y.ttype.name <> '<unnamed-tag>' AND y.ttype.name IS NOT NULL")
    List<Ttype> findById3(@Param("id") Integer id);

    @Query("SELECT DISTINCT u.ttype FROM Tdata u"
            + " WHERE u.id IN (SELECT w.ttype.id FROM Tdata w"
            +" WHERE w.id IN (SELECT p.ttype.id FROM Tdata p"
            + " WHERE p.id IN (SELECT k.ttype.id FROM Tdata k WHERE k.id= :id)))"
            + " AND u.ttype.name <> '<unnamed-tag>' AND u.ttype.name IS NOT NULL")
    List<Ttype> findById4(@Param("id") Integer id);

}