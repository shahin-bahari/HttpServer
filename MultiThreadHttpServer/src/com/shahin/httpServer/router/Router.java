package com.shahin.httpServer.router;

import com.shahin.httpServer.http.HttpRequest;
import com.shahin.httpServer.response.HttpResponse;
import com.shahin.httpServer.response.HttpResponseStatus;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Router {

    private List<StaticFile> staticFiles = new CopyOnWriteArrayList<>();
    private final Map<HttpResponseStatus, HttpResolver> responseTemplate = new ConcurrentHashMap<>();

    public void addStaticFile(StaticFile staticFile){
        staticFiles.add(staticFile);
    }

    public HttpResolver getServerResponse(HttpResponseStatus status){
        HttpResolver res = responseTemplate.get(status);
        if(res != null) return res;
        return request -> new ErrorPage(status).getResponse(request);
    }

    public void doRoute(HttpRequest request){
        String uri = request.getUri();

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

        getServerResponse(HttpResponseStatus.NOT_FOUND).resolve(request).send();
    }
}
