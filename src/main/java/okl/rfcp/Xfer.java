package okl.rfcp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.TimeZone;
import java.util.logging.Level;

/* (c) Olaf Klöcker 2010 */

public class Xfer {
    
    private static final int PORT = 9337;
    private static final int BLOCKSIZE = 1024 * 16;
    private static final String VERSION = "xfer3.2";
    private static Level logLevel = Level.INFO;
    private static final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.S z");
    private static int blocksize = BLOCKSIZE;
    private static boolean overwrite = false;
    private static final String EXISTS_WONT_OVERWRITE = "existsWontOverwrite";
    private static final String EXISTS_WILL_OVERWRITE = "existsWillOverwrite";
    private static final String EXISTS_NOT = "existsNot";
    private static final String FORCE_OVERWRITE = "forceOverwrite";
    
    static class Receiver extends Thread {
        private ServerSocket serverSocket = null;
        private Socket recvSocket = null;
        private File targetDir = null;
        private int port;
        private boolean closed = false;
        private int blocksize = BLOCKSIZE;
        private boolean overwrite = false;
        
        public Receiver (int port, File targetDir) {
            
            this.port = port;
            this.targetDir = targetDir;

            if (targetDir.exists()) {
                if (! targetDir.isDirectory()){
                    log (Level.SEVERE, "Target directory " + targetDir.getAbsolutePath() + " is an existing file!");
                    System.exit(1);
                }
            }
            
            this.setName("Rcv");
            try{
                log(Level.FINE, "Registering server socket on port " + port);
                log(Level.FINE, "Writing data to " + targetDir.getAbsolutePath());
                serverSocket = new ServerSocket(port);
            } catch (IOException ioe){
                log(Level.SEVERE, "Could not listen on port: "+port, ioe);
                System.exit(-1);
            }
            
        }
        
        public void setBlocksize(int blocksize){
            this.blocksize = blocksize;
        }
        
        public void setOverwrite (boolean overwrite){
            this.overwrite = overwrite;
        }

        public void run() {
            receive ();
        }
        
