import java.io.*;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.cert.Certificate;
import java.util.ArrayList;

import javax.management.relation.RelationServiceNotRegisteredException;
import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import java.util.regex.*;

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
	private static final String DEF_DIRECTORY_QUALIFIER = "HasNoChildren";
	
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
	private boolean theAllFlag;
	private boolean deleteFlag;
	
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
					   String [] inFolders, boolean inAllFlag, boolean inDeleteFlag)
	{
		/* Member data get the parameter(s). */
		theServer = inServer;
		thePort = inPort;
		theLogin = inLogin;
		thePassword = inPassword;
		theFolders = inFolders;
		theAllFlag = inAllFlag;
		deleteFlag = inDeleteFlag;
		
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
			sendCommand (loginCommand);
			
			/* Print authentication message. */
	        System.out.println(theInputBuffer.readLine ());
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
			String result, sender, subject, folderName;
			String [] splitResult, senders, subjects;

			/* If the all flag is set, all directories are polled. */
			if(theAllFlag)
			{
				theFolders = getFullDirectoryList();
			}
			
			/* Create the directories for storing the emails. */
			createDirectories();

			int numEmails = 0;
			int counter = 0; // Used to name the email folder
			
			/*
			 * Iterate through all the given directories and create them if
			 * they don't already exist.
			 */

			for( String folder : theFolders )
			{
				/* Create the command string. */
				String command = "$ SELECT " + folder + "\r\n";

				try
				{
					sendCommand (command);

					while((result = theInputBuffer.readLine()) != null) {

						// Get the number of emails present in the mailbox
						if (result.matches(".*\\bEXISTS\\b.*")) {
							numEmails = Integer.parseInt (result.replaceAll("\\D+",""));
						}

						// Breaks out on success or failure
						else if (result.matches(".*\\b(Success)\\b.*") ||
								result.matches(".*\\b(Failure)\\b.*")) {
							break;
						}
					}

					senders = new String [numEmails];
					subjects = new String [numEmails];

					command = "$ FETCH 1:" + numEmails + " (FLAGS BODY[HEADER.FIELDS (From)])\r\n";
					sendCommand (command);

					// Loop to get all the results of the fetch command
					for (int i = 0; i < numEmails; i++) {

						while ((result = theInputBuffer.readLine()) != null) {

							// Breaks out on success or failure, or parenthesis
							if (result.matches(".*\\b(Success)\\b.*") ||
									result.matches(".*\\b(Failure)\\b.*") ||
									result.equals(")")) {
								break;
							}

							// get the email between the angle  brackets
							if (result.contains("From")) {
								sender = theInputBuffer.readLine();
								sender = sender.split("<")[1].split(">")[0];
								senders[i] = sender;
							}
						}
					}

					theInputBuffer.readLine(); // get last success message out

					command = "$ FETCH 1:" + numEmails + " (FLAGS BODY[HEADER.FIELDS (Subject)])\r\n";
					sendCommand (command);

					for (int i = 0; i < numEmails; i++) {
						while ((result = theInputBuffer.readLine()) != null) {

							// Breaks out on success or failure
							if (result.matches(".*\\b(Success)\\b.*") ||
									result.matches(".*\\b(Failure)\\b.*") ||
									result.equals(")")) {
								break;
							}

							splitResult = result.split(":");
							if (splitResult[0].equals("Subject")) {
								subject = splitResult[1];
								subject = subject.trim();
								subject = subject.replaceAll("[^A-Za-z0-9]", "-");
								subjects[i] = subject;
							}
						}
					}

					theInputBuffer.readLine(); // get last success message

					command = "$ FETCH 1:" + numEmails + " BODY[TEXT]\r\n";
					sendCommand (command);


					// Create email folders within the mailbox
					for (int i = 0; i < numEmails; i++) {

						theInputBuffer.readLine(); // first line useless for our purposes
						folderName = Integer.toString(counter++) + "_" + senders[i] + "_" + subjects[i];
						new File (folder, folderName).mkdirs();

						PrintWriter writer = new PrintWriter(new FileWriter(
								folder + File.separator + folderName + File.separator + "content.txt"),
								true);

						while ((result = theInputBuffer.readLine()) != null) {

							if (result.equals(")")) {
								break;
							}

							writer.println (result);
						}

					}

					if (deleteFlag) {
						command = "$ DELETE " + folder + "\r\n";
						sendCommand(command);
					}

					return;

				} catch (IOException theException) {
					
					theException.printStackTrace();
				}
				
			}
		}
	}
	
	private String[] getFullDirectoryList()
	{
		ArrayList<String> output = new ArrayList<String>();
		String result;
		
		/* Regex variables. */
		Pattern extractionPattern = Pattern.compile("(\")(([A-Z]).*?)(\")$");
		Matcher extractionMatcher;
		
		/* Build the list command. */
		String listCommand = "$ LIST \"\" *\r\n" ;
		
		/* Attempt to run the command and extract the result. */
		try
		{
			theOutputStream.write(listCommand.getBytes());
			theOutputStream.flush();

		    while((result = theInputBuffer.readLine()) != null)
		    {

				if (result.matches(".*\\b(Success)\\b.*") ||
						result.matches(".*\\b(Failure)\\b.*")) {
					break;
				}

		    	// Check if matches no children before add.
		    	else if(result.contains("HasNoChildren"))
		    	{
		    		// get rid of all characters that can be problematic in creating the folder
					result = result.replaceAll("[^A-Za-z0-9\"\\/ ]", "");
					System.out.println (result);
		    		extractionMatcher = extractionPattern.matcher(result);
					extractionMatcher.find();
		    		result = extractionMatcher.group(2);
		    		output.add(result);

		    	}
		    }
		} 
		catch (IOException inException) {
			inException.printStackTrace();
		}
		
		return output.toArray(new String[output.size()]);
	}

	private void sendCommand (String command)
	{
		try
		{
			theOutputStream.write(command.getBytes());
			theOutputStream.flush();
		}

		catch (IOException exception)
		{
			exception.printStackTrace();
		}
	}

	private void createDirectories()
	{
		for( String folder : theFolders )
		{
			// Creates directories and any nested subdirectories
			if(new File(folder).mkdirs())
			{
				System.out.println("Created directory ./" + folder);
			}
			else
			{
				System.out.println("Failed to create directory ./" + folder);
			}
		}
		return;
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
