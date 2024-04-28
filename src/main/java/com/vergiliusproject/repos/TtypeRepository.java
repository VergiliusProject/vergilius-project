package com.vergiliusproject.repos;

import com.vergiliusproject.entities.Os;
import com.vergiliusproject.entities.Ttype;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface TtypeRepository extends CrudRepository<Ttype, Integer> {
    List<Ttype> findByOpersysAndIsConstFalseAndIsVolatileFalse(@Param("opersys") Os opersys);
    
    @Query("SELECT u FROM Ttype u"
            + " WHERE u.opersys= :opersys"
            + " AND u.name IS NOT NULL"
            + " AND (u.kind = 'STRUCT' OR u.kind = 'ENUM' OR u.kind = 'UNION')"
            + " AND u.isConst = FALSE"
            + " AND u.isVolatile = FALSE")
    List<Ttype> findStructEnumUnionByOpersys(@Param("opersys") Os opersys);

    Ttype findByIdAndOpersys(@Param("id") int id, @Param("opersys") Os opersys);

    List<Ttype> findByNameAndOpersys(@Param("name") String name, @Param("opersys") Os opersys);

    @Query("SELECT DISTINCT u.ttype FROM Tdata u"
            + " WHERE u.id= :id AND u.ttype.opersys= :opersys"
            + " AND u.ttype.name IS NOT NULL AND u.ttype.kind <>'POINTER'")
    List<Ttype> findByOpersysAndId1(@Param("opersys") Os opersys, @Param("id") Integer id);

    @Query("SELECT DISTINCT s.ttype FROM Tdata s"
            + " WHERE s.id in(SELECT m.ttype.id FROM Tdata m"
                                + " WHERE m.id= :id AND m.ttype.opersys= :opersys"
                                + " AND (m.ttype.kind = 'POINTER' OR m.ttype.kind = 'ARRAY' OR m.ttype.kind = 'FUNCTION'"
                                + " OR m.ttype.name IS NULL))"
            + " AND s.ttype.opersys= :opersys"
            + " AND s.ttype.name IS NOT NULL")
    List<Ttype> findByOpersysAndId2(@Param("opersys") Os opersys, @Param("id") Integer id);

    @Query("SELECT DISTINCT y.ttype FROM Tdata y"
            +" WHERE y.id IN(SELECT q.ttype.id FROM Tdata q"
                                + " WHERE q.ttype.opersys= :opersys AND q.id IN(SELECT z.ttype.id FROM Tdata z"
                                                                                    + " WHERE z.id= :id AND z.ttype.opersys= :opersys"
                                                                                    + " AND (z.ttype.kind = 'POINTER' OR z.ttype.kind = 'ARRAY'"
                                                                                    + " OR z.ttype.kind = 'FUNCTION' OR z.ttype.name IS NULL))"
                                + " AND (q.ttype.kind = 'POINTER' OR q.ttype.kind = 'ARRAY'"
                                + " OR q.ttype.kind = 'FUNCTION' OR q.ttype.name IS NULL))"
            + " AND y.ttype.opersys= :opersys"
            + " AND y.ttype.name IS NOT NULL")
    List<Ttype> findByOpersysAndId3(@Param("opersys") Os opersys, @Param("id") Integer id);

    @Query("SELECT DISTINCT u.ttype FROM Tdata u"
            + " WHERE u.id IN (SELECT w.ttype.id FROM Tdata w"
                                    +" WHERE w.id IN (SELECT p.ttype.id FROM Tdata p"
                                                        + " WHERE p.id IN (SELECT k.ttype.id FROM Tdata k"
                                                                                + " WHERE k.id= :id AND k.ttype.opersys= :opersys"
                                                                                + " AND (k.ttype.kind = 'POINTER' OR k.ttype.kind = 'ARRAY'"
                                                                                + " OR k.ttype.kind = 'FUNCTION' OR k.ttype.name IS NULL))"
                                                        + " AND p.ttype.opersys= :opersys"
                                                        + " AND (p.ttype.kind = 'POINTER' OR p.ttype.kind = 'ARRAY'"
                                                        + " OR p.ttype.kind = 'FUNCTION' OR p.ttype.name IS NULL))"
                                    + " AND w.ttype.opersys= :opersys"
                                    + " AND (w.ttype.kind = 'POINTER' OR w.ttype.kind = 'ARRAY'"
                                    + " OR w.ttype.kind = 'FUNCTION' OR w.ttype.name IS NULL))"
            + " AND u.ttype.opersys= :opersys"
            + " AND u.ttype.name IS NOT NULL")
    List<Ttype> findByOpersysAndId4(@Param("opersys") Os opersys, @Param("id") Integer id);

    @Query("SELECT DISTINCT u.ttype FROM Tdata u"
            + " WHERE u.id IN (SELECT w.ttype.id FROM Tdata w"
            + " WHERE w.id IN (SELECT p.ttype.id FROM Tdata p"
            + " WHERE p.id IN (SELECT k.ttype.id FROM Tdata k"
            + " WHERE k.id in("
            + " SELECT m.ttype.id FROM Tdata m WHERE m.id= :id AND m.ttype.opersys= :opersys"
            + " AND (m.ttype.kind = 'POINTER' OR m.ttype.kind = 'ARRAY' OR m.ttype.kind = 'FUNCTION'"
            + " OR m.ttype.name IS NULL))"
            + " AND k.ttype.opersys= :opersys"
            + " AND (k.ttype.kind = 'POINTER' OR k.ttype.kind = 'ARRAY'"
            + " OR k.ttype.kind = 'FUNCTION' OR k.ttype.name IS NULL))"
            + " AND p.ttype.opersys= :opersys"
            + " AND (p.ttype.kind = 'POINTER' OR p.ttype.kind = 'ARRAY'"
            + " OR p.ttype.kind = 'FUNCTION' OR p.ttype.name IS NULL))"
            + " AND w.ttype.opersys= :opersys"
            + " AND (w.ttype.kind = 'POINTER' OR w.ttype.kind = 'ARRAY'"
            + " OR w.ttype.kind = 'FUNCTION' OR w.ttype.name IS NULL))"
            + " AND u.ttype.opersys= :opersys"
            + " AND u.ttype.name IS NOT NULL")
    List<Ttype> findByOpersysAndId5(@Param("opersys") Os opersys, @Param("id") Integer id);
}