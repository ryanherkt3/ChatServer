package assignment;

import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.text.*;

/**
 * Class Description: The client interface class for this assignment, which is 
 * a GUI used to connect to the server and obtain a list of other currently 
 * connected clients (one by one from the server, via a ListMessage). 
 * It enables a client to send a text message to another chosen client (or all 
 * of them) or an image to all chosen clients via the server. It also displays 
 * the messages received from other clients. The list updates the GUI 
 * whenever a new client (including itself) connects to or disconnects 
 * from the server.
 * 
 * Threads:
 * 1) ClientGUI class EDT - checks for user input (ie action listener method)
 * 2) ClientGUI class' second thread - check for messages from server
 * 3) ClientMessages inner class thread/run method - send messages to server
 * 4) ClientImages inner class thread/run method - send images to server
 * 
 * @author Ryan Herkt (ID: 18022861)
 */
public class ClientGUI extends JPanel implements ActionListener, Runnable
{
    private final int HOST_PORT = 2207;  //host port number
    private final String HOST_NAME = "localhost";    //host name
    
    //attributes & component to keep track of connected clients:
    private JTextField connectedClients = new JTextField();
    private ArrayList<String> activeClients = new ArrayList<>();
    private String myName;  //the client's unique name
    
    private ObjectOutputStream oos;    //output stream to server
    private ObjectInputStream ois; //input stream from server
    
    private JFrame frame = new JFrame();  //frame for GUI
    
    //make a JTextPane for advanced feature #2 - sending images:
    private JTextPane sentMessages = new JTextPane();

    //components for sending a message:
    private JPanel sendMessagePanel = new JPanel(new GridLayout(1,1));
    private JTextField messageBox = new JTextField();  //text field to enter text
    private JButton sendMsgButton = new JButton("Send Text"); //button to send text
    private JButton sendImgButton = new JButton("Send Image"); //button to send images
    
    /**
     * Constructor which configures some of the GUI components and adds 
     * components to the overall frame.
     */
    public ClientGUI() 
    {
        //ensure users can't edit the message box or text area:
        messageBox.setEditable(false);
        sentMessages.setEditable(false);
        sentMessages.setContentType("text/html");
        
        //add action listeners:
        messageBox.addActionListener(this);
        sendMsgButton.addActionListener(this);
        sendImgButton.addActionListener(this);
        
        //add components to panel
        sendMessagePanel.add(messageBox);
        sendMessagePanel.add(sendMsgButton);
        sendMessagePanel.add(sendImgButton);
        
        //ensure client can't edit list of other active clients:
        connectedClients.setEditable(false);
        
        //add components to frame with appropriate positioning:
        frame.getContentPane().add(connectedClients, BorderLayout.NORTH);
        frame.getContentPane().add(sendMessagePanel, BorderLayout.SOUTH);
        frame.getContentPane().add(new JScrollPane(sentMessages));
    }

