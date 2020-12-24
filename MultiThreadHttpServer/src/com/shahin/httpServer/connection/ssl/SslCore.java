package com.shahin.httpServer.connection.ssl;

import com.shahin.httpServer.utils.BufferCache;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.nio.ByteBuffer;

/*
*                    app data

                |           ^
                |     |     |
                v     |     |
           +----+-----|-----+----+
           |          |          |
           |       SSL|Engine    |
   wrap()  |          |          |  unwrap()
           | OUTBOUND | INBOUND  |
           |          |          |
           +----+-----|-----+----+
                |     |     ^
                |     |     |
                v           |

                   net data
* */

public class SslCore {

    private final SSLEngine sslEngine;

    private int bufferSize;

    private final ByteBuffer inboundBuffer;
    private final ByteBuffer outboundBuffer;
    private final ByteBuffer wrapBuffer;
    private final ByteBuffer unwrapBuffer;

    public SslCore(SSLEngine sslEngine) {
        this.sslEngine = sslEngine;

        SSLSession sslSession = sslEngine.getSession();
        bufferSize = sslSession.getPacketBufferSize();
        if(bufferSize < BufferCache.BUFFER_SIZE){
            inboundBuffer = BufferCache.generateBuffer();
            outboundBuffer = BufferCache.generateBuffer();
        }else{
            inboundBuffer = ByteBuffer.allocate(bufferSize);
            outboundBuffer = ByteBuffer.allocate(bufferSize);
        }

        bufferSize = sslSession.getApplicationBufferSize();
        if(bufferSize < BufferCache.BUFFER_SIZE){
            wrapBuffer = BufferCache.generateBuffer();
            unwrapBuffer = BufferCache.generateBuffer();
        }else{
            wrapBuffer = ByteBuffer.allocate(bufferSize);
            unwrapBuffer = ByteBuffer.allocate(bufferSize);
        }

    }

    public void accept() throws SSLException {
        sslEngine.beginHandshake();
    }

    public void incomingData(ByteBuffer netInData){
        SSLEngineResult.HandshakeStatus handshakeStatus = sslEngine.getHandshakeStatus();
        SSLEngineResult result;
        while(handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED &&
                handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING)
        switch (handshakeStatus){
            case NEED_UNWRAP -> {
                try {
                    result = sslEngine.unwrap(netInData,unwrapBuffer);
                    unwrapBuffer.compact();
                    handshakeStatus = sslEngine.getHandshakeStatus();
                } catch (SSLException e) {
                    e.printStackTrace();
                    sslEngine.closeOutbound();
                    handshakeStatus = sslEngine.getHandshakeStatus();
                    break;
                }
                switch (result.getStatus()){
                    case OK -> {break;}

                }
            }

            case NEED_TASK -> {
                Runnable run = sslEngine.getDelegatedTask();
                while(run != null){
                    //run.run();
                    run = sslEngine.getDelegatedTask();
                    handshakeStatus = sslEngine.getHandshakeStatus();
                }
                handshakeStatus = sslEngine.getHandshakeStatus();
            }
        }
        int a = 1;
    }

    public void close(){

    }

    public void flush(){}

    public int unwrap(ByteBuffer applicationInBuffer) throws IOException{
        SSLEngineResult res = sslEngine.unwrap(applicationInBuffer,unwrapBuffer);
        SSLEngineResult.Status d = res.getStatus();
        switch (res.getStatus()){
            case BUFFER_OVERFLOW:
            break;
        }
        return 0;
    }

    public int wrap(ByteBuffer src) throws IOException { return 0;}

    private void doUnwrap(ByteBuffer dst){

    }
}
