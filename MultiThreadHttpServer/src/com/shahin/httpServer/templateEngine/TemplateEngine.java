package com.shahin.httpServer.templateEngine;

import com.shahin.httpServer.http.HttpRequest;
import com.shahin.httpServer.response.HttpResponse;
import com.shahin.httpServer.response.HttpResponseStatus;
import com.shahin.httpServer.response.TextResponse;
import com.shahin.httpServer.utils.BufferCache;
import com.shahin.httpServer.utils.MimeTypes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.Callable;

public class TemplateEngine {

    private long pos;
    private CharBuffer data;
    private Path templatePath = null;
    private final StringBuilder resultHtml = new StringBuilder();

    public TemplateEngine(Path templatePath){
        this.templatePath = templatePath;
    }

    public TemplateEngine(String template){
        data = CharBuffer.wrap(template);
    }

    public HttpResponse render(HttpRequest request, HttpResponseStatus stat, Map<String,Callable<String>> params){
        if(templatePath == null){
            parseTemplate(params);
            TextResponse response = new TextResponse(request,HttpResponseStatus.OK, MimeTypes.HTML);
            response.writeMessage(resultHtml.toString());
            return response;
        }
        FileChannel fileChannel;
        long fileSize;
        try {
            fileChannel = FileChannel.open(templatePath, StandardOpenOption.READ);
            fileSize = fileChannel.size();
        } catch (IOException e) {
            e.printStackTrace();
            return request.getConnection().getRouter()
                    .getServerDefaultResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR).resolve(request);
        }
        return new HttpResponse(request){

            @Override
            public void send() {
                status = stat;
                ByteBuffer buffer = BufferCache.generateBuffer();
                data = CharBuffer.allocate((int) fileSize);
                try {
                    boolean eof;
                    do{
                        int len = fileChannel.read(buffer);
                        buffer.flip();
                        pos += len;
                         eof = fileSize <= pos;
                         StandardCharsets.UTF_8.newDecoder().decode(buffer,data,eof);
                    }while(!eof);
                    data.flip();
                    parseTemplate(params);
                    sendData(ByteBuffer.wrap(resultHtml.toString().getBytes()), this::endResponse);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    enum TemplateState{
        Idle,
        Content
    }

    private void parseTemplate(Map<String,Callable<String>> params){
        MarkerMatcher matcher = new MarkerMatcher();
        matcher.setMarker(resultHtml,"<coffee".toCharArray());
        StringBuilder temp = new StringBuilder();
        TemplateState state = TemplateState.Idle;
        for(int pos = 0 ; pos <data.limit() ; pos++){
            char ch = data.get();
            switch (state){
                case Idle:
                    if(matcher.check(ch)){
                        state = TemplateState.Content;
                        matcher.setMarker(temp,"/>".toCharArray());
                    }
                    break;
                case Content:
                    if(matcher.check(ch)){
                        state = TemplateState.Idle;
                        Callable<String> method = params.get(temp.toString().trim());
                        if(method != null){
                            resultHtml.append(getContent(method));
                        }
                        matcher.setMarker(resultHtml,"<coffee".toCharArray());
                        temp.setLength(0);
                    }
            }
        }
    }

    private String getContent(Callable<String> item){
        try {
            return item.call();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