    /**
     * Thread #2 - responsible for listening for any messages from the server 
     * and determining what action to take (set client's name / update list of 
     * clients / show message).
     * 
     * After the loop exits, the client is notified of their disconnection from 
     * the server, where the I/O streams and socket are closed and they have 5 
     * seconds to read the message before the GUI frame is closed / disposed 
     * of.
     */
    @Override
    public void run() 
    {
        //create StyledDocument for the JTextPane and a 
        //SimpleAttributeSet for the StyledDocument's insertString 
        //method:
        StyledDocument sd = sentMessages.getStyledDocument();
        SimpleAttributeSet sas = new SimpleAttributeSet();
        
        Socket socket = null;
        
        try 
        {
            socket = new Socket(HOST_NAME, HOST_PORT);  //set up the socket
            
            //create an autoflush output stream for the socket, to the 
            //server:
            oos = new ObjectOutputStream(socket.getOutputStream());
            //create a buffered input stream for this socket, from the server:
            ois = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
            
            //listen for messages from the server until client quits server 
            //(at which point the server disconnects the client):
            Object obj;
            while ((obj=ois.readObject()) != null) 
            {
                if (obj instanceof ListMessage) //update the list
                {
                    //check if the user is being added or removed:
                    if (((ListMessage) obj).isAdded())
                    {
                        //only add the user if they are active and if they 
                        //haven't been added already:
                        if (!activeClients.contains(((ListMessage) obj).getMember()))
                            activeClients.add(((ListMessage) obj).getMember());
                    }
                    else    //remove the disconnected user from the list
                        activeClients.remove(((ListMessage) obj).getMember());
                    connectedClients.setText("Active Clients: " + activeClients);
                }
                else if (obj instanceof ImageMessage)
                {
                    //make label to put image on:
                    JLabel label = new JLabel(((ImageMessage) obj).getImage());

                    //set some attributes:
                    StyleContext context = new StyleContext();
                    Style labelStyle = context.getStyle(StyleContext.DEFAULT_STYLE);
                    StyleConstants.setComponent(labelStyle, label);

                    try
                    {
                        //display image to users:
                        sd.insertString(sd.getLength(), "\n", labelStyle);
                    }
                    catch (BadLocationException e)
                    {}
                }
                else if (obj instanceof StringMessage)
                {
                    String line = ((StringMessage) obj).getMessage();
                    
                    //get username from server, set myName as username and set 
                    //frame title accordingly:
                    if (myName == null)
                    {
                        myName = line;
                        this.frame.setTitle("Chat Service - " + myName);
                        messageBox.setEditable(true);
                    }
                    //update the message area by appending new messages to it. 
                    //sd.getLength() is used to insert the messages below 
                    //the previous ones:
                    else
                        sd.insertString(sd.getLength(), line + "\n", sas);
                }
            }
        } 
        catch (Exception e) //catch-all
        {}
        finally
        {
            //remove self from active clients, update list when done:
            activeClients.remove(myName);
            connectedClients.setText("Active Clients: " + activeClients);
            
            //disable the send message and send image buttons, and 'hide' 
            //the message box:
            messageBox.setEditable(false);
            sendMsgButton.setEnabled(false);
            sendImgButton.setEnabled(false);
            
            try
            {
                //close the input and output streams, and the socket:
                ois.close();
                oos.close();
                
                if (socket != null)
                    socket.close();
            }
            catch (IOException e) //catch-all
            {}
            
            //allow user to process info before auto-closing window:
            try
            {
                Thread.sleep(5000);
            }
            catch(InterruptedException e)
            {}
            frame.dispose();
        }
    }
    
    /**
     * Main method which runs a GUI that connects to the server (chat service).
     * 
     * @param args
     */
    public static void main(String[] args) throws IOException
    {
        ClientGUI client = new ClientGUI();
        client.createAndShowGUI();
        
        //create and start the second thread:
        Thread thread = new Thread(client);
        thread.start();
    }
    
    /**
     * Helper method to create the GUI and display it in the center of the 
     * screen.
     */
    public void createAndShowGUI()
    {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800,500);
        
