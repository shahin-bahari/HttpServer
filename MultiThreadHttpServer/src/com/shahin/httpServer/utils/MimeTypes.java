package com.shahin.httpServer.utils;

import java.util.HashMap;
import java.util.Map;

public class MimeTypes {
    public static final String PLAIN_TEXT = "text/plain";
    public static final String HTML = "text/html";
    public static final String CSS = "text/css";
    public static final String XML = "text/xml";
    public static final String JS = "text/javascript";
    public static final String CSV = "text/csv";

    public static final String JSON = "application/json";

    public static final String BITMAP_IMAGE = "image/bmp";
    public static final String PNG_IMAGE = "image/png";
    public static final String JPG_IMAGE = "image/jpeg";
    public static final String GIF_IMAGE = "image/gif";
    public static final String ICON_IMAGE = " image/x-icon";
    public static final String SVG_IMAGE = "image/svg+xml";
    public static final String WEBP_IMAGE = "image/webp";
    public static final String TIFF_IMAGE = "image/tiff";

    public static final String MP3_AUDIO = "audio/mpeg";
    public static final String OGG_AUDIO = "audio/ogg";
    public static final String PCM_AUDIO = "audio/wav";
    public static final String AAC_AUDIO = "audio/aac";
    public static final String MIDI_AUDIO = "audio/midi";
    public static final String M4A_AUDIO = "audio/mp4";

    public static final String MPEG_VIDEO = "video/mpeg";
    public static final String TS_VIDEO = "video/mp2t";
    public static final String _3GP_VIDEO = "video/3gpp";
    public static final String AVI_VIDEO = "video/x-msvideo";
    public static final String MKV_VIDEO = "video/x-matroska";
    public static final String MP4_VIDEO = "video/mp4";

    public static final String WEB_OPEN_FONT = "font/woff";
    public static final String WEB_OPEN_FONT2 = "font/woff";
    public static final String TRUE_TYPE_FONT = "font/ttf";

    private static final Map<String, String> extensionMap = new HashMap<>();

    static {
        extensionMap.put("html", HTML);
        extensionMap.put("htm", HTML);
        extensionMap.put("css", CSS);
        extensionMap.put("js", JS);
        extensionMap.put("csv", CSV);
        extensionMap.put("xml", XML);
        extensionMap.put("txt", PLAIN_TEXT);
        extensionMap.put("json", JSON);
        extensionMap.put("bmp", BITMAP_IMAGE);
        extensionMap.put("gif", GIF_IMAGE);
        extensionMap.put("jpg", JPG_IMAGE);
        extensionMap.put("jpeg", JPG_IMAGE);
        extensionMap.put("png", PNG_IMAGE);
        extensionMap.put("webp", WEBP_IMAGE);
        extensionMap.put("svg", SVG_IMAGE);
        extensionMap.put("tif", TIFF_IMAGE);
        extensionMap.put("tiff", TIFF_IMAGE);
        extensionMap.put("ico", ICON_IMAGE);

        extensionMap.put("wav", PCM_AUDIO);
        extensionMap.put("mp3", MP3_AUDIO);
        extensionMap.put("aac", AAC_AUDIO);
        extensionMap.put("mid", MIDI_AUDIO);
        extensionMap.put("midi", MIDI_AUDIO);
        extensionMap.put("m4a",M4A_AUDIO);
        extensionMap.put("ogg", OGG_AUDIO);
        extensionMap.put("ttf", TRUE_TYPE_FONT);
        extensionMap.put("woff", WEB_OPEN_FONT);
        extensionMap.put("woff2", WEB_OPEN_FONT2);
        extensionMap.put("avi", AVI_VIDEO);
        extensionMap.put("mkv", MKV_VIDEO);
        extensionMap.put("ts", TS_VIDEO);
        extensionMap.put("3gp", _3GP_VIDEO);
        extensionMap.put("mp4", MP4_VIDEO);
        extensionMap.put("mpg", MPEG_VIDEO);
    }

    public static void registerExtension(String extension, String mimeType) {
        extensionMap.put(extension, mimeType);
    }

    public static String getFileMimeType(String extension) {
        return extensionMap.get(extension.toLowerCase());
    }

    public static String getMimeTypeFromPath(String path){
        int pos = path.lastIndexOf(".");
        if(pos <0) return null;
        return getFileMimeType(path.substring(pos +1));
    }
}
