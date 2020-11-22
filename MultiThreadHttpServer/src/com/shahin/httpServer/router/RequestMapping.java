package com.shahin.httpServer.router;

import com.shahin.httpServer.http.HttpRequestMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD,ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestMapping {
    HttpRequestMethod method() default HttpRequestMethod.GET;
    String path();
}
