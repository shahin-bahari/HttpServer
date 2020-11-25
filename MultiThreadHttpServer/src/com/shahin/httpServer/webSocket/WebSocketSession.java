package com.shahin.httpServer.webSocket;

import com.shahin.httpServer.http.HttpRequest;
import com.shahin.httpServer.logger.Log;
import com.shahin.httpServer.response.HttpResponse;
import com.shahin.httpServer.response.HttpResponseStatus;
import com.shahin.httpServer.utils.ParserUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;

public class WebSocketSession {

    private final HttpRequest request;
    private final WebSocket ws;

    public WebSocketSession(HttpRequest req, WebSocket ws) {
        this.request = req;
        this.ws = ws;
        init();
    }

    public void startRead(){

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
        ws.onOpen(request);
    }

    private String generateWebSocketAcceptKey() throws NoSuchAlgorithmException {
        String WEB_SOCKET_GUID_KEY = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        String text = getKey() + WEB_SOCKET_GUID_KEY;
        md.update(text.getBytes(), 0, text.length());
        byte[] sha1hash = md.digest();
        return ParserUtils.encodeBase64(sha1hash);
    }
}
