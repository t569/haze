package com.example;

import java.io.Serializable;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.Transaction;
import java.util.List;

public class DataBase {
    private final static String name = "DataBase";

    // only one instance is created for all DataBases
    private static final SessionFactory sessionFactory;

    private final Session session;

    // this makes sure the code is run only once
    static
    {
        try
        {
            // Load settings from hibernate.cfg.xml
            Configuration config = new Configuration().configure();

            // Build registry with those settings
            ServiceRegistry registry = new StandardServiceRegistryBuilder()
                                        .applySettings(config.getProperties())
                                        .build();

            // Create SessionFactory
            sessionFactory = config.buildSessionFactory(registry);
        }
        catch(Throwable ex)
        {
            throw new ExceptionInInitializerError();
        }
    }


    // each database instance has its own session
    public DataBase()
    {
        this.session = sessionFactory.openSession();
    }

    public Session getSession()
    {
        return this.session;
    }

    // shutdown the session factory for all instances 
    public static void shutdown()
    {
        sessionFactory.close();
    }

    // close a database instance
    public void close()
    {
        if(session.isOpen())
        {
            session.close();
        }
    }


    /* CRUD FUNCTIONS */ 

    // Create Function
    public <T> void create(T entity)
    {
        Transaction tx =session.beginTransaction();
        session.save(entity);
        tx.commit();
    }


    // Read Function
    public <T> T read(Class<T> myclass, Serializable id)
    {
        return myclass.cast(session.get(myclass, id));
    }

     // Read All instances of the entity from the table
     @SuppressWarnings("unchecked")
     public <T> List<T> readAll(Class<T> myclass)
     {
         return session.createQuery("FROM" + myclass.getName()).list();
     }


    // Update function: Update an existing entity
    public <T> void update(T entity)
    {
        Transaction tx = session.beginTransaction();
        session.merge(entity);
        tx.commit();
    }


    // Delete function: Delete an entity from the database
    public <T> void delete(T entity)
    {
        Transaction tx = session.beginTransaction();
        session.delete(entity);
        tx.commit();
    }

   
}
