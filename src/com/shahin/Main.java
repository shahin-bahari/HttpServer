package com.shahin;


import com.shahin.httpServer.HttpServer;
import com.shahin.httpServer.http.*;
import com.shahin.httpServer.response.HttpResponse;
import com.shahin.httpServer.response.HttpResponseStatus;
import com.shahin.httpServer.response.TextResponse;
import com.shahin.httpServer.router.HttpResolver;
import com.shahin.httpServer.router.HttpResolverWithPost;
import com.shahin.httpServer.router.RequestMapping;
import com.shahin.httpServer.router.StaticFile;
import com.shahin.httpServer.utils.MimeTypes;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.*;

public class Main {

    public static void main(String[] args) {
	// write your code here

        HttpServer server = new HttpServer(2);
        server.getRouter().addStaticFile(new StaticFile("E://music",true));
        server.getRouter().setShowReportState(true);
        server.getRouter().addRedirectRule("/from_here","/to_here");

        server.getRouter().addRoute("/to_here",HttpRequestMethod.GET, resolver -> {
            TextResponse res = new TextResponse(resolver, HttpResponseStatus.OK, MimeTypes.PLAIN_TEXT);
            res.writeMessage("redirected here");
            res.send();
            return res;
        });
        server.getRouter().addRoute("/book/:id", HttpRequestMethod.GET, request -> {
            TextResponse res = new TextResponse(request, HttpResponseStatus.OK, MimeTypes.PLAIN_TEXT);
            res.writeMessage("book " + request.getParams().get(":id"));
            res.send();
            return res;
        });

        server.getRouter().addRoute("/upload", HttpRequestMethod.POST, (request, params) -> {
            TextResponse res = new TextResponse(request, HttpResponseStatus.OK, MimeTypes.PLAIN_TEXT);
            res.writeMessage("done ");
            res.send();
        });

        server.getRouter().addRoute("",new RouteTest());

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

        waitForCommand();
        server.stopServer();

    }

    private static void waitForCommand(){
        while(true){
            String t = new Scanner(System.in).nextLine();
            if(t.equalsIgnoreCase("gc")){
                System.gc();
                System.out.println("gc");
            }else if(t.equalsIgnoreCase("info")){
                Runtime runtime = Runtime.getRuntime();
                long maxMemory = runtime.maxMemory();
                long allocatedMemory = runtime.totalMemory();
                long freeMemory = runtime.freeMemory();
                System.out.println("max memory is " + humanReadableByteCountBin(maxMemory));
                System.out.println("allocated memory is " + humanReadableByteCountBin(allocatedMemory));
                System.out.println("free memory is " + humanReadableByteCountBin(freeMemory));
            }else if(t.equalsIgnoreCase("exit")){
                return;
            }else if(t.equals("report")){
                //System.out.println(server.getRouter().report());
            }else if(t.equals("clients")){
                /*for(SocketChannel sc : server.getClientList()){
                    System.out.println(sc.toString());
                }*/
            }
        }
    }

    private static String humanReadableByteCountBin(long size){
        final String[] units = new String[] { "B", "kB", "MB", "GB", "TB", "PB", "EB" };
        final int exp = (int) (Math.log(size)/Math.log(1024));
        return String.format("%.2f %s", size / Math.pow(1024,exp), units[exp]);
    }

    ///////////////////////
    public static class RouteTest{

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
                    "<form action=\"/upload\" method=\"POST\" enctype=\"application/x-www-form-urlencoded\">\n" +
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
