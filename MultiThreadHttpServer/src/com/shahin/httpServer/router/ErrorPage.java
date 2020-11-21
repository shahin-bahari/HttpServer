package com.shahin.httpServer.router;

import com.shahin.httpServer.http.HttpRequest;
import com.shahin.httpServer.response.HttpResponse;
import com.shahin.httpServer.response.HttpResponseStatus;
import com.shahin.httpServer.templateEngine.TemplateEngine;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class ErrorPage {

    private final String template = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <meta charset=\"utf-8\">\n" +
            "    <title>Error</title>\n" +
            "</head>\n" +
            "<body style=\"margin: 12vh 4vw 12vh 4vw;\">\n" +
            "\t<p style=\"\n" +
            "\tfont-family: 'Quicksand',sans-serif;\n" +
            "\tfont-size: 86px;\n" +
            "\ttext-transform: uppercase;\n" +
            "\tfont-weight: 700;\n" +
            "\tmargin-top: 0;\n" +
            "\tmargin-bottom: 8px;\n" +
            "\tcolor: #151515;\n" +
            "\ttext-align: center;\">\n" +
            "\tOops!\n" +
            "\t</p>\n" +
            "\n" +
            "\t<h2 style=\"\n" +
            "\tfont-family: 'Quicksand', sans-serif;\n" +
            "\tfont-size: 26px;\n" +
            "\tmargin: 0;\n" +
            "\tfont-weight: 700;\n" +
            "\tcolor: #151515;\n" +
            "\ttext-align: center;\">\n" +
            "\t<coffee message/>\n" +
            "\t</h2>\n" +
            "\t<div style=\"\n" +
            "\ttext-align: center;\">\n" +
            "\t\t<a href=\"/\" style=\"\n" +
            "\t\tfont-family: 'Quicksand', sans-serif;\n" +
            "\t\tfont-size: 14px;\n" +
            "\t\ttext-decoration: none;\n" +
            "\t\ttext-transform: uppercase;\n" +
            "\t\tbackground: #18e06f;\n" +
            "\t\tdisplay: inline-block;\n" +
            "\t\tpadding: 15px 30px\n" +
            "\t\t;border-radius: 5px\n" +
            "\t\t;color: #fff;\n" +
            "\t\tfont-weight: 700;\n" +
            "\t\tmargin-top: 20px;\">\n" +
            "\t\t\tgo back\n" +
            "\t\t</a></div>\n" +
            "</body></html>";

    private final HttpResponseStatus status;

    public ErrorPage(HttpResponseStatus status) {
        this.status = status;
    }

    public HttpResponse getResponse(HttpRequest request){
        Map<String, Callable<String>> params = new HashMap<>();
        params.put("message",()->"Error "+status.getStatusCode()
                + " : " + status.getStatusDescription());
        return new TemplateEngine(template).render(request,status,params);
    }
}