        //position the frame in the middle of the screen:
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenDimension = tk.getScreenSize();
        Dimension frameDimension = frame.getSize();
        frame.setLocation((screenDimension.width-frameDimension.width)/2,
           (screenDimension.height-frameDimension.height)/2);
        frame.setVisible(true);
    }

    /**
     * A method which defers the responsibility of sending messages to a third 
     * thread, which creates an instance of the ClientMessages inner class, 
     * then creates and starts an instance of the new thread (with the 
     * ClientMessages instance in the thread's constructor).
     * 
     * Note that the EDT constantly checks for action events.
     * 
     * @param e 
     */
    @Override
    public void actionPerformed(ActionEvent e) 
    {
        Object source = e.getSource();
        
        //a client can send a text message by pressing 'enter' or the 
        //sendMsgButton
        if (source == messageBox || source == sendMsgButton)
        {
            //send message only if messageBox isn't empty:
            if (!messageBox.getText().isEmpty())
            {
                //initialize inner class instance with contents of message 
                //box:
                ClientMessages cm = new ClientMessages(messageBox.getText());
                
                //create third thread to send messages to server:
                Thread thread = new Thread(cm);
                thread.start();
                
                messageBox.setText(""); //reset message box text
            }
        }
        else if (source == sendImgButton) //user wants to send an image
        {
            //create file choose to choose image from:
            JFileChooser image = new JFileChooser();

            //set directory, title and file selection mode for file chooser:
            image.setCurrentDirectory(new java.io.File("."));
            image.setDialogTitle("Image to Send");
            image.setFileSelectionMode(JFileChooser.FILES_ONLY);
            
            //set the file to what the user chooses:
            if (image.showOpenDialog(sendImgButton) == JFileChooser.APPROVE_OPTION)
            {}
            File imageToSend = image.getSelectedFile();
            
            //check if the user actually chose a file:
            if (imageToSend != null)
            {
                //initialize inner class instance with contents of message 
                //box:
                ClientImages ci = new ClientImages(imageToSend.toString());
                
                //create third thread to send messages to server:
                Thread thread = new Thread(ci);
                thread.start();
            }
            else    //user hasn't chosen a file
                showErrorMessage("No file selected");
        }
    }
    
    /**
     * Method which shows an error message pane if when the user goes to choose 
     * an image they either select no file, or don't select a file with the 
     * correct image ending (.jpg or .png or .gif)
     * 
     * @param errorMsg 
     */
    public void showErrorMessage(String errorMsg)
    {
        JOptionPane.showMessageDialog(this, errorMsg, "ERROR",
            JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Inner class which contains the code for sending a message through the 
     * client's output stream, via its run method (third thread):
     */
    private class ClientMessages implements Runnable
    {
        private String message; //message to send to server
        
        /**
         * A constructor for the inner class, containing the user's message to 
         * be sent
         * 
         * @param send the message to send
         */
        public ClientMessages(String send)
        {
            this.message = send;
        }
        
        /**
         * Thread #3 - responsible for sending messages to server via output
         * stream.
         */
        @Override
        public void run() 
        {
            try
            {
                Message m = new StringMessage(message);
                oos.writeObject(m);
                oos.flush();    //flush the stream
            }
            catch (IOException e)
            {
                System.out.println("Writing error: " + e);
            }
        }
    }
    
    /**
     * Inner class which contains the code for sending an image through the 
     * client's output stream, via its run method (fourth thread):
     */
    private class ClientImages implements Runnable
    {
        private ImageIcon image; //ImageIcon to send to server
        
        /**
         * A constructor for the inner class, containing the image to be sent. 
         * Is declared as a new ImageIcon with its filename as the chosen 
         * File's toString.
         * 
         * @param send the ImageIcon's filename (as File's toString)
         */
        public ClientImages(String send)
        {
            this.image = new ImageIcon(send);
        }
        
        /**
         * Thread #4 - responsible for sending images to server via output
         * stream.
         */
        @Override
        public void run() 
        {
            //check if the ImageIcon's file ending is valid (i.e. an image):
            boolean validFileType = image.toString().endsWith(".png") || 
                image.toString().endsWith(".jpg") || 
                image.toString().endsWith(".gif") || 
                image.toString().endsWith(".PNG") || 
                image.toString().endsWith(".JPG") || 
                image.toString().endsWith(".GIF");

            try 
            {
                //if the file's an image, send it as an ImageMessage object 
                //across the socket:
                if (validFileType)
                {
                    Message m = new ImageMessage(image);
                    oos.writeObject(m);
                    oos.flush();    //flush the stream
                }
                //if not, tell the user to choose a file with the correct 
                //file type:
                else
                    showErrorMessage("Please choose a .gif, .jpg or .png file");
            } 
            catch(IOException ex) //catch IO exception
            {
                System.out.println("Writing error: " + ex);
            }
        }
    }
}