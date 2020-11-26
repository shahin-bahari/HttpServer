package com.shahin.httpServer.webSocket;

import com.shahin.httpServer.http.ByteStringBuilder;
import com.shahin.httpServer.http.HttpRequest;
import com.shahin.httpServer.logger.Log;
import com.shahin.httpServer.response.HttpResponse;
import com.shahin.httpServer.response.HttpResponseStatus;
import com.shahin.httpServer.utils.BufferCache;
import com.shahin.httpServer.utils.ParserUtils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class WebSocketSession {

    private final HttpRequest request;
    private final WebSocket ws;
    private WebSocketFrame frame = new WebSocketFrame(BufferCache.BUFFER_SIZE);
    private WebSocketOpCode lastOpCode = null;
    private final ByteStringBuilder stringBuilder = new ByteStringBuilder();
    private boolean shouldClose = false;

    public WebSocketSession(HttpRequest req, WebSocket ws) {
        this.request = req;
        this.ws = ws;
        init();
    }

    public boolean send(String msg){
return true;
    }

    public void send(String msg,byte[] mask){

    }

    public void send(ByteBuffer buffer){

    }

    public void send(ByteBuffer buffer,byte[] mask){

    }

    public void ping(ByteBuffer data){

    }

    public void close(CloseCode closeCode,String reason){
        shouldClose = true;
        sendFrame(getCloseFrame(closeCode, reason));
    }

    public void startRead(){
        request.getConnection().readDataFromSocket(buffer -> {
            try {
                frame.appendData(buffer);
                if(frame.isComplete()){
                    handleNewFrame();
                    frame.recycle();
                    frame = new WebSocketFrame(BufferCache.BUFFER_SIZE);
                }
                startRead();
            } catch (WebSocketException e) {
                ws.onError(e);
            }
        });
    }

    private void handleNewFrame(){
        switch (frame.getOpCode()){
            case Text -> {
                //ws.onMessage(this, frame.getStringPayload(),!frame.isFinal());
                lastOpCode = WebSocketOpCode.Text;
            }
            case Binary -> {
                List<ByteBuffer> payloads = frame.getPayloads();
                lastOpCode = WebSocketOpCode.Binary;
                for(ByteBuffer buffer : payloads){
                    ws.onMessage(this,buffer,!frame.isFinal());
                    BufferCache.recycleBuffer(buffer);
                }
            }
            case Ping -> sendFrame(new WebSocketFrame(true,WebSocketOpCode.Pong,frame.getPayloads()));
            case Pong -> ws.onPong(frame.getPayloads().get(0));
            case Close -> handleCloseFrame();
            case Continuation -> {}
        }
    }

    private void handleCloseFrame(){
        CloseCode closeCode = CloseCode.NormalClosure;
        String closeReason = "";
        if(frame.getPayloads().size() > 1){
            ByteBuffer buffer = frame.getPayloads().get(0);
            if(buffer.remaining() > 1){
                int code = ((buffer.get() & 0xFF) <<8) + (buffer.get() & 0xFF);
                closeCode = CloseCode.lookup(code);
                closeReason = StandardCharsets.UTF_8.decode(buffer).toString();
            }
        }
        if(shouldClose){
            ws.onClose(this,closeCode,closeReason);
            request.getConnection().terminateConnection();
        }else{
            shouldClose = true;
            sendFrame(getCloseFrame(closeCode,closeReason));
        }
    }

    private WebSocketFrame getCloseFrame(CloseCode code,String reason){
        if(code != null){
            ByteBuffer buffer = BufferCache.generateBuffer();
            byte[] p = reason.getBytes(StandardCharsets.UTF_8);
            byte b = (byte) ((code.getValue() >>>8 ) & 0xFF);
            buffer.put(b);
            b = (byte) (code.getValue() & 0xFF);
            buffer.put(b);
            buffer.put(p);
            buffer.flip();
            List<ByteBuffer> lst = new ArrayList<>(1);
            lst.add(buffer);
            return new WebSocketFrame(true,WebSocketOpCode.Close,lst);
        }
        return null;
    }

    private String getKey(){
        return request.getHeaders().get(WebSocket.WEB_SOCKET_KEY_TAG);
    }

    private void init(){
        HttpResponse response = HttpResponse.quickResponse(request.getConnection()
                , HttpResponseStatus.SWITCHING_PROTOCOLS);
        response.addHeader(WebSocket.WEB_SOCKET_UPGRADE_TAG,WebSocket.WEB_SOCKET_UPGRADE_VALUE);
        response.addHeader(WebSocket.WEB_SOCKET_CONNECTION_TAG,WebSocket.WEB_SOCKET_UPGRADE_TAG);
        String resKey = null;
        try {
            resKey = generateWebSocketAcceptKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            Log.log(Level.SEVERE,"Web Socket","SHA-1 is not available");
            request.getConnection().terminateConnection();
        }
        response.addHeader(WebSocket.WEB_SOCKET_ACCEPT_TAG,resKey);
        response.send();
        ws.onOpen(this);
    }

    private String generateWebSocketAcceptKey() throws NoSuchAlgorithmException {
        String WEB_SOCKET_GUID_KEY = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        String text = getKey() + WEB_SOCKET_GUID_KEY;
        md.update(text.getBytes(), 0, text.length());
        byte[] sha1hash = md.digest();
        return ParserUtils.encodeBase64(sha1hash);
    }

    private void sendFrame(WebSocketFrame frame){
        if(frame != null){

        }
    }

    void t(){
        byte[] maskKey = new byte[4];
        /*maskKey[3] = (byte) ((mask >>24)&0xFF);
        maskKey[2] = (byte) ((mask >>16)&0xFF);
        maskKey[1] = (byte) ((mask >>8)&0xFF);
        maskKey[0] = (byte) ( mask &0xFF);*/
    }
}
