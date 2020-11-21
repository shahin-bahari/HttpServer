package com.shahin.httpServer.router;

import com.shahin.httpServer.http.HttpRequest;
import com.shahin.httpServer.response.HttpResponse;
import com.shahin.httpServer.response.HttpResponseStatus;
import com.shahin.httpServer.templateEngine.TemplateEngine;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;

public class DirectoryContentResponse {

    private final File file;
    private final String rootName;
    private final String template = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "\t<meta charset=\"utf-8\">\n" +
            "\t<title><coffee title/></title>\n" +
            "\t<style>\n" +
            "\t\t*{\n" +
            "\t\t\tmargin : 0;\n" +
            "\t\t\tpadding: 0;\n" +
            "\t\t\tfont-family:sans-serif;\n" +
            "\t\t\ttext-align: left;\n" +
            "\t\t}\n" +
            "\t\t.header_bar{\n" +
            "\t\t\tbackground-color : #4A148C;\n" +
            "\t\t\tpadding: 15px;\n" +
            "\t\t}\n" +
            "\t\t.header_bar span{\n" +
            "\t\t\tbackground-color : #0000;\n" +
            "\t\t\tcolor : #eee;\n" +
            "\t\t\tmargin : 5px;\n" +
            "\t\t}\n" +
            "\t\t.header_bar a{\n" +
            "\t\t\tcolor:#eee;\n" +
            "\t\t}\n" +
            "\t\t.header_bar a:hover{\n" +
            "\t\t\tcolor:#aaa;\n" +
            "\t\t}\n" +
            "\t\ttable{\n" +
            "\t\t\tborder-collapse: collapse;\n" +
            "\t\t\twidth: 100%;\n" +
            "\t\t}\n" +
            "\t\tthead {\n" +
            "\t\t\tcolor :#616161;\n" +
            "\t\t\tfont-size : 0.7rem;\n" +
            "\t\t}\n" +
            "\t\tth, td {\n" +
            "\t\t\tpadding:0.6rem;\n" +
            "\t\t\tborder: 0;\n" +
            "\t\t\tborder-bottom: 0.5px solid #ddd;\n" +
            "\t\t}\n" +
            "\t\ttd{\n" +
            "\t\t\tpadding:0.3rem;\n" +
            "\t\t\tfont-size:0.9rem;\n" +
            "\t\t}\n" +
            "\t\t.icon_column{\n" +
            "\t\t\twidth: 20px;\n" +
            "\t\t}\n" +
            "\t\ta:link{\n" +
            "\t\t\ttext-decoration: none;\n" +
            "\t\t\tcolor: #222;\n" +
            "\t\t}\n" +
            "\t\ta:visit{color:#3A047C;}\n" +
            "\t\ta:hover{color:#6A34AC;\n" +
            "\t\ta:active{color:#F00;}\n" +
            "\t</style>\n" +
            "</head>\n" +
            "<body>\n" +
            "\t<div class=\"header_bar\"><span><coffee address_bar/></span></div>\n" +
            "\t<table><thead>\n" +
            "\t\t<tr>\n" +
            "\t\t\t<th class=\"icon_column\"></th>\n" +
            "\t\t\t<th>Name</th>\n" +
            "\t\t\t<th>Size</th>\n" +
            "\t\t\t<th>Last modify date</th>\n" +
            "\t\t</tr>\n" +
            "\t</thead><tbody><coffee items/></tbody></table>\n" +
            "</body>\n" +
            "</html>";

    private final String folderIcon = "<svg height=\"24\"width=\"24\"fill =\"#CE8D2B\">\n" +
            "  <path d=\"M10,4H4c-1.1,0 -1.99,0.9 -1.99,2L2,18c0,1.1 0.9,2 2," +
            "2h16c1.1,0 2,-0.9 2,-2V8c0,-1.1 -0.9,-2 -2,-2h-8l-2,-2z\"/>\n" +
            "</svg>";

    private final String fileIcon = "<svg height=\"24\"width=\"24\"fill =\"#27133F\">\n" +
            "  <path d=\"M6,2c-1.1,0 -1.99,0.9 -1.99,2L4,20c0,1.1 0.89,2 1.99,2L18," +
            "22c1.1,0 2,-0.9 2,-2L20,8l-6,-6L6,2zM13,9L13,3.5L18.5,9L13,9z\"/>\n" +
            "</svg>";

    public DirectoryContentResponse(String rootName,File file) {
        this.file = file;
        this.rootName = rootName;
    }

    public HttpResponse getResponse(HttpRequest request) {
        String uri = request.getUri();
        Map<String, Callable<String>> params = new HashMap<>();
        params.put("title", file::getName);
        params.put("address_bar",()->generateAddressBar(uri));
        params.put("items",()-> getContent(uri));
        HttpResponse res = new TemplateEngine(template).render(request, HttpResponseStatus.OK,params);
        res.send();
        return res;
    }

    private String generateAddressBar(String uri){
        StringBuilder sb = new StringBuilder();
        if(uri.equals("/"+rootName)){
            return rootName;
        }
        String[] parts = uri.substring(1).split("/");
        sb.append("<a href=\"/").append(rootName).append("\">").append(rootName).append("</a> > ");
        for(int i =0; i < parts.length-1 ; i++){
            sb.append("<a href=\"/");
            for(int j =0 ; j <= i ; j++){
                sb.append(parts[j]).append("/");
            }
            sb.append("\">").append(parts[i]).append("</a> > ");
        }
        sb.append(file.getName());
        return sb.toString();
    }

    private String getContent(String uri){
        StringBuilder sb = new StringBuilder();
        File[] list = file.listFiles();
        if(list == null || list.length == 0){
            return sb.toString();
        }
        List<File> sort = sort(list);
        String parent;
        if(uri.equals("/"+rootName)){
            parent = "";
        }else{
            parent = uri;
        }

        if(!parent.endsWith("/")){
            parent += "/";
        }
        for(File file : sort){
            if(file.isDirectory()){
                sb.append("<tr><td>").append(folderIcon).append("</td><td>")
                        .append("<a href=\"").append(parent).append(file.getName()).append("\">")
                        .append(file.getName())
                        .append("</a></td><td>-</td><td>")
                        .append(new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(file.lastModified())))
                        .append("</td></tr>");
            }else{
                sb.append("<tr><td>").append(fileIcon).append("</td><td>")
                        .append("<a href=\"").append(parent).append(file.getName()).append("\">")
                        .append(file.getName())
                        .append("</a></td><td>")
                        .append(humanReadableByteCountBin(file.length()))
                        .append("</td><td>")
                        .append(new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(file.lastModified())))
                        .append("</td></tr>");
            }

        }
        return sb.toString();
    }

    private String humanReadableByteCountBin(long size){
        final String[] units = new String[] { "B", "kB", "MB", "GB", "TB", "PB", "EB" };
        final int exp = (int) (Math.log(size)/Math.log(1024));
        return String.format("%.2f %s", size / Math.pow(1024,exp), units[exp]);
    }

    private List<File> sort(File[] list){
        List<File> file = new ArrayList<>();
        List<File> folder = new ArrayList<>();
        for(File f : list){
            if(f.isDirectory()){
                folder.add(f);
            }else{
                file.add(f);
            }
        }
        List<File> res = new ArrayList<>(list.length);
        res.addAll(folder);
        res.addAll(file);
        return res;
    }
}
