package com.shahin.httpServer.templateEngine;

public class MarkerMatcher {

    private char[] marker;
    private StringBuilder resultBuffer;
    private int position;

    public void setMarker(StringBuilder resultBuffer,char[] marker){
        this.marker = marker;
        this.resultBuffer = resultBuffer;
        position = 0;
    }

    public boolean check(char ch){
        if(marker == null || marker.length <1) return false;
        if(ch == marker[position]){
            position++;
            if(position == marker.length){
                position =0;
                return true;
            }
        }else{
            if(position >0 && resultBuffer != null){
                resultBuffer.append(marker,0,position);
                position = 0;
            }
            if(resultBuffer != null) resultBuffer.append(ch);
            return false;
        }
        return false;
    }
}
