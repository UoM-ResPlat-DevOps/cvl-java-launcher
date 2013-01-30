package au.org.massive.launcher;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowListener;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.CellConstraints;
import java.util.prefs.Preferences;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.*;

import com.jcraft.jsch.*;

import com.turbovnc.rfb.Options;
import com.turbovnc.vncviewer.Tunnel;
import com.turbovnc.vncviewer.VncViewer;
import com.turbovnc.vncviewer.CConn;
import com.turbovnc.vncviewer.OptionsDialog;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;

import au.org.massive.launcher.VersionNumberCheck;
import au.org.massive.launcher.LauncherVersionNumber;
import au.org.massive.launcher.HtmlOptionPane;
import au.org.massive.launcher.StringAppender;
import au.org.massive.launcher.Pool;

public class LauncherMainFrame extends JFrame
{
    private Class launcherMainFrameClass = getClass();

    private LauncherMainFrame launcherMainFrame = this;

    // JW has modified TurboVNC's com.turbovnc.vncviewer.Viewport class to write to these variables.
    // Making the viewport static restricts the Launcher to only having one TurboVNC window open at a time (for now).
    public static JFrame turboVncViewport = null;
    // At present, the Launcher only uses the turboVncConnection object
    // to close the TurboVNC connection.  However, the CConn class contains
    // callbacks for the TurboVNC Options Dialog, so it would be nice to
    // be able to use it for that as well.
    public static CConn turboVncConnection = null;
    // If the TurboVNC Options Dialog is to be available outside of the Login thread,
    // (which it is in the Python Launcher), then we need to initialize a VncViewer
    // object and a CConn object outside of the login thread, so that we
    // can make use of the CConn object's OptionsDialogCallback methods.
    // Ths turboVncViewer object doesn't need to be public, because we 
    // will initialized it from within the LauncherMainFrame class, i.e.
    // it won't be written back to LauncherMainFrame by hacking into the
    // TurboVNC classes.
    private static com.turbovnc.vncviewer.VncViewer turboVncViewer = null;
    
    private OptionsDialog options;

    private Preferences prefs = Preferences.userNodeForPackage(au.org.massive.launcher.LauncherMainFrame.class);

    // For now, there is exactly one log window, however:
    //   (1) In the future, we might want allow the Launcher 
    //          to have multiple VNC sessions open at once.
    //   (2) Carlo has designed a new logging system.
    private JFrame launcherLogWindow = new JFrame("MASSIVE/CVL Launcher Log Window");
    private JTextArea launcherLogWindowTextArea = new JTextArea();
    private static Logger logger = Logger.getLogger(LauncherMainFrame.class);
    private static StringAppender sAppender = new StringAppender();

    private JTabbedPane tabbedPane = new JTabbedPane();

    private JComboBox massiveHostsComboBox = new JComboBox();
    private JComboBox massiveProjectsComboBox = new JComboBox();
    private JComboBox massiveVncDisplayResolutionComboBox = new JComboBox();
    private JComboBox massiveSshTunnelCiphersComboBox = new JComboBox();
    private JSpinner massiveHoursRequestedSpinnerField = new JSpinner();
    private JSpinner massiveVisNodesRequestedSpinnerField = new JSpinner();
    private JCheckBox massivePersistentModeCheckBox = new JCheckBox();
    private JTextField massiveUsernameField = new JTextField();
    private JPasswordField massivePasswordField = new JPasswordField();

    private String massiveHost = "";
    private String massiveProject = "";
    private String defaultProjectPlaceholder = "[Use my default project]";
    private String massiveHoursRequested = "4";
    private String massiveVisNodesRequested = "1";
    private ArrayList<String> massiveVisNodes = new ArrayList<String>();
    private boolean massivePersistentMode = false;
    private String massiveVncDisplayResolution = "";
    private String massiveSshTunnelCipher = "";
    private String massiveUsername = "";
    private String massivePassword = "";

    private JComboBox cvlHostsComboBox = new JComboBox();
    private JCheckBox cvlVncDisplayNumberAutomaticCheckBox = new JCheckBox("Automatic");
    private JSpinner cvlVncDisplayNumberSpinnerField = new JSpinner();
    private JComboBox cvlVncDisplayResolutionComboBox = new JComboBox();
    private JComboBox cvlSshTunnelCiphersComboBox = new JComboBox();
    private JTextField cvlUsernameField = new JTextField();
    private JPasswordField cvlPasswordField = new JPasswordField();

    private String cvlHost = "";
    private boolean cvlVncDisplayNumberAutomatic = true;
    private String cvlVncDisplayNumber = "1";
    private String cvlVncDisplayResolution = "";
    private String cvlSshTunnelCipher = "";
    private String cvlUsername = "";
    private String cvlPassword = "";

    private String massiveJobNumber = "";
    private String massiveJobNumberWithServer = "";

    private Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    private LauncherMenuBar launcherMainFrameMenuBar = null;
    private LauncherMenuBar launcherLogWindowMenuBar = null;

