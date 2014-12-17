package mail;
import java.net.*;
import java.io.*;
import java.util.*;

/**
 * Open an SMTP connection to a remote machine and send one mail.
 *
 */
public class SMTPConnection {
    /* The socket to the server */
    private Socket connection;

    /* Streams for reading and writing the socket */
    private BufferedReader fromServer;
    private DataOutputStream toServer;

    private static final int SMTP_PORT = 25;
    private static final String CRLF = "\r\n";
    private static String host = "smtp.";

    /* Are we connected? Used in close() to determine what to do. */
    private boolean isConnected = false;
    private String serverSentence;
    private String userName;
    private String password;

    /* Create an SMTPConnection object. Create the socket and the 
       associated streams. Initialize SMTP connection. */
    public SMTPConnection(Envelope envelope, String un, String pw) throws IOException {
    connection = new Socket(host.concat(envelope.DestHost), SMTP_PORT);
    fromServer = new BufferedReader(new InputStreamReader(connection.getInputStream()));
    toServer =   new DataOutputStream(connection.getOutputStream());
    userName = un.trim();
    password = pw.trim();
    /* Read a line from server and check that the reply code is 

220.
       If not, throw an IOException. */
    serverSentence = fromServer.readLine();
    System.out.println("connection response: "+serverSentence);
    
    if (220 != parseReply(serverSentence)) {
        System.out.println("connection failed");
        return;
    }

    /* SMTP handshake. We need the name of the local machine.
       Send the appropriate SMTP handshake command. */
    String localhost = "myhost";
    sendCommand("HELO "+localhost+CRLF, 250);

    isConnected = true;
    }

    /* Send the message. Write the correct SMTP-commands in the
       correct order. No checking for errors, just throw them to the
       caller. */
    public void send(Envelope envelope) throws IOException {
    /* Send all the necessary commands to send a message. Call
       sendCommand() to do the dirty work. Do _not_ catch the
       exception thrown from sendCommand(). */
        sendCommand("AUTH LOGIN"+CRLF, 334);
        //Base64Util.encode(userName.getBytes());
        //Base64Util.encode(password.getBytes());
        sendCommand(Base64Util.encode(userName.getBytes())+CRLF, 334);
        sendCommand(Base64Util.encode(password.getBytes())+CRLF, 235);
        sendCommand("MAIL FROM:"+envelope.Sender+CRLF, 250);
        sendCommand("RCPT TO:"+envelope.Recipient+CRLF, 250);
        sendCommand("DATA"+CRLF, 354);
        sendCommand(envelope.Message+CRLF+"."+CRLF, 250);
    }

    /* Close the connection. First, terminate on SMTP level, then
       close the socket. */
    public void close() {
    isConnected = false;
    try {
        sendCommand("QUIT"+CRLF, 221);
        connection.close();
    } catch (IOException e) {
        System.out.println("Unable to close connection: " + e);
        isConnected = true;
    }
    }

    /* Send an SMTP command to the server. Check that the reply code
       is what is is supposed to be according to RFC 821. */
    private void sendCommand(String command, int rc) throws 
IOException 

{
    /* Write command to server and read reply from server. */
        toServer.writeBytes(command);
        toServer.flush();
       serverSentence = fromServer.readLine();
       System.out.println(serverSentence);
       
       /* Check that the server's reply code is the same as the 
       parameter rc. If not, throw an IOException. */
       if (rc != parseReply(serverSentence)){
           throw new IOException("send command failed");
       }
    
    }

    /* Parse the reply line from the server. Returns the reply 
       code. */
    private int parseReply(String reply) {
        return Integer.parseInt(reply.split(" ")[0]);
    }

    /* Destructor. Closes the connection if something bad happens. */
    protected void finalize() throws Throwable {
    if(isConnected) {
        close();
    }
    super.finalize();
    }
}
