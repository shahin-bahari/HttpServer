package com.shahin.httpServer.router;

import com.shahin.httpServer.http.HttpRequest;
import com.shahin.httpServer.http.HttpRequestMethod;
import com.shahin.httpServer.http.PostDataParser;
import com.shahin.httpServer.http.PostParam;
import com.shahin.httpServer.logger.Log;
import com.shahin.httpServer.webSocket.WebSocket;

import java.lang.reflect.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

public class Routing {
    private final Object object;
    private final List<RouteRule> rules = new CopyOnWriteArrayList<>();
    private String base = "";

    public Routing(String path, HttpRequestMethod method,HttpResolver resolver){
        rules.add(new InlineRouteRule(method,path,resolver));
        object = null;
    }

    public Routing(String path, HttpRequestMethod method, HttpResolverWithPost resolver){
        rules.add(new InlineRouteRule(method,path,resolver));
        object = null;
    }

    public Routing(String basePath,Object obj){
        object = obj;
        base = basePath;
        extractRouteFunction(base,obj);
    }

    public Routing(String basePath,WebSocket ws){
        rules.add(new WebSocketRule(basePath,ws));
        object = null;
    }

    public boolean invokeIfMatch(HttpRequest request){
        String uri = request.getUri();
        if(object != null && uri != null && !uri.startsWith(base)){
            return false;
        }
        for (RouteRule rule : rules) {
            if (rule.checkIfMatch(request)) {
                if(object != null && rule instanceof FunctionRouteRule){
                    invoke(((FunctionRouteRule)(rule)).getMethod(),request);
                }
                return true;
            }
        }
        return false;
    }

    private void extractRouteFunction(String base,Object obj){
        for(Method method : obj.getClass().getMethods()){
            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
            if(mapping != null){
                if(method.getModifiers() != Modifier.PUBLIC){
                    Log.log(Level.WARNING,"Router",method.getName() +" function has to be public");
                    continue;
                }
                for(Parameter param : method.getParameters()){
                    if(param.getParameterizedType().equals(HttpRequest.class)){
                        rules.add(new FunctionRouteRule(mapping.method(),base +mapping.path(),method));
                    }
                }
            }
        }
    }

    private void invoke(Method method, HttpRequest req){
        Parameter[] params = method.getParameters();
        boolean needArgs = false;
        for (Parameter param : params) {
            PostParam post = param.getAnnotation(PostParam.class);
            if (post != null) {
                needArgs = true;
                break;
            }
        }
        if(needArgs){
            invokeWithPost(params,method,req);
        }else{
            invokeWithNoParam(params,method,req);
        }
    }

    private void invokeWithNoParam(Parameter[] params,Method method,HttpRequest req){
        Object[] args = new Object[params.length];
        for(int i = 0; i < params.length;i++) {
            Object currentObj = null;
            Parameter p = params[i];
            if (p.getType().equals(HttpRequest.class)) {
                currentObj = req;
            }
            args[i] = currentObj;
        }
        try {
            method.invoke(object,args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void invokeWithPost(Parameter[] params,Method method,HttpRequest req){
        new PostDataParser(req).getParams((res, result) -> {
            Object[] args = new Object[params.length];
            for(int i = 0; i < params.length;i++) {
                Object currentObj = null;
                Parameter p = params[i];
                if (p.getType().equals(HttpRequest.class)) {
                    currentObj = req;
                } else if (p.getType().equals(PostDataParser.PostResult.class)) {
                    currentObj = res;
                } else if (p.getType().equals(Map.class)) {
                    if (p.getParameterizedType() instanceof ParameterizedType) {
                        Type[] types = ((ParameterizedType) p.getParameterizedType()).getActualTypeArguments();
                        if (types.length == 2 && types[0].getTypeName().equals(String.class.getName())
                                && types[1].getTypeName().equals("java.util.List<java.lang.String>")) {
                            currentObj = result;
                        }
                    }
                }
                args[i] = currentObj;
            }
            try {
                method.invoke(object,args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        });
    }

    public String[] getRoutingPath(){
        String[] res = new String[rules.size()];
        for(int i = 0 ; i < rules.size() ; i++){
            res[i] = rules.get(i).toString();
        }
        return res;
    }

    public WebSocket getWebSocket(){
        if(!rules.isEmpty() && rules.get(0) instanceof WebSocketRule){
            return ((WebSocketRule)rules.get(0)).getWebSocket();
        }
        return null;
    }
}
