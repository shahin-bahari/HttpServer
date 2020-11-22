package com.shahin.httpServer.http;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public class PostDataParser {

    public interface PostResult {
        void paramResult(PostDataResult res, Map<String, List<String>> result);
    }

    private final HttpRequest request;
    private long currentLength;
    private int maxParamsLength = 4 * 1024;

    public PostDataParser(HttpRequest request) {
        this.request = request;
    }

    public PostDataParser setMaxParamsLength(int maxParamsLength) {
        this.maxParamsLength = maxParamsLength;
        return this;
    }

    public void getParams(PostResult cb){
        String boundary = getBoundary();
        long length = getContentLength();
        if(length < 0){
            if(cb != null){
                cb.paramResult(PostDataResult.UNKNOWN_CONTENT_LENGTH,null);
            }
            return;
        }
        if(length > maxParamsLength){
            if(cb != null){
                cb.paramResult(PostDataResult.LARGER_THAN_MAX,null);
            }
            return;
        }
        ByteBuffer head = request.getFirstDataBlock();
        if(boundary == null){
            if(checkIfUrlEncode()){
                UrlEncodePostParams upp = new UrlEncodePostParams(length, cb);
                currentLength += upp.check(head);
                readMoreUrlEncodeParams(upp,length);
            }
        }else{
            MultipartPostParams mpp = new MultipartPostParams(boundary, cb);
            currentLength += mpp.check(head);
            readMoreMultipartParams(mpp,length);
        }
    }

    private void readMoreMultipartParams(MultipartPostParams mpp,long length){
        if(currentLength < length){
            request.getConnection().readDataFromSocket(
                    (buffer) -> {
                        currentLength+=mpp.check(buffer);
                        readMoreMultipartParams(mpp,length);
                    });
        }
    }

    private void readMoreUrlEncodeParams(UrlEncodePostParams upp,long length){
        if(currentLength < length){
            request.getConnection().readDataFromSocket(
                    (buffer) -> {
                        currentLength+=upp.check(buffer);
                        readMoreUrlEncodeParams(upp,length);
                    });
        }
    }

    private long getContentLength() {
        String l = request.getHeaders().get("Content-Length");
        if(l != null){
            try {
                return Long.parseLong(l);
            }catch (NumberFormatException ignored){
            }
        }
        return -1;
    }

    public void upload(Map<String,UploadManifest> manifest, PostResult cb){
        String boundary = getBoundary();
        if(boundary == null){
            if(cb != null){
                cb.paramResult(PostDataResult.BAD_FRAME,null); //"No boundary found"
            }
            return;
        }
        long length = getContentLength();
        if(length < 0){
            if(cb != null){
                cb.paramResult(PostDataResult.UNKNOWN_CONTENT_LENGTH,null);
            }
            return;
        }
        ByteBuffer head = request.getFirstDataBlock();
        FileUploader uploader = new FileUploader(boundary,manifest,cb);
        int res = uploader.checkBuffer(head);
        if(res >0){
            currentLength = res;
            readMoreFiles(uploader,length);
        }
    }

    private void readMoreFiles(FileUploader fu,long length){
        if(currentLength < length){
            request.getConnection().readDataFromSocket(
                    (buffer) -> {
                        int res = fu.checkBuffer(buffer);
                        if(res > 0){
                            readMoreFiles(fu,length);
                            currentLength += res;
                        }
                    });
        }
    }

    private String getBoundary(){
        String ct = request.getHeaders().get("Content-Type");
        if(ct == null || !ct.contains("boundary")){
            return null;
        }
        int pos = ct.toLowerCase().indexOf("boundary=");
        if(pos < 0 ){
            return null;
        }
        return ct.substring(pos + 9);
    }

    private boolean checkIfUrlEncode(){
        String ct = request.getHeaders().get("Content-Type");
        if(ct == null){
            return false;
        }
        return ct.contains("application/x-www-form-urlencoded");
    }
}
