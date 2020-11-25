package com.shahin.httpServer.webSocket;

public class WebSocketException extends Exception {
    private final CloseCode closeCode;
    private final String reason;

    public WebSocketException(CloseCode closeCode, String reason) {
        this(closeCode,reason,null);
    }

    public WebSocketException(CloseCode closeCode, String reason,Exception cause) {
        super(closeCode + ": " + reason,cause);
        this.closeCode = closeCode;
        this.reason = reason;
    }

    public WebSocketException(Exception cause) {
        this(CloseCode.InternalServerError,cause.toString(),cause);
    }

    public CloseCode getCloseCode() {
        return closeCode;
    }

    public String getReason() {
        return reason;
    }
}
