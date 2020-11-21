package com.shahin.httpServer.response;

import com.shahin.httpServer.http.HttpRequest;
import com.shahin.httpServer.utils.BufferCache;
import com.shahin.httpServer.utils.MimeTypes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileResponse extends HttpResponse {

    private final FileChannel fileChannel;
    private long sentSize = 0;
    private long startPos = 0;
    private String contentRange = "";
    private final long fileSize;

    public FileResponse(HttpRequest request, Path path) throws IOException {
        super(request);
        fileChannel = FileChannel.open(path, StandardOpenOption.READ);
        fileSize = fileChannel.size();
        this.length = fileSize;
        this.mimeType = MimeTypes.getMimeTypeFromPath(path.toString());
        this.status = HttpResponseStatus.OK;
        headers.put("Accept-Ranges", "bytes");
        headers.put("Content-Disposition","inline");
        String range = request.getHeaders().get("Range");
        if(range != null){      // partial
            if(parseRange(range.toLowerCase())){
                status = HttpResponseStatus.PARTIAL_CONTENT;
                headers.put("Content-Range","bytes " + contentRange +"/" + fileSize);
            }else{
                status = HttpResponseStatus.REQUEST_RANGE_NOT_SATISFIABLE;
                length =0;
                sentSize=0;
            }
        }
    }

    @Override
    public void send() {
        if(sentSize < length){
            ByteBuffer buffer = BufferCache.generateBuffer();
            int size = 0;
            try {
                size =  fileChannel.read(buffer,startPos);
            } catch (IOException e) {
                e.printStackTrace();
            }

            startPos += size;
            sentSize += size;
            buffer.flip();
            sendData(buffer,this::send);
        }else{
            endResponse();
        }
    }

    private boolean parseRange(String range){
        if(range.startsWith("bytes=")){
            String vals = range.substring(6);
            String[] sub = vals.split("-");

            if(sub.length ==2){   //start-end
                int start = parseNumber(sub[0]);
                int end = parseNumber(sub[1]);
                if(start < 0 || end < 0){
                    return false;
                }
                length = end - start +1;
                startPos = start;
                contentRange = start +"-"+end;
                return true;
            }else if(sub.length ==1 && vals.startsWith("-")){    //-length
                int end = parseNumber(sub[0]);
                if(end < 0){
                    return false;
                }
                startPos = fileSize-end;
                contentRange = startPos +"-"+(fileSize-1);
                length = end;
                return true;
            }
            else if( sub.length ==1){ //start-
                int start = parseNumber(sub[0]);
                if(start < 0){
                    return false;
                }
                contentRange = start +"-"+(fileSize-1);
                length = fileSize-start;
                startPos = start;
                return true;
            }
        }
        return false;
    }

    private int parseNumber(String nr){
        try{
            return Integer.parseInt(nr);
        }catch (NumberFormatException e){
            return -1;
        }
    }
}
