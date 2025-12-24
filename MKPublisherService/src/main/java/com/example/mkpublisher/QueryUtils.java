package com.example.mkpublisher;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class QueryUtils {
    private QueryUtils() {}

    public static Map<String, String> parseQuery(String query) {
        if (query == null || query.isEmpty()) return Collections.emptyMap();
        Map<String, String> map = new HashMap<>();
        String[] parts = query.split("&");
        for (String p : parts) {
            int idx = p.indexOf('=');
            if (idx == -1) continue;
            String k = URLDecoder.decode(p.substring(0, idx), StandardCharsets.UTF_8);
            String v = URLDecoder.decode(p.substring(idx + 1), StandardCharsets.UTF_8);
            map.put(k, v);
        }
        return map;
    }
}
