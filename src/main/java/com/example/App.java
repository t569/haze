package com.example;
import com.example.socket.server.*;
import com.example.model.*;
/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        // start the server and database seperately
        // DataBase db = new DataBase();
        ProtoServer Haze = new ProtoServer(8080);


        // the ORMProvider for Users, Chats, and ChatInfo
        ORMProvider<User, Long> userProvider = new ORMProvider<>(User.class);
        ORMProvider<Chat, Long> chatProvider = new ORMProvider<>(Chat.class);
        ORMProvider<ChatInfo, Long> chatInfoProvider = new ORMProvider<>(ChatInfo.class);

        // spool the queries
        Haze.spoolQuery(User.class, userProvider);
        Haze.spoolQuery(Chat.class, chatProvider);
        Haze.spoolQuery(ChatInfo.class, chatInfoProvider);


        // TODO: add a button to close the server from javafx
        // Haze.stop();
    }
}
