import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.BindingGroup;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.ELProperty;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

/*
 * Created by JFormDesigner on Thu Jan 25 01:00:01 PST 2018
 */


public class ServerPanel extends JPanel implements ThreadCompletionListener
{
    // TODO -> "Save To" Variables
    private Path saveToPath = Paths.get ( FileSystemView.getFileSystemView ().getDefaultDirectory ().getPath () );
    private JFileChooser saveToFileChooser = null;

    // TODO -> Variables that will store Credentials and XOR-Key
    private final HashMap< String, String > credentialsMap = new HashMap<> ();

    private byte[] xorKey = null;

    // TODO -> Required Server Variables
    private ServerSocket serverSocket = null;
    private Socket clientSocket = null;
    private PrintWriter clientPrintWriter = null;
    private BufferedReader clientBufferedReader = null;

    private NotificationThread createServerThread = null;
    private NotificationThread responseThread = null;
    private NotificationThread fileThread = null;

    // TODO -> Useful for controlling Thread initiated by the "Start Server" Button
    private volatile boolean serverIsQuitting = false;

    // TODO -> When Set, Tells Server to Restart if a Disconnect Occurs
    private volatile boolean shouldRestartServer = false;

    private volatile boolean clientIsQuitting = false;

    public ServerPanel ()
    {
        initComponents ();

        // TODO -> Add Debug to Credentials Map
        credentialsMap.put ( "Debug", "" );

        // TODO -> Set "Save To" directory of Server as System Default
        saveTextArea.setText ( saveToPath.toString () );
    }

    @Override
    public void threadCompletedNotification ( Thread thread )
    {
        System.out.println ( "Server > threadCompletedNotification" );

        if ( thread.equals ( createServerThread ) )
        {
            createServerThread = null;

            if ( !serverIsQuitting )
            {
                // TODO -> Failed Authentication
                if ( shouldRestartServer )
                    stopButtonActionPerformed ();
                else
                    startResponseThread ();
            }
        }
        else if ( thread.equals ( responseThread ) )
        {
            responseThread = null;

            if ( clientIsQuitting )
                stopButtonActionPerformed ();
        }
        else if ( thread.equals ( fileThread ) )
        {
            fileThread = null;
        }
    }

    // TODO -> Waits For Input From The Client
    private void startResponseThread ()
    {
        System.out.println ( "Server > startResponseThread" );

        responseThread = new NotificationThread ()
        {
            @Override
            public void notifyingRunnable ()
            {
                while ( !clientIsQuitting && !serverIsQuitting )
                {
                    try
                    {
                        // TODO -> Only accept messages when Accepting File Thread is Null
                        // TODO -> If Accepting File Thread is not Null, then a File is being Processed
                        if ( fileThread == null && clientBufferedReader.ready () )
                        {
                            String serverInput = clientBufferedReader.readLine ();

                            System.out.println ( "Server > Received Message \"" + serverInput + "\"" );

                            // TODO -> Client has sent Quit message.
                            if ( serverInput.equals ( "CLIENT-QUIT" ) )
                            {
                                // TODO -> Update Log
                                String clientHostAddress = clientSocket.getInetAddress ().getHostAddress ();
                                logTextArea.append ( clientHostAddress + " has decided to close the connection" + "\n" );

                                // TODO -> Set Flags
                                shouldRestartServer = true;
                                clientIsQuitting = true;
                            }
                            // TODO -> Client has sent File message
                            else if ( serverInput.equals ( "CLIENT-FILE" ) )
                            {
                                System.out.println ( "Server > Processing File Request" );
                                startFileThread ();
                            }
                        }
                    }
                    catch ( IOException ioe )
                    {
                        ioe.printStackTrace ( System.err );
                    }
                }
            }
        };

        responseThread.addListener ( this );
        responseThread.start ();
    }


