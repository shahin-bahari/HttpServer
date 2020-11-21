package com.shahin.httpServer.connection;

import com.shahin.httpServer.http.HttpRequest;
import com.shahin.httpServer.response.FileResponse;
import com.shahin.httpServer.response.HttpResponseStatus;
import com.shahin.httpServer.response.TextResponse;
import com.shahin.httpServer.utils.MimeTypes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
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

    public Connection(ArrayList<SocketProxy.Packet> reqList,SocketChannel socket){
        this.socketChannel = socket;
        this.temporaryReqList = reqList;
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

    public void requestReady(){
        try {
            FileResponse file = new FileResponse(request, Paths.get("C:/Users/shahi/Desktop/Untitled-1.txt"));
            file.send();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
