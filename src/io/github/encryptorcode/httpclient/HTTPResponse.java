package io.github.encryptorcode.httpclient;

import jp.timeline.api.sekai.PackHelper;
import okhttp3.Headers;

public class HTTPResponse {
    private final int responseCode;
    private final String responseMessage;
    private byte[] body;
    private final Headers headers;

    HTTPResponse(int responseCode, String responseMessage, byte[] body, Headers headers) {
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        if(body != null) {
            this.body = body;
        }
        this.headers = headers;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public String getData() {
        try
        {
            return PackHelper.Unpack(this.body);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return "";
    }

    public Headers getHeaders() {
        return headers;
    }
}
