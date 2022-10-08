package io.github.encryptorcode.httpclient;

import okhttp3.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HTTPConnector {
    private static final Logger LOGGER = Logger.getLogger(HTTPConnector.class.getName());

    private static final boolean RETRY_ON_FAILURE = true;
    private static final int MAX_CONNECTIONS = 20;

    // timeouts in seconds
    private static final int READ_TIMEOUT = 5;
    private static final int CONNECT_TIMEOUT = 5;
    private static final int WRITE_TIMEOUT = 5;

    // timeout in minutes
    private static final long CONNECTION_ALIVE_DURATION = 5L;


    private static OkHttpClient client = null;
    private HTTPConnector(){}
    private static HTTPConnector okHttpRequestMaker;
    private static HTTPConnector getInstance(){
        if(okHttpRequestMaker == null){
            okHttpRequestMaker = new HTTPConnector();
        }
        return okHttpRequestMaker;
    }

    public static HTTPResponse request(HTTPRequest request) throws IOException {
        return getInstance().sendRequest(request);
    }

    private OkHttpClient getClient(){
        if(client == null){
            client = new OkHttpClient.Builder()
                    .readTimeout(READ_TIMEOUT,TimeUnit.SECONDS)
                    .connectTimeout(CONNECT_TIMEOUT,TimeUnit.SECONDS)
                    .writeTimeout(WRITE_TIMEOUT,TimeUnit.SECONDS)
                    .retryOnConnectionFailure(RETRY_ON_FAILURE)
                    .connectionPool(new ConnectionPool(
                            MAX_CONNECTIONS,
                            CONNECTION_ALIVE_DURATION,
                            TimeUnit.MINUTES
                    ))
                    .build();
        }
        return client;
    }

    private String makeUrl(String url, List<KeyValue<String,String>> params) {

        if(params != null){
            Iterator<KeyValue<String, String>> paramItr = params.iterator();
            StringBuilder urlBuilder = new StringBuilder(url);
            if (paramItr.hasNext()) {
                urlBuilder.append("?");
            }
            while(paramItr.hasNext()) {
                KeyValue<String, String> param = paramItr.next();
                String paramName = param.getKey();
                String paramValue = param.getValue();

                try {
                    urlBuilder.append(URLEncoder.encode(paramName, StandardCharsets.UTF_8.toString())).append("=").append(URLEncoder.encode(paramValue, StandardCharsets.UTF_8.toString()));
                } catch(Exception e) {
                    LOGGER.log(Level.WARNING,"invalid param: details:: name = {0} , value = {1}", new Object[]{paramName,paramValue});
                }

                if(paramItr.hasNext()) {
                    urlBuilder.append("&");
                }
            }
            url = urlBuilder.toString();
        }
        return url;
    }

    private Request constructRequest(HTTPRequest request){
        Request.Builder builder = new Request.Builder();
        builder.url(makeUrl(request.getUrl(),request.getParams()));

        if(request.getHeaders() != null){
            for (KeyValue<String, String> header : request.getHeaders()) {
                builder.header(header.getKey(),header.getValue());
            }
        }

        RequestBody body = request.getBody();
        if(body != null) {
            switch (request.getMethod()) {
                case GET:
                    throw new RuntimeException("Not Supported");
                case POST:
                    builder.post(body);
                    break;
                case PUT:
                    builder.put(body);
                    break;
                case PATCH:
                    builder.patch(body);
                    break;
                case DELETE:
                    builder.delete(body);
                    break;
            }
        }

        return builder.build();
    }

    private HTTPResponse sendRequest(HTTPRequest req) throws IOException {
        Request request = constructRequest(req);
        Response response;
        response = getClient().newCall(request).execute();

        int responseCode = response.code();
        String responseMessage = response.message();
        byte[] responseBody = null;
        if (response.body() != null)
            responseBody = Objects.requireNonNull(response.body()).bytes();
        Headers headers = response.headers();

        response.close();

        return new HTTPResponse(responseCode,responseMessage,responseBody,headers);
    }

}
