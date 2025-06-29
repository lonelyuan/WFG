package SA.tool.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DefinitionInfo {
    
    @JsonProperty("symbol_name")
    private String symbolName;
    
    @JsonProperty("definition_type")
    private String definitionType; // CLASS, METHOD, FIELD, VARIABLE, PARAMETER, ENUM, INTERFACE
    
    @JsonProperty("code_pos")
    private String codePos; // file.java:L10-L15
    
    @JsonProperty("definition_code")
    private String definitionCode; // 定义的完整源代码
    
    @JsonProperty("signature")
    private String signature; // 方法签名或类声明
    
    @JsonProperty("modifiers")
    private String modifiers; // public, private, static等修饰符
    
    @JsonProperty("scope")
    private String scope; // 所在的类或方法
    
    @JsonProperty("return_type")
    private String returnType; // 方法返回类型或字段类型
    
    @JsonProperty("parameters")
    private String parameters; // 方法参数

    public String getSymbolName() {
        return symbolName;
    }

    public void setSymbolName(String symbolName) {
        this.symbolName = symbolName;
    }

    public String getDefinitionType() {
        return definitionType;
    }

    public void setDefinitionType(String definitionType) {
        this.definitionType = definitionType;
    }

    public String getCodePos() {
        return codePos;
    }

    public void setCodePos(String codePos) {
        this.codePos = codePos;
    }

    public String getDefinitionCode() {
        return definitionCode;
    }

    public void setDefinitionCode(String definitionCode) {
        this.definitionCode = definitionCode;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getModifiers() {
        return modifiers;
    }

    public void setModifiers(String modifiers) {
        this.modifiers = modifiers;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }
} 