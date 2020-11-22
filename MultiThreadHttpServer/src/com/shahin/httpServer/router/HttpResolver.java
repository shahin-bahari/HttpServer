package com.shahin.httpServer.router;

import com.shahin.httpServer.http.HttpRequest;
import com.shahin.httpServer.response.HttpResponse;

public interface HttpResolver {
    HttpResponse resolve(HttpRequest request);
}
