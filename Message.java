package assignment;

import java.io.Serializable;

/**
 * An abstract class which determines what type of message object(s) to send 
 * over the sockets on both the server and client side
 * 
 * Subclasses:
 * 1) ListMessage - send a string ArrayList of clients from the server to all 
 * clients
 * 2) StringMessage - send a string message (received from the client, or from 
 * the server) to the intended clients, and from the client to the server
 * 3) ImageMessage - send an ImageIcon from the server to all clients
 * 
 * @author Ryan Herkt (ID: 18022861)
 */
public abstract class Message implements Serializable
{
    protected Object toSend;
    
    //abstract method to set the toSend attribute:
    public abstract void messageType();
}