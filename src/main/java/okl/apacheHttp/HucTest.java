package okl.apacheHttp;

import java.net.*;
import java.io.*;

public class HucTest {

    public static void main(String[] args) throws Exception {
        URL connectURL = new URL("http://localhost:8801");
        HttpURLConnection conn = (HttpURLConnection) connectURL.openConnection();
        String boundary = "BOUNDARY";
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setChunkedStreamingMode(2);
        conn.setRequestProperty("Transfer-Encoding","chunked");
        conn.setRequestMethod("GET");
        //conn.setRequestProperty("Connection", "Keep-Alive");


        // Read response
        // this works for small files when not in streaming mode
        // for large files, connection is reset by server (time outs)
        Reader reader = new InputStreamReader( conn.getInputStream() );
        int x;
        while ( (x=reader.read()) != -1 ){
            System.out.printf("%c", x);
        }
    }
}
