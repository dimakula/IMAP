
public class Main {

	public static void main(String[] args) {
		/* Organize command line arguments. */
		CLIHandler theClihandler = new CLIHandler(args);
		
		/*Spawn IMAP handler based on credentials.*/
		IMAPHandler theImapHandler = new IMAPHandler(theClihandler.getServer(), 
													 theClihandler.getPort(),
				                                     theClihandler.getLogin(), 
				                                     theClihandler.getPassword());
		
		/* Attempt to login and pull directories.  */
		theImapHandler.login();
		theImapHandler.pullDirectories();

		return;
	}

}
