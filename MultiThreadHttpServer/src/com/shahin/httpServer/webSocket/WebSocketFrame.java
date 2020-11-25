package com.shahin.httpServer.webSocket;

import java.nio.ByteBuffer;

public class WebSocketFrame {

    private WebSocketOpCode opCode;
    private boolean fin;
    private byte[] maskKey;
    private ByteBuffer payload;

    public WebSocketFrame(ByteBuffer incoming) throws WebSocketException{
        if(incoming.remaining() < 6){
            throw new WebSocketException(CloseCode.ProtocolError,"size mismatch");
        }
        byte head = incoming.get();
        fin = (head & 0x80) != 0;
        opCode = WebSocketOpCode.lookup((byte) (head & 0x0F));
        if ((head & 0x70) != 0) {
            throw new WebSocketException(CloseCode.ProtocolError, "The reserved bits (" + Integer.toBinaryString(head & 0x70) + ") must be 0.");
        }
        if (opCode == null) {
            throw new WebSocketException(CloseCode.ProtocolError, "Received frame with reserved/unknown opcode " + (head & 0x0F) + ".");
        } else if (opCode.isControlFrame() && !fin) {
            throw new WebSocketException(CloseCode.ProtocolError, "Fragmented control frame.");
        }
    }

    private int readUnsignedInt(ByteBuffer buffer,int len){
        int res = 0;
        for(int i = 0 ; i < len ; i++){
            res |= (buffer.get() & 0xFF) << (8*(len-i-1));
        }
        return res;
    }

    private long readUnsignedLong(ByteBuffer buffer,int len){
        long res = 0;
        for(int i = 0 ; i < len ; i++){
            res += (buffer.get() & 0xFF) << (8*(len-i-1));
        }
        return res;
    }
}
