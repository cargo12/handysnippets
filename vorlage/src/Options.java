package tc;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.logging.Level;

// if options logging is to be shown, "-l FINE" must be the FIRST option!
public class Options {
    public Integer optPort = 64321;
    public String optHost;
    public Integer optLocalPort = 0;
    public Boolean optUseEncryption = true;
    public File optLogfile;
    public Boolean optPopups = true;
    public Boolean optGui = false;
    public boolean unknownOptionFound = false;

    public Options (String[] args) {
        Deque<String> deq = new ArrayDeque<String>(Arrays.asList(args));
        
        while(!deq.isEmpty()){
            String opt = deq.pop();
            if (opt.startsWith("-")){
                
                // First the options without argument
                if (opt.startsWith("-c")) {
                    optUseEncryption = true;
                    Logger.log (Level.CONFIG, "Enabling encryption.");
                }  else if (opt.startsWith("-nop")) {
                    optPopups = false;
                    Logger.log (Level.CONFIG, "Disabling popups");
                }  else if (opt.startsWith("-gui")) {
                    optGui = true;
                    Logger.log (Level.CONFIG, "GUI version");
                } else {
                    
                    // Now options with argument
                    if (deq.isEmpty()) {
                        // second part of -x <option> missing
                        Logger.log (Level.WARNING, "Expected 1 argument for : " + opt);
                        unknownOptionFound = true;
                        break;
                    }
                
                    String parm = deq.poll();
                    // doesn't work with -k: argument list may start with negative number
                    if (opt.startsWith("-k") && parm.startsWith("-")) {
                        Logger.log(Level.SEVERE, "Argument for "+opt+" must not start with '-': " + parm);
                        unknownOptionFound = true;
                        break;
                    }
                    if (opt.startsWith("-p")){ // Port
                        try {
                            optPort = Integer.parseInt(parm);
                        } catch (NumberFormatException nfe){
                            Logger.log(Level.SEVERE, "Could not parse port "+parm);
                            throw new RuntimeException("Could not parse port "+parm);
                        }
                    } else if (opt.startsWith("-h")){ // Receiver Host
                        optHost = parm;
                    } else if (opt.startsWith("-l")){ // log level
                        if (parm.startsWith("FINEST")){
                            Logger.setLogLevel(Level.FINEST);
                        } else if (parm.startsWith("FINER")){
                            Logger.setLogLevel(Level.FINER);
                        } else if (parm.startsWith("FINE")){
                            Logger.setLogLevel(Level.FINE);
                        } else if (parm.startsWith("CONFIG")){
                            Logger.setLogLevel(Level.CONFIG);
                        } else if (parm.startsWith("INFO")){
                            Logger.setLogLevel(Level.INFO);
                        } else if (parm.startsWith("WARNING")){
                            Logger.setLogLevel(Level.WARNING);
                        } else if (parm.startsWith("SEVERE")){
                            Logger.setLogLevel(Level.SEVERE);
                        } else {
                            Logger.log (Level.WARNING, "Unknown log level: " + parm);
                        }
                        Logger.log (Level.CONFIG, "Setting log level to " + Logger.getLevel());
                    } else if (opt.startsWith("-r")) {
                        try {
                            optLocalPort = Integer.parseInt(parm);
                            Logger.log (Level.CONFIG, "Setting local port to " + parm);
                        } catch (NumberFormatException nfe){
                            Logger.log(Level.SEVERE, "Could not parse local port "+parm);
                            throw new RuntimeException("Could not parse local port "+parm);
                        }
                    } else if (opt.startsWith("-g")) {
                        optLogfile = new File(parm);
                        Logger.log (Level.CONFIG, "Setting log file to " + parm);
                        try {
                            Logger.setLogFile(optLogfile);
                        } catch (IOException e) {
                            System.err.println("Could not open log file for writing: " + parm);
                            optLogfile = null;
                        }
                    } else {
                        Logger.log (Level.WARNING, "Unknown option: " + opt);
                        unknownOptionFound = true;
                    }
                }
            } else { 
                Logger.log (Level.WARNING, "Unknown option: " + opt);
                unknownOptionFound = true;
                break;
                    
            }
        }

    }

    public void printUsage(){
        System.out.println("Available options:");
        System.out.println("  -g <log file>   (should be first command line option; '-' for stdout (default))");
        System.out.println("Call as: java -cp tc.jar tc.Hub     -> Server mode");
        System.out.println("         java -cp tc.jar tc.TextUI  -> text interface");
        
    }

}
