package com.shahin.httpServer.webSocket;

import com.shahin.httpServer.utils.BufferCache;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class WebSocketFrame {

    // only used during receiving data
    private final int maxFrameSize;
    private FrameState frameState = FrameState.WaitForHeader;
    private int tempIndex;

    private WebSocketOpCode opCode;
    private boolean fin;
    private byte[] maskKey;
    private long payloadLength;
    private final List<ByteBuffer> payloads = new ArrayList<>(1);

    private enum FrameState{
        WaitForHeader,
        WaitForLen,
        WaitForLenExtend16,
        WaitForLenExtend64,
        WaitForMask,
        WaitForPayload,
        MessageReady
    }

    public WebSocketFrame(int maxFrameSize){
        this.maxFrameSize = maxFrameSize;
    }

    public WebSocketFrame(boolean isFinal,WebSocketOpCode opCode,List<ByteBuffer> pl){
        fin = isFinal;
        this.opCode = opCode;
        payloads.addAll(pl);
        maxFrameSize = 0;
        for(ByteBuffer buffer : pl){
            payloadLength += buffer.remaining();
        }
    }

    public WebSocketFrame(boolean isFinal, ByteBuffer data){
        this(isFinal,data,null);
    }

    public WebSocketFrame(boolean isFinal,String msg){
        this(isFinal,msg,null);
    }

    public WebSocketFrame(boolean isFinal, ByteBuffer data,byte[] mask){
        maxFrameSize = 0;
        this.opCode = WebSocketOpCode.Binary;
        payloadLength = data.remaining();
        this.fin = isFinal;
        maskKey = mask;
        if(mask != null){
            int pos = 0;
            while(pos < payloadLength){
                ByteBuffer buffer = BufferCache.generateBuffer();
                for(int i = 0; i < BufferCache.BUFFER_SIZE && pos < payloadLength ; i++){
                    buffer.put((byte) (data.get() ^ mask[pos%4]));
                    pos++;
                }
                buffer.flip();
                payloads.add(buffer);
            }
        }else{
            payloads.add(data);
        }
    }

    public WebSocketFrame(boolean isFinal,String msg,byte[] mask){
        this(isFinal, ByteBuffer.wrap(msg.getBytes()),mask);
        this.opCode = WebSocketOpCode.Text;

    }

    public void appendData(ByteBuffer buffer) throws WebSocketException {
        int len = buffer.remaining();
        while(len > 0){
            byte b = buffer.get();
            appendByte(b);
            len--;
        }
        BufferCache.recycleBuffer(buffer);
    }

    public int getPayloadSize(){
        return (int) payloadLength;
    }

    public WebSocketOpCode getOpCode(){
        return opCode;
    }

    public List<ByteBuffer> getPayloads(){
        return payloads;
    }

    public boolean isComplete(){
        return frameState == FrameState.MessageReady;
    }

    private void appendByte(byte b) throws WebSocketException {
        switch (frameState){
            case WaitForHeader -> setHeader(b);
            case WaitForLen -> setLen(b);
            case WaitForLenExtend16, WaitForLenExtend64 -> setLenExtend(b);
            case WaitForMask -> setMask(b);
            case WaitForPayload -> setPayload(b);
        }
    }

    private void setHeader(byte head) throws WebSocketException {
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
        frameState = FrameState.WaitForLen;
        payloads.add(BufferCache.generateBuffer());
    }

    private void setLen(byte b) throws WebSocketException {
        boolean masked = (b & 0x80) != 0;
        if(masked){
            maskKey = new byte[4];
        }
        payloadLength = (byte)( 0x7F & b);
        if (payloadLength == 126) {
            frameState = FrameState.WaitForLenExtend16;
            tempIndex = 0;
            payloadLength = 0;
            return;
        }else if (payloadLength == 127) {
            frameState = FrameState.WaitForLenExtend64;
            tempIndex = 0;
            payloadLength = 0;
            return;
        }
        if (this.opCode == WebSocketOpCode.Close && this.payloadLength == 1) {
            throw new WebSocketException(CloseCode.ProtocolError, "Received close frame with payload len 1.");
        }

        if(masked){
            frameState = FrameState.WaitForMask;
            return;
        }
        frameState = payloadLength == 0 ? FrameState.MessageReady:FrameState.WaitForPayload;
    }

    private void setLenExtend(byte b) throws WebSocketException {
        int l = (frameState == FrameState.WaitForLenExtend16)?2:8;
        payloadLength |= (b & 0xFF) << (8*(l - tempIndex -1));
        tempIndex++;
        if(tempIndex == l){
            if (frameState == FrameState.WaitForLenExtend16 &&payloadLength < 126) {
                throw new WebSocketException(CloseCode.ProtocolError, "Invalid data frame 2byte length. (not using minimal length encoding)");
            }
            if (frameState == FrameState.WaitForLenExtend64 && payloadLength < 65536) {
                throw new WebSocketException(CloseCode.ProtocolError, "Invalid data frame 4byte length. (not using minimal length encoding)");
            }
            if (payloadLength > maxFrameSize) {
                throw new WebSocketException(CloseCode.MessageTooBig, "Max allowed frame length has been exceeded.");
            }
            if (this.opCode.isControlFrame() && payloadLength > 125) {
                throw new WebSocketException(CloseCode.ProtocolError, "Control frame with payload length > 125 bytes.");

            }
            frameState = (maskKey != null)?FrameState.WaitForMask : FrameState.WaitForPayload;
            tempIndex = 0;
        }
    }

    private void setMask(byte b){
        maskKey[tempIndex++] = b;
        if(tempIndex == 4){
            prepareWaitForPayload();
        }
    }

    private void prepareWaitForPayload(){
        frameState = payloadLength == 0 ? FrameState.MessageReady :FrameState.WaitForPayload;
        tempIndex = 0;
    }

    private void setPayload(byte b){
        byte data = b;
        if(maskKey != null){
            data ^= maskKey[tempIndex%4];
        }
        tempIndex++;
        ByteBuffer buffer = payloads.get(payloads.size()-1);
        buffer.put(data);

        if(buffer.remaining() == 0 || tempIndex == payloadLength){
            buffer.flip();
            if(tempIndex != payloadLength){
                payloads.add(BufferCache.generateBuffer());
            }
        }

        if(tempIndex == payloadLength){
            frameState = FrameState.MessageReady;
        }
    }

    public boolean isFinal() {
        return fin;
    }

    public List<ByteBuffer> getSendPackage(){
        List<ByteBuffer> pack = new ArrayList<>(payloads.size() + 1);
        ByteBuffer buffer = BufferCache.generateBuffer();
        fillMessageHeader(buffer);
        if(payloads.size() == 1 && payloads.get(0).remaining() < buffer.remaining()){
            buffer.put(payloads.get(0));
            buffer.flip();
            pack.add(buffer);
            BufferCache.recycleBuffer(payloads.get(0));
        }else{
            buffer.flip();
            pack.add(buffer);
            pack.addAll(payloads);
        }
        return pack;
    }

    private void fillMessageHeader(ByteBuffer buffer){
        byte header = 0;
        header |= fin ? 0x80 : 0x00;
        header |= (opCode.getValue() & 0x0F);
        buffer.put(header);
        boolean masked = maskKey != null;
        if(payloadLength <= 125){
            byte len =  (byte) payloadLength;
            len |= masked ? 0x80 :0x00;
            buffer.put(len);
        }else if(payloadLength < 0xFFFF){
            byte b0 = (byte) (masked ? 0xFE : 0x7E);
            byte b1 = (byte) (payloadLength >>>8);
            byte b2 = (byte) payloadLength;
            buffer.put(b0).put(b1).put(b2);
        }else{
            byte[] b = new byte[8];
            b[0] = (byte) (masked ? 0xFF : 0x7F);
            for(int i = 1 ; i < 8 ; i++){
                b[i] = (byte) (payloadLength >>> ((7-i)*8));
            }
            buffer.put(b);
        }
        if(masked){
            buffer.put(maskKey);
        }
    }
}
