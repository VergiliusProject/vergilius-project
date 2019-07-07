package vergilius.repos;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import vergilius.Os;
import vergilius.Tdata;
import vergilius.Ttype;

import java.util.List;


public interface TdataRepository extends CrudRepository<Tdata, Integer> {
    List<Tdata> findByTtype(Ttype ttype);

}