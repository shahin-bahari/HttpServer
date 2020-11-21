package com.shahin.httpServer.response;

import com.shahin.httpServer.http.HttpRequest;

import java.nio.ByteBuffer;

public class TextResponse extends HttpResponse {

    private StringBuilder msg = new StringBuilder();

    public TextResponse(HttpRequest request, HttpResponseStatus status, String mimeType) {
        super(request);
        this.mimeType = mimeType;
        this.status = status;
    }

    public void writeMessage(String message) {
        msg.append(message);
    }

    @Override
    public void send() {
        ByteBuffer buffer = ByteBuffer.wrap(msg.toString().getBytes());
        sendData(buffer,this::endResponse);
    }
}
