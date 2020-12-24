package com.shahin.httpServer;

import com.shahin.httpServer.connection.Connection;
import com.shahin.httpServer.connection.SocketHandler;
import com.shahin.httpServer.connection.SocketProxy;
import com.shahin.httpServer.connection.ssl.SslSocketHandler;
import com.shahin.httpServer.router.Router;
import com.shahin.httpServer.utils.BufferCache;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class HttpServer {

    public static String SERVER_NAME = "MultiThread Http Server";
    private final SocketProxy proxy;
    private final ExecutorService executor;
    private final ConcurrentHashMap<SocketChannel, Connection> clients = new ConcurrentHashMap<>();
    private final ArrayList<SocketHandler> socketHandlers = new ArrayList<>();
    private final Router router = new Router();
    private final List<SocketProxy.Packet> taskList = new CopyOnWriteArrayList<>();

    public HttpServer(int threadCount){
        executor = Executors.newFixedThreadPool(threadCount, new ThreadFactory() {
            private int counter = 1;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("Http server, thread " + counter++);
                return t;
            }
        });

        proxy = new SocketProxy(this::submitJob);
    }

    public void listen(SocketAddress address){
        socketHandlers.add(new SocketHandler(address,proxy));
    }

    SslSocketHandler s;
    public void listenSSL(SocketAddress address){

        s = new SslSocketHandler(address,getTestContext(),proxy);
    }

    public void stopServer(){
        executor.shutdownNow();
        for(SocketHandler sh : socketHandlers){
            sh.stop();
        }
    }

    public Router getRouter() {
        return router;
    }

    private void submitJob(SocketProxy.Packet packet){
        if(packet.socket == null){
            return;
        }
        executor.submit(() -> {
            switch (packet.type){
                case NEW_ACCEPTED_SOCKET -> clients.put(packet.socket,new Connection(taskList,packet.socket,router));
                case SOCKET_TERMINATE -> clients.remove(packet.socket);
                case WRITE_DATA_DONE -> {
                    Connection conn = clients.get(packet.socket);
                    if(conn != null){
                        conn.writeDoneAck(taskList);
                    }
                }
                case NEW_INCOMING_DATA -> {
                    Connection conn = clients.get(packet.socket);
                    if(packet.data != null){
                        if(conn != null){
                            conn.newIncomingData(taskList,packet.data);
                        }else{
                            BufferCache.recycleBuffer(packet.data);
                        }
                    }
                }
            }

            for(SocketProxy.Packet p : taskList){
                proxy.poolSendPacket(p);
            }
            taskList.clear();
        });
    }

    public List<Connection> getClientList(){
        return new ArrayList<>(clients.values());
    }

    private SSLContext getTestContext(){
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream("./key.keystore"),"javaServerPass".toCharArray());

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks,"javaServerPass".toCharArray());

            SSLContext context = SSLContext.getInstance("TLSv1.2");
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);
            context.init(kmf.getKeyManagers(),tmf.getTrustManagers(),new SecureRandom());
            SSLSession session = context.createSSLEngine().getSession();
            int bufferSize1 = session.getPacketBufferSize();
            session.invalidate();

            return context;

        } catch (NoSuchAlgorithmException | IOException |
                KeyStoreException | KeyManagementException |
                UnrecoverableKeyException | CertificateException e) {
            e.printStackTrace();
            return null;
        }
    }

}
