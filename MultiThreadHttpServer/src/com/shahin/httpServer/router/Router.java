package com.shahin.httpServer.router;

import com.shahin.httpServer.http.HttpRequest;
import com.shahin.httpServer.http.HttpRequestMethod;
import com.shahin.httpServer.response.HttpResponse;
import com.shahin.httpServer.response.HttpResponseStatus;
import com.shahin.httpServer.webSocket.WebSocket;

import java.io.File;
import java.util.Formatter;
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

    public void addRoute(String base,WebSocket ws){
        routings.add(new Routing(base,ws));
    }

    public void setShowReportState(boolean state){
        showReport = state;
    }

    public WebSocket doRoute(HttpRequest request){
        String uri = request.getUri();

        if(redirectRules.containsKey(uri)){
            HttpResponse res = HttpResponse.quickResponse(request.getConnection(),HttpResponseStatus.MOVED_PERMANENTLY);
            res.addHeader("Location",redirectRules.get(uri));
            res.addHeader("Content-Length","0");
            res.send();
            return null;
        }

        for(Routing routing : routings){
            if(routing.invokeIfMatch(request)){
                if (isWebSocket(request)) {
                    return routing.getWebSocket();
                }
                return null;
            }
        }

        for(StaticFile st : staticFiles){
            HttpResponse res = st.checkPublicDir(request);
            if(res != null){
                return null;
            }
        }

        if(uri.equals("/")){
            new WelcomePage().getResponse(request).send();
            return null;
        }

        if(showReport && uri.equals("/report")){
            new ReportPage(staticFiles,routings,redirectRules)
                    .getResponse(request).send();
            return null;
        }

        getServerDefaultResponse(HttpResponseStatus.NOT_FOUND).resolve(request).send();
        return null;
    }

    public String report(){
        StringBuilder sb = new StringBuilder();
        sb.append(new Formatter().format("%-50s | %-30s\n",
                "URL","redirect to"));
        sb.append("-".repeat(51)).append("|").append("-".repeat(31)).append("\n");
        for(Routing routing : routings){
            for(String path : routing.getRoutingPath()){
                sb.append(new Formatter().format("%-50s | %-30s\n",
                        path,redirectRules.getOrDefault(path, "-")));
            }
        }

        for(StaticFile p : staticFiles){
            String path = p.getPublicDir();
            String name = new File(path).getName();
            sb.append(new Formatter().format("/%-50s | %-30s\n",
                    name,path));
        }
        return sb.toString();
    }

    private boolean isWebSocket(HttpRequest request){
        if(!request.getMethod().equalsIgnoreCase("GET")){
            return false;
        }
        Map<String,String> headers = request.getHeaders();
        String upgrade = headers.get(WebSocket.WEB_SOCKET_UPGRADE_TAG);
        if(!WebSocket.WEB_SOCKET_UPGRADE_VALUE.equalsIgnoreCase(upgrade)){
            return false;
        }
        String connection = headers.get(WebSocket.WEB_SOCKET_CONNECTION_TAG);
        if(connection == null || !connection.contains(WebSocket.WEB_SOCKET_CONNECTION_VALUE)){
            return false;
        }
        String version = headers.get(WebSocket.WEB_SOCKET_VERSION_TAG);
        if(!WebSocket.WEB_SOCKET_VERSION_VALUE.equals(version)){
            return false;
        }
        return headers.containsKey(WebSocket.WEB_SOCKET_KEY_TAG);
    }
}
