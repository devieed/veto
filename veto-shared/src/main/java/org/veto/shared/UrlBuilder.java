package org.veto.shared;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class UrlBuilder {

    private String userInfo;

    private String protocol;

    private String host;

    private int port = -1;
    /**
     * 超链
     */
    private String fragment;

    private List<String> path;

    public List<String> getPath() {
        return path;
    }

    private Map<String, List<String>> query;

    private UrlBuilder(){

    }

    public static UrlBuilder formUrl(String url) throws URISyntaxException {
        return formUrl(new URI(url));
    }

    public static UrlBuilder formUrl(URI url){
        UrlBuilder urlBuilder = new UrlBuilder();

        urlBuilder.setProtocol(url.getScheme());

        urlBuilder.setHost(url.getHost());

        urlBuilder.setPort(url.getPort());

        urlBuilder.path = new ArrayList<>(Arrays.asList(url.getPath().split("/")));

        urlBuilder.query = new HashMap<>();
        if (url.getQuery() != null) {
            for (String s : url.getQuery().split("&")) {
                String key = s.split("=")[0];
                String val = s.split("=").length == 2 ? s.split("=")[1] : "";

                if (!urlBuilder.query.containsKey(key)) {
                    urlBuilder.query.put(key, new ArrayList<>());
                }
                urlBuilder.query.get(key).add(val);
            }
        }

        urlBuilder.fragment = url.getFragment();

        urlBuilder.userInfo = url.getUserInfo();

        return urlBuilder;
    }

    public UrlBuilder addQuery(String key, String val){
        if (!this.query.containsKey(key)){
            this.query.put(key, new ArrayList<>());
        }
        this.query.get(key).add(val);

        return this;
    }

    public UrlBuilder setDomain(String domain) throws URISyntaxException {
        URI uri = new URI(domain);

        if (uri.getScheme() != null){
            this.protocol = uri.getScheme();
        }
        if (uri.getHost() != null){
            this.host = uri.getHost();
        }

        this.port = uri.getPort();

        return this;
    }

    public UrlBuilder addPath(String path){
        this.path.add(path);

        return this;
    }

    public UrlBuilder addPath(String ...paths){
        for (String s : paths) {
            addPath(s);
        }
        return this;
    }

    public UrlBuilder setPath(String path){
        this.path.clear();
        this.path = new ArrayList<>(Arrays.asList(path.split("/")));

        return this;
    }

    public UrlBuilder setQuery(String query){
        this.query.clear();
        for (String s : query.split("&")) {
            String key = s.split("=")[0];
            String val = s.split("=").length == 2 ? s.split("=")[1] : "";

            if (!this.query.containsKey(key)) {
                this.query.put(key, new ArrayList<>());
            }
            this.query.get(key).add(val);
        }

        return this;
    }

    public UrlBuilder removePath(String path){
        this.path.remove(path);
        return this;
    }

    public UrlBuilder clearPath(){
        this.path.clear();

        return this;
    }

    public UrlBuilder setUserInfo(String userInfo) {
        this.userInfo = userInfo;
        return this;
    }

    public UrlBuilder removeUserInfo(){
        this.userInfo = null;

        return this;
    }

    public UrlBuilder removeQuery(String key){
        this.query.remove(key);

        return this;
    }

    public UrlBuilder clearQuery(){
        this.query.clear();

        return this;
    }

    public UrlBuilder setProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public UrlBuilder setHost(String host) {
        this.host = host;
        return this;
    }

    public UrlBuilder setPort(int port) {
        this.port = port;
        return this;
    }

    public UrlBuilder setFragment(String fragment) {
        this.fragment = fragment;

        return this;
    }

    public UrlBuilder removeFragment(){
        this.fragment = null;

        return this;
    }

    @Override
    public String toString() {

        List<String> querys = new ArrayList<>();
        for (String key : this.query.keySet()) {
            querys.addAll(this.query.get(key).stream().map(val -> key + "=" + val).toList());
        }

        String resultQuery = querys.isEmpty() ? null : String.join("&", querys);

        try {
            String path = String.join("/", this.path);
            return new URI(this.protocol, this.userInfo, this.host, this.port, path.startsWith("/") ? path : "/" + path, resultQuery, this.fragment).toString();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    public UrlBuilder addPathWithQuery(String pathWithQuery) {
        if (pathWithQuery == null || pathWithQuery.isEmpty()) {
            throw new IllegalArgumentException("Path with query cannot be null or empty");
        }

        // 检查是否包含查询参数
        if (pathWithQuery.contains("?")) {
            String[] parts = pathWithQuery.split("\\?", 2);
            String pathPart = parts[0];
            String queryPart = parts[1];

            // 添加路径部分
            if (!pathPart.isEmpty()) {
                this.path.addAll(Arrays.asList(pathPart.split("/")));
            }

            // 添加查询参数部分
            for (String param : queryPart.split("&")) {
                String[] keyValue = param.split("=", 2);
                String key = keyValue[0];
                String value = keyValue.length > 1 ? keyValue[1] : "";

                this.addQuery(key, value);
            }
        } else {
            // 如果没有查询参数，直接添加路径部分
            this.path.addAll(Arrays.asList(pathWithQuery.split("/")));
        }

        return this;
    }

}
