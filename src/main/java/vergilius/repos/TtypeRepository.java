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
            + " WHERE u.id= :id AND u.ttype.opersys= :opersys"
            + " AND u.ttype.name <> '<unnamed-tag>' AND u.ttype.name IS NOT NULL")
    List<Ttype> findByOpersysAndId1(@Param("opersys") Os opersys, @Param("id") Integer id);

    @Query("SELECT DISTINCT s.ttype FROM Tdata s"
            + " WHERE s.id in(SELECT m.ttype.id FROM Tdata m"
            + " WHERE m.id= :id AND m.ttype.opersys= :opersys)"
            + " AND s.ttype.opersys= :opersys"
            + " AND s.ttype.name <> '<unnamed-tag>' AND s.ttype.name IS NOT NULL")
    List<Ttype> findByOpersysAndId2(@Param("opersys") Os opersys, @Param("id") Integer id);

    @Query("SELECT DISTINCT y.ttype FROM Tdata y"
            +" WHERE y.id IN(SELECT q.ttype.id FROM Tdata q"
            + " WHERE q.ttype.opersys= :opersys AND q.id IN(SELECT z.ttype.id FROM Tdata z"
            + " WHERE z.id= :id AND z.ttype.opersys= :opersys))"
            + " AND y.ttype.opersys= :opersys"
            + " AND y.ttype.name <> '<unnamed-tag>' AND y.ttype.name IS NOT NULL")
    List<Ttype> findByOpersysAndId3(@Param("opersys") Os opersys, @Param("id") Integer id);

    @Query("SELECT DISTINCT u.ttype FROM Tdata u"
            + " WHERE u.id IN (SELECT w.ttype.id FROM Tdata w"
            +" WHERE w.id IN (SELECT p.ttype.id FROM Tdata p"
            + " WHERE p.id IN (SELECT k.ttype.id FROM Tdata k WHERE k.id= :id AND k.ttype.opersys= :opersys) AND p.ttype.opersys= :opersys) AND w.ttype.opersys= :opersys)"
            + " AND u.ttype.opersys= :opersys"
            + " AND u.ttype.name <> '<unnamed-tag>' AND u.ttype.name IS NOT NULL")
    List<Ttype> findByOpersysAndId4(@Param("opersys") Os opersys, @Param("id") Integer id);

}