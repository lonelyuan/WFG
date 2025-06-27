package SA.tool.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ReferenceInfo {
    
    @JsonProperty("symbol_name")
    private String symbolName;
    
    @JsonProperty("reference_type")
    private String referenceType; // METHOD_CALL, FIELD_ACCESS, TYPE_REFERENCE, VARIABLE_ACCESS
    
    @JsonProperty("code_pos")
    private String codePos;
    
    @JsonProperty("context")
    private String context; // 引用所在的上下文（方法名、类名等）
    
    @JsonProperty("line_content")
    private String lineContent; // 引用所在行的完整内容

    public String getSymbolName() {
        return symbolName;
    }

    public void setSymbolName(String symbolName) {
        this.symbolName = symbolName;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getCodePos() {
        return codePos;
    }

    public void setCodePos(String codePos) {
        this.codePos = codePos;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getLineContent() {
        return lineContent;
    }

    public void setLineContent(String lineContent) {
        this.lineContent = lineContent;
    }
} 