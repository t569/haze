package com.example;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

import com.example.model.Chat;
import com.example.model.User;
import com.example.socket.server.DataProvider;


public class ORMProvider <T, ID extends Serializable> implements DataProvider<T>, AutoCloseable{
    private static SessionFactory sessionFactory;

    private final Session session;

    private final Class<T> entityClass;

    // this makes sure the code is run only once
    static
    {
        try
        {
            // Load settings from hibernate.cfg.xml
            Configuration config = new Configuration().configure();

            config
            .addAnnotatedClass(com.example.model.User.class)
            .addAnnotatedClass(com.example.model.Chat.class)
            .addAnnotatedClass(com.example.model.ChatInfo.class);
            // Build registry with those settings
            ServiceRegistry registry = new StandardServiceRegistryBuilder()
                                        .applySettings(config.getProperties())
                                        .build();

            // Create SessionFactory
            sessionFactory = config.buildSessionFactory(registry);
        }

        // This is where the error is
        catch(Throwable ex)
        {
            ex.printStackTrace();
            throw new ExceptionInInitializerError(ex);
        }
    }


    public ORMProvider(Class<T> entityClass)
    {
        this.session = sessionFactory.openSession();
        this.entityClass = entityClass;
    }

    public Session getSession()
    {
        return this.session;
    }

    // shutdown the session for an instance
    public void close()
    {
       if(session.isOpen())
       {
        session.close();
       } 
    }

    // shutdown all active sessions
    public void shutdown()
    {
        sessionFactory.close();
    }

    // get function: the get method for the particular ORM member
    @Override
    public T get(Object id) throws Exception
    {
        Transaction tx = null;
        try
        {
            tx = session.beginTransaction();

           
            T entity = entityClass.cast(session.get(entityClass, (ID) id));

            tx.commit();
            return entity;
        }
     
        catch(Exception e)
        {
            if(tx != null)
            {
                tx.rollback();
            }
            throw new Exception("Error fetching: " + entityClass.getSimpleName(), e);
        }
    }

    // post function: create an entity
    @Override
    public void post(T entity) throws Exception
    {
        Transaction tx = null;
        try 
        {
            tx = session.beginTransaction();
            session.save(entity);
            tx.commit();
        }
        catch(Exception e)
        {
            if(tx != null)
            {
                tx.rollback();
            }
            throw new Exception("Error saving " + entityClass.getSimpleName(), e);
        }
    }

    // update function: update an entity
    @Override
    public void update(T entity) throws Exception
    {
        Transaction tx = null;
        try 
        {
            tx = session.beginTransaction();
            session.merge(entity);
            tx.commit();
        }
        catch(Exception e)
        {
            if(tx != null)
            {
                tx.rollback();
            }
            throw new Exception("Error updating " + entityClass.getSimpleName(), e);
        }
    }

    // delete function: delete an entity
    @Override
    public void delete(Object id) throws Exception
    {
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            T entity = entityClass.cast(session.get(entityClass, (ID) id));
            if (entity == null) {
                throw new Exception("Error deleting object: object does not exist");
            }
            // Handle User deletion with chat cleanup
            // In ORMProvider.delete(...)
            if (entity instanceof User user) {
                session.createNativeQuery("DELETE FROM user_chats WHERE user_id = :uid")
                .setParameter("uid", user.getId())
                .executeUpdate();
            session.remove(user);

            // Handle Chat deletion with user cleanup
            } else if (entity instanceof Chat) {
                Chat chat = (Chat) entity;
                for (User user : new HashSet<>(chat.getUsers())) {
                    user.getChats().remove(chat);
                    session.merge(user);
                }
                session.delete(chat);

            // Default deletion
            } else {
                session.delete(entity);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new Exception("Error deleting " + entityClass.getSimpleName(), e);
        }
    }

    @Override
    public List<T> getAll() throws Exception
    {
        Transaction tx = null;
        try 
        {
            tx = session.beginTransaction();
            List<T> entities = session.createQuery("FROM " + entityClass.getName(), entityClass).list();
            tx.commit();
            return entities;
        }
        catch(Exception e)
        {
            if(tx != null)
            {
                tx.rollback();
            }
            throw new Exception("Error fetching all " + entityClass.getSimpleName(), e);
        }
    }

    @Override
    public void deleteAll() throws Exception
    {
        Transaction tx = null;
        try 
        {
            tx = session.beginTransaction();
            session.createQuery("DELETE FROM " + entityClass.getSimpleName()).executeUpdate();
            tx.commit();
        }
        catch(Exception e)
        {
            if(tx != null)
            {
                tx.rollback();
            }
            throw new Exception("Error deleting all " +entityClass.getSimpleName());
        }
    }


}
