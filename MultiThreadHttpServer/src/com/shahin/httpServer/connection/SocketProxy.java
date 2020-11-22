package com.shahin.httpServer.connection;

import com.shahin.httpServer.utils.BufferCache;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SocketProxy {

    public interface ProxyToPool{
        void newPacket(Packet packet);
    }

    public enum Type{
        NEW_ACCEPTED_SOCKET,
        NEW_INCOMING_DATA,
        WRITE_DATA_DONE,
        SOCKET_TERMINATE,
        READ_REQUEST,
        WRITE_REQUEST,
        CLOSE_REQUEST
    }

    public static class Packet{
        public final Type type;
        public final SocketChannel socket;
        public ByteBuffer data;

        public Packet(SocketChannel socket,Type type){
            this.type = type;
            this.socket = socket;
        }
    }

    private final ConcurrentHashMap<SocketAddress,Runnable>
            socketsWakeUp = new ConcurrentHashMap<>();
    private final ProxyToPool proxyToPool;
    private final ConcurrentHashMap<SocketAddress, ConcurrentLinkedQueue<Packet>>
            pipes = new ConcurrentHashMap<>();

    public SocketProxy(ProxyToPool proxyToPool) {
        this.proxyToPool = proxyToPool;
    }

    public void socketRegister(SocketAddress address, Runnable wakeUpFunc){
        socketsWakeUp.put(address, wakeUpFunc);
        pipes.put(address,new ConcurrentLinkedQueue<>());
    }

    public void socketSendPacket(Packet packet){
        proxyToPool.newPacket(packet);
    }

    public Packet socketGetPacket(SocketAddress socketAddress){
        return pipes.get(socketAddress).poll();
    }

    public void poolSendPacket(Packet packet){
        SocketAddress address;
        try {
            SocketChannel socket = packet.socket;
            if(!socket.isOpen()){
                BufferCache.recycleBuffer(packet.data);
                return;
            }
            address = socket.getLocalAddress();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        if(address == null){
            return;
        }
        pipes.get(address).offer(packet);
        socketsWakeUp.get(address).run();
    }


}