        private void receive (){
            while (true) {
                log(Level.INFO, "=====================================");
                try {
                    if (!closed){
                        log(Level.FINE, "Listening");
                        recvSocket = serverSocket.accept();
                    }
                } catch (SocketException se){
                    log(Level.FINE, "SocketException because of shutdown");
                    return;
                } catch (IOException e) {
                    log(Level.SEVERE, "Accept failed: "+port+ "  ", e);
                    System.exit(-1);
                }
    
                BufferedOutputStream sendOs = null;
                BufferedInputStream sendIs = null;
                BufferedOutputStream bfos = null;
                long modDate = 0L;
                long fileSize = 0L;
                
                MessageDigest digest = null;
                try {
                    digest = MessageDigest.getInstance("MD5");
                } catch (NoSuchAlgorithmException nsae){
                    log(Level.WARNING, "MD5 not available", nsae);
                }
                StringBuilder md5src = new StringBuilder();
                boolean finished = false;
                
                try {
                    sendOs = new BufferedOutputStream(recvSocket.getOutputStream()); 
                    sendIs = new BufferedInputStream(recvSocket.getInputStream());
                    
                    // send my version
                    sendOs.write(VERSION.getBytes());
                    sendOs.write(0);
                    sendOs.flush();

                    StringBuilder senderForcesOverwrite = new StringBuilder();
                    int c;
                    while ((c = sendIs.read()) != -1 && c != 0){
                        senderForcesOverwrite.append((char)c);
                    }
                    if (FORCE_OVERWRITE.equals(senderForcesOverwrite.toString())){
                        log(Level.INFO, "Sender forces overwrite: "+senderForcesOverwrite);
                        overwrite = true;
                    }

                    StringBuilder fileName = new StringBuilder();
                    while ((c = sendIs.read()) != -1 && c != 0){
                        fileName.append((char)c);
                    }
                    log(Level.INFO, "Receiving: "+fileName);
                    File outFile = new File (targetDir, fileName.toString());
                    
                    // receiving file modification date
                    StringBuilder modDateStr = new StringBuilder();
                    while ((c = sendIs.read()) != -1 && c != 0){
                        modDateStr.append((char)c);
                    }
                    try {
                        modDate = Long.parseLong(modDateStr.toString());
                    } catch (NumberFormatException nfe){}
                    
                    // receiving file size
                    StringBuilder sizeStr = new StringBuilder();
                    while ((c = sendIs.read()) != -1 && c != 0){
                        sizeStr.append((char)c);
                    }
                    try {
                        fileSize = Long.parseLong(sizeStr.toString());
                    } catch (NumberFormatException nfe){}
    
                    
                    if (outFile.exists()) {
                        if (outFile.isDirectory()){
                            log (Level.SEVERE, "Output file " + outFile.getAbsolutePath() + " is an existing directory");
                            continue;
                        }
                        if (overwrite) {
                            log(Level.WARNING, "Output file " + outFile.getAbsolutePath() + " exists already, will be overwritten");
                            sendOs.write(EXISTS_WILL_OVERWRITE.getBytes());
                            sendOs.write(0);
                            sendOs.flush();
                        } else {
                            log(Level.WARNING, "Output file " + outFile.getAbsolutePath() + " exists already, will NOT be overwritten");
                            sendOs.write(EXISTS_WONT_OVERWRITE.getBytes());
                            sendOs.write(0);
                            sendOs.flush();
                            continue;
                        }
                    } else {
                        sendOs.write(EXISTS_NOT.getBytes());
                        sendOs.write(0);
                        sendOs.flush();
                        log(Level.FINE, "Creating dirs for " + outFile.getAbsoluteFile().getParent());
                        outFile.getAbsoluteFile().getParentFile().mkdirs();
                    }
                    bfos = new BufferedOutputStream(new FileOutputStream (outFile));
                    log(Level.FINE, "Writing to: "+outFile.getAbsolutePath());
                    byte[] buf = new byte[blocksize];
                    int len = 0;
                    long totalRead = 0;
                    byte[] secondPart = null;
                    long transferStartTime = System.currentTimeMillis();
                    while ((len = sendIs.read(buf)) != -1) {
                        totalRead += len;
                        if (totalRead > fileSize){
                            secondPart = new byte[(int)(totalRead-fileSize)];
                            // System.arraycopy compatible with java1.5, Arrays.copyOfRange not
                            System.arraycopy(buf, len-(int)(totalRead-fileSize), secondPart, 0, secondPart.length);
//                            secondPart = Arrays.copyOfRange(buf, len-(int)(totalRead-fileSize), len);
                            finished = secondPart[secondPart.length-1] == 0;
//                            byte[] firstPart = Arrays.copyOfRange(buf,0,len-(int)(totalRead-fileSize));
                            byte[] firstPart = new byte[len-(int)(totalRead-fileSize)];
                            System.arraycopy(buf, 0, firstPart, 0, firstPart.length);
                            if (digest != null)
                                digest.update(firstPart, 0, firstPart.length);
                            bfos.write(firstPart, 0, firstPart.length);
                            break;
                        }
                        if (digest != null)
                            digest.update(buf, 0, len);
                        bfos.write(buf, 0, len);
                    }
                    bfos.flush();

                    long transferEndTime = System.currentTimeMillis();
                    double transferTime = transferEndTime - transferStartTime;
                    String bytesPerSec = null;
                    double xferRate = (totalRead / (transferTime / 1000)) / 1024.0;
                    if (xferRate < 10000) {
                        bytesPerSec = String.format("%4.2f KB/s", xferRate );
                    } else {
                        bytesPerSec = String.format("%4.2f MB/s", (xferRate / 1024) );
                    }
                    log(Level.INFO, "Received " + totalRead + " bytes in " + transferTime + 
                            "ms = " + bytesPerSec);

                    outFile.setLastModified(modDate);
                    
                    if (totalRead == 0){
                        log (Level.WARNING, "Nothing transferred.");
                        continue;
                    }
                    
                    // receiving md5 hash
                    //   first add already read bytes
                    if (secondPart != null) {
                        for (byte b : secondPart) {
                            if (b != 0) {
                                md5src.append((char) b);
                            }
                        }
                    }
                    log(Level.FINEST, "preliminary md5: " + md5src);
                    //   then read the rest of the file
                    while (!finished && ((c = sendIs.read()) != -1) && (c != 0)){
                        md5src.append((char)c);
                    }
                    log(Level.FINEST, "final md5: " + md5src);
                    
                    sendOs.write(md5src.toString().getBytes());
                    sendOs.write(0);
                    sendOs.flush();
        
                } catch (IOException ioe) {
                    log(Level.SEVERE, "echo failed: ",  ioe);
                } finally {
                    try {
                        if (bfos != null) bfos.close();
                        if (sendOs != null) sendOs.close();
                        if (sendIs != null) sendIs.close();
                        recvSocket.close();
                    } catch (IOException ioe1){
                        //
                    }
                }
        
                if (digest != null){
                    byte[] md5sum = digest.digest();
                    BigInteger bigInt = new BigInteger(1, md5sum);
                    String output = bigInt.toString(16);
                    log(Level.FINE, "MD5: " + output);
                    if (!output.equals(md5src.toString())){
                        log(Level.WARNING, "MD5 hashes don't agree: src="+md5src.toString());
                    } else {
                        log(Level.FINE, "MD5 hashes agree.");
                    }
                }
            }   
        }
        
