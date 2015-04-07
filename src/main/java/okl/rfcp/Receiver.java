package okl.rfcp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Receiver {

    public static int PORT = 4444;
    
    private ServerSocket serverSocket = null;
    private Socket recvSocket = null;

    public Receiver (int port) {
        try{
            serverSocket = new ServerSocket(port);
        } catch (IOException ioe){
            System.err.println("Could not listen on port: "+port);
            System.exit(-1);
        }
        
        try {
            recvSocket = serverSocket.accept();
        } catch (IOException e) {
            System.err.println("Accept failed: "+port);
            System.exit(-1);
        }

    }
    
    private void receive (File targetDir){
        BufferedOutputStream os = null;
        BufferedInputStream is = null;
        BufferedOutputStream bfos = null;
        
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsae){
            System.err.println("MD5 not available");
        }
        
        try {
            os = new BufferedOutputStream(recvSocket.getOutputStream()); 
            is = new BufferedInputStream(recvSocket.getInputStream());
            
            StringBuilder fileName = new StringBuilder();
            int c;
            while ((c = is.read()) != -1 && c != 0){
                fileName.append((char)c);
            }
            System.out.println("Receiving: "+fileName);
            File outFile = new File (targetDir, fileName.toString());
            
            bfos = new BufferedOutputStream(new FileOutputStream (outFile));
            System.out.println("Writing to: "+outFile.getAbsolutePath());
            byte[] buf = new byte[1024];
            int len = 0;
            while ((len = is.read(buf)) != -1) {
                if (digest != null)
                    digest.update(buf, 0, len);
                bfos.write(buf, 0, len);
            }
            bfos.flush();

        } catch (IOException ioe) {
            System.err.println("echo failed: " + ioe);
        } finally {
            try {
                if (bfos != null) bfos.close();
                if (os != null) os.close();
                if (is != null) is.close();
            } catch (IOException ioe1){
                //
            }
        }

        if (digest != null){
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String output = bigInt.toString(16);
            System.out.println("MD5: " + output);
        }
        
    }
    
    private void shutdown(){
        try {
            serverSocket.close();
        } catch (IOException ioe){
            System.err.println("Shutdown not successfull: "+ioe);
        }
    }
    
    public static void main (String[] args){
        
        int port = PORT;
        File targetDir = new File(".");
        
        int a = 0;
        while(a < args.length){
            String opt = args[a];
            if (opt.startsWith("-")){
                String parm = args[++a];
                if (opt.startsWith("-p")){ // Port
                    try { 
                        port = Integer.parseInt(parm);
                    } catch (NumberFormatException nfe){
                        System.err.println("Could not parse port "+parm);
                        port = PORT;
                    }
                } else if (opt.startsWith("-t")){ // Target Directory on receiver
                    targetDir = new File(parm);
                }
            } else {
                break;
            }
        }
        System.out.println("Starting on port "+port);
        
        
        
        while (true) {
            System.out.println("Listening");
            Receiver s = new Receiver(port);
            s.receive(targetDir);
            s.shutdown();
            System.out.println("Finished");
        }
        

    }
}
