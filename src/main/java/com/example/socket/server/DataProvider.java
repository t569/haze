package com.example.socket.server;
/*
 * Hey guys; now remember, T is always the type were manipulating
 * The providers just define where we store them
 */

import java.util.List;

public interface DataProvider<T> {
    T get(Object obj) throws Exception;
    List<T> getAll() throws Exception;
    void post(T entity) throws Exception;
    void update(T entity) throws Exception;
    void delete(Object obj) throws Exception;
}
