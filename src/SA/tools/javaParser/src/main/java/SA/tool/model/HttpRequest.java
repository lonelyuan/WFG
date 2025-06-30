package SA.tool.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {

    @JsonProperty("method")
    private String method;

    @JsonProperty("path")
    private String path;

    @JsonProperty("query_params")
    private Map<String, String> queryParams = new HashMap<>();

    @JsonProperty("body")
    private Map<String, Object> body = new HashMap<>();

    // Getters and Setters
    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(Map<String, String> queryParams) {
        this.queryParams = queryParams;
    }

    public void addQueryParam(String key, String value) {
        if (this.queryParams == null) {
            this.queryParams = new HashMap<>();
        }
        this.queryParams.put(key, value);
    }

    public Map<String, Object> getBody() {
        return body;
    }

    public void setBody(Map<String, Object> body) {
        this.body = body;
    }
} 