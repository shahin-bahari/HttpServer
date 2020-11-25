package com.shahin.httpServer.webSocket;

public enum WebSocketOpCode {
    Continuation(0),
    Text(1),
    Binary(2),
    Close(8),
    Ping(9),
    Pong(10);

    public static WebSocketOpCode lookup(byte value) {
        for (WebSocketOpCode opcode : values()) {
            if (opcode.getValue() == value) {
                return opcode;
            }
        }
        return null;
    }

    private final byte code;

    WebSocketOpCode(int code) {
        this.code = (byte) code;
    }

    public byte getValue() {
        return this.code;
    }

    public boolean isControlFrame() {
        return this == Close || this == Ping || this == Pong;
    }
}
