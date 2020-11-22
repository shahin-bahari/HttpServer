package com.shahin.httpServer.http;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;

public class UploadManifest {

    public interface UploadFileNameSelector{
        String getName(String fileName,String ext);
    }

    private String[] mimeType = null;
    private final int maxNumber;
    private long maxItemSize = Long.MAX_VALUE;
    private final String destinationPath;
    private UploadFileNameSelector nameSelector;

    private int itemCount = 0;

    public UploadManifest(String destinationPath,int maxNumber) {
        this.destinationPath = destinationPath;
        this.maxNumber = maxNumber;
    }

    public UploadManifest setMimeType(String[] mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    public UploadManifest setMaxItemSize(long maxItemSize) {
        this.maxItemSize = maxItemSize;
        return this;
    }

    public long getMaxItemSize() {
        return maxItemSize;
    }

    public UploadManifest setNameSelector(UploadFileNameSelector nameSelector) {
        this.nameSelector = nameSelector;
        return this;
    }

    public boolean checkMimeType(String mime){
        if(mimeType == null){
            return true;
        }
        for(String m : mimeType){
            if(m.equalsIgnoreCase(mime)){
                return true;
            }
        }
        return false;
    }

    public boolean acceptMoreFile(){
        return itemCount < maxNumber;
    }

    private String getPath(String fileName,String ext){
        if(nameSelector == null){
            return destinationPath + File.separator +fileName + ext;
        }
        return destinationPath + File.separator +nameSelector.getName(fileName,ext);
    }

    public Path getFile(String fileName) {
        int point = fileName.indexOf(".");
        String name = fileName;
        String ext = "";
        if(point>0){
            name = fileName.substring(0,point);
            ext = fileName.substring(point);
        }
        itemCount++;
        long time = Instant.now().getNano();
        return findNewFileName(time + "_" +name,ext).toPath();
    }

    private File findNewFileName(String name, String ext){
        File file = new File(getPath(name,ext));
        int counter = 0;
        while(file.exists()){
            file = new File(getPath(name + "_" + counter,ext));
            counter++;
        }
        return file;
    }
}
