package com.example.socket.server;
import java.net.*;
import java.util.Optional;
import java.util.List;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.example.model.Chat;
import com.example.model.ChatInfo;
import com.example.model.User;
import com.example.socket.routines.Echo;
import com.example.Config;

import java.io.*;

/*
 * Recieve all incoming connections
 * 
 */
public class ProtoServer{
    private int PORT;
    private ServerSocket serverSocket;
    private static String name = Config.SERVER_NAME;
    private static final ConcurrentHashMap<String, Boolean> ackedClients = new ConcurrentHashMap<String, Boolean>();
    private static final ConcurrentHashMap<String, ObjectOutputStream> ackedClientsOutStreams = new ConcurrentHashMap<String, ObjectOutputStream>();
    private final ConcurrentHashMap<Class<?>, QueryHandler<?>> queries = new ConcurrentHashMap<>();
    private final int maxThreads = 10;
    private final int maxQueue = 20;
    private Echo server_parrot = new Echo(name);


    // thread pool for spoolign connections
    ThreadPoolExecutor executor = new ThreadPoolExecutor(maxThreads, maxThreads,0L, TimeUnit.MILLISECONDS,  new ArrayBlockingQueue<>(maxQueue), new ThreadPoolExecutor.CallerRunsPolicy());

    /*
     * MODIFICATION: 
     * The modification Creates a new sub class called BindHandler that handles different data and is what is actually bound to the database
     *
     * 
     */

    
    public ProtoServer(int port)
    {   
        this.PORT = port;
        try 
        {
            this.serverSocket = new ServerSocket(PORT);
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        
    }

    // The general behaviour of the Server is to keep listening to a thread of a particular client
    // Until the client themself close the connection. 


    // So in essence, while true keep accepting and handling clients
    public void start() throws IOException, ClassNotFoundException
    {
        // create a thread pool to handle all the threads
        // Thread acceptThread = new Thread(() -> {
        //     server_parrot.log("Server listening on port: " + PORT);
        //     while (true) {
        //         try {
        //             Socket client = serverSocket.accept();
        //             executor.execute(() -> handleClient(client));
        //         } catch (RejectedExecutionException rex) {
        //             System.err.println("Server overloaded, rejecting connection");
        //         } catch (IOException io) {
        //             io.printStackTrace();
        //         }
        //     }
        // });
        // acceptThread.setDaemon(true);
        // acceptThread.start();

        new Thread(() -> {
            server_parrot.log("Server listening on port: " + PORT);
            while (true) {
                try {
                    Socket client = serverSocket.accept();
                    executor.execute(() -> handleClient(client));
                }catch(SocketException e)
                {
                    server_parrot.log("Accept aborted; server closed");
                    return;
                } catch (RejectedExecutionException rex) {
                    System.err.println("Server overloaded, rejecting connection");
                } catch (IOException io) {
                    io.printStackTrace();
                }
            }
        }).start();
    }

    // proper stop function
    public void stop() {
        try {
            server_parrot.log("Shutting down server...");
            executor.shutdown(); // Terminate thread pool
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            server_parrot.log_err_with_ret(e);
        }

        ackedClients.forEach((clientId, connected)  -> {
            if(connected)
            {
                try 
                {
                    server_parrot.log("Seding disconnect to: " + clientId);

                    ObjectOutputStream client_out = ackedClientsOutStreams.get(clientId);

                    client_out.writeObject(
                        new Protocol(
                            Protocol.Status.CONN_DISCONNECT,
                            new Protocol.Packet(
                                Config.SERVER_NAME,
                                clientId,
                                "Server is shutting down",
                                new Protocol.Packet.MetaData()
                            )
                        )
                    );
                    client_out.flush();
                    client_out.close();
                }
                catch(IOException ioe)
                {
                    server_parrot.log_err_with_ret(ioe);
                }
            }            
        });
        ackedClients.clear();
        ackedClientsOutStreams.clear();

        try{
            serverSocket.close(); // Close server socket
        }
        catch(IOException ioe)
        {
            server_parrot.log_err_with_ret(ioe);
        }
        server_parrot.log("Server shutdown complete");
    }

    
    // accept request from client
        // send a temp connection trigger
        // client recieves this and sends a CONN_REQ
        // server recieves this and sends a CONN_ACK

        // if client sends a CONN_CONF
        // server checks if they are part of server clients
        // if not boot the client

    // TODO: use of CONN_CONF seems to be depreciated, meaning has changed

    // run this on a thread Note this should run only once
    
    // ADHOC IMPLEMENTATION TO SHUTDOWN THE VARIOUS CLIENT CONNECTIONS
    public void handleClient(Socket socket)
    {
        // NETWORK HANDSHAKE
        ObjectInputStream in = null;
        ObjectOutputStream out = null;
        // under this model we will first 
        try
        {
            out = new ObjectOutputStream(socket.getOutputStream());
            in  = new ObjectInputStream(socket.getInputStream());
            // get the name of the client first
            String clientId = (String) in.readObject();

            // search the name in the acked clients
            if(!ackedClients.contains(clientId))
            {
                // handshake successful proceed
                if(handshake(in, out, socket)){
                ackedClients.put(clientId, Boolean.TRUE);
                ackedClientsOutStreams.put(clientId, out);

                // now add the socket to our list of sockets that are active


                server_parrot.log("Adding "+ clientId+ " to acked clients");

                // process requests in infinite loop until we break socket connection
                protocolProcess(socket, in, out);
                }

                else
                {
                    throw new Exception("HandShake failed: Abort");
                }
            }
            else
            {
                // if it is, process request in an infinite loop until we break socket connection
                protocolProcess(socket, in, out);
            }
            
        }

        // this is used to catch when protocolProcess fails
        catch(Exception e)
        {
            server_parrot.log_err_with_ret(e);
        }

        // =socket closing happens when we break out from protocolProcess
        return;
    }
        
    
    private boolean handshake(ObjectInputStream in, ObjectOutputStream out, Socket socket) throws IOException, ClassNotFoundException
    {
        
         // 1. CONN_INIT_HANDSHAKE TRIGGER
         out.writeObject(new Protocol(Protocol.Status.CONN_INIT_HANDSHAKE,new Protocol.Packet(name,
                            socket.getInetAddress().toString(),
                             "BEGIN HANDSHAKE",
                            new Protocol.Packet.MetaData())));
        out.flush();


        // 2. Check Response Type from the init sequence
        Protocol req = (Protocol) in.readObject();
        String clientId = req.getPacket().getSender();

        // if they make a request, check if we known them and add them, else tell them we acknowledge them
        if(req.getStatus() == Protocol.Status.CONN_REQ)
        {
                server_parrot.log("Connection request from "+ clientId);

                // 3. Send CONN_ACK
                out.writeObject(new Protocol(
                Protocol.Status.CONN_ACK,
                new Protocol.Packet(
                    name,                                   /*sender*/  
                    clientId,                              /*receiver*/ 
                    "HANDSHAKE ACK",                   /*text*/ 
                    new Protocol.Packet.MetaData()
                    )
                ));
                server_parrot.log("Connection Request Acknowledged: "+ clientId);
                out.flush();
        }

        else 
        {
                // they send a request as if we know them boot that nigga
                bootClient(socket, out, "Unknown user");
                return false;
        }


        // the handshake was successful
        return true;



    }

    public void protocolProcess(Socket socket, ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException, Exception
    {
        while(true)
        {
            // while the protocol status is not closed, listen and handle any incoming message
            // main loop
            // incoming message
            
            Protocol msg = (Protocol) in.readObject();

            // this only happens when we disconnect
            if(!handleClientProtocol(msg, socket, in, out))
            {
                break;
            }
        }
        socket.close();
        return;

    }

    public boolean handleClientProtocol(Protocol protocol, Socket socket, ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException
    {
        // Handles client message and does what he's asked to do
        // TODO: this will be like an entry into another set of utils

        String clientId = protocol.getPacket().getSender();

        Protocol.Status st = protocol.getStatus();

        if(st == Protocol.Status.CONN_DISCONNECT)
        {
            server_parrot.log("Client " + clientId + " disconnected");
            return false;
        }

        Protocol response = handleRequest(protocol);
        out.writeObject(response);
        out.flush();


        return true;
    }

    private void bootClient(Socket socket, ObjectOutputStream out, String reason)
    {
        try
        {
            // System.out.println("Booting client" + reason);
            server_parrot.log("Booting client: " + socket.getInetAddress() + socket.getPort() +" Reason: " + reason);
            out.writeObject(new Protocol(
                Protocol.Status.CONN_BOOT,
                new Protocol.Packet(
                    name,                                   /*sender*/  
                    socket.getInetAddress().toString(),                              /*receiver*/ 
                    "BOOT: "+ reason,                    /*text*/ 
                    new Protocol.Packet.MetaData()
                )
            ));
            out.flush();
            socket.close();
        }
        catch(IOException e) {server_parrot.log_err_with_ret(e);}
    }

    public Protocol handleRequest(Protocol msg)
    {
        
        // This is finally when we consider MetaData

        
        Optional<Protocol.Packet.MetaData.CommProtocol> opt = msg.getPacket().getMetaData().getCommProtocol();


        // what we will return to the client
        Protocol returnProtocol;


        // if we actually have some metadata
        if(opt.isPresent())
        {
            // get the protocol
            Protocol.Packet.MetaData.CommProtocol protocol = opt.get();

            switch(protocol)
            {
                case GET:
                    // do something
                    returnProtocol = handleGet(msg);
                    break;

                case POST:
                    // do something
                    returnProtocol = handlePost(msg);
                    break;

                case UPDATE:
                    // do something
                    returnProtocol = handleUpdate(msg);
                    break;

                case DELETE:
                    // do something
                    returnProtocol = handleDelete(msg);
                    break;

                case GET_ALL:
                    returnProtocol = handleGetAll(msg);
                    break;

                case GET_ALL_BY_OBJ:
                    returnProtocol = handleGetAllByObj(msg);
                    break;

                case DELETE_ALL:
                    returnProtocol = handleDeleteAll(msg);
                    break;

                default:
                    returnProtocol = new Protocol(Protocol.Status.CONN_OK,
                                    new Protocol.Packet(
                                        name,
                                        msg.getPacket().getSender(),
                                        "Request recieved",
                                        new Protocol.Packet.MetaData()
                                    )
                                );
                    break;
            }

            // we are done with parsing
            return returnProtocol;
        }

        else
        {
            returnProtocol = new Protocol(Protocol.Status.CONN_OK,
                                    new Protocol.Packet(
                                        name,
                                        msg.getPacket().getSender(),
                                        "Request recieved",
                                        new Protocol.Packet.MetaData()
                                    )
                                );
            return returnProtocol;
        }
        

    }

    // Note: this implementation works with Object id objects, so searching by id or a field
    public <T> Protocol handleGet(Protocol msg)
    {
        String clientID = msg.getPacket().getReceiver();

        // payload is just and Object. In this case a String or an id
        Optional<Object> payload = msg.getPacket().getMetaData().getPayload();

        // get the actual key that we're searching
        Optional<String> typekey = msg.getPacket().getMetaData().getKey();

        // check if the key exists and error if it does not;
        // Note: key should only exist for GET and DELETE methods

        QueryHandler<T> handler;
        if(typekey.isPresent())
        {
            // now search query to see if we get a match on type key
             handler = findQueryHandlerByTypeKey(typekey.get(), queries);

            // we dont have a handler for this type specified
            if(handler == null)
            {
                return new Protocol(Protocol.Status.CONN_OK,
                                    new Protocol.Packet(name,
                                                     clientID,
                                                      "GET failed: No handler for type",
                                                       new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_ERR)
                                                       ));
            }

            // now check if we actually have a payload
            if(!payload.isPresent())
            {
                return new Protocol(Protocol.Status.CONN_OK,
                                     new Protocol.Packet(name,
                                                         clientID,
                                                          "GET failed: No payload",
                                                           new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_ERR)
                                                        ));
            }

            Object object_id_to_get = payload.get();
            try 
            {
                T object_to_get = handler.getQuery().get(object_id_to_get);
                return new Protocol(Protocol.Status.CONN_OK,
                                     new Protocol.Packet(name,
                                                        clientID,
                                                         "GET: success", 
                                                         new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_OK, object_to_get)
                                                         ));

            }
            catch(Exception e)
            {
                return new Protocol(Protocol.Status.CONN_OK,
                                     new Protocol.Packet(name,
                                                         clientID,
                                                          "GET failed: " + e.getMessage(),
                                                          new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_ERR)
                                                          ));
            }
        }