    public LauncherMainFrame()
    {
        // Reset logger configuration.
        Logger.getLogger(LauncherMainFrame.class).getLoggerRepository().resetConfiguration();
        String PATTERN = "%d [%p|%c|%C{1}] %m%n";

        // Add a console appender.
        ConsoleAppender console = new ConsoleAppender();
        console.setLayout(new PatternLayout(PATTERN));
        console.setThreshold(Level.DEBUG);
        console.activateOptions();
        Logger.getRootLogger().addAppender(console);

        // Add a string appender.
        sAppender.setLayout(new PatternLayout(PATTERN));
        sAppender.setThreshold(Level.DEBUG);
        sAppender.activateOptions();
        Logger.getRootLogger().addAppender(sAppender);

        VersionNumberCheck versionNumberCheck = new VersionNumberCheck();

        String versionNumberFromWebPage = new VersionNumberCheck().getVersionNumberFromWebPage();
        if (!LauncherVersionNumber.javaLauncherVersionNumber.equals(versionNumberFromWebPage))
        {
            Icon massiveIcon = new ImageIcon(launcherMainFrameClass.getResource("MASSIVElogoTransparent64x64.png"));
            String htmlContent = "You are running version " + LauncherVersionNumber.javaLauncherVersionNumber + "<br><br>" +
                                    "The latest version is " + versionNumberFromWebPage + "<br><br>" +
                                                "Please download a new version from:<br><br>" +
                                                "<a href=\"https://www.massive.org.au/userguide/cluster-instructions/massive-launcher\">https://www.massive.org.au/userguide/cluster-instructions/massive-launcher</a>";
            //JOptionPane.showMessageDialog(this, message, "MASSIVE/CVL Launcher Error", JOptionPane.ERROR_MESSAGE, massiveIcon);
            HtmlOptionPane.showMessageDialog(this, htmlContent, "MASSIVE/CVL Launcher Error", JOptionPane.ERROR_MESSAGE, massiveIcon);
            System.exit(0);
        }

        // Initialize Launcher's main frame

        launcherMainFrame.setTitle("MASSIVE/CVL Launcher");
        launcherMainFrame.setSize(440,500);
        launcherMainFrame.setLocationRelativeTo(null);
        //launcherMainFrame.setLocationByPlatform(true);

        // Initialize menu for Launcher's main frame and log window
        
        if (System.getProperty("os.name").startsWith("Mac")) 
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        launcherMainFrame.launcherMainFrameMenuBar = new LauncherMenuBar(launcherMainFrame);
        launcherMainFrame.setJMenuBar(launcherMainFrame.launcherMainFrameMenuBar);

        launcherMainFrame.addWindowListener(new WindowAdapter()
        {
            public void windowActivated(WindowEvent e)
            {
                launcherMainFrame.launcherMainFrameMenuBar.selectLauncherMainFrameInWindowMenu();
                if (launcherLogWindowMenuBar!=null)
                    launcherMainFrame.launcherLogWindowMenuBar.selectLauncherMainFrameInWindowMenu();
            }
        });

        launcherLogWindow.addWindowListener(new WindowAdapter()
        {
            public void windowActivated(WindowEvent e)
            {
                launcherMainFrame.launcherMainFrameMenuBar.selectLauncherLogWindowInWindowMenu();
                if (launcherLogWindowMenuBar!=null)
                    launcherMainFrame.launcherLogWindowMenuBar.selectLauncherLogWindowInWindowMenu();
            }
        });

        // Initialize TurboVNC objects, so that we can plug the TurboVNC Options Dialog into the Launcher.
        VncViewer.setLookAndFeel();
        String[] turboVncArguments = new String[] {};
        LauncherMainFrame.turboVncViewer = new VncViewer(turboVncArguments);
        // For now, we are only allowing one connection per Launcher instance, so makeing turboVncConnection static is OK.
        boolean performInitialisation = false;
        // In TurboVNC, the CConn object would normally be created from the worker thread, equivalent of our login thread.
        // But we can't do that if we want the Options Dialog callbacks to be available before the login thread has commenced.
        LauncherMainFrame.turboVncConnection = new CConn(LauncherMainFrame.turboVncViewer, null, performInitialisation);

        massiveHost = prefs.get("massiveHost", "");
        massiveProject = prefs.get("massiveProject", "");
        massiveHoursRequested = prefs.get("massiveHoursRequested", "4");
        massiveVisNodesRequested = prefs.get("massiveVisNodesRequested", "1");
        String massivePersistentModeString = prefs.get("massivePersistentMode", "False");
        massivePersistentMode = massivePersistentModeString.equals("True");
        massiveVncDisplayResolution = prefs.get("massiveVncDisplayResolution", "");
        massiveSshTunnelCipher = prefs.get("massiveSshTunnelCipher", "");
        massiveUsername = prefs.get("massiveUsername", "");

        cvlHost = prefs.get("cvlHost", "");
        cvlVncDisplayResolution = prefs.get("cvlVncDisplayResolution", "");
        cvlSshTunnelCipher = prefs.get("cvlSshTunnelCipher", "");
        cvlUsername = prefs.get("cvlUsername", "");

        JPanel massivePanel = new JPanel();
        tabbedPane.addTab("MASSIVE", massivePanel);
        JPanel cvlPanel = new JPanel();
        tabbedPane.addTab("CVL", cvlPanel);

        getContentPane().setLayout(new BorderLayout());

        getContentPane().add(tabbedPane,BorderLayout.CENTER);
        getContentPane().add(new JLabel(" "),BorderLayout.NORTH);

        JPanel buttonsPanel = new JPanel();
        JButton optionsButton = new JButton("Options...");

        options = LauncherMainFrame.turboVncConnection.getOptionsDialog();

        optionsButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                 options.showDialog(launcherMainFrame);
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                System.exit(0);
            }
        });

        JButton loginButton = new JButton("Login");
        getRootPane().setDefaultButton(loginButton);
        loginButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                Thread loginThread = new Thread()
                {
                    public void run()
                    {
                        boolean massiveTabSelected = (tabbedPane.getSelectedIndex()==0);
                        boolean cvlTabSelected = (tabbedPane.getSelectedIndex()==1);

                        // Only supporting access to a single visnode for now...
                        String massiveVisNode = "";

                        if (massiveTabSelected)
                        {
                            massiveHost = (String) massiveHostsComboBox.getSelectedItem();
                            massiveProject = (String) massiveProjectsComboBox.getSelectedItem();
                            massiveHoursRequested = "" + massiveHoursRequestedSpinnerField.getValue();
                            massiveVisNodesRequested = "" + massiveVisNodesRequestedSpinnerField.getValue();
                            massivePersistentMode = massivePersistentModeCheckBox.isSelected();
                            massiveVncDisplayResolution = (String) massiveVncDisplayResolutionComboBox.getSelectedItem();
                            massiveSshTunnelCipher = (String) massiveSshTunnelCiphersComboBox.getSelectedItem();
                            massiveUsername = massiveUsernameField.getText();
                            massivePassword = massivePasswordField.getText();

                            if (massiveProject == defaultProjectPlaceholder)
                            {
                                try
                                {
                                    XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
                                    config.setServerURL(new java.net.URL("https://m2-web.massive.org.au/kgadmin/xmlrpc/"));
                                    XmlRpcClient client = new XmlRpcClient();
                                    client.setConfig(config);
                                    Object[] params = new Object[]{massiveUsername};
                                    massiveProject = (String) client.execute("get_project", params);
                                }
                                catch(java.net.MalformedURLException mue)
                                {
                                }
                                catch(org.apache.xmlrpc.XmlRpcException xre)
                                {
                                }
                            }

                            prefs.put("massiveHost", massiveHost);
                            prefs.put("massiveProject", massiveProject);
                            prefs.put("massiveHoursRequested", massiveHoursRequested);
                            prefs.put("massiveVisNodesRequested", massiveVisNodesRequested);
                            prefs.put("massivePersistentMode", (massivePersistentMode?"True":"False"));
                            prefs.put("massiveVncDisplayResolution", massiveVncDisplayResolution);
                            prefs.put("massiveSshTunnelCipher", massiveSshTunnelCipher);
                            prefs.put("massiveUsername", massiveUsername);
                        }

                        if (cvlTabSelected)
                        {
                            cvlHost = (String) cvlHostsComboBox.getSelectedItem();
                            cvlVncDisplayNumberAutomatic = cvlVncDisplayNumberAutomaticCheckBox.isSelected();
                            cvlVncDisplayNumber = "" + cvlVncDisplayNumberSpinnerField.getValue();
                            cvlVncDisplayResolution = (String) cvlVncDisplayResolutionComboBox.getSelectedItem();
                            cvlSshTunnelCipher = (String) cvlSshTunnelCiphersComboBox.getSelectedItem();
                            cvlUsername = cvlUsernameField.getText();
                            cvlPassword = cvlPasswordField.getText();

                            prefs.put("cvlHost", cvlHost);
                            prefs.put("cvlVncDisplayResolution", cvlVncDisplayResolution);
                            prefs.put("cvlSshTunnelCipher", cvlSshTunnelCipher);
                            prefs.put("cvlUsername", cvlUsername);
                        }

                        launcherLogWindowTextArea.setFont(new Font("Courier", Font.PLAIN, 13));
                        launcherLogWindow.getContentPane().setLayout(new BorderLayout());
                        launcherLogWindowTextArea.setText("");
                        launcherLogWindowTextArea.setLineWrap(true);
                        launcherLogWindowTextArea.setEditable(false);
                        JScrollPane scrollPane = new JScrollPane();
                        scrollPane.setViewportView(launcherLogWindowTextArea);
                        launcherLogWindow.getContentPane().add(scrollPane, BorderLayout.CENTER);
                        launcherLogWindow.setSize(700,450);
                        launcherLogWindow.setLocationByPlatform(true);
                        if (System.getProperty("os.name").startsWith("Mac")) 
                        {
                            launcherMainFrame.launcherLogWindowMenuBar = new LauncherMenuBar(launcherMainFrame, launcherLogWindow);
                            launcherMainFrame.launcherMainFrameMenuBar.addLogWindowToWindowsMenu(launcherLogWindow);
                            launcherLogWindow.setJMenuBar(launcherMainFrame.launcherLogWindowMenuBar);
                        }
                        launcherLogWindow.setVisible(true);

                        //RedirectSystemStreams.redirectSystemStreams(launcherLogWindowTextArea);

                        try
                        {
                            JSch jsch = new JSch();
                            Session session = null;

                            if (massiveTabSelected)
                            {
                                writeToLogWindow(launcherLogWindowTextArea, "Attempting to log in to " + massiveHost + "...\n");
                                session = jsch.getSession(massiveUsername, massiveHost, 22);
                                session.setPassword(massivePassword);
                                session.setConfig("StrictHostKeyChecking", "no");
                                session.connect();
                                session.setServerAliveInterval(1000);
                                session.setServerAliveCountMax(30);
                            }

                            if (cvlTabSelected)
                            {
                                writeToLogWindow(launcherLogWindowTextArea, "Attempting to log in to " + cvlHost + "...\n");
                                session = jsch.getSession(cvlUsername, cvlHost, 22);
                                session.setPassword(cvlPassword);
                                session.setConfig("StrictHostKeyChecking", "no");
                                session.connect();
                                session.setServerAliveInterval(1000);
                                session.setServerAliveCountMax(30);
                            }

                            writeToLogWindow(launcherLogWindowTextArea, "First login done.\n");
                            writeToLogWindow(launcherLogWindowTextArea, "\n");

                            RemoteCommand remoteCommand;
                            String commandOutput;

                            if (massiveTabSelected)
                            {
                                if (!massivePersistentMode)
                                {
                                    writeToLogWindow(launcherLogWindowTextArea, "Checking whether you have any existing jobs in the Vis node queue...\n");
                                    remoteCommand = new RemoteCommand("/usr/local/bin/showq -w class:vis -u " + massiveUsername + " | grep " + massiveUsername);
                                    commandOutput = sendCommand(session, remoteCommand, true, launcherLogWindowTextArea);
                                    if (remoteCommand.exitCode==0)
                                    {
                                        writeToLogWindow(launcherLogWindowTextArea, commandOutput + "\n");
                                        writeToLogWindow(launcherLogWindowTextArea, "Error: MASSIVE Launcher only allows you to have one job in the Vis node queue.\n");
                                        throw new Exception("Error: MASSIVE Launcher only allows you to have one job in the Vis node queue.\n");
                                    }
                                    else
                                        writeToLogWindow(launcherLogWindowTextArea, "You don't have any jobs already in the Vis node queue, which is good.\n");

                                    writeToLogWindow(launcherLogWindowTextArea, "\n");
                                }

                                writeToLogWindow(launcherLogWindowTextArea, "Checking quota...\n");

                                remoteCommand = new RemoteCommand("mybalance --hours");
                                commandOutput = sendCommand(session, remoteCommand, true, launcherLogWindowTextArea);
                                writeToLogWindow(launcherLogWindowTextArea, commandOutput + "\n");

                                if (massiveHost.startsWith("m2"))
                                {
                                    remoteCommand = new RemoteCommand("echo `showq -w class:vis | grep \"processors in use by local jobs\" | awk '{print $1}'` of 9 nodes in use");
                                    commandOutput = sendCommand(session, remoteCommand, false, launcherLogWindowTextArea);
                                    writeToLogWindow(launcherLogWindowTextArea, commandOutput + "\n");
                                }
                            }

                            if (massiveTabSelected)
                            {
                                // For CVL, this is done in an argument to the vncsession script.
                                remoteCommand = new RemoteCommand("/usr/local/desktop/set_display_resolution.sh " + massiveVncDisplayResolution);
                                sendCommand(session, remoteCommand, true, launcherLogWindowTextArea);
                                writeToLogWindow(launcherLogWindowTextArea, remoteCommand.getStdout() + "\n");
                            }

                            if (massiveTabSelected)
                            {
                                writeToLogWindow(launcherLogWindowTextArea, "Requesting remote desktop...\n");

                                // For now, when using the JSch API, I can only get this to work in
                                //  batch mode (a.k.a. persistent mode), not in interactive mode.
                                //  Instead of determining whether qsub is run in interactive mode
                                //  or batch mode, the "persistent mode" checkbox could simply determine
                                //  whether the Launcher performs a qdel when it exits.
                                //  Two new options at the end of request_visnode.sh specify whether 
                                //  request_visnode.sh should run qstat (to monitor whether the job has started)
                                //  and qpeek to get the output from the job once it has started.
                                //  The Launcher is using False False, because it will run qstat and qpeek later.
                                RemoteCommand qsubCommand = new RemoteCommand("/usr/local/desktop/request_visnode.sh " + massiveProject + " " + massiveHoursRequested + " " + massiveVisNodesRequested + " " + (massivePersistentMode?"True":"True") + " False False");
                                sendCommand(session, qsubCommand, true, launcherLogWindowTextArea); 
                                String[] qsubOutput = qsubCommand.getStdout().split("\n");
                                massiveJobNumberWithServer = qsubOutput[1];

                                writeToLogWindow(launcherLogWindowTextArea, "\n");

                                writeToLogWindow(launcherLogWindowTextArea, "MASSIVE job number: " + massiveJobNumberWithServer + "\n");

                                if (massiveJobNumberWithServer.contains("."))
                                {
                                    String[] massiveJobNumberWithServerComponents = massiveJobNumberWithServer.split("\\.");
                                    massiveJobNumber = massiveJobNumberWithServerComponents[0];
                                }
                                else
                                {
                                    writeToLogWindow(launcherLogWindowTextArea, "Malformed MASSIVE job number.\n");
                                    throw new Exception("Malformed MASSIVE job number.");
                                }

                                writeToLogWindow(launcherLogWindowTextArea, "\nWaiting for vis node(s)");

                                writeToLogWindow(launcherLogWindowTextArea, ".");
                                RemoteCommand qstatCommand = new RemoteCommand("qstat  -f " + massiveJobNumber + " | grep exec_host");
                                String execHostString = sendCommand(session, qstatCommand, false, launcherLogWindowTextArea);
                                //if (qstatCommand.getExitCode()==0)
                                    //writeToLogWindow(launcherLogWindowTextArea, execHostString + "\n");
                                while (qstatCommand.getExitCode()!=0)
                                {
                                    Thread.sleep(1000);
                                    writeToLogWindow(launcherLogWindowTextArea, ".");
                                    qstatCommand = new RemoteCommand("qstat  -f " + massiveJobNumber + " | grep exec_host");
                                    execHostString = sendCommand(session, qstatCommand, false, launcherLogWindowTextArea);
                                }
                                writeToLogWindow(launcherLogWindowTextArea, "\n\n");

                                if (execHostString.contains("exec_host = "))
                                {
                                    String[] execHostStringComponents = execHostString.split("exec_host = ");
                                    execHostString = execHostStringComponents[1];
                                    execHostStringComponents = execHostString.split("\\/");
                                    massiveVisNode = execHostStringComponents[0];
                                    writeToLogWindow(launcherLogWindowTextArea, "Vis node: " + massiveVisNode + "\n");
                                }
                                else
                                {
                                    writeToLogWindow(launcherLogWindowTextArea, "Malformed exec_hosts string in qstat output: " + execHostString + "\n");
                                    throw new Exception("Malformed exec_hosts string in qstat output: " + execHostString);
                                }

                                massiveVisNodes.add(massiveVisNode);
                            }

                            if (cvlTabSelected)
                            {
                                remoteCommand = new RemoteCommand("vncsession --vnc tigervnc --geometry \"" + cvlVncDisplayResolution + "\"");
                                //remoteCommand = new RemoteCommand("vncsession --vnc turbovnc --geometry \"" + cvlVncDisplayResolution + "\"");
                                if (!cvlVncDisplayNumberAutomatic)
                                    remoteCommand.setCommand(remoteCommand.getCommand() + " --display " + cvlVncDisplayNumber);
                                sendCommand(session, remoteCommand, true, launcherLogWindowTextArea);
                                String vncsessionStderr = remoteCommand.getStderr();
                                writeToLogWindow(launcherLogWindowTextArea, vncsessionStderr);

                                String[] vncsessionStderrLines = vncsessionStderr.split("\n");

                                boolean foundDisplayNumber = false;
                                for (int i=0; i<vncsessionStderrLines.length; i++)
                                {
                                    String vncsessionStderrLine = vncsessionStderrLines[i];
                                    if (vncsessionStderrLine.contains("desktop is"))
                                    {
                                        String[] lineComponents = vncsessionStderrLine.split(":");
                                        // An extra parsing step is required for TigerVNC server output, compared with TurboVNC
                                        String[] displayComponents = lineComponents[1].split(" ");
                                        cvlVncDisplayNumber = displayComponents[0];
                                        foundDisplayNumber = true;
                                    }
                                }

                                if (!cvlVncDisplayNumberAutomatic)
                                    writeToLogWindow(launcherLogWindowTextArea, "CVL VNC Display Number is " + cvlVncDisplayNumber + "\n");
                                if (cvlVncDisplayNumberAutomatic)
                                {
                                    if (foundDisplayNumber)
                                    {
                                        writeToLogWindow(launcherLogWindowTextArea, "CVL VNC Display Number is " + cvlVncDisplayNumber + "\n");
                                        cvlVncDisplayNumberSpinnerField.setValue(Integer.valueOf(cvlVncDisplayNumber));
                                    }
                                    else
                                        writeToLogWindow(launcherLogWindowTextArea, "Failed to parse vncsession output for display number.\n");
                                }

                                writeToLogWindow(launcherLogWindowTextArea, "\n");
                            }

                            // SSH tunnel

                            // TurboVNC options:
                            turboVncConnection.opts.tunnel = true;
                            if (massiveTabSelected)
                                turboVncConnection.opts.cipher = massiveSshTunnelCipher;
                            if (cvlTabSelected)
                                turboVncConnection.opts.cipher = cvlSshTunnelCipher;

                            if (massiveTabSelected)
                            {
                                turboVncConnection.opts.serverName = massiveHost + ":1";
                                turboVncConnection.opts.remoteServerName = massiveVisNode;
                                turboVncConnection.opts.port = 5901;
                                turboVncConnection.opts.username = massiveUsername;
                                turboVncConnection.opts.password = massivePassword;
                            }

                            if (cvlTabSelected)
                            {
                                turboVncConnection.opts.serverName = cvlHost + ":" + cvlVncDisplayNumber;
                                turboVncConnection.opts.remoteServerName = "localhost";
                                turboVncConnection.opts.port = 5900 + Integer.valueOf(cvlVncDisplayNumber);
                                turboVncConnection.opts.username = cvlUsername;
                                turboVncConnection.opts.password = cvlPassword;
                            }

                            writeToLogWindow(launcherLogWindowTextArea, "\nAttempting to create tunnel using TurboVNC's tunnel class.");

                            Tunnel.createTunnel(turboVncConnection.opts);

                            for (int i=0; i<5; i++)
                            {
                                Thread.sleep(1000);
                                writeToLogWindow(launcherLogWindowTextArea, ".");
                            }

                            writeToLogWindow(launcherLogWindowTextArea, "\n");

                            writeToLogWindow(launcherLogWindowTextArea, "Created tunnel using TurboVNC's tunnel class.\n");

                            writeToLogWindow(launcherLogWindowTextArea, "Local port = " + turboVncConnection.opts.tunnelLocalPort + "\n");
                            writeToLogWindow(launcherLogWindowTextArea, "Remote port = " + turboVncConnection.opts.port + "\n");

                            writeToLogWindow(launcherLogWindowTextArea, "\nLaunching TurboVNC.\n");
                            for (int i=0; i<3; i++)
                            {
                                Thread.sleep(1000);
                                writeToLogWindow(launcherLogWindowTextArea, ".");
                            }

                            //String[] turboVncArguments = null;
                            //if (massiveTabSelected)
                                //turboVncArguments = new String[] {"-encoding","Tight","-user",massiveUsername,"-password",massivePassword,"localhost::" + turboVncConnection.opts.tunnelLocalPort};
                            //else
                                //turboVncArguments = new String[] {"-encoding","Tight","-user",cvlUsername,"-password",cvlPassword,"localhost::" + turboVncConnection.opts.tunnelLocalPort};

                            // Launch TurboVNC:
                            
                            //VncViewer.main(turboVncArguments);
                            // Instead of passing commmand-line arguments, what we really should do, is set the properties in the VncViewer object's opts object, and or CConn's opts object.

                            Thread turboVncThread = new Thread()
                            {
                                public void run()
                                {
                                    //turboVncConnection.opts.preferredEncoding = com.turbovnc.rfb.Encodings.encodingNum("Tight");
                                    turboVncConnection.opts.serverName = "localhost::" + turboVncConnection.opts.tunnelLocalPort;

                                    //turboVncConnection.opts.print();

                                    //LauncherMainFrame.turboVncViewer.opts = new Options(turboVncConnection.opts);
                                    try
                                    {
                                        turboVncConnection.initCConn(LauncherMainFrame.turboVncViewer, null);
                                        while (!turboVncConnection.shuttingDown())
                                            turboVncConnection.processMsg(false);
                                        System.exit(0);
                                    }
                                    catch(com.turbovnc.rdr.EndOfStream e)
                                    {
                                        System.exit(0);
                                    }
                                    turboVncConnection.reset();
                                    System.gc();
                                }
                            };
                            LauncherMainFrame.turboVncViewer.nViewers++;
                            turboVncThread.start();

                            // JW has hacked TurboVNC's
                            // com.turbovnc.vncviewer.Viewport class
                            // to record the Viewport instance in the
                            // LauncherMainFrame class's
                            // public static "turboVncViewport" object.
                            while (turboVncViewport==null)
                                Thread.sleep(1000);

                            // To toggle full-screen mode on Mac OS X :
                            //com.apple.eawt.Application.getApplication().requestToggleFullScreen(turboVncViewport);

                            if (cvlTabSelected && cvlVncDisplayNumberAutomatic)
                            {
                                WindowListener[] wl = (WindowListener[]) turboVncViewport.getListeners(WindowListener.class);  
                                for (int i = 0; i < wl.length; i++)
                                {
                                    //System.out.println("Removing window listener " + i);  
                                    turboVncViewport.removeWindowListener(wl[i]);  
                                }
                                turboVncViewport.addWindowListener(new WindowAdapter()
                                {
                                    public void windowClosing(WindowEvent e)
                                    {
                                        Icon massiveIcon = new ImageIcon(launcherMainFrameClass.getResource("MASSIVElogoTransparent64x64.png"));
                                        int result = JOptionPane.showOptionDialog(null,
                                            "Do you want to keep your VNC session (Display #" + 
                                                cvlVncDisplayNumber + ") running for future use?",
                                            "MASSIVE/CVL Launcher",
                                            JOptionPane.YES_NO_OPTION,
                                            JOptionPane.INFORMATION_MESSAGE,
                                            massiveIcon,
                                            new String[]{"Discard VNC Session", "Save VNC Session"},
                                            "default");

                                        if (result==JOptionPane.YES_OPTION)
                                        {
                                            writeToLogWindow(launcherLogWindowTextArea, "Discarding VNC session.\n");

                                            try
                                            {
                                                JSch jsch = new JSch();
                                                Session session = jsch.getSession(cvlUsername, cvlHost, 22);
                                                session.setPassword(cvlPassword);
                                                session.setConfig("StrictHostKeyChecking", "no");
                                                session.connect();
                                                session.setServerAliveInterval(1000);
                                                session.setServerAliveCountMax(30);

                                                RemoteCommand remoteCommand = new RemoteCommand("vncsession stop " + cvlVncDisplayNumber);
                                                sendCommand(session, remoteCommand, true, launcherLogWindowTextArea);

                                                if (session!=null)
                                                    session.disconnect();
                                            }
                                            catch (Exception ex)
                                            {
                                                writeToLogWindow(launcherLogWindowTextArea, ex.getMessage() + "\n");
                                            }
                                             
                                        }
                                        else
                                            writeToLogWindow(launcherLogWindowTextArea, "Saving VNC session for future use.\n");

                                        System.exit(0);
                                    }
                                });
                            }

                            /*
                            if (massiveTabSelected)
                            {
                                // Disconnect if not running persistent mode.
                                
                                if (!massivePersistentMode)
                                {
                                    writeToLogWindow(launcherLogWindowTextArea, "\n");
                                    RemoteCommand qdelCommand = new RemoteCommand("qdel " + massiveJobNumber);
                                    sendCommand(session, qdelCommand, true, launcherLogWindowTextArea);
                                }
                            }
                            */

                            /*
                            if (session!=null)
                                session.disconnect();
                                */

                        }
                        catch(Exception e)
                        {
                            if (e instanceof JSchException && e.toString().contains("Auth fail"))
                                writeToLogWindow(launcherLogWindowTextArea, "Authentication failed.");
                                //System.out.println("Authentication failed.");
                            else
                                writeToLogWindow(launcherLogWindowTextArea, e.toString());
                                //System.out.println(e);
                        }
                    }
                };
                loginThread.start();

            }
        });

        buttonsPanel.setLayout(new FlowLayout());
        buttonsPanel.add(optionsButton);
        buttonsPanel.add(cancelButton);
        buttonsPanel.add(loginButton);
        getContentPane().add(buttonsPanel,BorderLayout.SOUTH);

        // MASSIVE panel
        
        FormLayout massivePanelLayout = new FormLayout(
            "10dlu,pref,15dlu,pref:grow,25dlu", // columns
            "10dlu,pref,10dlu,pref,10dlu,pref,10dlu,pref,10dlu,pref,10dlu,pref,10dlu,pref,10dlu,pref"); //rows
        massivePanel.setLayout(massivePanelLayout);

        CellConstraints cc = new CellConstraints();

        massivePanel.add(new JLabel("Host"), cc.xy(2,2));
        String massiveHosts[] = {
            "m1-login1.massive.org.au",
            "m1-login2.massive.org.au",
            "m2-login1.massive.org.au",
            "m2-login2.massive.org.au"
        };
        massiveHostsComboBox.setModel(new DefaultComboBoxModel(massiveHosts));
        if (Arrays.asList(massiveHosts).contains(massiveHost))
            massiveHostsComboBox.setSelectedItem(massiveHost);
        massiveHostsComboBox.setEditable(true);
        massiveHostsComboBox.setSelectedIndex(3);
        massivePanel.add(massiveHostsComboBox, cc.xy(4,2));

        String massiveProjects[] = {
            defaultProjectPlaceholder,
            "ASync001","ASync002","ASync003","ASync004","ASync005","ASync006",
            "ASync007","ASync008","ASync009","ASync010","ASync011",

            "CSIRO001","CSIRO002","CSIRO003","CSIRO004","CSIRO005","CSIRO006",
            "CSIRO007",

            "Desc001","Desc002","Desc003","Desc004",

            "Monash001","Monash002","Monash003","Monash004",
            "Monash005","Monash006","Monash007","Monash008",
            "Monash009","Monash010","Monash011","Monash012","Monash013",
            "Monash014","Monash015","Monash016","Monash017","Monash018",
            "Monash019","Monash020","Monash021","Monash022","Monash023",
            "Monash024","Monash025","Monash026","Monash027","Monash028",
            "Monash029","Monash030","Monash031","Monash032","Monash033",
            "Monash034","Monash035","Monash036",

            "NCId75","NCIdb5","NCIdc0","NCIdd2","NCIg61","NCIg75",
            "NCIq97","NCIr14","NCIw25","NCIw27","NCIw67","NCIw81","NCIw91",
            "NCIy40","NCIy95","NCIy96",

            "pDeak0023","pDeak0024","pDeak0026",

            "pLaTr0011",

            "pMelb0095","pMelb0100","pMelb0103","pMelb0104",

            "pMOSP",

            "pRMIT0074","pRMIT0078","pRMIT0083",

            "pVPAC0005",

            "Training"
        };
        massivePanel.add(new JLabel("MASSIVE project"), cc.xy(2,4));
        massiveProjectsComboBox.setModel(new DefaultComboBoxModel(massiveProjects));
        if (Arrays.asList(massiveProjects).contains(massiveProject))
            massiveProjectsComboBox.setSelectedItem(massiveProject);
        massiveProjectsComboBox.setEditable(true);
        massivePanel.add(massiveProjectsComboBox, cc.xy(4,4));

        massivePanel.add(new JLabel("Hours requested"), cc.xy(2,6));
        massiveHoursRequestedSpinnerField.setValue(Integer.valueOf(massiveHoursRequested));
        massiveVisNodesRequestedSpinnerField.setValue(Integer.valueOf(massiveVisNodesRequested));

        JPanel massiveHoursAndVisNodesPanel = new JPanel();
        FormLayout massiveHoursAndVisNodesPanelLayout = new FormLayout(
            "35dlu,10dlu:grow,pref,5dlu,35dlu",
            "3dlu,pref,3dlu");
        massiveHoursAndVisNodesPanel.setLayout(massiveHoursAndVisNodesPanelLayout);
        massiveHoursAndVisNodesPanel.add(massiveHoursRequestedSpinnerField, cc.xy(1,2));
        massiveHoursAndVisNodesPanel.add(new JLabel("Vis nodes"), cc.xy(3,2));
        massiveHoursAndVisNodesPanel.add(massiveVisNodesRequestedSpinnerField, cc.xy(5,2));
        massivePanel.add(massiveHoursAndVisNodesPanel, cc.xy(4,6));

        massivePanel.add(new JLabel("Persistent mode"), cc.xy(2,8));
        if (massivePersistentMode)
            massivePersistentModeCheckBox.setSelected(true);
        massivePanel.add(massivePersistentModeCheckBox, cc.xy(4,8));

        massivePanel.add(new JLabel("Resolution"), cc.xy(2,10));

        String defaultResolution = screenSize.width + "x" + screenSize.height;
        String massiveVncDisplayResolutions[] = { 
            defaultResolution, 
            "1024x768", "1152x864", "1280x800", "1280x1024", "1360x768", "1366x768", "1440x900", "1600x900", "1680x1050", "1920x1080", "1920x1200", "7680x3200"
            };
        massiveVncDisplayResolutionComboBox.setModel(new DefaultComboBoxModel(massiveVncDisplayResolutions));
        if (Arrays.asList(massiveVncDisplayResolutions).contains(massiveVncDisplayResolution))
            massiveVncDisplayResolutionComboBox.setSelectedItem(massiveVncDisplayResolution);
        massiveVncDisplayResolutionComboBox.setEditable(true);
        massivePanel.add(massiveVncDisplayResolutionComboBox, cc.xy(4,10));

        massivePanel.add(new JLabel("SSH tunnel cipher"), cc.xy(2,12));
        String massiveDefaultCipher = "";
        String[] massiveSshTunnelCiphers = {};
        massiveDefaultCipher = "arcfour128";
        massiveSshTunnelCiphers = new String[] {"3des-cbc", "aes128-cbc", "blowfish-cbc", "arcfour128"};
        massiveSshTunnelCiphersComboBox.setModel(new DefaultComboBoxModel(massiveSshTunnelCiphers));
        massiveSshTunnelCiphersComboBox.setSelectedIndex(3); // Default to arcfour128
        if (Arrays.asList(massiveSshTunnelCiphers).contains(massiveSshTunnelCipher))
            massiveSshTunnelCiphersComboBox.setSelectedItem(massiveSshTunnelCipher);
        massiveSshTunnelCiphersComboBox.setEditable(true);
        massivePanel.add(massiveSshTunnelCiphersComboBox, cc.xy(4,12));

        massivePanel.add(new JLabel("Username"), cc.xy(2,14));
        massiveUsernameField.setText(massiveUsername);
        massivePanel.add(massiveUsernameField, cc.xy(4,14));

        massivePanel.add(new JLabel("Password"), cc.xy(2,16));
        massivePanel.add(massivePasswordField, cc.xy(4,16));

        // CVL panel
        
        FormLayout cvlPanelLayout = new FormLayout(
            "10dlu,pref,15dlu,pref:grow,25dlu", // columns
            "10dlu,pref,10dlu,pref,10dlu,pref,10dlu,pref,10dlu,pref,10dlu,pref,10dlu"); //rows
        cvlPanel.setLayout(cvlPanelLayout);

        cvlPanel.add(new JLabel("Host"), cc.xy(2,2));
        String cvlHosts[] = {
            "115.146.93.198",
            "115.146.94.0",
            "115.146.85.189"
        };
        cvlHostsComboBox.setModel(new DefaultComboBoxModel(cvlHosts));
        if (Arrays.asList(cvlHosts).contains(cvlHost))
            cvlHostsComboBox.setSelectedItem(cvlHost);
        cvlHostsComboBox.setEditable(true);
        cvlHostsComboBox.setSelectedItem(cvlHost);
        cvlPanel.add(cvlHostsComboBox, cc.xy(4,2));

        cvlPanel.add(new JLabel("Display number"), cc.xy(2,4));
        JPanel cvlVncDisplayNumberPanel = new JPanel();
        FormLayout cvlVncDisplayNumberPanelLayout = new FormLayout(
            "pref,25dlu:grow,35dlu",
            "3dlu,pref,3dlu");
        cvlVncDisplayNumberPanel.setLayout(cvlVncDisplayNumberPanelLayout);
        cvlVncDisplayNumberAutomaticCheckBox.setSelected(true);
        cvlVncDisplayNumberPanel.add(cvlVncDisplayNumberAutomaticCheckBox, cc.xy(1,2));
        cvlVncDisplayNumberSpinnerField.setValue(1);
        cvlVncDisplayNumberPanel.add(cvlVncDisplayNumberSpinnerField, cc.xy(3,2));

        cvlPanel.add(cvlVncDisplayNumberPanel, cc.xy(4,4));

        cvlPanel.add(new JLabel("Resolution"), cc.xy(2,6));
        defaultResolution = screenSize.width + "x" + screenSize.height;
        String cvlVncDisplayResolutions[] = { 
            defaultResolution, 
            "1024x768", "1152x864", "1280x800", "1280x1024", "1360x768", "1366x768", "1440x900", "1600x900", "1680x1050", "1920x1080", "1920x1200", "7680x3200"
            };
        cvlVncDisplayResolutionComboBox.setModel(new DefaultComboBoxModel(cvlVncDisplayResolutions));
        if (Arrays.asList(cvlVncDisplayResolutions).contains(cvlVncDisplayResolution))
            cvlVncDisplayResolutionComboBox.setSelectedItem(cvlVncDisplayResolution);
        cvlVncDisplayResolutionComboBox.setEditable(true);
        cvlPanel.add(cvlVncDisplayResolutionComboBox, cc.xy(4,6));

        cvlPanel.add(new JLabel("SSH tunnel cipher"), cc.xy(2,8));
        String cvlDefaultCipher = "";
        String[] cvlSshTunnelCiphers = {};
        cvlDefaultCipher = "arcfour128";
        cvlSshTunnelCiphers = new String[] {"3des-cbc", "aes128-cbc", "blowfish-cbc", "arcfour128"};
        cvlSshTunnelCiphersComboBox.setModel(new DefaultComboBoxModel(cvlSshTunnelCiphers));
        cvlSshTunnelCiphersComboBox.setSelectedIndex(3); // Default to arcfour128
        if (Arrays.asList(cvlSshTunnelCiphers).contains(cvlSshTunnelCipher))
            cvlSshTunnelCiphersComboBox.setSelectedItem(cvlSshTunnelCipher);
        cvlSshTunnelCiphersComboBox.setEditable(true);
        cvlPanel.add(cvlSshTunnelCiphersComboBox, cc.xy(4,8));

        cvlPanel.add(new JLabel("Username"), cc.xy(2,10));
        cvlUsernameField.setText(cvlUsername);
        cvlPanel.add(cvlUsernameField, cc.xy(4,10));

        cvlPanel.add(new JLabel("Password"), cc.xy(2,12));
        cvlPanel.add(cvlPasswordField, cc.xy(4,12));

        setVisible(true);
        massiveUsernameField.requestFocus();
    }

    private void writeToLogWindow(final JTextArea textArea, final String text)
    {
        logger.info(text);

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                textArea.append(text);
            }
        });
    }

    /**
     * This method sets up some buffers for reading STDOUT and STDERR
     *  from a remote command, and then waits for the command to
     * finish before reading the STDOUT and STDERR from the buffers.
     * This method is not be suitable for long-running commands where
     * the Launcher needs to parse the output before the command has
     * completed. The remoteCommand object passed into this method
     * is updated with the resulting STDOUT and STDERR strings.
     * The method returns the STDOUT string.
     */

    private String sendCommand(Session session, RemoteCommand remoteCommand, boolean showCommand, JTextArea launcherLogWindowTextArea)
    {
        StringBuilder commandStderrBuffer = new StringBuilder();
        StringBuilder commandStdoutBuffer = new StringBuilder();

        try
        {
            Channel channel = session.openChannel("exec");
            if (showCommand)
                writeToLogWindow(launcherLogWindowTextArea, remoteCommand.getCommand() + "\n");
            ((ChannelExec) channel).setCommand(remoteCommand.getCommand());
            channel.setInputStream(null);
            //((ChannelExec) channel).setErrStream(System.err);

            // Direct stderr output of command
            InputStream fromChannelStderrStream = ((ChannelExec)channel).getErrStream();
            InputStreamReader fromChannelStderrStreamReader = new InputStreamReader(fromChannelStderrStream, "UTF-8");
            BufferedReader fromChannelStderrBufferedReader = new BufferedReader(fromChannelStderrStreamReader);

            // Direct stdout output of command
            InputStream fromChannelStdoutStream = channel.getInputStream();
            InputStreamReader fromChannelStdoutStreamReader = new InputStreamReader(fromChannelStdoutStream, "UTF-8");
            BufferedReader fromChannelStdoutBufferedReader = new BufferedReader(fromChannelStdoutStreamReader);

            channel.connect();

            byte[] tmp = new byte[1024];
            int ch;
            while (true)
            {
                if (channel.isClosed())
                {
                    remoteCommand.setExitCode(channel.getExitStatus());
                    break;
                }
                try
                {
                    Thread.sleep(1000);
                }
                catch (Exception ee)
                {
                    throw new JSchException("Cannot execute remote command: " + remoteCommand.getCommand() + " : " + ee.getMessage());
                }
            }
            channel.disconnect();

            while ((ch = fromChannelStderrBufferedReader.read()) > -1)
                commandStderrBuffer.append((char)ch);

            while ((ch = fromChannelStdoutBufferedReader.read()) > -1)
                commandStdoutBuffer.append((char)ch);

        }
        catch (Exception e)
        {
            System.out.println(e);
        }

        remoteCommand.setStderr(commandStderrBuffer.toString());
        remoteCommand.setStdout(commandStdoutBuffer.toString());

        return remoteCommand.getStdout();
    }

    private class RemoteCommand
    {
        private String command = "";
        private String stdout = "";
        private String stderr = "";
        private int exitCode = 0;
        private boolean channelWasClosed = false;

        public RemoteCommand(String command)
        {
            this.command = command;
        }

        public String getCommand()
        {
            return command;
        }

        public void setCommand(String command)
        {
            this.command = command;
        }

        public String getStdout()
        {
            return stdout;
        }

        public void setStdout(String stdout)
        {
            this.stdout = stdout;
        }

        public String getStderr()
        {
            return stderr;
        }

        public void setStderr(String stderr)
        {
            this.stderr = stderr;
        }

        public int getExitCode()
        {
            return exitCode;
        }

        public void setExitCode(int exitCode)
        {
            this.exitCode = exitCode;
        }

        public boolean channelWasClosed()
        {
            return channelWasClosed;
        }

        public void setChannelWasClosed(boolean channelWasClosed)
        {
            this.channelWasClosed = channelWasClosed;
        }
    }

    private void submitLogfile()
    {
        try {
            LogManager.shutdown();

            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("https://cvl.massive.org.au/cgi-bin/log_drop.py");
            StringBody logfile = new StringBody(sAppender.log.toString());

            MultipartEntity reqEntity = new MultipartEntity();
            reqEntity.addPart("logfile", logfile);
            httppost.setEntity(reqEntity);

            HttpResponse response = httpclient.execute(httppost);
            InputStream result = response.getEntity().getContent();
        } catch (IOException e) {
            // FIXME What can we sensibly do here?
            // e.printStackTrace();
        }
    }
}

