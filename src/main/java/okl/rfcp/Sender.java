package okl.rfcp;

import static okl.rfcp.Receiver.PORT;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Sender {

    private Socket sendSocket = null;
    
    private void send(String host, int port, File sendFile) {
        try {
            sendSocket = new Socket(host, port);
        } catch (IOException sockEx){
            System.err.println("Could not open port: " + host + ":" + port);
            return;
        }
        
        BufferedInputStream bfis = null;
        try {
            bfis = new BufferedInputStream(
                    new FileInputStream(sendFile));
        } catch (FileNotFoundException fnfe) {
            System.err.println("File not found: "+sendFile.getAbsolutePath());
        }
        
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsae){
            System.err.println("MD5 not available");
        }

        
        BufferedOutputStream os = null;
        
        byte[] buf = new byte[1024];
        try {
            os = new BufferedOutputStream(sendSocket.getOutputStream());
            
            os.write(sendFile.getName().getBytes());
            os.write(0);
            os.flush();

            int len = 0;
            while ((len = bfis.read(buf)) != -1) {
                if (digest != null)
                    digest.update(buf, 0, len);
                os.write(buf, 0, len);
            }
            os.flush();

        } catch (IOException ioe){
            System.err.println(ioe);
        } finally {
            try { 
                if (bfis != null) bfis.close();
                if (os != null) os.close();
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

    public static void main (String[] args){
       
        String fileName = null;
        String host = null;
        File sendFile = null;
        int port = PORT;
        
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
                } else if (opt.startsWith("-h")){ // Receiver Host
                    int i = parm.indexOf(':');
                    if (i >= 0) {
                        host = parm.substring(0, i);
                        try { 
                            port = Integer.parseInt(parm.substring(i+1));
                        } catch (NumberFormatException nfe){
                            System.err.println("Could not parse port "+parm);
                            port = PORT;
                        }
                    } else {
                        host = parm;
                    }
                    a++;
                    break;

                }
            } else {
                break;
            }
        }
        
        System.out.println("Sending to: "+host+":"+port);

        for (int n = a; n < args.length; n++){
            fileName = args[n];
            sendFile = new File(fileName);

            Sender s = null;
            s = new Sender();
            System.out.println("Sending "+sendFile.getAbsolutePath());
            s.send(host, port, sendFile);
        }

        System.out.println("Finished");

    }
}
