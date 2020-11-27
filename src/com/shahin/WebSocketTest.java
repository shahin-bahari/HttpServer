package com.shahin;

import com.shahin.httpServer.webSocket.CloseCode;
import com.shahin.httpServer.webSocket.WebSocket;
import com.shahin.httpServer.webSocket.WebSocketException;
import com.shahin.httpServer.webSocket.WebSocketSession;

public class WebSocketTest implements WebSocket {

    @Override
    public void onOpen(WebSocketSession session) {
        System.out.println("new web socket, client from : "
                + session.getRequest().getConnection().toString());
    }

    @Override
    public void onClose(WebSocketSession session, CloseCode closeCode, String reason) {
        System.out.println("close " + reason);
    }

    @Override
    public void onError(WebSocketException exception) {
        System.out.println(exception.getCloseCode());
    }

    @Override
    public void onMessage(WebSocketSession session, String msg, boolean hasMore) {
        System.out.println(msg);
        session.send(msg);  // echo
        if(msg.equals("close")){
            session.close(CloseCode.GoingAway,"bye bye");
        }
    }
}
