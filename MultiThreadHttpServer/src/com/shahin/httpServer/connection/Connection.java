package com.shahin.httpServer.connection;

import com.shahin.httpServer.http.HttpRequest;
import com.shahin.httpServer.router.Router;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

public class Connection {

    public interface ReadListener{
        void onRead(ByteBuffer buffer);
    }

    public interface WriteListener{
        void onWriteDone();
    }

    private final SocketChannel socketChannel;
    private ReadListener readListener ;
    private WriteListener writeListener;
    private HttpRequest request;
    private ArrayList<SocketProxy.Packet> temporaryReqList;
    private final Router router;

    public Connection(ArrayList<SocketProxy.Packet> reqList,SocketChannel socket,Router router){
        this.socketChannel = socket;
        this.temporaryReqList = reqList;
        this.router = router;
        request = new HttpRequest(this);
    }

    public void newIncomingData(ArrayList<SocketProxy.Packet> reqList,ByteBuffer buffer){
        this.temporaryReqList = reqList;
        if(readListener != null){
            readListener.onRead(buffer);
        }
    }

    public void writeDoneAck(ArrayList<SocketProxy.Packet> reqList){
        this.temporaryReqList = reqList;
        if(writeListener != null){
            writeListener.onWriteDone();
        }
    }

    public void readDataFromSocket(ReadListener cb){
        readListener = cb;
        temporaryReqList.add(new SocketProxy.Packet(socketChannel, SocketProxy.Type.READ_REQUEST));
    }

    public void writeDataToSocket(ByteBuffer buffer,WriteListener cb){
        writeListener = cb;
        SocketProxy.Packet packet = new SocketProxy.Packet(socketChannel, SocketProxy.Type.WRITE_REQUEST);
        packet.data = buffer;
        temporaryReqList.add(packet);
    }

    public void terminateConnection(){
        temporaryReqList.add(new SocketProxy.Packet(socketChannel, SocketProxy.Type.CLOSE_REQUEST));
    }

    public void endOfResponse(){
        request.recycle();
        writeListener = null;
        readListener = null;
        request = new HttpRequest(this);
        //todo add websocket start read
    }

    public Router getRouter() {
        return router;
    }

    public void requestReady(){
        router.doRoute(request);
    }




}
