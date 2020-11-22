package com.shahin.httpServer.http;

public enum PostDataResult {
        OK,
        UNKNOWN_CONTENT_LENGTH,
        LARGER_THAN_MAX,
        BAD_FRAME,
        IO_ERROR,
        TOO_MANY_FILE,
        MIME_TYPE_MISMATCH,
        UNEXPECTED_NAME
}
