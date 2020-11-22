package com.shahin.httpServer.http;

import com.shahin.httpServer.connection.Connection;
import com.shahin.httpServer.utils.BufferCache;
import com.shahin.httpServer.utils.ParserUtils;

import java.nio.ByteBuffer;
import java.util.*;

public class HttpRequest {

    private final Map<String,String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, List<String>> params = new HashMap<>();
    private ByteBuffer firstDataBlock;  // keep reference for file upload
    private String uri;
    private String method;
    private String httpVersion;
    private final Connection connection;

    public HttpRequest(Connection connection) {
        this.connection = connection;
        connection.readDataFromSocket(buffer -> {
            if(parseHeader(buffer)){
                firstDataBlock = buffer;
                connection.requestReady();
            }else{
                connection.terminateConnection();
            }
        });
    }

    private boolean parseHeader(ByteBuffer buffer){
        boolean findN = false;
        boolean headerFound = false;
        StringBuilder sb = new StringBuilder();
        while(buffer.remaining() != 0){
            byte b = buffer.get();
            if(b == '\n'){
                findN = true;
                if(headerFound){
                    if(!parseAttr(sb.toString())){
                        return false;
                    }
                }else{
                    if(!parseHeaderLead(sb.toString())){
                        return false;
                    }
                    headerFound = true;
                }

                sb = new StringBuilder();
            }else if(b == '\r'){
                if(findN){
                    return buffer.get() == '\n';
                }
            }else{
                sb.append((char)b);
                findN = false;
            }
        }
        return false;
    }

    private boolean parseAttr(String str){
        int pos = str.indexOf(':');
        if(pos>0){
            headers.put(str.substring(0,pos),str.substring(pos+1).trim());
            return true;
        }
        return false;
    }

    private boolean parseHeaderLead(String str){
        StringTokenizer tokenizer = new StringTokenizer(str," ");
        if (!tokenizer.hasMoreElements()) {
            // bad Request
            return false;
        }
        method = tokenizer.nextToken().toUpperCase();
        if (!tokenizer.hasMoreElements()) {
            // bad Request
            return false;
        }
        String temp = tokenizer.nextToken();
        uri = ParserUtils.parseIncomingUri(temp,params);
        uri = uri.replaceAll("(//+)","/");
        if (!tokenizer.hasMoreElements()) {
            // bad Request
            return false;
        }
        httpVersion = tokenizer.nextToken();
        return true;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, List<String>> getParams() {
        return params;
    }

    public String getUri() {
        return uri;
    }

    public String getMethod() {
        return method;
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public Connection getConnection() {
        return connection;
    }

    public ByteBuffer getFirstDataBlock() {
        return firstDataBlock;
    }

    public void recycle(){
        BufferCache.recycleBuffer(firstDataBlock);
    }
}