        private void shutdown(){
            try {
                closed = true;
                log(Level.FINE, "Closing server socket");
                serverSocket.close();
            } catch (IOException ioe){
                log(Level.SEVERE, "Shutdown not successful: ", ioe);
            }
        }
    }
    
    static class Sender {
        private Socket sendSocket = null;
        private int blocksize = BLOCKSIZE;

        private void send(String host, int port, File sendFile) {

            log(Level.INFO, "=====================================");
            log(Level.INFO, "Sending "+sendFile);
            long modDate = sendFile.lastModified();
            try {
                log(Level.FINE, "Connect to " + host + ":" + port);
                sendSocket = new Socket(host, port);
            } catch (IOException sockEx){
                log(Level.SEVERE, "Could not open port: " + host + ":" + port, sockEx);
                return;
            }
            
            BufferedInputStream bfis = null;
            try {
                bfis = new BufferedInputStream(
                        new FileInputStream(sendFile));
            } catch (FileNotFoundException fnfe) {
                log(Level.SEVERE, "File not found: "+sendFile.getAbsolutePath(), fnfe);
                return;
            }
            
            MessageDigest digest = null;
            try {
                digest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException nsae){
                log(Level.WARNING, "MD5 not available");
            }
            
            long sendFileSize = sendFile.length();
    
            BufferedOutputStream rcvos = null;
            BufferedInputStream rcvis = null;
            
            byte[] buf = new byte[blocksize];
            try {
                rcvos = new BufferedOutputStream(sendSocket.getOutputStream());
                rcvis = new BufferedInputStream(sendSocket.getInputStream());
                
                int c;
                StringBuilder rcvVer = new StringBuilder();
                while ((c = rcvis.read()) != -1 && c != 0){
                    rcvVer.append((char)c);
                }
                if (! rcvVer.toString().equals (VERSION)){
                    log(Level.SEVERE, "Receiver's version wrong: " + rcvVer + " vs my " + VERSION);
                    System.exit(1);
                } else {
                    log(Level.FINE, "Receiver sends version " + rcvVer);
                }

                if (overwrite)
                    rcvos.write(FORCE_OVERWRITE.getBytes());
                else
                    rcvos.write("x".getBytes());
                rcvos.write(0);
                rcvos.flush();

                // send file name to the other side
                rcvos.write(sendFile.getPath().getBytes());
                rcvos.write(0);
                rcvos.flush();

                int len = 0;
                // send file modification date to the other side
                rcvos.write(Long.toString(modDate).getBytes());
                rcvos.write(0);
                // send file size to the other side
                log(Level.INFO, "Sending " + sendFileSize + " bytes");
                rcvos.write(Long.toString(sendFileSize).getBytes());
                rcvos.write(0);
                rcvos.flush();

                StringBuilder existsOnOtherSide = new StringBuilder();
                while ((c = rcvis.read()) != -1 && c != 0){
                    existsOnOtherSide.append((char)c);
                }
                log(Level.FINEST, "Exists on other side: " + existsOnOtherSide);
                if (existsOnOtherSide.toString().equals(EXISTS_WONT_OVERWRITE)){
                    log(Level.WARNING, "File exists on other side, not sending.");
                    return;
                }

                while ((len = bfis.read(buf)) != -1) {
                    if (digest != null)
                        digest.update(buf, 0, len);
                    rcvos.write(buf, 0, len);
                }
                rcvos.flush();

                // send file md5 hash to the other side
                String md5 = "";
                if (digest != null){
                    byte[] md5sum = digest.digest();
                    BigInteger bigInt = new BigInteger(1, md5sum);
                    md5 = bigInt.toString(16);
                    log(Level.FINE, "MD5: " + md5);
                }
                rcvos.write(md5.getBytes());
                rcvos.write(0);
                rcvos.flush();
                
                // expecting receiver to bounce md5 sum
                StringBuilder rcvmd5 = new StringBuilder();
                while ((c = rcvis.read()) != -1 && c != 0){
                    rcvmd5.append((char)c);
                }
                if (! rcvmd5.toString().equals (md5)){
                    log(Level.SEVERE, "Receiver advises wrong md5 sum: " + rcvmd5 + " vs my " + md5);
                } else {
                    log(Level.FINE, "Receiver advises correct md5 sum");
                }
    
            } catch (IOException ioe){
                log(Level.SEVERE, "", ioe);
            } finally {
                try { 
                    if (bfis != null) bfis.close();
                    if (rcvos != null) rcvos.close();
                    sendSocket.close();
                } catch (IOException ioe1){
                    //
                }
            }
    
        }
        
