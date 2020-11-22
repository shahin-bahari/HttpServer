package com.shahin.httpServer.router;

import com.shahin.httpServer.http.HttpRequest;
import com.shahin.httpServer.http.HttpRequestMethod;
import com.shahin.httpServer.http.PostDataParser;
import com.shahin.httpServer.http.PostDataResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InlineRouteRule extends RouteRule {

    private final HttpResolver resolver;
    private final HttpResolverWithPost resolverWithPost;

    public InlineRouteRule(HttpRequestMethod httpMethod, String path,HttpResolver resolver) {
        super(httpMethod, path);
        this.resolver = resolver;
        this.resolverWithPost = null;
    }

    public InlineRouteRule(HttpRequestMethod httpMethod, String path,HttpResolverWithPost resolver) {
        super(httpMethod, path);
        this.resolver = null;
        this.resolverWithPost = resolver;
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
        if(resolver != null){
            resolver.resolve(request);
        }
        if(resolverWithPost != null){
            new PostDataParser(request).getParams((res, result) -> {
                if(res == PostDataResult.OK){
                    resolverWithPost.resolve(request,result);
                }
            });
        }
        return true;
    }
}
