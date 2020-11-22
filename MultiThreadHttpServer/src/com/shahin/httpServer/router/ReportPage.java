package com.shahin.httpServer.router;

import com.shahin.httpServer.http.HttpRequest;
import com.shahin.httpServer.response.HttpResponse;
import com.shahin.httpServer.response.HttpResponseStatus;
import com.shahin.httpServer.templateEngine.TemplateEngine;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class ReportPage {

    private final static String template = "<!DOCTYPE html>\n" +
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
            "\t<div class=\"header_bar\"><span>Router's Report</span></div>\n" +
            "\t<table><thead>\n" +
            "\t\t<tr>\n" +
            "\t\t\t<th>URL</th><th>redirect to</th>\n" +
            "\t\t</tr>\n" +
            "\t</thead><tbody><coffee links /></tbody></table>\n" +
            "</body>\n" +
            "</html>";
    private final List<StaticFile> staticFileList;
    private final Map<String,String> redirectRules;
    private final List<Routing> routings;

    public ReportPage(List<StaticFile> staticFileList,
                      List<Routing> routing,
                      Map<String, String> redirectRules) {
        this.staticFileList = staticFileList;
        this.redirectRules = redirectRules;
        this.routings = routing;
    }

    public HttpResponse getResponse(HttpRequest request){
        Map<String, Callable<String>> params = new HashMap<>();
        params.put("links",this::getLinks);
        return new TemplateEngine(template).render(request, HttpResponseStatus.OK,params);
    }

    private String getLinks(){
        StringBuilder sb = new StringBuilder();

        for(String from : redirectRules.keySet()){
            sb.append("<tr><td><a href=\"")
                    .append(from).append("\">").append(from)
                    .append("</a></td><td>")
                    .append(redirectRules.getOrDefault(from, "-"))
                    .append("</td></tr>");
        }

        for(Routing routing : routings){
            for(String path : routing.getRoutingPath()){
                sb.append("<tr><td><a href=\"")
                        .append(path).append("\">").append(path)
                        .append("</a></td><td>")
                        .append(redirectRules.getOrDefault(path, "-"))
                        .append("</td></tr>");
            }
        }
        for(StaticFile p : staticFileList){
            String path = p.getPublicDir();
            String name = new File(path).getName();
            sb.append("<tr><td><a href=\"/")
                    .append(name).append("\">/").append(name)
                    .append("</a></td><td>Static File : ").append(p.getPublicDir())
                    .append("</td></tr>");
        }
        return sb.toString();
    }
}
