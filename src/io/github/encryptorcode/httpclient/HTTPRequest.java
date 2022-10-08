package io.github.encryptorcode.httpclient;

import jp.timeline.api.sekai.PackHelper;
import okhttp3.FormBody;
import okhttp3.RequestBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HTTPRequest {
    private final Method method;
    private final String url;
    private RequestBodyType requestBodyType;
    private final List<KeyValue<String,String>> params;
    private final List<KeyValue<String,String>> headers;
    private final List<KeyValue<String,String>> formData;
    private String body;

    public enum Method{
        GET,
        POST,
        PUT,
        PATCH,
        DELETE
    }

    private enum RequestBodyType {
        FORM_DATA,
        JSON_BODY
    }

    public HTTPRequest(Method method, String url) {
        this.method = method;
        this.url = url;
        this.params = new ArrayList<>();
        this.headers = new ArrayList<>();
        this.formData = new ArrayList<>();
    }

    public HTTPRequest param(String name, String value) {
        if(name == null)
            throw new NullPointerException("Request param name is null");
        if(value == null)
            throw new NullPointerException("Request param value is null");
        this.params.add(new KeyValue<>(name,value));
        return this;
    }

    public HTTPRequest header(String name, String value) {
        if(name == null)
            throw new NullPointerException("Request header name is null");
        if(value == null)
            throw new NullPointerException("Request header value is null");
        this.headers.add(new KeyValue<>(name,value));
        return this;
    }

    public HTTPRequest formParam(String name, String value) {
        if(this.method == Method.GET)
            throw new RuntimeException("Form param cannot be set for a GET request");
        if(this.requestBodyType != null && this.requestBodyType != RequestBodyType.FORM_DATA)
            throw new RuntimeException("You cannot set "+ RequestBodyType.FORM_DATA+" is the "+this.requestBodyType +" is set");
        if(name == null)
            throw new NullPointerException("Request header name is null");
        if(value == null)
            throw new NullPointerException("Request header value is null");
        this.requestBodyType = RequestBodyType.FORM_DATA;
        this.formData.add(new KeyValue<>(name,value));
        return this;
    }

    public HTTPRequest setJsonData(String json){
        if(this.method == Method.GET)
            throw new RuntimeException("Form param cannot be set for a GET request");
        if(this.requestBodyType != null && requestBodyType != RequestBodyType.JSON_BODY)
            throw new RuntimeException("You cannot set "+ RequestBodyType.JSON_BODY+" is the "+this.requestBodyType +" is set");
        this.requestBodyType = RequestBodyType.JSON_BODY;
        this.body = json;
        return this;
    }

    public HTTPResponse getResponse() throws IOException {
        return HTTPConnector.request(this);
    }

    Method getMethod() {
        return method;
    }

    String getUrl() {
        return url;
    }

    List<KeyValue<String, String>> getParams() {
        return params;
    }

    List<KeyValue<String, String>> getHeaders() {
        return headers;
    }

    RequestBody getBody(){
        if(this.requestBodyType == null){
            return null;
        }
        switch (this.requestBodyType){
            case FORM_DATA:
                if(this.formData.size() == 0)
                    return null;

                FormBody.Builder builder = new FormBody.Builder();
                for (KeyValue<String, String> entry : this.formData) {
                    builder.add(entry.getKey(),entry.getValue());
                }
                return builder.build();

            case JSON_BODY:
                if(this.body == null)
                    return null;

                try
                {
                    return RequestBody.create(PackHelper.Pack(this.body));
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
        }
        return null;
    }
}
