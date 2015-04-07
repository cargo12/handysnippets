package okl.apacheHttp;

import java.net.*;
import java.io.*;
import java.util.Locale;

import org.apache.http.entity.*;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpVersion;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.RequestLine;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.util.EntityUtils;

import org.apache.http.impl.DefaultHttpServerConnection;

public class HCoreTest {
    static DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
    static volatile boolean running = true;

    public static void main(String[] args) throws Exception {

        // HTTP parameters for the server
        final HttpParams mHttpParams = new BasicHttpParams();
        mHttpParams.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 30000)
                   .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 1024*8)
                   .setParameter(CoreProtocolPNames.ORIGIN_SERVER, "PortServer/1.1");

        // Create HTTP protocol processing chain
        BasicHttpProcessor httpProc = new BasicHttpProcessor();
        // Use standard server-side protocol interceptors
        httpProc.addInterceptor(new ResponseDate());
        httpProc.addInterceptor(new ResponseServer());
        httpProc.addInterceptor(new ResponseContent());

        HttpService httpService = new HttpService(httpProc,
                                       new NoConnectionReuseStrategy(),
                                       new DefaultHttpResponseFactory());

        httpService.setParams(mHttpParams);


        HttpRequestHandler requestHandler = new HttpRequestHandler() {
            @Override
                public void handle(HttpRequest request, HttpResponse response, HttpContext context)  throws HttpException, IOException {
                    //PcmiServer.davSendFileFromAssets(url, request, response, mAssetManager);
                    System.out.println("handle");
                    final RequestLine line = request.getRequestLine();
                    System.out.printf("%s %s :: %s%n", line.getMethod().toUpperCase(Locale.ENGLISH), line.getUri(), 
                        response.getStatusLine().getStatusCode());
                    if (request instanceof HttpEntityEnclosingRequest) {
                        conn.receiveRequestEntity((HttpEntityEnclosingRequest) request);
                        HttpEntity entity = ((HttpEntityEnclosingRequest) request)
                            .getEntity();
                        if (entity != null) {
                            // Do something useful with the entity and, when done, ensure all
                            // content has been consumed, so that the underlying connection
                            // could be re-used
                            EntityUtils.consume(entity);
                        }
                    }
                    response.setEntity(new StringEntity("Got it"));
                    response.setStatusCode(HttpStatus.SC_OK);

                    response.setHeader("Content-Type", "multipart/x-mixed-replace;boundary=BOUNDARY");
                    response.setHeader("Transfer-Type", "Chunked");
                    response.setHeader("Connection", "Close");

                }
        };

        HttpRequestHandlerRegistry handlerResolver = new HttpRequestHandlerRegistry();
        handlerResolver.register("/*", requestHandler);
        handlerResolver.register("/", requestHandler);

        httpService.setHandlerResolver(handlerResolver);

        ServerSocket serverSocket = new ServerSocket(8801);

        while (running){
            Socket socket = serverSocket.accept();

            HttpParams params = new BasicHttpParams();
            conn.bind(socket, params);
            HttpContext context = new BasicHttpContext();;

            boolean active = true;
            try {
                while (active && conn.isOpen()) {
                    httpService.handleRequest(conn, context);
                }
            } finally {
                conn.shutdown();
            }
        }

    }

}
