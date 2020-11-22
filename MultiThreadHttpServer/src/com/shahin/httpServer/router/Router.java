package com.shahin.httpServer.router;

import com.shahin.httpServer.http.HttpRequest;
import com.shahin.httpServer.http.HttpRequestMethod;
import com.shahin.httpServer.response.HttpResponse;
import com.shahin.httpServer.response.HttpResponseStatus;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Router {

    private List<StaticFile> staticFiles = new CopyOnWriteArrayList<>();
    private final Map<HttpResponseStatus, HttpResolver> responseTemplate = new ConcurrentHashMap<>();
    private final Map<String,String> redirectRules = new ConcurrentHashMap<>();
    private final List<Routing> routings = new CopyOnWriteArrayList<>();
    private boolean showReport = false;

    public void addStaticFile(StaticFile staticFile){
        staticFiles.add(staticFile);
    }

    public HttpResolver getServerDefaultResponse(HttpResponseStatus status){
        HttpResolver res = responseTemplate.get(status);
        if(res != null) return res;
        return request -> new ErrorPage(status).getResponse(request);
    }

    public void registerErrorTemplatePage(HttpResponseStatus status,HttpResolver resolver){
        responseTemplate.put(status,resolver);
    }

    public void addRedirectRule(String srcPath,String location){
        redirectRules.put(srcPath,location);
    }

    public void addRoute(String path, HttpRequestMethod method, HttpResolver resolver){
        routings.add(new Routing(path, method,resolver));
    }

    public void addRoute(String path, HttpRequestMethod method, HttpResolverWithPost resolver){
        routings.add(new Routing(path, method,resolver));
    }

    public void addRoute(String basePath,Object obj){
        routings.add(new Routing(basePath,obj));
    }

    public void setShowReportState(boolean state){
        showReport = state;
    }

    public void doRoute(HttpRequest request){
        String uri = request.getUri();

        if(redirectRules.containsKey(uri)){
            HttpResponse res = HttpResponse.quickResponse(request.getConnection(),HttpResponseStatus.MOVED_PERMANENTLY);
            res.addHeader("Location",redirectRules.get(uri));
            res.addHeader("Content-Length","0");
            res.send();
            return;
        }

        for(Routing routing : routings){
            if(routing.invokeIfMatch(request)){
                /*if (isWebSocket(request)) {
                    return routing.getWebSocket();
                }*/
                return;
            }
        }

        for(StaticFile st : staticFiles){
            HttpResponse res = st.checkPublicDir(request);
            if(res != null){
                return;
            }
        }

        if(uri.equals("/")){
            new WelcomePage().getResponse(request).send();
            return;
        }

        if(showReport && uri.equals("/report")){
            new ReportPage(staticFiles,routings,redirectRules)
                    .getResponse(request).send();
            return;
        }

        getServerDefaultResponse(HttpResponseStatus.NOT_FOUND).resolve(request).send();
    }
}
