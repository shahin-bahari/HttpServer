package com.shahin;


import com.shahin.httpServer.HttpServer;
import com.shahin.httpServer.connection.Connection;
import com.shahin.httpServer.http.*;
import com.shahin.httpServer.response.HttpResponseStatus;
import com.shahin.httpServer.response.TextResponse;
import com.shahin.httpServer.router.RequestMapping;
import com.shahin.httpServer.router.StaticFile;
import com.shahin.httpServer.utils.MimeTypes;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;

public class Main {

    public static void main(String[] args) {

        int threadCount = 2;
        HttpServer server = new HttpServer(threadCount);

        server.getRouter().addStaticFile(new StaticFile("E://music",true));
        server.getRouter().setShowReportState(true);

        server.getRouter().addRedirectRule("/from_here","/to_here");

        server.getRouter().addRoute("/to_here",HttpRequestMethod.GET, request -> {
            TextResponse res = new TextResponse(request, HttpResponseStatus.OK, MimeTypes.PLAIN_TEXT);
            res.writeMessage(request.getUri() + "\t" + request.getMethod() + "\n");
            res.writeMessage(request.getParams().toString() + "\n");
            res.writeMessage(request.getHeaders().toString());
            res.send();
            return res;
        });

        server.getRouter().addRoute("/article/:id", HttpRequestMethod.GET, request -> {
            TextResponse res = new TextResponse(request, HttpResponseStatus.OK, MimeTypes.PLAIN_TEXT);
            res.writeMessage("get Argument in uri using \":\"\n");
            res.writeMessage("article " + request.getParams().get(":id"));
            res.send();
            return res;
        });

        server.getRouter().addRoute("/upload", HttpRequestMethod.POST, (request, params) -> {
            TextResponse res = new TextResponse(request, HttpResponseStatus.OK, MimeTypes.PLAIN_TEXT);
            res.writeMessage("get post data: " + params);
            res.send();
        });

        server.getRouter().addRoute("/base",new RouteTest());

        // just localhost
        //server.listen(new InetSocketAddress(8080));

        try {
            for(NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())){
                if(!ni.isVirtual() && ni.isUp() ){
                    System.out.println(ni.toString());
                    for(InetAddress ia : Collections.list(ni.getInetAddresses())){
                        if(ia.getAddress().length == 4){
                            server.listen(new InetSocketAddress(ia,8080));
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        waitForCommand(server);
        server.stopServer();

    }

    private static void waitForCommand(HttpServer server){
        while(true){
            String t = new Scanner(System.in).nextLine();
            if(t.equalsIgnoreCase("exit")){
                return;
            }else if(t.equals("report")){
                System.out.println(server.getRouter().report());
            }else if(t.equals("clients")){
                for(Connection conn : server.getClientList()){
                    System.out.println(conn.toString());
                }
            }
        }
    }


    ///////////////////////
    public static class RouteTest{

        @RequestMapping(path = "/simple")
        public void simple(HttpRequest req){
            TextResponse res = new TextResponse(req, HttpResponseStatus.OK, MimeTypes.PLAIN_TEXT);
            res.writeMessage("simple 1");
            res.send();
        }

        @RequestMapping(path = "/simple_post" , method = HttpRequestMethod.POST)
        public void simplePost(HttpRequest req,@PostParam Map<String,List<String>> str){
            TextResponse res = new TextResponse(req, HttpResponseStatus.OK, MimeTypes.PLAIN_TEXT);
            res.writeMessage("post args : " + str);
            res.send();
        }

        @RequestMapping(path = "/page1",method = HttpRequestMethod.GET)
        public void page1Func(HttpRequest request){
            TextResponse res = new TextResponse(request, HttpResponseStatus.OK, MimeTypes.PLAIN_TEXT);
            res.writeMessage("page 1");
            res.send();
        }

        @RequestMapping(path = "/file")
        public void file(HttpRequest req){
            TextResponse tr = new TextResponse(req,HttpResponseStatus.OK,MimeTypes.HTML);
            String msg = "<form action=\"/upload_file\" method=\"post\" enctype=\"multipart/form-data\">\n" +
                    "\t\tsign in:\n" +
                    "    <input type=\"text\" name=\"name\" id=\"name\">\n" +
                    "\t<input type=\"text\" name=\"last_name\" id=\"last_name\">\n" +
                    "\t<input type=\"file\" name=\"fileToUpload_name\" id=\"fileToUpload_id\" multiple>\n" +
                    "    <input type=\"submit\" value=\"Upload Image\" name=\"submit\">\n" +
                    "</form><br><br>" +"" +
                    "<form action=\"/simple_post\" method=\"POST\" enctype=\"application/x-www-form-urlencoded\">\n" +
                    "    <input type=\"text\" name=\"username\" value=\"sidthesloth\"/>\n" +
                    "    <input type=\"text\" name=\"password\" value=\"slothsecret\"/>\n" +
                    "    <input type=\"submit\" value=\"Submit\" />\n" +
                    "</form>\n";
            tr.writeMessage(msg);
            tr.send();
        }

        @RequestMapping(path = "upload_file", method = HttpRequestMethod.POST)
        public void uploadFile(HttpRequest req, @PostParam Map<String,List<String>> str){
            String des = "C:/Users/shahi/Desktop/upload";
            Map<String, UploadManifest> m = new HashMap<>();
            m.put("fileToUpload_name",new UploadManifest(des,2));
            TextResponse res2 = new TextResponse(req, HttpResponseStatus.OK, MimeTypes.PLAIN_TEXT);
            res2.writeMessage("upload + " +str);
            new PostDataParser(req).upload(m, (res, result) -> {
                System.out.println(res);
                if(result != null){
                    for(String msg : result.keySet()){
                        System.out.println(msg + " : " + result.get(msg));
                    }
                }
                res2.send();
            });
        }
    }
}
