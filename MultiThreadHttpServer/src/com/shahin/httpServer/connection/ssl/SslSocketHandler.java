package com.shahin.httpServer.connection.ssl;

import com.shahin.httpServer.connection.SocketHandler;
import com.shahin.httpServer.connection.SocketProxy;
import com.shahin.httpServer.logger.Log;
import com.shahin.httpServer.utils.BufferCache;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class SslSocketHandler implements Runnable {

    private final SocketAddress address;
    private final SocketProxy proxy;
    private ServerSocketChannel serverSocket;
    private final Thread thread;
    private Selector selector;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Map<SocketChannel, ByteBuffer> pendingBuffer = new HashMap<>();
    private final SSLContext sslContext;
    private final Map<SocketChannel,SslCore> sslMap = new HashMap<>();

    public SslSocketHandler(SocketAddress address,SSLContext sslContext,SocketProxy proxy) {
        this.address = address;
        this.sslContext = sslContext;
        this.proxy = proxy;
        proxy.socketRegister(address,()->selector.wakeup());
        thread = new Thread(this);
        thread.setName("SSLSocketHandler " + address.toString());
        thread.start();
    }

    public void stop(){
        running.set(false);
        selector.wakeup();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        initServerSocket();
        while(running.get()){
            serveLoop();
            handleRequest();
        }
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initServerSocket(){
        try{
            selector = Selector.open();
            serverSocket = ServerSocketChannel.open();
            serverSocket.bind(address);
            serverSocket.configureBlocking(false);
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);
        }catch(IOException e){
            e.printStackTrace();
            Log.log(Level.SEVERE, SocketHandler.class.getName(),
                    "Failed to init server Socket\nAddress is " + address.toString());
        }
    }

    private void serveLoop(){
        selectorSelect();
        Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
        while(keys.hasNext()){
            SelectionKey key = keys.next();
            keys.remove();
            if(!key.isValid()){
                continue;
            }
            if(key.isAcceptable()){
                SocketChannel socket = getSocketFromServerSocket();
                if(socket != null){
                    dispatchAcceptedSocket(socket);
                }
            }else if(key.isReadable()){
                SocketChannel socket = getSocket(key);
                if(socket != null && !readyToRead(socket)){
                    terminateConnection(socket,key);
                }
            }else if(key.isWritable()){
                SocketChannel socket = getSocket(key);
                if(socket != null && !readyToWrite(socket)){
                    terminateConnection(socket,key);
                }
            }
        }
    }

    private void handleRequest(){
        SocketProxy.Packet packet = proxy.socketGetPacket(address);
        while(packet != null){
            SocketChannel socket = packet.socket;
            if(socket == null || !socket.isOpen()){
                BufferCache.recycleBuffer(packet.data);
                packet = proxy.socketGetPacket(address);
                continue;
            }
            switch (packet.type) {
                case READ_REQUEST -> registerRead(socket);
                case WRITE_REQUEST -> writeData(socket, packet.data);
                case CLOSE_REQUEST -> terminateConnection(socket, null);
            }
            packet = proxy.socketGetPacket(address);
        }
    }

    private void selectorSelect(){
        try{
            selector.select();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private SocketChannel getSocket(SelectionKey key){
        try{
            SocketChannel socket = (SocketChannel) key.channel();
            socket.configureBlocking(false);
            return socket;
        }catch(IOException e){
            e.printStackTrace();
            return null;
        }
    }

    private SocketChannel getSocketFromServerSocket(){
        try{
            SocketChannel socket = serverSocket.accept();
            if(socket == null){
                return null;
            }
            socket.configureBlocking(false);
            SSLEngine sslEngine = sslContext.createSSLEngine();
            sslEngine.setUseClientMode(false);
            sslEngine.setNeedClientAuth(false);
            sslEngine.setWantClientAuth(false);
            sslMap.put(socket,new SslCore(sslEngine));
            return socket;
        }catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }

    private void terminateConnection(SocketChannel socket,SelectionKey key){
        sslMap.remove(socket);
        try{
            socket.close();
        }catch(IOException e){
            e.printStackTrace();
        }
        if(key != null){
            key.cancel();
        }
        BufferCache.recycleBuffer(pendingBuffer.remove(socket));
        proxy.socketSendPacket(new SocketProxy.Packet(socket, SocketProxy.Type.SOCKET_TERMINATE));
    }

    private void dispatchAcceptedSocket(SocketChannel socket) {
        try {
            sslMap.get(socket).accept();
        } catch (SSLException e) {
            e.printStackTrace();
            sslMap.remove(socket);
            return;
        }
        proxy.socketSendPacket(new SocketProxy.Packet(socket, SocketProxy.Type.NEW_ACCEPTED_SOCKET));
    }

    private boolean readyToRead(SocketChannel socket){
        ByteBuffer buffer = BufferCache.generateBuffer();
        SslCore ssl = sslMap.get(socket);
        try{
            int len = socket.read(buffer);
            if(len < 0){    // reach end of stream / connection close
                BufferCache.recycleBuffer(buffer);
                return false;
            }
            if(len > 0){
                buffer.flip();
                ssl.incomingData(buffer);
                /*SocketProxy.Packet packet = new SocketProxy.Packet(
                        socket,SocketProxy.Type.NEW_INCOMING_DATA);
                packet.data = buffer;
                proxy.socketSendPacket(packet);*/
                return true;
            }
        }catch (IOException e){
            BufferCache.recycleBuffer(buffer);
            return false;
        }
        BufferCache.recycleBuffer(buffer); // len = 0
        return true;
    }

    private boolean readyToWrite(SocketChannel socket){
        ByteBuffer buffer = pendingBuffer.get(socket);
        try{
            socket.write(buffer);
            if(buffer.remaining() == 0){
                BufferCache.recycleBuffer(buffer);
                pendingBuffer.remove(socket,buffer);
                SocketProxy.Packet packet = new SocketProxy.Packet
                        (socket,SocketProxy.Type.WRITE_DATA_DONE);
                proxy.socketSendPacket(packet);
                socket.register(selector,SelectionKey.OP_READ);
            }
            return true;
        }catch(IOException e){
            return false;
        }
    }

    private void registerRead(SocketChannel socket){
        try{
            int ops = SelectionKey.OP_READ;
            SelectionKey key = socket.keyFor(selector);
            if(key != null && key.isValid()){
                ops |= key.interestOps();
            }
            socket.register(selector,ops);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void writeData(SocketChannel socket,ByteBuffer buffer){
        if(buffer == null){
            return;
        }
        try{
            socket.write(buffer);
            if(buffer.remaining() == 0){
                BufferCache.recycleBuffer(buffer);
                proxy.socketSendPacket(new SocketProxy.Packet
                        (socket,SocketProxy.Type.WRITE_DATA_DONE));

            }else{
                pendingBuffer.put(socket, buffer);
                int ops = SelectionKey.OP_WRITE;
                SelectionKey key = socket.keyFor(selector);
                if(key!= null && key.isValid()){
                    ops |= key.interestOps();
                }
                socket.register(selector,ops);
            }
        }catch (IOException e){
            //e.printStackTrace();
            BufferCache.recycleBuffer(buffer);
        }
    }
}
