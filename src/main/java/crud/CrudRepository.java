
package crud;

import java.util.List;

public interface CrudRepository<T> {

    void save(T obj) throws Exception;

    void update(T obj) throws Exception;

    void delete(T obj) throws Exception;

    List<T> findAll(Class<T> clazz) throws Exception;

}

