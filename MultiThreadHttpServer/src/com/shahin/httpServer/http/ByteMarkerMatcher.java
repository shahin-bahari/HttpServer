package com.shahin.httpServer.http;

import com.shahin.httpServer.utils.BufferCache;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class ByteMarkerMatcher {

    interface MarkerMatcherCallback{
        void startTag(Map<String,String> tags);
        void data(ByteBuffer buffer);
        void endTag();
        void error(String err);
    }

    private final byte[] marker;
    private final StringBuilder tag = new StringBuilder();
    private int position;
    private ByteBuffer buffer = BufferCache.generateBuffer();
    private int state;
    private final MarkerMatcherCallback cb;
    private final int WAIT_MARKER = 1;
    private final int INFO = 2;
    private final int FILE_CONTENT = 3;

    private byte last;
    private boolean firstNewLine;
    private long currentLen;
    private final Map<String,String> tags = new HashMap<>();

    public ByteMarkerMatcher(String boundary, MarkerMatcherCallback cb) {
        this.cb = cb;
        byte[] bb = boundary.getBytes();
        this.marker = new byte[bb.length+4];
        this.marker[0] = '\r';
        this.marker[1] = '\n';
        this.marker[2] = '-';
        this.marker[3] = '-';
        System.arraycopy(bb,0,this.marker,4,bb.length);
        state = WAIT_MARKER;
        position = 2;
    }

    public void check(byte b){
        if(!checkLen()){
            cb.error("unexpected length");
            return;
        }
        switch (state){
            case WAIT_MARKER:
                if(b == marker[position]){
                    position++;
                    if(position == marker.length){
                        state = INFO;
                        last = 0;
                        firstNewLine = true;
                        position = 0;
                    }
                }else{
                    cb.error("No Start boundary");
                }
                break;

            case INFO:
                if(b == '\n' && last == '\r'){
                    if(tag.length() > 0){
                        extractTagInfo(tag.toString());
                        tag.setLength(0);
                    }else{
                        if(!firstNewLine){
                            state = FILE_CONTENT;
                            cb.startTag(tags);
                            tags.clear();
                        }
                        firstNewLine = false;
                    }
                }else if(b == '-' && last == '-'){
                    cb.endTag();
                    tag.setLength(0);
                    break;
                }
                if(b != '\n' && b != '\r'){
                    tag.append((char)b);
                }
                last = b;
                break;

            case FILE_CONTENT:
                if(b == marker[position]){
                    position++;
                    if(position == marker.length){
                        state = INFO;
                        firstNewLine= true;
                        last = 0;
                        position = 0;
                        buffer.flip();
                        cb.data(buffer);
                        buffer = BufferCache.generateBuffer();
                    }
                }else{
                    if(position>0){
                        for(int i = 0 ; i < position ; i++){
                            addToBuffer(marker[i]);
                        }
                        position =0;
                        if(b == marker[0]){
                            position++;
                            return;
                        }
                    }
                    addToBuffer(b);
                }
                break;
        }
    }

    private int lastState;

    private boolean checkLen() {
        if(lastState != state){
            lastState = state;
            currentLen = 0;
        }else{
            if(state == WAIT_MARKER && currentLen > 75){
                return false;
            }else return state != INFO || currentLen <= 300;
        }
        currentLen++;
        return true;
    }

    private void addToBuffer(byte b){
        buffer.put(b);
        if(buffer.position() == buffer.capacity()-2){
            buffer.flip();
            cb.data(buffer);
            buffer = BufferCache.generateBuffer();
        }
    }

    private void extractTagInfo(String rawTag){
        String[] parts = rawTag.split(";");
        String temp;
        for(String str : parts){
            str = str.trim();
            if(str.startsWith("name")){
                temp  = str.substring("name=\"".length(),str.length()-1);
                tags.put("name",temp);
            }else if(str.startsWith("filename")){
                temp = str.substring("filename=\"".length(),str.length()-1);
                tags.put("filename",temp);
            }else if(str.startsWith("Content-Type")){
                temp = str.substring("Content-Type:".length()).trim();
                tags.put("Content-Type",temp);
            }
        }
    }
}