    // TODO -> Attempts to open Credentials file and populate Credentials ArrayList
    private void credentialsButtonActionPerformed ()
    {
        // TODO -> Find credentials file that will be used by the Server ( Should be Strings )
        JFileChooser fileChooser = new JFileChooser ( FileSystemView.getFileSystemView ().getDefaultDirectory () );
        fileChooser.setFileSelectionMode ( JFileChooser.FILES_ONLY );

        if ( fileChooser.showOpenDialog ( getParent () ) == JFileChooser.APPROVE_OPTION )
        {
            // TODO -> Get path to credentials
            Path credentialsPath = fileChooser.getSelectedFile ().toPath ();

            // TODO -> Try to open the file for reading and store every credential in said file
            try ( BufferedReader bufferedReader = Files.newBufferedReader ( credentialsPath ) )
            {
                for ( String line; ( line = bufferedReader.readLine () ) != null; )
                {
                    String[] usernameAndPassword = line.split ( "\\p{Space}?:\\p{Space}?" );

                    if ( usernameAndPassword.length == 2 )
                        credentialsMap.put ( usernameAndPassword[ 0 ], usernameAndPassword[ 1 ] );
                    else
                        credentialsMap.put ( usernameAndPassword[0], "" );
                }

                credentialsMap.put ( "Debug", "" );

                // TODO -> Update GUI
                credentialTextArea.setText ( credentialsPath.toString () );
            }
            // TODO -> Usually an encoding error. File is expected to contain Strings, and nothing else
            catch ( IOException ioe )
            {
                ioe.printStackTrace ( System.err );

                // TODO -> Clear all credentials that failed to load
                credentialsMap.clear ();

                // TODO -> Update GUI
                credentialTextArea.setText ( null );

                String message = "Credentials file could not be read";
                JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.ERROR_MESSAGE );
            }
        }
    }


    private void credentialClearButtonActionPerformed ()
    {
        // TODO -> Clear Credential Text Area
        credentialTextArea.setText ( null );
        credentialsMap.clear ();
        credentialsMap.put ( "Debug", "" );
    }


    // TODO -> Attempts to open file to be used as XOR-Key
    private void xorButtonActionPerformed ()
    {
        // TODO -> Load XOR Key file that will be used by the Server
        JFileChooser fileChooser = new JFileChooser ( FileSystemView.getFileSystemView ().getDefaultDirectory () );
        fileChooser.setFileSelectionMode ( JFileChooser.FILES_ONLY );

        if ( fileChooser.showOpenDialog ( getParent () ) == JFileChooser.APPROVE_OPTION )
        {
            // TODO -> Save Path
            Path xorPath = fileChooser.getSelectedFile ().toPath ();

            try ( FileInputStream fileInputStream = new FileInputStream ( xorPath.toFile () ) )
            {
                long fileSize = fileInputStream.getChannel ().size ();

                if ( fileSize > Integer.MAX_VALUE )
                    throw new IOException ();

                xorKey = new byte[ ( int ) fileSize ];

                // TODO -> Convert Bytes of file to a binary representation
                for ( Integer inputByte, count = 0; ( inputByte = fileInputStream.read () ) != -1; )
                    xorKey[ count++ ] = inputByte.byteValue ();

                xorTextArea.setText ( xorPath.toString () );

                System.out.println ( "Server > XOR Key Bytes : " + Arrays.toString ( xorKey ) );
            }

            catch ( IOException ioe )
            {
                System.out.println ( Arrays.toString ( ioe.getStackTrace () ) );

                xorKey = null;

                // TODO -> When XOR file could not be found, alert Server
                String message = "XOR file could not be read";
                JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.ERROR_MESSAGE );
            }
        }
    }


    private void xorClearButtonActionPerformed ()
    {
        // TODO -> Clear XOR Text Area
        xorTextArea.setText ( null );
        xorKey = null;
    }


    // TODO -> Attempts to find new "Save To" path
    private void saveToButtonActionPerformed ()
    {
        if ( saveToFileChooser == null )
        {
            saveToFileChooser = new JFileChooser ( saveToPath.toFile () );

            // TODO -> Customize settings for "Save To" JFileChooser
            saveToFileChooser.setFileSelectionMode ( JFileChooser.DIRECTORIES_ONLY );
            saveToFileChooser.setAcceptAllFileFilterUsed ( false );
            saveToFileChooser.setFileFilter ( new FileFilter ()
            {
                @Override
                public boolean accept ( File file ) { return file.isDirectory (); }

                @Override
                public String getDescription () { return "Directory"; }
            } );
        }


        if ( saveToFileChooser.showOpenDialog ( getParent () ) == JFileChooser.APPROVE_OPTION )
        {
            // TODO -> Update the "Save To" path and GUI
            saveToPath = Paths.get ( saveToFileChooser.getSelectedFile ().getPath () );
            saveTextArea.setText ( saveToPath.toString () );
        }
    }


    private void startFileThread ()
    {
        fileThread = new NotificationThread ()
        {
            @Override
            public void notifyingRunnable ()
            {
                try
                {
                    // TODO -> Receive filename
                    String filename = clientBufferedReader.readLine ();

                    // TODO -> Receive File-Options and Chunk-Size from Server
                    String[] fileInfo = clientBufferedReader.readLine ().split ( " && " );

                    // TODO -> Receive options
                    String options = fileInfo[ 0 ];

                    // TODO -> Receive size of file in Bytes
                    Long sizeOfFile = new Long ( fileInfo[ 1 ] );

                    boolean asciiArmoring = options.startsWith ( "A" );
                    String fileOption = options.substring ( options.length () - 1 );

                    // TODO -> Receive Chunk Size
                    int chunkSize = Integer.parseInt ( fileInfo[ 2 ] );

                    System.out.println ( "Server > Received \"" + filename + "\" with options \"" + options + "\" of size \"" + sizeOfFile + "\" in bytes and chunk-size of \"" + chunkSize + "\"" );

                    byte[] dataBytes = new byte[ sizeOfFile.intValue () ];
                    int counter = 0;

                    String clientInput;

                    while ( !serverIsQuitting && ( clientInput = clientBufferedReader.readLine () ) != null )
                    {
                        if ( clientInput.equals ( "BYTES-DONE" ) )
                        {
                            System.out.println ( "Server > File > BYTES-DONE" );

                            break;
                        }
                        else if ( clientInput.equals ( "FILE-CANCELLED" ) )
                        {
                            System.out.println ( "Transfer of file \"" + filename + "\" was cancelled" );
                            logTextArea.append ( "Transfer of file \"" + filename + "\" was cancelled" );

                            return;
                        }

                        // TODO -> Split Hash and Data into Separate Strings ( [ 0 ] is Hash and [ 1 ] is Data )
                        String[] hashAndData = clientInput.split ( " && " );

                        // TODO -> Get Client's Hash Value and Convert it to Bytes
                        String clientHashString = hashAndData[ 0 ];
                        byte[] clientHashBytes = Utility.stringToBytes ( clientHashString );

                        // TODO -> Get Client's String of Data Bytes
                        String clientDataString = hashAndData[ 1 ];

                        byte[] tempDataBytes;

                        // TODO -> Decode Data Bytes using ASCII-Armoring, if ASCII-Armoring was requested
                        if ( asciiArmoring )
                        {
                            System.out.println ( "Server > BASE64 > " + clientDataString );

                            tempDataBytes = MIME.base64Decoding ( clientDataString );
                        }
                        // TODO -> If ASCII-Armoring is not request, process Data Bytes normally
                        else
                        {
                            tempDataBytes = Utility.stringToBytes ( clientDataString );
                        }

                        // TODO -> Decrypt using Hash Bytes and Data Bytes using XOR Cipher, is available
                        if ( xorKey != null )
                        {
                            System.out.println ( "Server > XOR Data Bytes > " + clientDataString );
                            System.out.println ( "Server > XOR Hash Bytes > " + clientHashString );

                            tempDataBytes = XORCipher.decrypt ( tempDataBytes, xorKey );
                            clientHashBytes = XORCipher.decrypt ( clientHashBytes, xorKey );

                            clientDataString = Arrays.toString ( tempDataBytes );
                            clientHashString = Arrays.toString ( clientHashBytes );
                        }

                        // TODO -> Output the Plain ( Decrypted ) Hash Bytes and Data Bytes
                        System.out.println ( "Server > Plain Data Bytes > " + clientDataString );
                        System.out.println ( "Server > Plain Hash Bytes > " + clientHashString );

                        // TODO -> Compute Server-Side Hash Value ( Using Data Bytes ) and Compare With The Client-Hash-Value
                        // TODO -> If matching, proceed, but if not, then inform Client to retry
                        Long clientHashValue = Utility.bytesToLong ( clientHashBytes );
                        Long serverHashValue = Utility.hash ( tempDataBytes );

                        System.out.println ( "Server > Client's Plain Hash Value > " + clientHashValue );
                        System.out.println ( "Server > Plain Hash Value > " + serverHashValue );

                        Boolean identicalHash = serverHashValue.equals ( clientHashValue );

                        if ( !identicalHash )
                        {
                            System.out.println ( "Server > Hash Comparison Result > FAILED" );

                            clientPrintWriter.println ( "SERVER-HASH-FAILED" );
                        }
                        else
                        {
                            System.out.println ( "Server > Hash Comparison Result > SUCCESS" );

                            clientPrintWriter.println ( "SERVER-HASH-SUCCESS" );

                            // TODO -> Store Bytes with all other transferred Bytes
                            for ( byte byteValue : tempDataBytes )
                                if ( counter < dataBytes.length )
                                    dataBytes[ counter++ ] = byteValue;
                        }
                    }

                    if ( !serverIsQuitting )
                    {
                        Path filePath = Paths.get ( saveToPath.toString (), filename );

                        switch ( fileOption )
                        {
                            case "C":
                            {
                                if ( Files.exists ( filePath ) )
                                {
                                    int lastPeriod = filename.lastIndexOf ( "." );
                                    filename = filename.substring ( 0, lastPeriod ) + "@" + ( new Date () ) + "." + filename.substring ( lastPeriod + 1 );
                                    filePath = Paths.get ( saveToPath.toString (), filename );
                                }

                                Files.write ( filePath, dataBytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE );

                                logTextArea.append ( "\"" + filename + "\" was copied" + "\n" );

                                break;
                            }
                            case "O":
                            {
                                Files.write ( filePath, dataBytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING );

                                logTextArea.append ( "\"" + filename + "\" was overwritten" + "\n" );

                                break;
                            }
                        }
                    }
                }
                catch ( IOException ioe )
                {
                    ioe.printStackTrace ( System.err );
                }
            }
        };

        fileThread.addListener ( this );
        fileThread.start ();
    }


    // TODO -> As the name of the function implies, it performs many checks and if successful, starts the Server
    private void startButtonActionPerformed ()
    {
        System.out.println ( "Server > startButtonActionPerformed" );

        createServerThread = new NotificationThread ()
        {
            @Override
            public void notifyingRunnable ()
            {
                try
                {
                    // TODO -> Parse Port. If not a valid Integer, Exception will be thrown
                    String portString = portTextField.getText ();
                    Integer port = Integer.parseInt ( portString );

                    // TODO -> Update GUI
                    dynamicStatusLabel.setText ( "Listening" );
                    startButton.setEnabled ( false );

                    // TODO -> Update Log to show Server is now Listening
                    logTextArea.append ( "Server is now listening" + "\n" );

                    // TODO -> Initialize Socket/Stream Variables
                    serverSocket = new ServerSocket ( port );
                    clientSocket = serverSocket.accept ();

                    clientPrintWriter = new PrintWriter ( clientSocket.getOutputStream (), true );
                    clientBufferedReader = new BufferedReader ( new InputStreamReader ( clientSocket.getInputStream () ) );

                    // TODO -> Try to Authenticate Client
                    String encryptedCredentials = clientBufferedReader.readLine ();
                    boolean validClient = false;

                    // TODO -> Check to see if they match any of the stored Credentials
                    /*
                    for ( Map.Entry< String, String > entry : credentialsMap.entrySet () )
                    {
                        String potentialCredential = entry.getKey () + ":" + entry.getValue ();

                        if ( potentialCredential.equals ( encryptedCredentials ) )
                        {
                            validClient = true;
                            break;
                        }
                    }
                    */

                    for ( Map.Entry< String, String > entry : credentialsMap.entrySet () )
                    {
                        String potentialCredential = entry.getKey () + ":" + entry.getValue ();
                        String decryptedCredential = AES.decrypt ( encryptedCredentials, entry.getKey () );

                        if ( decryptedCredential != null && decryptedCredential.equals ( potentialCredential ) )
                        {
                            validClient = true;
                            break;
                        }
                    }

                    if ( validClient )
                    {
                        // TODO -> Send Success Response to Client
                        clientPrintWriter.println ( "AUTH-SUCCESS" );

                        // TODO -> Log Success Message to Log Text Area
                        String clientHostAddress = clientSocket.getInetAddress ().getHostAddress ();
                        logTextArea.append ( clientHostAddress + " has connected" + "\n" );

                        // TODO -> Update GUI with new Status and log the Client's connection
                        dynamicStatusLabel.setText ( "Running" );
                    }
                    else
                    {
                        // TODO -> Send Failed Response to Client
                        clientPrintWriter.println ( "AUTH-FAILED" );

                        // TODO -> Set Flag to restart Server
                        shouldRestartServer = true;

                        // TODO -> Log Failed Message to Log Text Area
                        String clientHostAddress = clientSocket.getInetAddress ().getHostAddress ();
                        logTextArea.append ( clientHostAddress + " tried to connect but failed authentication" + "\n" );
                    }
                }
                catch ( IOException ioe )
                {
                    // TODO -> Usually occurs when Port cannot be bound. E.g : Port 22 (SSH)
                    if ( ioe instanceof BindException )
                    {
                        String message = "Port \"" + portTextField.getText () + "\" is not usable";
                        JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.ERROR_MESSAGE );
                    }

                    ioe.printStackTrace ( System.err );
                }
                catch ( NumberFormatException nfe )
                {
                    // TODO -> Triggered when a Port is not a valid Integer. E.g : Port "Hello, World!"
                    String message = "\"Port\" must be a number";
                    JOptionPane.showMessageDialog ( getParent (), message, null, JOptionPane.ERROR_MESSAGE );

                    nfe.printStackTrace ( System.err );
                }
            }
        };

        createServerThread.setName ( "Server Creation Thread" );
        createServerThread.addListener ( this );
        createServerThread.start ();
    }


    // TODO -> Attempts to close any connections and tries to restore GUI for future connections
    private void stopButtonActionPerformed ()
    {
        System.out.println ( "Server > stopButtonActionPerformed" );

        // TODO -> When Creating a Server Thread, if the Server presses cancel before the Client has the opportunity to
        // TODO -> connect, then set the quit Flag ( serverIsQuitting ) and then terminate the Thread by closing
        // TODO -> serverSocket. Afterwards, wait for Thread to completely finish before moving on ( join )
        if ( createServerThread != null )
        {
            logTextArea.append ( "Server has terminated connection" + "\n" );

            serverIsQuitting = true;
            try
            {
                serverSocket.close ();
                createServerThread.join ();
            }
            catch ( IOException | InterruptedException exception )
            {
                exception.printStackTrace ( System.err );
            }
        }

        // TODO -> When Response Thread is active and Server presses disconnect, then terminate Thread by setting
        // TODO -> Flag ( serverIsQuitting ) which will break the Response Loop. Afterwards,  wait for Thread to
        // TODO -> completely finish before moving on ( join )
        // TODO -> When Response Thread is active and Server presses disconnect, then terminate Thread by setting
        // TODO -> Flag ( serverIsQuitting ) which will break the Response Loop. Afterwards,  wait for Thread to
        // TODO -> completely finish before moving on ( join )
        if ( responseThread != null || fileThread != null )
        {
            // TODO -> Break Response Loop's While Loop
            serverIsQuitting = true;
            try
            {
                if ( responseThread != null )
                    responseThread.join ();
                else
                    fileThread.join ();
            }
            catch ( InterruptedException ie )
            {
                ie.printStackTrace ( System.err );
            }

            // TODO -> Message Client that Server is Quitting
            if ( clientPrintWriter != null )
                clientPrintWriter.println ( "SERVER-QUIT" );

            logTextArea.append ( "Server decided to terminate connection" + "\n" );
        }

        // TODO -> Closes all Sockets needed by the Server
        try
        {
            if ( clientSocket != null )
            {
                System.out.println ( "Server > Closing Client Socket" );

                if ( !clientSocket.isClosed () )
                    clientSocket.close ();

                clientSocket = null;
                clientPrintWriter = null;
                clientBufferedReader = null;
            }

            if ( serverSocket != null )
            {
                System.out.println ( "Server > Closing Server Socket" );

                if ( !serverSocket.isClosed () )
                    serverSocket.close ();

                serverSocket = null;
            }
        }
        catch ( IOException ioe )
        {
            ioe.printStackTrace ();
        }

        // TODO -> Restore GUI
        dynamicStatusLabel.setText ( "Stopped" );
        startButton.setEnabled ( true );

        if ( !shouldRestartServer )
        {
            xorKey = null;

            credentialTextArea.setText ( null );
            xorTextArea.setText ( null );
            saveTextArea.setText ( saveToPath.toString () );
            portTextField.setText ( "1492" );
        }

        logTextArea.append ( "Server was terminated " + "\n\n" );

        if ( serverIsQuitting )
            serverIsQuitting = false;

        if ( clientIsQuitting )
            clientIsQuitting = false;

        // TODO -> Restart Server, if requested
        if ( shouldRestartServer )
        {
            shouldRestartServer = false;
            startButton.doClick ();
        }
    }


    private void initComponents ()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Evaluation license - Erik Huerta
        JLabel staticStatusLabel = new JLabel ();
        JButton credentialButton = new JButton ();
        JButton saveButton = new JButton ();
        startButton = new JButton ();
        JButton stopButton = new JButton ();
        JLabel portLabel = new JLabel ();
        portTextField = new JTextField ();
        dynamicStatusLabel = new JLabel ();
        JLabel logLabel = new JLabel ();
        JScrollPane logScrollPane = new JScrollPane ();
        logTextArea = new JTextArea ();
        JScrollPane saveScrollPane = new JScrollPane ();
        saveTextArea = new JTextArea ();
        JScrollPane credentialScrollPane = new JScrollPane ();
        credentialTextArea = new JTextArea ();
        JButton xorButton = new JButton ();
        JScrollPane xorScrollPane = new JScrollPane ();
        xorTextArea = new JTextArea ();
        JButton credentialClearButton = new JButton ();
        JButton xorClearButton = new JButton ();

        //======== this ========
        setPreferredSize ( new Dimension ( 400, 900 ) );
        setOpaque ( false );

        // JFormDesigner evaluation mark
        setBorder ( new javax.swing.border.CompoundBorder (
                new javax.swing.border.TitledBorder ( new javax.swing.border.EmptyBorder ( 0, 0, 0, 0 ),
                        "JFormDesigner Evaluation", javax.swing.border.TitledBorder.CENTER,
                        javax.swing.border.TitledBorder.BOTTOM, new java.awt.Font ( "Dialog", java.awt.Font.BOLD, 12 ),
                        java.awt.Color.red ), getBorder () ) );
        addPropertyChangeListener ( new java.beans.PropertyChangeListener ()
        {
            public void propertyChange ( java.beans.PropertyChangeEvent e )
            {
                if ( "border".equals ( e.getPropertyName () ) ) throw new RuntimeException ();
            }
        } );


        //---- staticStatusLabel ----
        staticStatusLabel.setText ( "Status" );
        staticStatusLabel.setFont ( staticStatusLabel.getFont ().deriveFont ( staticStatusLabel.getFont ().getStyle () | Font.BOLD, staticStatusLabel.getFont ().getSize () + 5f ) );

        //---- credentialButton ----
        credentialButton.setText ( "Credentials Path" );
        credentialButton.setFont ( credentialButton.getFont ().deriveFont ( credentialButton.getFont ().getStyle () | Font.BOLD, credentialButton.getFont ().getSize () + 5f ) );
        credentialButton.setHorizontalAlignment ( SwingConstants.LEADING );
        credentialButton.setToolTipText ( "Click" );
        credentialButton.addActionListener ( e -> credentialsButtonActionPerformed () );

        //---- saveButton ----
        saveButton.setText ( "Save-Files-To Path" );
        saveButton.setFont ( saveButton.getFont ().deriveFont ( saveButton.getFont ().getStyle () | Font.BOLD, saveButton.getFont ().getSize () + 5f ) );
        saveButton.setToolTipText ( "Click" );
        saveButton.setHorizontalAlignment ( SwingConstants.LEADING );
        saveButton.addActionListener ( e -> saveToButtonActionPerformed () );

        //---- startButton ----
        startButton.setText ( "Start" );
        startButton.setFont ( startButton.getFont ().deriveFont ( startButton.getFont ().getStyle () | Font.BOLD, startButton.getFont ().getSize () + 5f ) );
        startButton.addActionListener ( e -> startButtonActionPerformed () );

        //---- stopButton ----
        stopButton.setText ( "Stop" );
        stopButton.setFont ( stopButton.getFont ().deriveFont ( stopButton.getFont ().getStyle () | Font.BOLD, stopButton.getFont ().getSize () + 5f ) );
        stopButton.addActionListener ( e -> stopButtonActionPerformed () );

        //---- portLabel ----
        portLabel.setText ( "Port" );
        portLabel.setFont ( portLabel.getFont ().deriveFont ( portLabel.getFont ().getStyle () | Font.BOLD, portLabel.getFont ().getSize () + 5f ) );

        //---- portTextField ----
        portTextField.setText ( "1492" );
        portTextField.setBorder ( new MatteBorder ( 0, 0, 1, 0, new Color ( 204, 204, 204 ) ) );
        portTextField.setForeground ( new Color ( 153, 0, 0 ) );
        portTextField.setFont ( portTextField.getFont ().deriveFont ( Font.BOLD | Font.ITALIC ) );
        portTextField.setColumns ( 1 );

        //---- dynamicStatusLabel ----
        dynamicStatusLabel.setText ( "Stopped" );
        dynamicStatusLabel.setFont ( dynamicStatusLabel.getFont ().deriveFont ( Font.BOLD | Font.ITALIC ) );
        dynamicStatusLabel.setForeground ( new Color ( 153, 0, 0 ) );

        //---- logLabel ----
        logLabel.setText ( "Log" );
        logLabel.setFont ( logLabel.getFont ().deriveFont ( logLabel.getFont ().getStyle () | Font.BOLD, logLabel.getFont ().getSize () + 5f ) );

        //======== logScrollPane ========
        {
            logScrollPane.setVerticalScrollBarPolicy ( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
            logScrollPane.setBorder ( null );
            logScrollPane.setAutoscrolls ( true );

            //---- logTextArea ----
            logTextArea.setForeground ( new Color ( 153, 0, 0 ) );
            logTextArea.setRows ( 1 );
            logTextArea.setEditable ( false );
            logTextArea.setFont ( logTextArea.getFont ().deriveFont ( Font.BOLD | Font.ITALIC ) );
            logTextArea.setBorder ( null );
            logTextArea.setTabSize ( 0 );
            logTextArea.setColumns ( 1 );
            logScrollPane.setViewportView ( logTextArea );
        }

        //======== saveScrollPane ========
        {
            saveScrollPane.setVerticalScrollBarPolicy ( ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER );
            saveScrollPane.setBorder ( new MatteBorder ( 0, 0, 1, 0, new Color ( 204, 204, 204 ) ) );

            //---- saveTextArea ----
            saveTextArea.setFont ( saveTextArea.getFont ().deriveFont ( Font.BOLD | Font.ITALIC ) );
            saveTextArea.setForeground ( new Color ( 153, 0, 0 ) );
            saveTextArea.setEditable ( false );
            saveTextArea.setBorder ( null );
            saveTextArea.setTabSize ( 0 );
            saveTextArea.setRows ( 1 );
            saveTextArea.setColumns ( 1 );
            saveScrollPane.setViewportView ( saveTextArea );
        }

        //======== credentialScrollPane ========
        {
            credentialScrollPane.setVerticalScrollBarPolicy ( ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER );
            credentialScrollPane.setBorder ( new MatteBorder ( 0, 0, 1, 0, new Color ( 204, 204, 204 ) ) );

            //---- credentialTextArea ----
            credentialTextArea.setFont ( credentialTextArea.getFont ().deriveFont ( Font.BOLD | Font.ITALIC ) );
            credentialTextArea.setForeground ( new Color ( 153, 0, 0 ) );
            credentialTextArea.setRows ( 1 );
            credentialTextArea.setEditable ( false );
            credentialTextArea.setBorder ( null );
            credentialTextArea.setTabSize ( 0 );
            credentialScrollPane.setViewportView ( credentialTextArea );
        }

        //---- xorButton ----
        xorButton.setText ( "XOR-Key Path" );
        xorButton.setFont ( xorButton.getFont ().deriveFont ( xorButton.getFont ().getStyle () | Font.BOLD, xorButton.getFont ().getSize () + 5f ) );
        xorButton.setToolTipText ( "Click" );
        xorButton.setHorizontalAlignment ( SwingConstants.LEADING );
        xorButton.addActionListener ( e -> xorButtonActionPerformed () );

        //======== xorScrollPane ========
        {
            xorScrollPane.setVerticalScrollBarPolicy ( ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER );
            xorScrollPane.setBorder ( new MatteBorder ( 0, 0, 1, 0, new Color ( 204, 204, 204 ) ) );

            //---- xorTextArea ----
            xorTextArea.setRows ( 1 );
            xorTextArea.setTabSize ( 0 );
            xorTextArea.setBorder ( null );
            xorTextArea.setForeground ( new Color ( 153, 0, 0 ) );
            xorTextArea.setFont ( xorTextArea.getFont ().deriveFont ( Font.BOLD | Font.ITALIC ) );
            xorTextArea.setEditable ( false );
            xorTextArea.setColumns ( 1 );
            xorScrollPane.setViewportView ( xorTextArea );
        }

        //---- credentialClearButton ----
        credentialClearButton.setText ( "Clear" );
        credentialClearButton.setFont ( credentialClearButton.getFont ().deriveFont ( credentialClearButton.getFont ().getStyle () | Font.BOLD ) );
        credentialClearButton.setToolTipText ( "Click" );
        credentialClearButton.addActionListener ( e -> credentialClearButtonActionPerformed () );

        //---- xorClearButton ----
        xorClearButton.setText ( "Clear" );
        xorClearButton.setFont ( xorClearButton.getFont ().deriveFont ( xorClearButton.getFont ().getStyle () | Font.BOLD ) );
        xorClearButton.setToolTipText ( "Click" );
        xorClearButton.addActionListener ( e -> xorClearButtonActionPerformed () );

        GroupLayout layout = new GroupLayout ( this );
        setLayout ( layout );
        layout.setHorizontalGroup (
                layout.createParallelGroup ()
                        .addGroup ( layout.createSequentialGroup ()
                                .addGap ( 25, 25, 25 )
                                .addGroup ( layout.createParallelGroup ()
                                        .addGroup ( layout.createSequentialGroup ()
                                                .addComponent ( staticStatusLabel )
                                                .addContainerGap ( GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE ) )
                                        .addGroup ( GroupLayout.Alignment.TRAILING, layout.createSequentialGroup ()
                                                .addGroup ( layout.createParallelGroup ( GroupLayout.Alignment.TRAILING )
                                                        .addGroup ( layout.createSequentialGroup ()
                                                                .addComponent ( xorButton, GroupLayout.PREFERRED_SIZE, 160, GroupLayout.PREFERRED_SIZE )
                                                                .addPreferredGap ( LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE )
                                                                .addComponent ( xorClearButton ) )
                                                        .addComponent ( portTextField, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE )
                                                        .addComponent ( logScrollPane, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE )
                                                        .addComponent ( saveScrollPane, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE )
                                                        .addComponent ( xorScrollPane, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE )
                                                        .addComponent ( credentialScrollPane, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE )
                                                        .addComponent ( dynamicStatusLabel, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE )
                                                        .addGroup ( GroupLayout.Alignment.LEADING, layout.createSequentialGroup ()
                                                                .addComponent ( credentialButton, GroupLayout.PREFERRED_SIZE, 180, GroupLayout.PREFERRED_SIZE )
                                                                .addPreferredGap ( LayoutStyle.ComponentPlacement.RELATED, 66, Short.MAX_VALUE )
                                                                .addComponent ( credentialClearButton ) )
                                                        .addComponent ( stopButton, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE )
                                                        .addComponent ( startButton, GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE ) )
                                                .addGap ( 50, 50, 50 ) )
                                        .addGroup ( layout.createSequentialGroup ()
                                                .addGroup ( layout.createParallelGroup ()
                                                        .addComponent ( saveButton )
                                                        .addComponent ( logLabel )
                                                        .addComponent ( portLabel ) )
                                                .addGap ( 0, 0, Short.MAX_VALUE ) ) ) )
        );
        layout.setVerticalGroup (
                layout.createParallelGroup ()
                        .addGroup ( layout.createSequentialGroup ()
                                .addGap ( 25, 25, 25 )
                                .addComponent ( staticStatusLabel )
                                .addGap ( 18, 18, 18 )
                                .addComponent ( dynamicStatusLabel )
                                .addGap ( 18, 18, 18 )
                                .addGroup ( layout.createParallelGroup ( GroupLayout.Alignment.BASELINE )
                                        .addComponent ( credentialClearButton )
                                        .addComponent ( credentialButton ) )
                                .addGap ( 18, 18, 18 )
                                .addComponent ( credentialScrollPane, GroupLayout.PREFERRED_SIZE, 31, GroupLayout.PREFERRED_SIZE )
                                .addGap ( 18, 18, 18 )
                                .addGroup ( layout.createParallelGroup ( GroupLayout.Alignment.BASELINE )
                                        .addComponent ( xorButton )
                                        .addComponent ( xorClearButton ) )
                                .addGap ( 18, 18, 18 )
                                .addComponent ( xorScrollPane, GroupLayout.PREFERRED_SIZE, 31, GroupLayout.PREFERRED_SIZE )
                                .addGap ( 18, 18, 18 )
                                .addComponent ( saveButton )
                                .addGap ( 18, 18, 18 )
                                .addComponent ( saveScrollPane, GroupLayout.PREFERRED_SIZE, 31, GroupLayout.PREFERRED_SIZE )
                                .addGap ( 18, 18, 18 )
                                .addComponent ( portLabel )
                                .addGap ( 18, 18, 18 )
                                .addComponent ( portTextField, GroupLayout.PREFERRED_SIZE, 32, GroupLayout.PREFERRED_SIZE )
                                .addGap ( 18, 18, 18 )
                                .addComponent ( logLabel )
                                .addGap ( 18, 18, 18 )
                                .addComponent ( logScrollPane, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE )
                                .addPreferredGap ( LayoutStyle.ComponentPlacement.RELATED, 35, Short.MAX_VALUE )
                                .addComponent ( startButton )
                                .addPreferredGap ( LayoutStyle.ComponentPlacement.RELATED )
                                .addComponent ( stopButton )
                                .addGap ( 25, 25, 25 ) )
        );

        //---- bindings ----
        bindingGroup = new BindingGroup ();
        bindingGroup.addBinding ( Bindings.createAutoBinding ( UpdateStrategy.READ,
                startButton, ELProperty.create ( "${!enabled}" ),
                stopButton, BeanProperty.create ( "enabled" ) ) );
        bindingGroup.addBinding ( Bindings.createAutoBinding ( UpdateStrategy.READ,
                startButton, BeanProperty.create ( "enabled" ),
                saveButton, BeanProperty.create ( "enabled" ) ) );
        bindingGroup.addBinding ( Bindings.createAutoBinding ( UpdateStrategy.READ,
                startButton, BeanProperty.create ( "enabled" ),
                portTextField, BeanProperty.create ( "enabled" ) ) );
        bindingGroup.addBinding ( Bindings.createAutoBinding ( UpdateStrategy.READ,
                startButton, BeanProperty.create ( "enabled" ),
                saveTextArea, BeanProperty.create ( "enabled" ) ) );
        bindingGroup.addBinding ( Bindings.createAutoBinding ( UpdateStrategy.READ,
                startButton, BeanProperty.create ( "enabled" ),
                credentialTextArea, BeanProperty.create ( "enabled" ) ) );
        bindingGroup.addBinding ( Bindings.createAutoBinding ( UpdateStrategy.READ,
                startButton, BeanProperty.create ( "enabled" ),
                xorButton, BeanProperty.create ( "enabled" ) ) );
        bindingGroup.addBinding ( Bindings.createAutoBinding ( UpdateStrategy.READ,
                startButton, BeanProperty.create ( "enabled" ),
                credentialButton, BeanProperty.create ( "enabled" ) ) );
        bindingGroup.addBinding ( Bindings.createAutoBinding ( UpdateStrategy.READ,
                startButton, BeanProperty.create ( "enabled" ),
                xorTextArea, BeanProperty.create ( "enabled" ) ) );
        bindingGroup.addBinding ( Bindings.createAutoBinding ( UpdateStrategy.READ,
                startButton, BeanProperty.create ( "enabled" ),
                credentialClearButton, BeanProperty.create ( "enabled" ) ) );
        bindingGroup.addBinding ( Bindings.createAutoBinding ( UpdateStrategy.READ,
                startButton, BeanProperty.create ( "enabled" ),
                xorClearButton, BeanProperty.create ( "enabled" ) ) );
        bindingGroup.bind ();
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - Erik Huerta
    private JButton startButton;
    private JTextField portTextField;
    private JLabel dynamicStatusLabel;
    private JTextArea logTextArea;
    private JTextArea saveTextArea;
    private JTextArea credentialTextArea;
    private JTextArea xorTextArea;
    private BindingGroup bindingGroup;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
