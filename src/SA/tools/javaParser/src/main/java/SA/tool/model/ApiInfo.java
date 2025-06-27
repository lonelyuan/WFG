package SA.tool.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiInfo {

    @JsonProperty("controller_name")
    private String controllerName;

    @JsonProperty("method_name")
    private String methodName;

    @JsonProperty("code_pos")
    private String codePos;

    @JsonProperty("req")
    private HttpRequest req;

    // Getters and Setters
    public String getControllerName() {
        return controllerName;
    }

    public void setControllerName(String controllerName) {
        this.controllerName = controllerName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getCodePos() {
        return codePos;
    }

    public void setCodePos(String codePos) {
        this.codePos = codePos;
    }

    public HttpRequest getReq() {
        return req;
    }

    public void setReq(HttpRequest req) {
        this.req = req;
    }
} 