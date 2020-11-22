package com.shahin.httpServer.router;

import com.shahin.httpServer.http.HttpRequest;
import com.shahin.httpServer.http.PostParam;

import java.util.List;
import java.util.Map;

public interface HttpResolverWithPost {
    void resolve(HttpRequest request , @PostParam Map<String, List<String>> params);
}
