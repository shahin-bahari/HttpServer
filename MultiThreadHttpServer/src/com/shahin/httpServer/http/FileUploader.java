package com.shahin.httpServer.http;

import com.shahin.httpServer.utils.BufferCache;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileUploader {

    private final Map<String,UploadManifest> manifest;
    private final ByteMarkerMatcher matcher;
    private final PostDataParser.PostResult cb;
    private boolean error = false;

    public FileUploader(String boundary,
                        Map<String,UploadManifest> manifest,
                        PostDataParser.PostResult cb) {
        this.manifest = manifest;
        this.cb = cb;
        matcher = new ByteMarkerMatcher(boundary,cbMatcher);
    }

    public int checkBuffer(ByteBuffer buffer){
        int len = buffer.remaining();
        while(buffer.remaining() != 0 && !error){
            matcher.check(buffer.get());
        }
        return error?-1:len;
    }


    private final ByteMarkerMatcher.MarkerMatcherCallback cbMatcher = new ByteMarkerMatcher.MarkerMatcherCallback() {

        private FileChannel file;
        private Map<String, List<String>> result = new HashMap<>();
        private long totalSize = 0;
        private long allowedSize;
        private final ByteStringBuilder bsb = new ByteStringBuilder();
        private String pName = null;

        @Override
        public void startTag(Map<String, String> tags) {
            initFile(tags);
        }

        @Override
        public void data(ByteBuffer buffer) {
            if(file != null){
                if(totalSize > 200000){
                    int a =3;
                }
                totalSize += buffer.remaining();
                try {
                    file.write(buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                    setError(PostDataResult.IO_ERROR);
                }
                if(totalSize > allowedSize){
                    setError(PostDataResult.LARGER_THAN_MAX);
                }
            }else{  // params
                if(bsb.length() > 4*1024){
                    setError(PostDataResult.LARGER_THAN_MAX);
                    return;
                }
                while (buffer.remaining() != 0){    //to avoid break utf-8 between 2 packet
                    bsb.appendByte(buffer.get());
                }
                BufferCache.recycleBuffer(buffer);
            }

        }

        @Override
        public void endTag() {
            if(file == null){
                result.computeIfAbsent(pName,k->new ArrayList<>()).add(bsb.toString());
            }
            if(cb != null){
                cb.paramResult(PostDataResult.OK,result);
            }
        }

        @Override
        public void error(String err) {
            setError(PostDataResult.BAD_FRAME);
        }

        private void initFile(Map<String,String> tags){
            if(file != null){
                try {
                    file.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else if(pName != null){
                result.computeIfAbsent(pName,k->new ArrayList<>()).add(bsb.toString());
                pName = null;
                file = null;
            }
            String name = tags.get("name");
            String fileName = tags.get("filename");
            String mime = tags.get("Content-Type");
            if(name == null) {
                setError(PostDataResult.BAD_FRAME);
            }else if(fileName == null) {
                file = null;
                pName = name;
            }else if(fileName.isBlank()){
                setError(PostDataResult.BAD_FRAME);
            }else{
                UploadManifest um = manifest.get(name);
                if(um != null){
                    if(um.acceptMoreFile()){
                        if(um.checkMimeType(mime)){
                            Path p = um.getFile(fileName);
                            allowedSize = um.getMaxItemSize();
                            result.computeIfAbsent(name,k->new ArrayList<>()).add(p.toString());
                            try {
                                file = FileChannel.open(p,
                                        StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
                            } catch (IOException e) {
                                e.printStackTrace();
                                setError(PostDataResult.IO_ERROR);
                            }
                        }else{
                            setError(PostDataResult.MIME_TYPE_MISMATCH);
                        }
                    }else{
                        setError(PostDataResult.TOO_MANY_FILE);
                    }
                }else{
                    setError(PostDataResult.UNEXPECTED_NAME);
                }
            }
        }

        private void setError(PostDataResult err){
            error = true;
            removeFiles();
            if(cb != null){
                cb.paramResult(err,null);
            }
        }

        private void removeFiles(){
            for(String n:result.keySet()){
                for(String path : result.get(n)){
                    new File(path).delete();
                }
            }
        }
    };
}
