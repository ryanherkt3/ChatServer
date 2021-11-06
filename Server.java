package assignment;

import java.io.*;
import java.util.*;
import java.net.*;
import javax.swing.ImageIcon;

/**
 * Class Description: The server class for this assignment, which maintains a 
 * list of clients (ChatServers) that are currently connected (each 
 * with a unique name) and passes messages from one client (ChatServer) to 
 * another. It notifies clients when the server will terminate their connection 
 * (but only if they don't close their window), and also notifies clients when 
 * another client disconnects from the server. This class demonstrates the 
 * singleton design pattern, when it creates one instance of the 
 * SingletonServerEmail class.
 * 
 * Threads:
 * 1) Server class EDT - checks for client connections (ie startServer method)
 * 2) Server class' second thread - send e-mails to list of e-mail addresses
 * 3-n [where n >= 4]) Inner class thread/run method - send message(s) 
 * to client(s)
 * 
 * @author Ryan Herkt (ID: 18022861)
 */
public class Server implements Runnable
{
    //keep list of usernames (incase a user wants to send a message privately to 
    //someone), and one of the clients currently connected to the server:
    private final ArrayList<ChatServer> ALL_CLIENTS = new ArrayList<>();
    
    private static final int PORT = 2207;   //random host port number
    private final String NAME = "CLIENT";  //prefix of unique client name
    
    //used to assign a unique client number to new clients:
    private int totalClients = 0;
    
    /**
     * Default constructor, has nothing in it; could initialize above attributes 
     * in here though.
     */
    public Server()
    {
        
    }
    
    /**
     * The server's second / Runnable thread, which allows the server to send 
     * e-mails via the SingletonServerEmail class, while the main thread 
     * is left to listen for any client connections. This means clients can 
     * connect to the server while the e-mail is being sent.
     */
    @Override
    public void run()
    {
        //printout to demonstrate the two threads are running:
        System.out.println("Sending emails via the server...");
        
        //get the only ServerEmail object:
        SingletonServerEmail sendEmails = 
            SingletonServerEmail.getSingletonServerEmail();
        //set properties first (this method calls sendMessage(...), which 
        //sends the emails):
        sendEmails.setProperties();
    }
    
    /**
     * Method which starts the server (if it hasn't been already), by making a 
     * ServerSocket instance and running an infinite loop to listen for new 
     * users / client connections attempting to join the chat service.
     */
    public void startServer()
    {
        ServerSocket ss = null;
        try
        {
            ss = new ServerSocket(PORT);
            System.out.println("Server is ready to accept client connections...");
            
            //accept connections while the boolean is set to true:
            while (true)
            {
                Socket socket = ss.accept();
                
                //increments and then pass in totalClients as a parameter 
                //for the server subclass:
                totalClients++;
                ChatServer cs = new ChatServer(socket, totalClients);
                
                Thread thread = new Thread(cs);
                thread.start();
            }
        }
        catch (Exception e) //catch-all
        {
            System.out.println("Couldn't connect to server: " + e);
        }
        finally
        {
            //server is no longer accepting connections
            System.out.println("Server is no longer accepting client connections.");
            try 
            {
                ss.close(); //close ServerSocket
            }
            catch (IOException ex) 
            {}
        }
    }
    
    /**
     * Driver main method which implements advanced feature #1 (sending e-mails 
     * to a list of addresses) via a second thread (multi-threading), then 
     * starts the server.
     * 
     * @param args
     */
    public static void main(String[] args)
    {
        Server server = new Server();
        Thread thread = new Thread(server);
        
        thread.start(); //start thread which sends emails to addresses
        server.startServer();   //accept connections
    }
    
    /**
     * Inner class that represents a single Chat Server for a client to use 
     * across a socket. Has data values for the socket, the client's unique 
     * username, a BufferedReader and a PrintWriter.
     */
    public class ChatServer implements Runnable, Serializable
    {
        private Socket socket;  //socket for client/server communication
        private String username;    //unique username
        private ObjectOutputStream oos; //output stream to server
        private ObjectInputStream ois;   //input stream from server
        
        /**
         * A constructor for the chat service across a socket for client/server 
         * communication. Initializes the socket and the username of the client
         * 
         * @param s Socket, 
         * @param clientID ID of newly connected client
         */ 
        public ChatServer(Socket s, int clientID) throws 
            FileNotFoundException, IOException
        {
            this.socket = s;
            this.username = NAME + clientID; 
        }
        
        /**
         * A method that sends messages to clients
         * 
         * @param message the message to send
         */
        public void sendMessage(String message)
        {
            Message m;
            
            //Check if any users on the server have been mentioned in a 
            //user-sent message with a foreach loop. Flag as true if there is 
            //a mention and store every mentioned name in an ArrayList.
            boolean mentions = false;
            ArrayList<String> allMentioned = new ArrayList<>();
            for (ChatServer cs : ALL_CLIENTS)
            {
                if (message.contains(cs.username))
                {
                    mentions = true;
                    allMentioned.add(cs.username);
                }
            }
            
            //Sending the message. If it's not a server message, the 
            //algorithm uses the mentions flag to check if a user wants to 
            //send a message to anyone in particular - if so, it will check if 
            //the current ChatServer's username matches any of the names in 
            //the ArrayList and send the message to that person (plus the 
            //user themselves). If not the message is sent to everyone as 
            //normal
            for (ChatServer cs : ALL_CLIENTS)
            {
                //personalize join message for user joining the server:
                if (message.equals("Server - New User: " + username) && cs == this)
                    message = "Server - New User: You have successfully joined";

                try
                {
                    m = new StringMessage(message);
                    
                    //check if message is from server:
                    if (message.contains("Server"))
                        cs.oos.writeObject(m);
                    else
                    {
                        //if the mentions flag is true, restrict sending (and 
                        //displaying) the message to only the sender and all their 
                        //intended recipients. Otherwise send the message to 
                        //everyone and show who sent the message:
                        if (!mentions || (mentions && 
                                (allMentioned.contains(cs.username) || cs == this)))
                        {
                            //personalize output:
                            if (cs != this)
                                m = new StringMessage(username + ": " + message);
                            else
                                m = new StringMessage("You: " + message);
                            cs.oos.writeObject(m);
                        }
                    }
                    cs.oos.flush();
                }
                catch (IOException e)
                {}
            }
        }
        
