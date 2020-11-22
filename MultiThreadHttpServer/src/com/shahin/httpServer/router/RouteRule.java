package com.shahin.httpServer.router;

import com.shahin.httpServer.http.HttpRequest;
import com.shahin.httpServer.http.HttpRequestMethod;

import java.util.HashMap;
import java.util.Map;

public abstract class RouteRule {

    protected final String[] pattern;
    protected final String staticPath;
    protected final HttpRequestMethod httpMethod;

    public RouteRule(HttpRequestMethod httpMethod,String path){
        this.httpMethod = httpMethod;
        String[] s = separateStaticPath(path);
        this.staticPath = s[0];
        this.pattern = split(s[1]);
    }

    public abstract boolean checkIfMatch(HttpRequest request);

    protected Map<String,String> compareUri(String uri){
        Map<String,String> params = new HashMap<>();
        if(uri.isEmpty()){
            return params;
        }
        String[] uris = split(uri);
        for(int i = 0 ; i < pattern.length && i < uris.length ; i++){
            if(pattern[i].equals("*")){
                return params;        //match
            }
            if(pattern[i].startsWith(":")){
                params.put(pattern[i],uris[i]);
            }else if(!pattern[i].equalsIgnoreCase(uris[i])){
                return null;
            }
            if(i == uris.length-1){
                return params;
            }
        }
        return null;
    }

    protected String[] split(String uri){
        if(uri == null) return null;
        if(uri.startsWith("/")){
            uri = uri.substring(1);
        }
        if(uri.endsWith("/")){
            uri =uri.substring(0,uri.length()-1);
        }
        return uri.split("/");
    }

    private String[] separateStaticPath(String path){
        String[] parts = split(path);
        StringBuilder staticPath = new StringBuilder();
        StringBuilder argPath = new StringBuilder();
        boolean findArg = false;
        staticPath.append("/");
        for(String str : parts){
            if(findArg){
                argPath.append(str).append("/");
            }else{
                if(str.startsWith(":")){
                    argPath.append(str).append("/");
                    findArg = true;
                }else{
                    staticPath.append(str).append("/");
                }
            }
        }
        if(staticPath.length() > 0){        // remove last /
            staticPath.deleteCharAt(staticPath.length()-1);
        }
        if(argPath.length() > 0){
            argPath.deleteCharAt(argPath.length()-1);
        }
        return new String[]{staticPath.toString(), argPath.toString()};
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(staticPath);
        for (String s : pattern) {
            if (!s.isBlank()) {
                sb.append("/");
                sb.append(s);
            }
        }
        return sb.toString();
    }
}
