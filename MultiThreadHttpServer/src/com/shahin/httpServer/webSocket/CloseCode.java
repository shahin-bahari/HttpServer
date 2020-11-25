package com.shahin.httpServer.webSocket;

public enum CloseCode {
    NormalClosure(1000),
    GoingAway(1001),
    ProtocolError(1002),
    UnsupportedData(1003),
    NoStatusReceived(1005),
    AbnormalClosure(1006),
    InvalidFramePayloadData(1007),
    PolicyViolation(1008),
    MessageTooBig(1009),
    MandatoryExt(1010),
    InternalServerError(1011),
    ServiceRestart(1012),
    TryAgainLater(1013),
    BadGateway(1014),
    TLSHandshake(1015);

    public static CloseCode lookup(int value) {
        for (CloseCode code : values()) {
            if (code.getValue() == value) {
                return code;
            }
        }
        return null;
    }

    private final int code;

    CloseCode(int code) {
        this.code = code;
    }

    public int getValue() {
        return this.code;
    }
}