        public void setBlocksize(int blocksize){
            this.blocksize = blocksize;
        }

    }

    static String dolog (Level level, String s){
        StringBuilder output = new StringBuilder()
        .append("[")
        .append(level).append('|')
        .append(Thread.currentThread().getName()).append('|')
        .append(dateFormat.format(System.currentTimeMillis()))
        .append("]: ")
        .append(s).append(' ')
        .append(System.getProperty("line.separator"));
        return output.toString();
    }
    
    static String dolog (Level level, String s, Exception e){
        return dolog(level, s) + " " + e.toString();
    }
    
    static void log (Level level, String s){
        if (level.intValue() >= logLevel.intValue()){
            if (level.intValue() >= Level.WARNING.intValue()){
                System.err.print(dolog(level, s));
            } else {
                System.out.print(dolog(level, s));
            }
        }
    }
 
    static void log (Level level, String s, Exception e){
        if (level.intValue() >= logLevel.intValue()){
            System.err.println(dolog(level, s, e));
            if (logLevel.intValue() <= Level.FINE.intValue()){
                for (StackTraceElement ste : e.getStackTrace()){
                    System.err.print(String.format("         %s : %n", ste.toString(), ste.getLineNumber()));
                }
            }
        }
    }

    private static void usage() {
        log (Level.SEVERE, "Usage: [[-t <output dir>] || ");
        log (Level.SEVERE, "  [-h <target host>]]");
        log (Level.SEVERE, "  [-p <port>]");
        log (Level.SEVERE, "  [-B <blocksize in bytes>]");
        log (Level.SEVERE, "  [-o <1|o>] (1 for overwrite, 0 for don't overwrite)"); 
        log (Level.SEVERE, "  [-l SEVERE|WARNING|INFO|FINE|FINER|FINEST]");
        log (Level.SEVERE, "  <files|dir>");
        log (Level.SEVERE, "If <files|dir> is a directory, it will be copied recursively.");
        log (Level.SEVERE, "Semantics of dir like in rsync (adir vs adir/).");
    }

