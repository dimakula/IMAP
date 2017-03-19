
// command line parsing
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;

import java.io.*;
import java.net.InetAddress;

import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.*;

import java.math.BigInteger;

import javax.net.ssl.HandshakeCompletedListener;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public class ImapDownload {

    public static void main (String [] args) {

        Options options = new Options();

        Option S = Option.builder("S")
                .longOpt("Server")
                .hasArg()
                .required()
                .desc("Specify the imap server to connect to")
                .build();

        Option P = Option.builder("P")
                .longOpt("Port")
                .hasArg()
                .required()
                .desc("Connect to the specific port")
                .build();

        Option l = Option.builder ("l")
                .longOpt("Login")
                .hasArg()
                .required()
                .desc("Login name for imap server")
                .build();

        Option p = Option.builder ("p")
                .longOpt("Password")
                .hasArg()
                .desc("Password if not stdin")
                .build();

        Option d = Option.builder ("d")
                .longOpt("Delete")
                .desc("Delete mail after downloading")
                .build();

        Option a = Option.builder ("a")
                .longOpt("All")
                .desc("Download from all the folders")
                .build();

        Option f = Option.builder ("f")
                .longOpt("Folder")
                .required()
                .hasArg()
                .desc("Download from the specified folder")
                .build();

        Option help = Option.builder("h")
                .longOpt("help")
                .build();

        options.addOption(S);
        options.addOption(P);
        options.addOption(l);
        options.addOption(p);
        options.addOption(d);
        options.addOption(a);
        options.addOption(f);
        options.addOption(help);

        CommandLineParser parser = new DefaultParser();

        InetAddress server;
        String login, password, port;
        String [] folders;
        password = null;

        try {
            CommandLine line = parser.parse(options, args);

            // automatically generate the help statement
            HelpFormatter formatter = new HelpFormatter();

            if (line.hasOption('h')) {
                formatter.printHelp("Imap Downloader", options);

            } else {
                server = InetAddress.getByName (line.getOptionValue('S'));
                port = line.getOptionValue('P');
                login = line.getOptionValue('l');
                folders = line.getOptionValues('f');

                if (line.hasOption('p')) {
                    password = line.getOptionValue('p');
                }

                SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
                SSLSocket socket = (SSLSocket) ssf.createSocket(server, Integer.parseInt(port));

                String[] suites = socket.getSupportedCipherSuites();
                socket.setEnabledCipherSuites(suites);

                socket.addHandshakeCompletedListener(new HandshakeListener());

                SSLSession session = socket.getSession();
                Certificate[] certChain = session.getPeerCertificates();

                //BufferedWriter output = new BufferedWriter
                //        ( new OutputStreamWriter(socket.getOutputStream()));

                OutputStream output =
                        new BufferedOutputStream(socket.getOutputStream(),8192);

                BufferedReader input = new BufferedReader
                        (new InputStreamReader(socket.getInputStream()));

                BufferedReader stdIn = new BufferedReader
                        (new InputStreamReader(System.in));


                if (password == null) {
                    password = stdIn.readLine();
                }

                int counter = 0;

                // login to server
                String send = "$ LOGIN " + login + " " + password + "\r\n";
                output.write(send.getBytes());
                output.flush();

                ReadThread thread = new ReadThread (input);
                thread.

                for (String folder : folders) {
                    send = "$ list " + folder + " *" + "\r\n";
                    output.write (send.getBytes());
                    output.flush();
                }

                /*System.out.println("The Certificates used by peer");

                for (int i = 0; i < certChain.length; i++) {
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
                */
            }

        }

        catch (ParseException e) {
            System.err.println("Parsing failed: " + e.getMessage());
        }

        catch (UnknownHostException e) {
            System.err.println("Parsing server failed: " + e.getMessage());
        }

        catch (IOException e) {
            System.err.println("Socket failed: " + e.getMessage());
        }
    }
}

class HandshakeListener implements HandshakeCompletedListener {

    @Override
    public void handshakeCompleted(HandshakeCompletedEvent e) {
        System.out.println("Handshake succesful!");
    }
}

class ReadThread extends Thread {

    BufferedReader input;

    ReadThread (BufferedReader input) {
        this.input = input;
    }
    public void run() {
        String result;
        try {
            while ((result = input.readLine()) != null) {
                System.out.println(result);
            }
        }

        catch (IOException e) {
            System.out.println ("ReadThread: " + e.getMessage());
        }
    }
}