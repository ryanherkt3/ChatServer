package assignment;

import java.io.Serializable;

/**
 * A class which extends Message, and sends a primitive String of the client's 
 * username and a boolean add condition to the client, who unpacks it and 
 * determines whether to add or remove the client from the list (or do nothing 
 * at all if the client has already been added).
 * 
 * @author Ryan Herkt (ID: 18022861)
 */
public class ListMessage extends Message implements Serializable
{
    private String member;
    private boolean added;  //whether to add the client or remove it from list

    /**
     * Constructor for this class
     * 
     * @param client the client to add/remove
     * @param added whether the client is to be added/removed from list
     */
    public ListMessage(String client, boolean added)
    {
        super();
        this.member = client;
        this.added = added;
    }

    /**
     * Set the toSend object as the member to add/remove from the list.
     */
    @Override
    public void messageType() 
    {
        toSend = getMember();
    }

    /**
     * @return the message
     */
    public String getMember() 
    {
        return member;
    }

    /**
     * @return the added
     */
    public boolean isAdded() 
    {
        return added;
    }
}