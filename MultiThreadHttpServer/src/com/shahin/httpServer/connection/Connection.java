package com.shahin.httpServer.connection;

import com.shahin.httpServer.http.HttpRequest;
import com.shahin.httpServer.router.Router;
import com.shahin.httpServer.webSocket.WebSocket;
import com.shahin.httpServer.webSocket.WebSocketSession;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;

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
    private List<SocketProxy.Packet> temporaryReqList;
    private final Router router;
    private WebSocketSession wsSession;

    public Connection(List<SocketProxy.Packet> reqList,SocketChannel socket,Router router){
        this.socketChannel = socket;
        this.temporaryReqList = reqList;
        this.router = router;
        request = new HttpRequest(this);
    }

    public void newIncomingData(List<SocketProxy.Packet> reqList,ByteBuffer buffer){
        this.temporaryReqList = reqList;
        if(readListener != null){
            readListener.onRead(buffer);
        }
    }

    public void writeDoneAck(List<SocketProxy.Packet> reqList){
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
        request.recycle();
        temporaryReqList.add(new SocketProxy.Packet(socketChannel, SocketProxy.Type.CLOSE_REQUEST));
    }

    public void endOfResponse(){
        request.recycle();
        writeListener = null;
        readListener = null;
        if(wsSession == null){
            request = new HttpRequest(this);
        }else{
            request = null;
            wsSession.startRead();
        }
    }

    public Router getRouter() {
        return router;
    }

    public void requestReady(){
        WebSocket ws = router.doRoute(request);
        if(ws !=null){
            wsSession = new WebSocketSession(request,ws);
        }
    }

    @Override
    public String toString() {
        return "Connection{" +
                socketChannel +
                '}';
    }
}