    public static void main (String[] args){
        
        log(Level.SEVERE, "Sending: java -cp xfer.jar rfcp.Xfer [-p port] [-h host] [-l FINE (logging)] <file(s)>");
        log(Level.SEVERE, "Receiving: java -cp xfer.jar rfcp.Xfer [-p port] [-l FINE (logging)] [-t targetDir]");
        
        int port = PORT;
        File targetDir = new File(".");
        String host = "";
        List<File> sendFiles = new ArrayList<File>();
        
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        int a = 0;
        while(a < args.length){
            String opt = args[a];
            if (opt.startsWith("-")){
                a++;
                if (a >= args.length){
                    usage();
                    continue;
                }
                String parm = args[a];
                if (opt.startsWith("-p")){ // Port
                    try { 
                        port = Integer.parseInt(parm);
                        a++;
                    } catch (NumberFormatException nfe){
                        log(Level.SEVERE, "Could not parse port "+parm);
                        port = PORT;
                    }
                } else if (opt.startsWith("-t")){ // Target Directory on receiver
                    targetDir = new File(parm);
                    a++;
                } else if (opt.startsWith("-h")){ // Receiver Host
                        host = parm;
                        a++;
                } else if (opt.startsWith("-o")){ // overwrite
                    overwrite = "1".equals(parm);
                    a++;
                } else if (opt.startsWith("-l")){ // log level
                    if (parm.startsWith("FINEST")){
                        logLevel = Level.FINEST;
                    } else if (parm.startsWith("FINER")){
                        logLevel = Level.FINER;
                    } else if (parm.startsWith("FINE")){
                        logLevel = Level.FINE;
                    } else if (parm.startsWith("INFO")){
                        logLevel = Level.INFO;
                    } else if (parm.startsWith("WARNING")){
                        logLevel = Level.WARNING;
                    } else if (parm.startsWith("SEVERE")){
                        logLevel = Level.SEVERE;
                    }
                    log (Level.FINE, "Setting log level to " + logLevel.getName());
                    a++;
                } else if (opt.startsWith("-B")){ // block size
                    try { 
                        blocksize = Integer.parseInt(parm);
                        a++;
                    } catch (NumberFormatException nfe){
                        log(Level.SEVERE, "Could not parse block size "+parm);
                        blocksize = BLOCKSIZE;
                    }
                } else if (opt.startsWith("-")){
                    log (Level.ALL, "Unknown option: " + opt);
                    usage();
                    System.exit(1);
                }
            } else { // files to send
                sendFiles.add(new File(args[a++]));
            }
        }
        
        if (sendFiles.size() == 0){
            log(Level.FINE, "Starting in receiving mode.");
            final Receiver rcvThread = new Receiver(port, targetDir);
            rcvThread.setBlocksize(blocksize);
            rcvThread.setOverwrite(overwrite);
            rcvThread.start();

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    rcvThread.shutdown();
                }
             });

        } else {
            log(Level.FINE, "Starting in sending mode.");
        
            ListIterator<File> it = sendFiles.listIterator();
            while (it.hasNext()) {
                File sendFile = it.next();
                List<File> dirTree = new ArrayList<File>();
                if (sendFile.isDirectory()){
                    // copy whole recursive tree
                    dirTree = DirTree.getFileListing(sendFile);
                } else {
                    // only copy the one file
                    dirTree.add(sendFile);
                }
                
                for (File oneFile : dirTree){
                    Sender sender = new Sender();
                    sender.setBlocksize(blocksize);
                    sender.send(host, port, oneFile);
                }
            }
        }
    }
}
