package com.shahin.httpServer.http;

public enum HttpRequestMethod {

    GET("GET"),
    POST("POST"),
    HEAD("HEAD"),
    PUT("PUT"),
    OPTION("OPTION"),
    DELETE("DELETE"),
    CONNECT("CONNECT"),
    TRACE("TRACE"),
    PATCH("PATCH"),
    OTHER("");

    private String name;

    HttpRequestMethod(String name) {
        this.name = name;
    }

    public static HttpRequestMethod lookup(String method){
        for(HttpRequestMethod v : values()){
            if(v != OTHER && v.name.equalsIgnoreCase(method)){
                return v;
            }
        }
        return OTHER;
    }

    public String getMethodName() {
        return this.name();
    }
}
