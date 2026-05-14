package crud;

import java.util.List;

public class GrudService<T> {
    private CrudRepository<T> repository;

    public GrudService(CrudRepository<T> repository) {
        this.repository = repository;
    }

    public void create(T entity) throws Exception {
        repository.save(entity);
    }

    public void modify(T entity) throws Exception {
        repository.update(entity);
    }

    public void remove(T entity) throws Exception {
        repository.delete(entity);
    }

    public List<T> listAll(Class<T> clazz) throws Exception {
        return repository.findAll(clazz);
    }
}