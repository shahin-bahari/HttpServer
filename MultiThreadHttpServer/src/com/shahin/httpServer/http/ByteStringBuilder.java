package com.shahin.httpServer.http;

public class ByteStringBuilder {

    private final StringBuilder sb = new StringBuilder(64);
    private int oldValue;
    private int remainChar;

    public void appendByte(byte b){
        int value = b & 0xFF;
        if(value <= 0x7F){           // 0xxx xxxx   single byte char
            sb.append((char)b);
        }else if(value <= 0xBF){     // 10xx xxxx    byte 2/3/4
            oldValue = (oldValue << 6) + (value & 0x3F);
            if(--remainChar == 0){
                sb.appendCodePoint(oldValue);
            }
        }else if(value <= 0xDF){     // 110x xxxx    2 byte char, byte 1
            oldValue = value & 0x1F;
            remainChar = 1;
        }else if(value <= 0xEF){     // 1110 xxxx    3 byte char, byte 1
            oldValue = value & 0x0F;
            remainChar = 2;
        }else if(value <= 0xF7){     // 1111 0xxx    4 byte char, byte 1
            oldValue = value & 0x07;
            remainChar = 3;
        }
    }

    @Override
    public String toString(){
        String str = sb.toString();
        sb.setLength(0);
        oldValue = 0;
        remainChar = 0;
        return str;
    }

    public void clear(){
        sb.setLength(0);
        oldValue = 0;
        remainChar = 0;
    }

    public int length(){
        return sb.length();
    }

}
