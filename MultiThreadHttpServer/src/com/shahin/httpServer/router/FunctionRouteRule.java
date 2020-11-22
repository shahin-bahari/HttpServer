package com.shahin.httpServer.router;

import com.shahin.httpServer.http.HttpRequest;
import com.shahin.httpServer.http.HttpRequestMethod;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FunctionRouteRule extends RouteRule {

    private final Method method;

    public FunctionRouteRule(HttpRequestMethod httpMethod, String path, Method method) {
        super(httpMethod, path);
        this.method = method;
    }

    @Override
    public boolean checkIfMatch(HttpRequest request) {
        String uri = request.getUri();
        if(httpMethod != HttpRequestMethod.lookup(request.getMethod()) ||
                uri == null){
            return false;
        }
        if(!uri.equals("/") && uri.endsWith("/")){
            uri = uri.substring(0,uri.length()-1);
        }
        if(!uri.startsWith(staticPath)){
            return false;
        }
        uri = uri.substring(staticPath.length());
        Map<String,String> params = compareUri(uri);
        if(params == null){
            return false;
        }
        for(String key : params.keySet()){
            List<String> vals = request.getParams().computeIfAbsent(key, k -> new ArrayList<>());
            vals.add(params.get(key));
        }
        return true;
    }

    public Method getMethod() {
        return method;
    }
}