        /**
         * Method which allows clients to obtain a list of the other connected 
         * clients, by sending them each member of the list through a 
         * ListMessage object (and two for loops). The ListMessage object 
         * extends Message, and contains the user's name and if they are being 
         * added or removed.
         * 
         * If the boolean add is true, all the active users in the list are 
         * sent to every client, who only adds them to the list if they are 
         * a new user. If it's false, all active clients are sent the name of 
         * the client who is leaving.
         * 
         * @param client the client to add/remove
         * @param add whether the client is being added to / removed from 
         * the server
         */
        public void updateList(String client, boolean add)
        {
            Message m;
            for (ChatServer cs : ALL_CLIENTS)   //update list for all clients
            {
                //go through list of connected clients' usernames:
                for (ChatServer cs2 : ALL_CLIENTS)
                {
                    try
                    {
                        //send each client all active users:
                        if (add)    //is true
                            m = new ListMessage(cs2.username, add);
                        //send each client only the removed user:
                        else
                            m = new ListMessage(client, add);
                        
                        cs.oos.writeObject(m);
                        cs.oos.flush();
                    }
                    catch (Exception e) //catch-all
                    {
                        System.out.println("Error: " + e);
                    }
                }
            }
        }
        
        /**
         * The run method, which passes messages from one client to another (or 
         * all clients) and sends its own messages to clients as well. It is 
         * also responsible for updating the ArrayList of clients.
         */
        @Override
        public void run() 
        {
            Message m;
            try 
            {
                //create an autoflush output stream for the socket, to the 
                //server:
                oos = new ObjectOutputStream(socket.getOutputStream());
                //create a buffered input stream for this socket:
                ois = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
                            
                //add this instance of subclass to current clients list:
                ALL_CLIENTS.add(this);
                
                //console printout on server side:
                System.out.println(username + " has joined!");
                
                //Pass client's username to their chat window:
                m = new StringMessage(username);
                oos.writeObject(m);
                oos.flush();
                
                //Notify new users of how to send messages to specified clients:
                m = new StringMessage("Server - To send a message to a specific client, "
                        + "type their name (e.g. CLIENT2) somewhere in the "
                        + "message.");
                oos.writeObject(m);
                oos.flush();

                //Server notifies all users of currently connected users, and 
                //the new user joining:
                updateList(username, true);
                sendMessage("Server - New User: " + username);
                
                //Send messages if: a) the message isn't empty and b) the 
                //user doesn't want to quit
                boolean hasQuit = false;
                while (!hasQuit) 
                {
                    Object input = ois.readObject();
                    
                    if (input instanceof StringMessage)
                    {
                        String message = ((StringMessage) input).getMessage();
                        
                        if (message.equals("quit"))   //end messages 
                        {
                            //Tell user they're about to be disconnected from 
                            //the server (if they haven't already closed the 
                            //window):
                            m = new StringMessage("You're disconnected "
                                    + "from the server. The window will "
                                    + "close in 5 seconds.");
                            oos.writeObject(m);
                            oos.flush();
                            hasQuit = true;
                        }
                        else if (!message.isEmpty())  //send the message
                            sendMessage(message);
                    }
                    else if (input instanceof ImageMessage)
                    {
                        //get ImageIcon:
                        ImageIcon image = ((ImageMessage) input).getImage();
                        
                        //set Message as an ImageMessage instance:
                        m = new ImageMessage(image);
                        
                        //notify users of the user sending the image:
                        sendMessage("Server: " + username + " has sent an image");
                        
                        for (ChatServer cs : ALL_CLIENTS)
                        {
                            cs.oos.writeObject(m);
                            cs.oos.flush();
                        }
                    }
                }
            } 
            catch (Exception e) //catch-all
            {
                System.out.println("Disconnection error: " + e);
            } 
            finally 
            {
                if (username != null) 
                {
                    //console printout on server side:
                    System.out.println(username + " has left");

                    //Remove this ChatServer instance from the list of 
                    //all ChatServer instances:
                    ALL_CLIENTS.remove(this);
                    
                    //Server notifies all users of user leaving, and updates 
                    //list on client side:
                    updateList(username, false);
                    sendMessage("Server - Departing User: " + username);
                }
                try 
                {
                    ois.close();
                    oos.close();
                    socket.close(); //close the socket
                    
                    if (ALL_CLIENTS.isEmpty())  //close the server
                    {
                        System.out.println("Server is closing!");
                        System.exit(0);
                    }
                } 
                catch (IOException e) 
                {
                    System.err.println("Server error with chat service: " + e);
                }
            }
        }
    }
}