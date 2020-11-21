package com.shahin.httpServer.utils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class ParserUtils {

    public final static char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

    public static String parseIncomingUri(String r, Map<String, List<String>> params) {
        int s = r.indexOf("?");
        if (s < 0) {
            return urlDecode(r);
        }
        decodeParams(r.substring(s + 1), params);
        return urlDecode(r.substring(0, s));
    }

    public static String urlDecode(String str) {
        return URLDecoder.decode(str, StandardCharsets.UTF_8);
    }

    public static void decodeParams(String str, Map<String, List<String>> result) {
        StringTokenizer tokenizer = new StringTokenizer(str, "&");
        while (tokenizer.hasMoreElements()) {
            String p = tokenizer.nextToken();
            int pos = p.indexOf("=");
            String key;
            String value;
            if (pos > 0) {
                key = urlDecode(p.substring(0, pos));
                value = urlDecode(p.substring(pos + 1));
            } else {
                key = urlDecode(p);
                value = "";
            }
            List<String> vals = result.computeIfAbsent(key, k -> new ArrayList<>());
            vals.add(value);
        }
    }

    public static String encodeBase64(byte[] buf) {
        int size = buf.length;
        char[] ar = new char[(size + 2) / 3 * 4];
        int a = 0;
        int i = 0;
        while (i < size) {
            byte b0 = buf[i++];
            byte b1 = i < size ? buf[i++] : 0;
            byte b2 = i < size ? buf[i++] : 0;

            int mask = 0x3F;
            ar[a++] = ALPHABET[b0 >> 2 & mask];
            ar[a++] = ALPHABET[(b0 << 4 | (b1 & 0xFF) >> 4) & mask];
            ar[a++] = ALPHABET[(b1 << 2 | (b2 & 0xFF) >> 6) & mask];
            ar[a++] = ALPHABET[b2 & mask];
        }
        switch (size % 3) {
            case 1:
                ar[--a] = '=';
            case 2:
                ar[--a] = '=';
        }
        return new String(ar);
    }
}
