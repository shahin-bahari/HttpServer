package com.shahin.httpServer.webSocket;

import com.shahin.httpServer.http.HttpRequest;

import java.nio.ByteBuffer;

public interface WebSocket {

    String WEB_SOCKET_VERSION_TAG = "Sec-WebSocket-Version";
    String WEB_SOCKET_VERSION_VALUE = "13";
    String WEB_SOCKET_KEY_TAG = "Sec-WebSocket-Key";
    String WEB_SOCKET_UPGRADE_TAG = "Upgrade";
    String WEB_SOCKET_UPGRADE_VALUE = "websocket";
    String WEB_SOCKET_CONNECTION_TAG = "Connection";
    String WEB_SOCKET_CONNECTION_VALUE = "Upgrade";
    String WEB_SOCKET_ACCEPT_TAG = "sec-websocket-accept";

    default void onOpen(HttpRequest session){

    }

    default void onClose(WebSocketSession session, CloseCode closeCode,String reason){

    }

    default void onError(Throwable throwable){

    }

    default void onMessage(WebSocketSession session, String msg){

    }

    default void onMessage(WebSocketSession session, ByteBuffer data){

    }
}
