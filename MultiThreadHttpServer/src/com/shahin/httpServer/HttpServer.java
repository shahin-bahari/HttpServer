package com.shahin.httpServer;

import com.shahin.httpServer.connection.Connection;
import com.shahin.httpServer.connection.SocketHandler;
import com.shahin.httpServer.connection.SocketProxy;
import com.shahin.httpServer.router.Router;
import com.shahin.httpServer.utils.BufferCache;

import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.concurrent.*;

public class HttpServer {

    public static String SERVER_NAME = "MultiThread Http Server";
    private final SocketProxy proxy;
    private final ExecutorService executor;
    private final ConcurrentHashMap<SocketChannel, Connection> clients = new ConcurrentHashMap<>();
    private final ArrayList<SocketHandler> socketHandlers = new ArrayList<>();
    private final Router router = new Router();

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
        //executor = Executors.newFixedThreadPool(threadCount);

        proxy = new SocketProxy(this::submitJob);
    }

    public void listen(SocketAddress address){
        socketHandlers.add(new SocketHandler(address,proxy));
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
            ArrayList<SocketProxy.Packet> taskList = new ArrayList<>(1);
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
        });
    }

}
