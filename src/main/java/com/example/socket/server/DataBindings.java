package com.example.socket.server;

import java.util.List;

public class DataBindings <T>{
    
    private DataProvider<T> provider;

    public DataBindings()
    {
        // initially unbound
        this.provider = null;
    }

    /* now we have to make sure we cant do anthing stupid */
    public T get(Object obj) throws Exception
    {
        ensureProvider();
        return provider.get(obj);
    }

    public List<T> getAll() throws Exception
    {
        ensureProvider();
        return provider.getAll();
    }

    public void post(T entity) throws Exception
    {
        ensureProvider();
        provider.post(entity);
    }

    public void update(T entity) throws Exception
    {
        ensureProvider();
        provider.update(entity);
    }

    public void delete(Object obj) throws Exception
    {
        ensureProvider();
        provider.delete(obj);
    }

    public void deleteAll() throws Exception
    {
        ensureProvider();
        provider.deleteAll();
    }
    public void bindToDataBase(DataProvider<T> provider)
    {
        this.provider = provider;
    }

    // override the default provider for ORMs
    public void close() throws Exception
    {
        if (provider != null) {
            provider.close();
            provider = null;
        }
    }

    private void ensureProvider() {
        if (provider == null) {
            throw new IllegalStateException("No DataProvider bound. Call bindToDatabase() first.");
        }
    }
}   
