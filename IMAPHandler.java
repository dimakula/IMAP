import java.io.*;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.security.cert.Certificate;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.omg.PortableServer.ThreadPolicyOperations;

public class IMAPHandler 
{
	//////////////////////////////
	// Private Static Constants //
	//////////////////////////////
	
	private static final int DEF_OUTPUT_STREAM_SIZE = 8192;
	private static boolean DEF_AUTH_FLAG_SETTING = false;
	
	/* Default exception messages */
	private static final String DEF_PASSWORD_REQ_MSG = "Please enter your password:";
	
	/////////////////////////
	// Private Member Data //
	/////////////////////////
	
	/* Socket specific data. */
	private String[] theSuites;
	private SSLSocketFactory theSocketFactory;
	private SSLSocket theSocket;
	private SSLSession theSession;
	private Certificate[] theCertChain;
	
	/* Message specific data. */
	private OutputStream theOutputStream;
	private BufferedReader theInputBuffer;
	private BufferedReader theStdinBuffer;
	
	/* Internet address specific data. */
	private InetAddress theServer;
	private String thePort;
	private String thePassword;
	private String theLogin;
	private boolean theAuthenticationFlag;
	private String[] theFolders;
	
	////////////////////////////
	// Private Member Methods //
	////////////////////////////
	
	private void defaultInitialization()
	{
		theAuthenticationFlag = DEF_AUTH_FLAG_SETTING;
		theSocketFactory = (SSLSocketFactory)SSLSocketFactory.getDefault();
	
		try
		{
			theSocket = (SSLSocket) theSocketFactory.createSocket( theServer, Integer.parseInt(thePort) );
			theSuites = theSocket.getSupportedCipherSuites();
			theSocket.setEnabledCipherSuites(theSuites);
			theSocket.addHandshakeCompletedListener(new HandShakeListener());
			theSession = theSocket.getSession();
			theCertChain = theSession.getPeerCertificates();
			
			theOutputStream = new BufferedOutputStream(theSocket.getOutputStream(), DEF_OUTPUT_STREAM_SIZE );
			theInputBuffer = new BufferedReader(new InputStreamReader(theSocket.getInputStream() ) );
			theStdinBuffer = new BufferedReader( new InputStreamReader( System.in ) );
			
			/* 
			 * Keep soliciting a password from stdin while the password is 
			 * null. 
			 */
			while( thePassword.equals(null) )
			{
				requestPassword(theStdinBuffer);
			}
			
		}
		catch (NumberFormatException inException)
		{
			inException.printStackTrace();
		} 
		catch (IOException inException)
		{
			inException.printStackTrace();
		}
		
		return;
    }
	
	private void requestPassword( BufferedReader inReader )
	{
		/*Solicit a password from the user using the given buffered input reader.*/
		System.out.println(DEF_PASSWORD_REQ_MSG);
		try
		{
			thePassword = inReader.readLine();
		} 
		catch (IOException inException) {
			inException.printStackTrace();
		}
		return;
	}
	
	///////////////////////////
	// Public Member Methods //
	///////////////////////////
	
	public IMAPHandler(InetAddress inServer, String inPort, String inLogin, String inPassword,
					   String [] inFolders)
	{
		/* Member data get the parameter(s). */
		theServer = inServer;
		thePort = inPort;
		theLogin = inLogin;
		thePassword = inPassword;
		theFolders = inFolders;
		
		defaultInitialization();
		return;
	}
	
	public void login()
	{
		/* Assemble the login command string from the object fields. */
		final String loginCommand = "$ Login " + theLogin + " " + thePassword + "\r\n";  
		
		/* Attempt to login. Throw an error at failure. */
		try
		{
			theOutputStream.write(loginCommand.getBytes());
			theOutputStream.flush();
			
			/* Print authentication message. */
	        System.out.println(theInputBuffer.readLine ());
	        System.out.println(theInputBuffer.readLine ()); 
		}
		catch (IOException inException) {
			inException.printStackTrace();
		}	
		/* Flip the authentication flag.*/
		theAuthenticationFlag = true;
		return;
	}
	
	public void pullDirectories()
	{
		if( theAuthenticationFlag )
		{
			String result;
			
			/*
			 * Iterate through all the given directories and create them if
			 * they don't already exist.
			 */
			for( String folder : theFolders )
			{
				/* Create the command string. */
				final String dirCreateCommand = "$ SELECT " + folder + "\r\n";
				
				try
				{
					theOutputStream.write(dirCreateCommand.getBytes());
					theOutputStream.flush();

					/* checks the second word in the line to see if it's NO. */
                    //result = theInputBuffer.readLine().split(" ")[1];
                    
                    /* Grab the result an print it out. */
                    for( int i = 0; i < 8; i++ )
                    {
						result = theInputBuffer.readLine();
                    	System.out.println(result);
                    }
                    
				} catch (IOException theException) {
					
					theException.printStackTrace();
				}
				
			}
		}
	}
	
	////////////////////
	// Nested Classes //
	////////////////////
	
	/**
	 * Nested event class used for socket handshake.
	 */
	class HandShakeListener implements HandshakeCompletedListener
	{
		private static final String DEF_HANDSHAKE_MSG = "Handshake Successful!";
		
		@Override
		public void handshakeCompleted( HandshakeCompletedEvent inEvent)
		{
			System.out.println( DEF_HANDSHAKE_MSG );
		}
	}
}

/********************************
 * 
 * LEFTOVERS
 * 
 * 
 * 
    System.out.println("The Certificates used by peer");
 	
 	for (int i = 0; i < certChain.length; i++) 
 	{
 		System.out.println(((X509Certificate) certChain[i]).getSubjectDN());
	}
    
    System.out.println("Peer host is " + session.getPeerHost());
    System.out.println("Cipher is " + session.getCipherSuite());
    System.out.println("Protocol is " + session.getProtocol());
    System.out.println("ID is " + new BigInteger(session.getId()));
    System.out.println("Session created in " + session.getCreationTime());
    System.out.println("Session accessed in " + session.getLastAccessedTime());
    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    String x = in.readLine();
    System.out.println(x);
    in.close();
 *
 *
 */
