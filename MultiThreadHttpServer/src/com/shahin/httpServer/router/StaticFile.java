package com.shahin.httpServer.router;

import com.shahin.httpServer.http.HttpRequest;
import com.shahin.httpServer.logger.Log;
import com.shahin.httpServer.response.FileResponse;
import com.shahin.httpServer.response.HttpResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class StaticFile {

    private final String publicDir;
    private boolean allowBrowsePublicDir;
    private final String rootName;
    private final ConcurrentHashMap<String,String> staticHeaders = new ConcurrentHashMap<>();

    public StaticFile(String publicDir,boolean allowBrowsePublicDir){
        this.allowBrowsePublicDir = allowBrowsePublicDir;
        this.publicDir = publicDir;
        File file = new File(publicDir);
        if(file.isDirectory()){
            rootName = file.getName();
        }else{
            rootName = null;
            Log.log(Level.WARNING,"Static file",publicDir + " is not a directory!");
        }
    }

    public void setAllowBrowsePublicDir(boolean allowBrowsePublicDir) {
        this.allowBrowsePublicDir = allowBrowsePublicDir;
    }

    public void addHeader(String key,String value){
        staticHeaders.put(key,value);
    }

    public void removeAllHeader(){
        staticHeaders.clear();
    }

    public void removeHeader(String key){
        staticHeaders.remove(key);
    }

    public String getPublicDir() {
        return publicDir;
    }

    public boolean isAllowBrowsePublicDir() {
        return allowBrowsePublicDir;
    }

    public HttpResponse checkPublicDir(HttpRequest request){
        if(request.getMethod().equalsIgnoreCase("GET")){
            String uri = request.getUri();
            if(uri == null || uri.equals("/") || rootName == null){
                return null;
            }
            File file;
            if(uri.equals("/"+rootName)){
                file = new File(publicDir);
            }else{
                file = new File(publicDir + uri);
            }
            if(file.exists()){
                try {
                    if(file.isFile()){
                        return prepareFileResponse(request,file);
                    }else{
                        return new DirectoryContentResponse(rootName,file).getResponse(request);
                    }
                }catch (IOException e){
                    Log.log(Level.SEVERE,"Static File : " + publicDir ,e.getMessage());
                }
            }
        }
        return null;
    }

    private HttpResponse prepareFileResponse(HttpRequest request, File file) throws IOException {
        FileResponse response = new FileResponse(request, Paths.get(file.getPath()));
        for(String key : staticHeaders.keySet()){
            response.addHeader(key,staticHeaders.get(key));
        }
        response.send();
        return response;
    }
}
