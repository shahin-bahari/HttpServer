package com.shahin.httpServer.http;

import com.shahin.httpServer.utils.ParserUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UrlEncodePostParams {

    private final PostDataParser.PostResult cb;
    private final ByteStringBuilder bsb = new ByteStringBuilder();
    private final Map<String, List<String>> result = new HashMap<>();
    private final long length;
    private long currentLength;
    private int state = 0;
    private String pName;

    public UrlEncodePostParams(long length, PostDataParser.PostResult cb) {
        this.cb = cb;
        this.length = length;
    }

    public int check(ByteBuffer buffer){
        int len = buffer.remaining();
        while(buffer.remaining() != 0){
            checkByte(buffer.get());
        }
        currentLength += len;
        if(currentLength == length){
            result.computeIfAbsent(pName,k->new ArrayList<>())
                    .add(ParserUtils.urlDecode(bsb.toString()));
            if(cb != null){
                cb.paramResult(PostDataResult.OK,result);
            }
        }
        return len;
    }


    private void checkByte(byte b){
        if(state == 0){
            if(b == '='){
                pName = ParserUtils.urlDecode(bsb.toString());
                state = 1;
            }else{
                bsb.appendByte(b);
            }
        }else if(state == 1){
            if(b == '&'){
                state = 0;
                result.computeIfAbsent(pName,k->new ArrayList<>())
                        .add(ParserUtils.urlDecode(bsb.toString()));
            }else{
                bsb.appendByte(b);
            }
        }
    }
}
