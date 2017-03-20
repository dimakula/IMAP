import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CLIHandler 
{
	///////////////////////////////
	// Private Static Constants  //
	///////////////////////////////
	
	private static final String DEF_SERVER_OPT = "S";
	private static final String DEF_SERVER_LONGOPT = "server";
	private static final String DEF_SERVER_OPT_DESC = "Specify the IMAP server to connect to.";

	private static final String DEF_PORT_OPT = "P";
	private static final String DEF_PORT_LONGOPT = "port";
	private static final String DEF_PORT_OPT_DESC = "Connect to the specified port.";
	
	private static final String DEF_LOGIN_OPT = "l";
	private static final String DEF_LOGIN_LONGOPT = "login";
	private static final String DEF_LOGIN_OPT_DESC = "Login name for the IMAP server.";
	
	private static final String DEF_PASSWORD_OPT = "p";
	private static final String DEF_PASSWORD_LONGOPT = "password";
	private static final String DEF_PASSWORD_OPT_DESC = "Password, if not stdin.";
	
	private static final String DEF_DELETE_OPT = "d";
	private static final String DEF_DELETE_LONGOPT = "delete";
	private static final String DEF_DELETE_OPT_DESC = "Delete mail after downloading.";
	
	private static final String DEF_ALL_OPT = "a";
	private static final String DEF_ALL_LONGOPT = "all";
	private static final String DEF_ALL_OPT_DESC = "Download from all subfolders.";
	
	private static final String DEF_FOLDERS_OPT = "f";
	private static final String DEF_FOLDERS_LONGOPT = "folder";
	private static final String DEF_FOLDERS_OPT_DESC = "Download from specified folder.";
	
	private static final String DEF_HELP_OPT = "h";
	private static final String DEF_HELP_LONGOPT = "help";
	private static final String DEF_HELP_OPT_DESC = "Display help literature.";
	
	private static final String DEF_PROG_DESC = "IMAP Downloader";
	private static final String DEF_PARSE_EXCEPT_MSG = "ERROR: Parsing Failed.";
	
	/////////////////////////
	// Private Member Data //
	/////////////////////////
	
	/* Holds all defined options. */
	private Options theCommandLineOptions;
	
	private CommandLineParser theCommandLineParser;
	
	private CommandLine theCommandLineHandler;
	
	private HelpFormatter theHelpFormatter;
	
	/* Individually specified options. */
	private Option theOption_S;
	private Option theOption_P;
	private Option theOption_l;
	private Option theOption_p;
	private Option theOption_d;
	private Option theOption_a; 
	private Option theOption_f;
	private Option theOption_h;
	
	////////////////////////////
	// Private Member Methods //
	////////////////////////////
	
	/**
	 * Initializes the options to predefined signifiers.
	 */
	private void defaultInitializer()
	{
		theOption_S = Option.builder(DEF_SERVER_OPT)
                .longOpt(DEF_SERVER_LONGOPT)
                .desc(DEF_SERVER_OPT_DESC)
                .hasArg()
                .required()
                .build();

		theCommandLineOptions.addOption(theOption_S);

		theOption_P = Option.builder(DEF_PORT_OPT)
                .longOpt(DEF_PORT_LONGOPT)
                .hasArg()
                .required()
                .desc(DEF_PORT_OPT_DESC)
                .build();

		theCommandLineOptions.addOption(theOption_P);

		theOption_l = Option.builder(DEF_LOGIN_OPT)
                .longOpt(DEF_LOGIN_LONGOPT)
                .hasArg()
                .required()
                .desc(DEF_LOGIN_OPT_DESC)
                .build();

		theCommandLineOptions.addOption(theOption_l);

		theOption_p = Option.builder(DEF_PASSWORD_OPT)
                .longOpt(DEF_PASSWORD_LONGOPT)
                .hasArg()
                .desc(DEF_PASSWORD_OPT_DESC)
                .build();

		theCommandLineOptions.addOption(theOption_p);

		theOption_d = Option.builder(DEF_DELETE_OPT).
                longOpt(DEF_DELETE_LONGOPT)
                .desc(DEF_DELETE_OPT_DESC)
                .build();

		theCommandLineOptions.addOption(theOption_d);

		theOption_a = Option.builder(DEF_ALL_OPT)
                .longOpt(DEF_ALL_LONGOPT)
                .desc(DEF_ALL_OPT_DESC)
                .build();

		theCommandLineOptions.addOption(theOption_a);

		theOption_f = Option.builder(DEF_FOLDERS_OPT)
                .longOpt(DEF_FOLDERS_LONGOPT)
                .required()
                .hasArg()
                .desc(DEF_FOLDERS_OPT_DESC).build();

		theCommandLineOptions.addOption(theOption_f);

		theOption_h = Option.builder(DEF_HELP_OPT)
                .longOpt(DEF_HELP_LONGOPT)
                .desc(DEF_HELP_OPT_DESC)
                .build();

		theCommandLineOptions.addOption(theOption_h);
		return;
	}
	
	///////////////////////////
	// Public Member Methods //
	///////////////////////////
	
	/**
	 * Default constructor.
	 * 
	 * @param args Arguments taken in from the command line at the beginning of execution.
	 */
	public CLIHandler(final String[] args)
	{
	    theCommandLineOptions = new Options ();
		/* Initialize Default Values */
		defaultInitializer();
		
		/*
		 * Initialize the command line handler and parser objects, and build
		 * the help statement.
		 */	
		try
		{
			theCommandLineParser = new DefaultParser();
			theCommandLineHandler = theCommandLineParser.parse( theCommandLineOptions, args);
			theHelpFormatter = new HelpFormatter();
		}
		catch( ParseException exception )
		{
			System.err.println( DEF_PARSE_EXCEPT_MSG );
		}
		
		/*Print the help statement if flag found.*/
		if( theCommandLineHandler.hasOption(DEF_HELP_OPT) )
		{
			theHelpFormatter.printHelp(DEF_PROG_DESC, theCommandLineOptions);
		}
		
		return;
	}
	
	/*
	 * Getters for all the opts.
	 */
	
	public String getPassword()
	{
		return theCommandLineHandler.getOptionValue(DEF_PASSWORD_OPT);
	}
	
	public String[] getFolders()
	{
		return theCommandLineHandler.getOptionValues(DEF_FOLDERS_OPT);
	}
	
	public String getPort()
	{
		return theCommandLineHandler.getOptionValue(DEF_PORT_OPT);
	}
	
	public String getLogin()
	{
		return theCommandLineHandler.getOptionValue( DEF_LOGIN_OPT );
	}
	
	public InetAddress getServer()
	{
		InetAddress output = null;
		try 
		{
			output = InetAddress.getByName(theCommandLineHandler.getOptionValue(DEF_SERVER_OPT));
		} 
		catch (UnknownHostException inException) {
			
			inException.printStackTrace();
		} 
		
		return output;
	}
	
}
