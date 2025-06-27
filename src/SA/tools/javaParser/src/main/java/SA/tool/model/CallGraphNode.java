package SA.tool.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class CallGraphNode {
    
    @JsonProperty("method_signature")
    private String methodSignature; // 完整的方法签名
    
    @JsonProperty("class_name")
    private String className;
    
    @JsonProperty("method_name")
    private String methodName;
    
    @JsonProperty("code_pos")
    private String codePos;
    
    @JsonProperty("callers")
    private List<String> callers = new ArrayList<>(); // 调用此方法的方法列表
    
    @JsonProperty("callees")
    private List<String> callees = new ArrayList<>(); // 此方法调用的方法列表
    
    @JsonProperty("call_sites")
    private List<CallSite> callSites = new ArrayList<>(); // 具体的调用位置信息

    public String getMethodSignature() {
        return methodSignature;
    }

    public void setMethodSignature(String methodSignature) {
        this.methodSignature = methodSignature;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
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

    public List<String> getCallers() {
        return callers;
    }

    public void setCallers(List<String> callers) {
        this.callers = callers;
    }

    public List<String> getCallees() {
        return callees;
    }

    public void setCallees(List<String> callees) {
        this.callees = callees;
    }

    public List<CallSite> getCallSites() {
        return callSites;
    }

    public void setCallSites(List<CallSite> callSites) {
        this.callSites = callSites;
    }

    public static class CallSite {
        @JsonProperty("target_method")
        private String targetMethod;
        
        @JsonProperty("code_pos")
        private String codePos;
        
        @JsonProperty("call_type")
        private String callType; // DIRECT, VIRTUAL, INTERFACE, SUPER

        public String getTargetMethod() {
            return targetMethod;
        }

        public void setTargetMethod(String targetMethod) {
            this.targetMethod = targetMethod;
        }

        public String getCodePos() {
            return codePos;
        }

        public void setCodePos(String codePos) {
            this.codePos = codePos;
        }

        public String getCallType() {
            return callType;
        }

        public void setCallType(String callType) {
            this.callType = callType;
        }
    }
} 