# MultiThreadHttpServer
The multithread HTTP server is a lightweight HTTP server that uses a certain number of threads to increase the throughput. It creates a queue for the requests and processes them using a thread pool.  
  
# Quick start
Using this HttpServer is really easy. you should set number of threads and a socket address. Socket address also defines your interface and port. the following code listens to localhost on port 8080.  The server can listen to different socket addresses at the same time.

``` java
int threadCount = 4;
HttpServer server = new HttpServer(threadCount);
server.listen(new InetSocketAddress(8080));
```
Now, if you enter http://localhost:8080 in your browser, you should see the welcome page.

# Static files
The static file makes the contents of a folder accessible. This is useful when there is static content like HTML, CSS, image, etc. The flag allowBrowsePublicDir determines whether the index of the content can be accessed. The following code shows the index of the folder at http://localhost:8080/music .

``` java
server.getRouter().addStaticFile(new StaticFile("some path/music",true));

```

# Router
Router helps to intercept incoming request. the Simplest way to declare a routing Rule is inline Rule.

``` java
server.getRouter().addRoute("/to_here",HttpRequestMethod.GET, request -> {
    TextResponse res = new TextResponse(request, HttpResponseStatus.OK, MimeTypes.PLAIN_TEXT);
    res.writeMessage(request.getUri() + "\t" + request.getMethod() + "\n");
    res.writeMessage(request.getParams().toString() + "\n");
    res.writeMessage(request.getHeaders().toString());
    res.send();
    return res;
    });
```
  


And when the request contains some Post fields:
  
  ``` java
server.getRouter().addRoute("/post_req", HttpRequestMethod.POST, (request, params) -> {
    TextResponse res = new TextResponse(request, HttpResponseStatus.OK, MimeTypes.PLAIN_TEXT);
    res.writeMessage("get post data: " + params);
    res.send();
});
```

  
- It's also possible to get Arguments from uri using ":"

``` java
server.getRouter().addRoute("/article/:id", HttpRequestMethod.GET, request -> {
    TextResponse res = new TextResponse(request, HttpResponseStatus.OK, MimeTypes.PLAIN_TEXT);
    res.writeMessage("get Argument in uri using \":\"\n");
    res.writeMessage("article " + request.getParams().get(":id"));
    res.send();
    return res;
});
```
  
  \
The second way to intercept requests is to use a routing object. This method is clearer and is recommended. This method is used to encapsulate similar requests.

``` java
public class RouteTest{

    @RequestMapping(path = "/simple")
    public void simple(HttpRequest req){
        // will map to http://localhost:8080/base/simple
        TextResponse res = new TextResponse(req, HttpResponseStatus.OK, MimeTypes.PLAIN_TEXT);
        res.writeMessage("simple 1");
        res.send();
    }

    @RequestMapping(path = "simple_post" , method = HttpRequestMethod.POST)
    public void simplePost(HttpRequest req,@PostParam Map<String,List<String>> str){
        TextResponse res = new TextResponse(req, HttpResponseStatus.OK, MimeTypes.PLAIN_TEXT);
        res.writeMessage("post args : " + str);
        res.send();
    }
}
```

- And bind this class to server:

``` java
server.getRouter().addRoute("/base",new RouteTest());
```
  
     
# Redirect rules
Redirect rules allow you to seamlessly direct traffic from one location to another.

  
``` java
server.getRouter().addRedirectRule("/from_here","/to_here");
```
   

# Report Page
Report page helps to see every valid addresses in the Router. It helps to debug the routing rules.
 By activating this option, the http://localhost:8080/report will show every registered address.
   
``` java
server.getRouter().setShowReportState(true);
``` 
   
# Upload files
coming soon

# Websocket
under construction

# cookie
under construction