        else
        {
            return new Protocol(Protocol.Status.CONN_OK,
                                 new Protocol.Packet(name,
                                                     clientID,
                                                      "GET failed: No supplied type Key",
                                                      new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_ERR)
                                                    )); 
        }

        

    }
    
    public <T> Protocol handleGetAll(Protocol msg) {
        String clientID = msg.getPacket().getReceiver();
        Optional<String> typekey = msg.getPacket().getMetaData().getKey();

        // uses typekey to search if a particular database is there

        // if so return all its elements
        if (!typekey.isPresent()) {
            return errorResponse(clientID, "GET_ALL failed: No type key provided");
        }

        QueryHandler<T> handler = findQueryHandlerByTypeKey(typekey.get(), queries);
        if (handler == null) {
            return errorResponse(clientID, "GET_ALL failed: No handler for type key " + typekey.get());
        }

        try {
            List<T> results = handler.getQuery().getAll();

            return new Protocol(
                Protocol.Status.CONN_OK,
                new Protocol.Packet(
                    name,
                    clientID,
                    "GET_ALL success",
                    new Protocol.Packet.MetaData(
                        Protocol.Packet.MetaData.CommProtocol.RESPONSE_OK,
                        results
                    )
                )
            );
        } catch (Exception e) {
            return errorResponse(clientID, "GET_ALL failed: " + e.getMessage());
        }
    }

    // Bad idea i know
    // very very big note, this is only for the chat app. It does NOT work in the general case
    public <T> Protocol handleGetAllByObj(Protocol msg) {
        String clientID = msg.getPacket().getReceiver();

        // 1) Extract typeKey (child type) and payload (parent stub)
        Optional<String> childKeyOpt    = msg.getPacket().getMetaData().getKey();
        Optional<Object> parentStubOpt  = msg.getPacket().getMetaData().getPayload();

        if (childKeyOpt.isEmpty()) {
            return errorResponse(clientID, "GET_ALL_BY_OBJ failed: No child key provided");
        }
        if (parentStubOpt.isEmpty()) {
            return errorResponse(clientID, "GET_ALL_BY_OBJ failed: No parent payload provided");
        }

        String childKey    = childKeyOpt.get();           // e.g. "Chat" or "ChatInfo"
        Object parentStub  = parentStubOpt.get();         // e.g. new User(id) or new Chat(id)

        try {
            switch (childKey) {
                case "Chat": {
                    // 2a) Load the User parent fully
                    User stubUser = (User) parentStub;
                    QueryHandler<User> userHandler =
                        findQueryHandlerByTypeKey("User", queries);
                    if (userHandler == null) {
                        throw new IllegalStateException("No handler for User");
                    }
                    User managedUser = userHandler.getQuery().get(stubUser.getId());

                    // 3a) Extract all Chat children
                    Collection<Chat> chats = managedUser.getChats();
                    return new Protocol(Protocol.Status.CONN_OK,
                                     new Protocol.Packet(name,
                                                        clientID,
                                                         "GET_ALL_OBJ: success", 
                                                         new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_OK,
                                                          chats)
                                                         ));
                }

                case "ChatInfo": {
                    // 2b) Load the Chat parent fully
                    Chat stubChat = (Chat) parentStub;
                    QueryHandler<Chat> chatHandler =
                        findQueryHandlerByTypeKey("Chat", queries);
                    if (chatHandler == null) {
                        throw new IllegalStateException("No handler for Chat");
                    }
                    Chat managedChat = chatHandler.getQuery().get(stubChat.getId());

                    // 3b) Extract all ChatInfo children
                    // Note: infos is messages
                    Collection<ChatInfo> infos = managedChat.getChatInfos();
                    return new Protocol(Protocol.Status.CONN_OK,
                                     new Protocol.Packet(name,
                                                        clientID,
                                                         "GET_ALL_BY_OBJ: chatInfos for chat " + managedChat.getId(), 
                                                         new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_OK,
                                                         infos)
                                                         ));
                }

                default:
                    return errorResponse(clientID,
                        "GET_ALL_BY_OBJ failed: Unsupported child key '" + childKey + "'");
            }
        } catch (Exception e) {
            return errorResponse(clientID, "GET_ALL_BY_OBJ failed: " + e.getMessage());
        }
    }
   
    // TODO: make this used by all the other methods
    private <T> Protocol errorResponse(String clientID, String message) {
        return new Protocol(
            Protocol.Status.CONN_OK,
            new Protocol.Packet(
                name,
                clientID,
                message,
                new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_ERR)
            )
        );
    }

    @SuppressWarnings("unchecked")
    public <T> Protocol handlePost(Protocol msg)
    {
        String clientID = msg.getPacket().getReceiver();
        Optional<Object> payload = msg.getPacket().getMetaData().getPayload();


        if(!payload.isPresent())
        {
            return new Protocol(Protocol.Status.CONN_OK,
                                 new Protocol.Packet(name,
                                                     clientID, 
                                                     "POST failed: No payload provided",
                                                      new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_ERR)
                                                    ));
        }

        Object object_to_post = payload.get();
       
       QueryHandler<T> handler = getQuery((Class<T>) object_to_post.getClass());

       if(handler == null)
       {
            return new Protocol(Protocol.Status.CONN_OK,
                                 new Protocol.Packet(name,
                                                     clientID,
                                                      "POST failed: No handler for type: " + object_to_post.getClass().getName(),
                                                       new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_ERR)
                                                    ));
       }


       try 
       {    
        // we're sending it back because we still need to use it in the front end
        // TODO: we need to make that design better
            handler.getQuery().post((T) object_to_post);
            return new Protocol(Protocol.Status.CONN_OK,
                                 new Protocol.Packet(name,
                                                    clientID,
                                                     "POST: success",
                                                      new Protocol.Packet.MetaData(
                                                        Protocol.Packet.MetaData.CommProtocol.RESPONSE_OK,
                                                        object_to_post)
                                                    ));
       }
       catch(Exception e)
       {
            return new Protocol(Protocol.Status.CONN_OK,
                                 new Protocol.Packet(name,
                                  clientID, "POST failed: " + e.getMessage(),
                                  new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_ERR)
                                ));
       }
    } 

    @SuppressWarnings("unchecked")
    public <T> Protocol handleUpdate(Protocol msg)
    {
        String clientID = msg.getPacket().getReceiver();
        Optional<Object> payload = msg.getPacket().getMetaData().getPayload();

        if(!payload.isPresent())
        {
            return new Protocol(Protocol.Status.CONN_OK, 
                                new Protocol.Packet(name,
                                                 clientID, 
                                                 "UPDATE failed: No payload provided",
                                                  new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_ERR)
                                                  ));
        }

        Object object_to_update = payload.get();
        QueryHandler<T> handler = getQuery((Class<T>) object_to_update.getClass());

        if(handler == null){
            return new Protocol(Protocol.Status.CONN_OK,
                                 new Protocol.Packet(name,
                                                     clientID,
                                                      "UPDATE failed: No handler for type: " + object_to_update.getClass().getName(), 
                                                      new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_ERR)
                                                    ));
        }

        try
        {
            handler.getQuery().update((T) object_to_update);
            return new Protocol(Protocol.Status.CONN_OK,
                                new Protocol.Packet(name,
                                                 clientID,
                                                  "UPDATE success", 
                                                  new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_OK)
                                                  ));
        }
        catch(Exception e)
        {
            return new Protocol(Protocol.Status.CONN_OK,
                                new Protocol.Packet(name,
                                                     clientID,
                                                      "UPDATE failed: " + e.getMessage(), 
                                                      new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_ERR)
                                                    ));
        }

    }

    public <T> Protocol handleDelete(Protocol msg)
    {
        String clientID = msg.getPacket().getReceiver();

        // payload is just and Object. In this case a String or an id
        Optional<Object> payload = msg.getPacket().getMetaData().getPayload();

        // get the actual key that we're searching
        Optional<String> typekey = msg.getPacket().getMetaData().getKey();

        // check if the key exists and error if it does not;
        // Note: key should only exist for GET and DELETE methods

        QueryHandler<T> handler;
        if(typekey.isPresent())
        {
            // now search query to see if we get a match on type key
             handler = findQueryHandlerByTypeKey(typekey.get(), queries);

            // we dont have a handler for this type specified
            if(handler == null)
            {
                return new Protocol(Protocol.Status.CONN_OK,
                                    new Protocol.Packet(name,
                                                     clientID,
                                                      "DELETE failed: No handler for type", 
                                                      new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_ERR)
                                                      ));
            }

            // now check if we actually have a payload
            if(!payload.isPresent())
            {
                return new Protocol(Protocol.Status.CONN_OK,
                                     new Protocol.Packet(name,
                                                         clientID,
                                                          "DELETE failed: No payload",
                                                           new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_ERR)
                                                        ));
            }

            Object object_id_to_get = payload.get();
            try 
            {
                handler.getQuery().delete(object_id_to_get);
                return new Protocol(Protocol.Status.CONN_OK,
                                     new Protocol.Packet(name,
                                                        clientID, 
                                                    "DELETE: success", 
                                                        new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_OK)
                                                        ));

            }
            catch(Exception e)
            {
                return new Protocol(Protocol.Status.CONN_OK,
                                     new Protocol.Packet(name,
                                                         clientID,
                                                        "DELETE failed: " + e.getMessage(),
                                                         new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_ERR)
                                                         ));
            }
        }

        else
        {
            return new Protocol(Protocol.Status.CONN_OK,
                                 new Protocol.Packet(name,
                                                     clientID,
                                                "DELETE failed: No supplied type Key",
                                                    new Protocol.Packet.MetaData(Protocol.Packet.MetaData.CommProtocol.RESPONSE_ERR)
                                                    )); 
        }
    }

    public <T> Protocol handleDeleteAll(Protocol msg)
    {
        String clientID = msg.getPacket().getReceiver();
        Optional<String> typekey = msg.getPacket().getMetaData().getKey();

        // uses typekey to search if a particular database is there

        // if so return all its elements
        if (!typekey.isPresent()) {
            return errorResponse(clientID, "DELETE_ALL failed: No type key provided");
        }

        QueryHandler<T> handler = findQueryHandlerByTypeKey(typekey.get(), queries);
        if (handler == null) {
            return errorResponse(clientID, "DELETE_ALL failed: No handler for type key " + typekey.get());
        }

        try {
            handler.getQuery().deleteAll();

            return new Protocol(
                Protocol.Status.CONN_OK,
                new Protocol.Packet(
                    name,
                    clientID,
                    "DELETE_ALL success",
                    new Protocol.Packet.MetaData(
                        Protocol.Packet.MetaData.CommProtocol.RESPONSE_OK,
                        null
                    )
                )
            );
        } catch (Exception e) {
            return errorResponse(clientID, "DELETE_ALL failed: " + e.getMessage());
        }
    }

    private class QueryHandler<T>
     {
        private DataBindings<T> query = new DataBindings<>();
        public QueryHandler(Class <T> modelClass)
        {

        }

        // bind the bindhandler query object to a Data provider
        public void bindToDataBase(DataProvider<T> provider)
        {
            query.bindToDataBase(provider);
        }

        public DataBindings<T> getQuery()
        {
            return this.query;
        }
     }
     // spool server method
     public <T> void spoolQuery(Class<T> myclass, DataProvider<T> provider)
     {
        if(queries.containsKey(myclass))
        {
             throw new IllegalStateException("Server instance for type already exists" + myclass.getSimpleName());
        } 
 
        // if not, we're good, add the instance to the spool pool lmao
        /*
         * Bind it to the provider first
         * Then add it to the hashmap
         */
        QueryHandler<T> query = new QueryHandler<>(myclass);
        query.bindToDataBase(provider);
        queries.put(myclass, query);
        String message = "Successful, server for instance: " + myclass.getSimpleName() + " created.";
 
 
     //    System.out.println("Successful, server for instance: " + myclass.getSimpleName() + " created.");
 
     // log the error message
        server_parrot.log(message);
     }
 
     // get server from ConcurrentHashmap method
     @SuppressWarnings("unchecked")
     public <T> QueryHandler<T> getQuery(Class<T> myclass)
     {
         return (QueryHandler<T>) queries.get(myclass);
     }
 
     public boolean matchesTypeKey(String typekey, Class<?> modelClass)
     {
         return modelClass.getSimpleName().equalsIgnoreCase(typekey);
     }
 
     @SuppressWarnings("unchecked")
     public <T> QueryHandler<T> findQueryHandlerByTypeKey(String typekey, ConcurrentHashMap<Class<?>, QueryHandler<?>>queries)
     {
         for(Class<?> modelClass: queries.keySet())
         {
             // this makes sure we ignore case sensitivity e.g. User and USER works
             if(modelClass.getSimpleName().equalsIgnoreCase(typekey))
             {
                 return (QueryHandler<T>) queries.get(modelClass);
             }
         }
 
         // nothing found
         return null;
     }
 

// The whole idea is that there is only one server
// However, that server has subservers that handle individual ORMS
// so the methods not marked as static are for them
// the methods marked as static are for the main server
/*

     */

   

}
    