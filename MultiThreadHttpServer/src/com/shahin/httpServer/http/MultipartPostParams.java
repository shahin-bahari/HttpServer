package com.shahin.httpServer.http;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultipartPostParams {

    private final byte[] marker;
    private final Map<String,String> tags = new HashMap<>();
    private final ByteStringBuilder bsb = new ByteStringBuilder();
    private final Map<String, List<String>> result = new HashMap<>();

    private final PostDataParser.PostResult cb;
    private final int WAIT_MARKER = 1;
    private final int INFO = 2;
    private final int FILE_CONTENT = 3;

    private int position;
    private int state;
    private byte last;
    private boolean firstNewLine;
    private boolean validParams = true;
    private long currentLen;

    public MultipartPostParams(String boundary, PostDataParser.PostResult cb) {
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

    public int check(ByteBuffer buffer){
        int len = buffer.remaining();
        while(buffer.remaining() != 0){
            checkByte(buffer.get());
        }
        return len;
    }

    private void checkByte(byte b){
        if(!checkLen()){
            cb.paramResult(PostDataResult.BAD_FRAME,null);
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
                    cb.paramResult(PostDataResult.BAD_FRAME,null);  //error
                    return;
                }
                break;

            case INFO:
                if(b == '\n' && last == '\r'){
                    if(bsb.length() > 0){
                        extractTagInfo(bsb.toString());
                    }else{
                        if(!firstNewLine){
                            state = FILE_CONTENT;
                            validParams = !tags.containsKey("filename");
                        }
                        firstNewLine = false;
                    }
                }else if(b == '-' && last == '-'){
                    cb.paramResult(PostDataResult.OK,result);
                    bsb.clear();
                    break;
                }
                if(b != '\n' && b != '\r'){
                    bsb.appendByte(b);
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
                        if(validParams){
                            result.computeIfAbsent(tags.get("name")
                                    ,k->new ArrayList<>()).add(bsb.toString());
                        }
                        tags.clear();
                    }
                }else{
                    if(validParams){
                        if(position>0){
                            for(int i = 0 ; i < position ; i++){
                                bsb.appendByte(marker[i]);
                            }
                            position =0;
                            if(b == marker[0]){
                                position++;
                                return;
                            }
                        }
                        bsb.appendByte(b);
                    }
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
            }
        }
    }
}
