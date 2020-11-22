package com.shahin.httpServer.router;

import com.shahin.httpServer.http.HttpRequest;
import com.shahin.httpServer.response.HttpResponse;
import com.shahin.httpServer.response.HttpResponseStatus;
import com.shahin.httpServer.response.TextResponse;
import com.shahin.httpServer.utils.MimeTypes;

public class WelcomePage {

    String html = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <meta charset=\"utf-8\">\n" +
            "    <title>Welcome</title>\n" +
            "</head>\n" +
            "<body style=\"margin: 12vh 4vw 12vh 4vw;\">\n" +
            "\t<p style=\"\n" +
            "\tfont-family: 'Quicksand',sans-serif;\n" +
            "\tfont-size: 6rem;\n" +
            "\ttext-transform: uppercase;\n" +
            "\tfont-weight: 700;\n" +
            "\tmargin-top: 0;\n" +
            "\tmargin-bottom: 8px;\n" +
            "\tcolor: #151515;\n" +
            "\ttext-align: center;\">\n" +
            "\tHello!\n" +
            "\t</p>\n" +
            "\n" +
            "\t<h2 style=\"\n" +
            "\tfont-family: 'Quicksand', sans-serif;\n" +
            "\tfont-size: 2rem;\n" +
            "\tmargin: 0;\n" +
            "\tfont-weight: 700;\n" +
            "\tcolor: #151515;\n" +
            "\ttext-align: center;\">\n" +
            "\tWelcome to the Http Server...\n" +
            "\t</h2>\n" +
            "</body></html>";

    public HttpResponse getResponse(HttpRequest request){
        TextResponse response = new TextResponse(request, HttpResponseStatus.OK, MimeTypes.HTML);
        response.writeMessage(html);
        return response;
    }
}
