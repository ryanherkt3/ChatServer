package assignment;

import java.io.Serializable;
import javax.swing.ImageIcon;

/**
 * A class which extends Message, and sends an ImageIcon to the client, who 
 * unpacks it for display (inside a JLabel) on their end.
 * 
 * @author Ryan Herkt (ID: 18022861)
 */
public class ImageMessage extends Message implements Serializable
{
    private ImageIcon image;

    /**
     * Constructor for this class
     * 
     * @param image the image to send
     */
    public ImageMessage(ImageIcon image)
    {
        super();
        this.image = image;
    }

    /**
     * Set the toSend object as the image/ImageIcon to send.
     */
    @Override
    public void messageType() 
    {
        toSend = getImage();
    }

    /**
     * @return the message
     */
    public ImageIcon getImage() 
    {
        return image;
    }
}