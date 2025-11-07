package org.veto.shared;

public class Response {
    private String code = "1";
    private Object data;
    private String message;

    public Response(String code, Object data) {
        this.code = code;
        this.data = data;
    }

    public Response() {
    }

    public String getCode() {
        return code;
    }

    public Response setCode(String code) {
        this.code = code;
        return this;
    }

    public Object getData() {
        return data;
    }

    public Response setData(Object data) {
        this.data = data;
        return this;
    }

    public static Response me() {
        return new Response();
    }

    public static Response error(String code){
        return new Response(code, null);
    }

    public static Response error(String code, String message){
        return new Response(code, message);
    }

    public static Response success(Object data) {
        return new Response("1", data);
    }

    public static Response success() {
        return new Response("1", null);
    }
}
