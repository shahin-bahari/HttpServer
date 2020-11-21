package com.shahin.httpServer.response;

import com.shahin.httpServer.connection.Connection;
import com.shahin.httpServer.http.HttpRequest;
import com.shahin.httpServer.utils.BufferCache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPOutputStream;

public abstract class HttpResponse {

    protected final String newLineCode = "\r\n";
    protected HttpResponseStatus status = HttpResponseStatus.OK;
    protected HttpRequest request;
    protected final Map<String,String> headers = new HashMap<>();
    protected String mimeType;
    protected long length;
    protected boolean useGzip = false;
    protected boolean chunkedTransfer = false;
    private boolean isHeaderSent = false;
    private final Connection connection;

    public HttpResponse(HttpRequest request) {
        this.request = request;
        this.connection = request.getConnection();
    }

    private HttpResponse(Connection connection){
        this.connection = connection;
    }

    public void addHeader(String key,String value){
        headers.put(key,value);
    }

    public abstract void send();

    protected void sendData(ByteBuffer buffer,Connection.WriteListener cb){
        if(!isHeaderSent){
            isHeaderSent = true;
            connection.writeDataToSocket(getHeader(), () -> sendData(buffer,cb));
        }else{
            if(buffer == null){
                if(cb!= null){
                    cb.onWriteDone();
                }
                return;
            }
            ByteBuffer sendBuffer;
            if (chunkedTransfer) {
                if (useGzip) {
                    sendBuffer = gzip(buffer);
                } else {
                    sendBuffer = chunkedBuffer(buffer);
                }
            } else {
                sendBuffer = buffer;
            }
            connection.writeDataToSocket(sendBuffer,cb);
        }
    }

    protected void endResponse(){
        if(chunkedTransfer){
            String res = "0"+newLineCode+newLineCode;
            connection.writeDataToSocket(ByteBuffer.wrap(res.getBytes()), connection::endOfResponse);
        }else{
            connection.endOfResponse();
        }
    }

    private ByteBuffer getHeader() {
        //chunkedTransfer |= length < 0;
        useGzip = useGzip || useGzipWhenAccepted();
        chunkedTransfer |= useGzip;
        StringBuilder headerResponse = new StringBuilder();
        headerResponse.append("HTTP/1.1 ").append(status.getStatusDescription()).append(newLineCode);

        if (mimeType != null && !mimeType.isEmpty()) {
            headerResponse.append("Content-Type: ").append(mimeType).append(newLineCode);
        }
        if (headers.get("Date") == null) {
            SimpleDateFormat sdf = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
            sdf.setTimeZone(TimeZone.getDefault());
            headerResponse.append("Date: ").append(sdf.format(new Date())).append(newLineCode);
        }

        for (String key : headers.keySet()) {
            String value = this.headers.get(key);
            headerResponse.append(key).append(": ").append(value).append(newLineCode);
        }

        if (!headers.containsKey("connection")) {
            headerResponse.append("Connection: ").append(keepAlive() ? "keep-alive" : "close").append(newLineCode);
        }

        if (useGzip) {
            chunkedTransfer = true;
            headerResponse.append("Content-Encoding: gzip").append(newLineCode);
        }

        if (request !=null && !request.getMethod().equalsIgnoreCase("HEAD")&& chunkedTransfer) {
            headerResponse.append("Transfer-Encoding: chunked").append(newLineCode);
        }

        if (!chunkedTransfer && !headers.containsKey("Content-Length") && length > 0) {
            headerResponse.append("Content-Length: ").append(length).append(newLineCode);
            useGzip = false;
        }
        headerResponse.append(newLineCode);
        String s = headerResponse.toString();
        return ByteBuffer.wrap(s.getBytes());
    }

    private ByteBuffer gzip(ByteBuffer buffer) {
        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream;
        try {
            gzipOutputStream = new GZIPOutputStream(bas);
            //todo buffer.array should change.
            gzipOutputStream.write(buffer.array(), 0, buffer.remaining());
            gzipOutputStream.close();
            BufferCache.recycleBuffer(buffer);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        int size = bas.size();
        String header = String.format("%x\r\n", size);
        ByteBuffer chunk = ByteBuffer.allocateDirect(size + header.length() + 2 );
        chunk.put(header.getBytes());
        chunk.put(bas.toByteArray());
        chunk.put(newLineCode.getBytes());
        chunk.flip();
        return chunk;
    }

    private ByteBuffer chunkedBuffer(ByteBuffer buffer) {
        int size = buffer.remaining();
        String header = String.format("%x\r\n", size);
        ByteBuffer chunk = ByteBuffer.allocateDirect(size + header.length() + 2);
        chunk.put(header.getBytes());
        chunk.put(buffer);
        chunk.put(newLineCode.getBytes());
        BufferCache.recycleBuffer(buffer);
        chunk.flip();
        return chunk;
    }

    private boolean useGzipWhenAccepted() {
        return (mimeType != null && (mimeType.toLowerCase().contains("text/") || mimeType.toLowerCase().contains("/json")));
    }

    private boolean keepAlive() {
        if(request == null) return false;
        String con = request.getHeaders().get("Connection");
        if (con == null || con.isEmpty()) {
            return false;
        }
        return con.toLowerCase().contains("keep-alive");
    }

    public static HttpResponse quickResponse(Connection connection,HttpResponseStatus status){
        HttpResponse response = new HttpResponse(connection) {

            @Override
            public void send() {
                sendData(null,this::endResponse);
            }
        };
        response.status = status;
        return response;
    }
}
