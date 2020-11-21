package com.shahin.httpServer.response;

public enum HttpResponseStatus {

    /*Information response*/
    CONTINUE(100, "Continue"),
    SWITCHING_PROTOCOLS(101, "Switching Protocols"),
    PROCESSING(102, "Processing"),

    /*success response*/
    OK(200, "OK"),
    CREATED(201, "Created"),
    ACCEPTED(202, "Accepted"),
    NON_AUTHORITATIVE_INFORMATION(203, "Non-Authoritative Information"),
    NO_CONTENT(204, "No Content"),
    RESET_CONTENT(205, "Reset Content"),
    PARTIAL_CONTENT(206, "Partial Content"),
    MULTI_STATUS(207, "Multi-Status"),
    ALREADY_REPORTED(208, "Already Reported"),
    IM_USED(226, " IM Used"),

    /*redirection response*/
    MULTIPLE_CHOICES(300, "Multiple Choices"),
    MOVED_PERMANENTLY(301, "Moved Permanently"),
    FOUND(302, "Found"),
    SEE_OTHER(303, "See Other"),
    NOT_MODIFIED(304, "Not Modified"),
    USE_PROXY(305, "Use Proxy"),
    TEMPORARY_REDIRECT(307, "Temporary Redirect"),
    PERMANENT_REDIRECT(308, "Permanent Redirect"),

    /*client error*/
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    PAYMENT_REQUIRED(402, "Payment Required"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    NOT_ACCEPTABLE(406, "Not Acceptable"),
    PROXY_AUTHENTICATION_REQUIRED(407, "Proxy Authentication Required"),
    REQUEST_TIME_OUT(408, "Request Time-out"),
    CONFLICT(409, "Conflict"),
    GONE(410, "gone"),
    LENGTH_REQUIRED(411, "Length Required"),
    PRECONDITION_FAILED(412, "Precondition Failed"),
    REQUEST_ENTITY_TOO_LARGE(413, "Request Entity Too Large"),
    REQUEST_URI_TOO_LARGE(414, "Request-URI Too Large"),
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
    REQUEST_RANGE_NOT_SATISFIABLE(416, "Requested range not satisfiable"),
    EXPECTATION_FAILED(417, "Expectation Failed"),
    UNPROCESSABLE_ENTITY(422, "Unprocessable Entity"),
    LOCKED(423, "Locked"),
    FAILED_DEPENDENCY(424, "Failed Dependency"),
    UPGRADE_REQUIRED(426, "Upgrade Required"),
    PRECONDITION_REQUIRED(428, "Precondition Required"),
    TOO_MANY_REQUESTS(429, "Too Many Requests"),
    REQUEST_HEADER_FIELDS_TOO_LARGE(431, "Request Header Fields Too Large"),

    /*server error*/
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    NOT_IMPLEMENTED(501, "Not Implemented"),
    BAD_GATEWAY(502, "Bad Gateway"),
    SERVICE_UNAVAILABLE(503, "Service Unavailable"),
    GATEWAY_TIME_OUT(504, "Gateway Time-out"),
    HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version not supported"),
    INSUFFICIENT_STORAGE(507, "Insufficient Storage"),
    LOOP_DETECTED(508, "Loop Detected"),
    NOT_EXTENDED(510, "Not Extended"),
    NETWORK_AUTHENTICATION_REQUIRED(511, "Network Authentication Required");

    private final int statusCode;
    private final String statusText;

    HttpResponseStatus(int statusCode, String statusText) {
        this.statusCode = statusCode;
        this.statusText = statusText;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusDescription() {
        return this.statusCode + " " + this.statusText;
    }
}
