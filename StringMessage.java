package assignment;

import java.io.Serializable;

/**
 * A class which extends Message, and sends a primitive String message to the 
 * client, who unpacks it for display on their end.
 * 
 * @author Ryan Herkt (ID: 18022861)
 */
public class StringMessage extends Message implements Serializable
{
    private String message;

    /**
     * Constructor for this class
     * 
     * @param send the message to send
     */
    public StringMessage(String send)
    {
        super();
        this.message = send;
    }

    /**
     * Set the toSend object as the message to send.
     */
    @Override
    public void messageType() 
    {
        toSend = getMessage();
    }

    /**
     * @return the message
     */
    public String getMessage() 
    {
        return message;
    }
}