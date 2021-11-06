package assignment;

import java.util.*;  
import javax.mail.*;  
import javax.mail.internet.*;
import javax.activation.*;  //not really needed
  
/**
 * Advanced Feature #1: When the server starts the methods in this class are 
 * used to send e-mails to a list of addresses notifying them of the IP 
 * address and port of the server. 
 * 
 * Design Pattern: This class implements the Singleton design pattern, as we 
 * only want to send the e-mails once. An instance of this class is called for 
 * from the Server's main method, after which it calls the setProperties 
 * method.
 * 
 * Note: I downloaded the jars (activation, mail) for this class from:
 * https://static.javatpoint.com/src/mail/mailactivation.zip
 * 
 * Note 2: I got the source code for this advanced feature from the 
 * TutorialsPoint page, but have adapted it for the purposes of the assignment 
 * (e.g. setting extra properties such as my email account's username and 
 * password). The web page where I got the code from is linked below. It was my 
 * idea to implement the Singleton design pattern for this class.
 * 
 * @author TutorialsPoint (original)
 * @see https://www.tutorialspoint.com/java/java_sending_email.htm
 * 
 * @author Ryan Herkt (adapted, ID: 18022861)
 */
public class SingletonServerEmail  
{  
    //create an object of SingletonServerEmail
    private static SingletonServerEmail serverEmail = new SingletonServerEmail();

    /**
     * Private, default constructor.
     */
    private SingletonServerEmail()
    {}
    
    /**
     * Method which returns only object of class available
     * 
     * @return one instance of this class
     */
    public static SingletonServerEmail getSingletonServerEmail()
    {
       return serverEmail;
    }
    
    /**
     * Set the properties in order to send the email.
     */
    public void setProperties()
    {  
        //Change email to your own (must be a gmail account):
        String sendFrom = "";
        //A list of email addresses to send the email to (change accordingly):
        String[] sendTo = new String[]{/*add email addresses in here*/};

        //Set properties:
        Properties properties = System.getProperties();  
        properties.setProperty("mail.smtp.host", "smtp.gmail.com");  
        properties.setProperty("mail.smtp.auth", "true");  
        properties.setProperty("mail.smtp.starttls.enable", "true");  
        properties.put("mail.smtp.port", 587);  
        properties.setProperty("mail.smtp.user", sendFrom);
        
		//Make the password the same as your gmail one:
		properties.setProperty("mail.smtp.password", "");
        
        //Get the session object, add credentials:
        Session session = Session.getDefaultInstance(properties, 
            new javax.mail.Authenticator() 
        {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() 
            {
                return new PasswordAuthentication(sendFrom, "$%N6QkY9d");
            }
        }); 
        
        sendMessage(session, sendFrom, sendTo);   //call sendMessage method
   }  
    
    /**
     * Method which sends the message to the email addresses
     * 
     * @param session the Session instance, has all the properties
     * @param sendFrom the address sending the email
     * @param sendTo the addresses receiving the email
     */
    public void sendMessage(Session session, String sendFrom, String[] sendTo)
    {
        //Compose and send message:
        try
        {  
            //Create MimeMessage object:
            MimeMessage message = new MimeMessage(session);  

            //Set the sender's address:
            message.setFrom(new InternetAddress(sendFrom));  

            //Add all recepients from the sendTo array:
            for (int i = 0; i < sendTo.length; i++)
            {
                message.addRecipient(javax.mail.Message.RecipientType.TO, 
                        new InternetAddress(sendTo[i]));
            }
            
            //Set the subject:
            message.setSubject("Client-Server Application");  
            
            //Set the content of the email:
            message.setText("Hello, \n\nI am starting a client-server "
                    + "application. The server's IP address is 'localhost' and "
                    + "its port number is 2207. \n\n - Ryan Herkt, AUT");  

            //Send message, check if sent:
            Transport.send(message);  
            System.out.println("The email has been sent!");
        }
        catch (MessagingException e) 
        {
            e.printStackTrace();
        }  
    }
}  