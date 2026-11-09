package com.example;

import java.io.InputStreamReader; // <--- ADDED
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONTokener; // <--- ADDED

public class Main {
  
    public static void main(String[] args) {
        System.out.println("Write Json to vless://");
        try {
            // Now this line will compile properly
            JSONObject json = new JSONObject(new JSONTokener(new InputStreamReader(System.in, StandardCharsets.UTF_8)));
            
            JSONObject proxyOutbound = null;
            JSONArray outbounds = json.getJSONArray("outbounds");
            for (int i = 0; i < outbounds.length(); i++) {
                JSONObject outbound = outbounds.getJSONObject(i);
                if ("vless".equals(outbound.optString("protocol"))) {
                    proxyOutbound = outbound;
                    break;
                }
            }
            
            if (proxyOutbound == null) {
                System.out.println("VLESS outbound not found.");
                return;
            }
            
            JSONObject vnext = proxyOutbound.getJSONObject("settings").getJSONArray("vnext").getJSONObject(0);
            String address = vnext.getString("address");
            int port = vnext.getInt("port");
            
            JSONObject user = vnext.getJSONArray("users").getJSONObject(0);
            String uuid = user.getString("id");
            String encryption = user.optString("encryption", "none");
            String flow = user.optString("flow", "");
            
            JSONObject stream = proxyOutbound.getJSONObject("streamSettings");
            String network = stream.optString("network", "tcp");
            String security = stream.optString("security", "none");
            
            StringBuilder query = new StringBuilder();
            addParam(query, "encryption", encryption);
            addParam(query, "security", security);
            addParam(query, "type", network);
            addParam(query, "flow", flow);
            
            if ("grpc".equals(network) && stream.has("grpcSettings")) {
                JSONObject grpc = stream.getJSONObject("grpcSettings");
                addParam(query, "serviceName", grpc.optString("serviceName", ""));
                addParam(query, "mode", grpc.optString("mode", "gun"));
            }
            
            if ("tls".equals(security) && stream.has("tlsSettings")) {
                JSONObject tls = stream.getJSONObject("tlsSettings");
                addParam(query, "sni", tls.optString("serverName", ""));
                addParam(query, "fp", tls.optString("fingerprint", ""));
            }
            
            if ("reality".equals(security) && stream.has("realitySettings")) {
                JSONObject reality = stream.getJSONObject("realitySettings");
                addParam(query, "sni", reality.optString("serverName", ""));
                addParam(query, "fp", reality.optString("fingerprint", ""));
                addParam(query, "pbk", reality.optString("publicKey", ""));
                addParam(query, "sid", reality.optString("shortId", ""));
            }
            
            String remarks = json.optString("remarks", "VLESS Proxy");
            String vlessLink = "vless://" + uuid + "@" + address + ":" + port + "?" + query + "#" + remarks;
            System.out.println("VLESS://");
            System.out.println(vlessLink);
            
        } catch (Exception e) {
            System.out.println("JSON error: " + e.getMessage());
        }
    }
    
    private static void addParam(StringBuilder query, String key, String value ) {
        if (value == null || value.isEmpty()) {
            return;
        }
        if (query.length() > 0) {
            query.append("&");
        }
        query.append(encode(key)).append("=").append(encode(value));
    }
    
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}