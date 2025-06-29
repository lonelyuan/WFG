package SA.tool.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SymbolUsage {
    @JsonProperty("api_method")
    private String apiMethod;
    
    @JsonProperty("usage_type")
    private String usageType; // METHOD_CALL, FIELD_ACCESS, INSTANTIATION, PARAMETER
    
    @JsonProperty("usage_line")
    private int usageLine;
    
    @JsonProperty("usage_code")
    private String usageCode;

    // Constructors
    public SymbolUsage() {}
    
    public SymbolUsage(String apiMethod, String usageType, int usageLine, String usageCode) {
        this.apiMethod = apiMethod;
        this.usageType = usageType;
        this.usageLine = usageLine;
        this.usageCode = usageCode;
    }

    // Getters and Setters
    public String getApiMethod() { return apiMethod; }
    public void setApiMethod(String apiMethod) { this.apiMethod = apiMethod; }

    public String getUsageType() { return usageType; }
    public void setUsageType(String usageType) { this.usageType = usageType; }

    public int getUsageLine() { return usageLine; }
    public void setUsageLine(int usageLine) { this.usageLine = usageLine; }

    public String getUsageCode() { return usageCode; }
    public void setUsageCode(String usageCode) { this.usageCode = usageCode; }
} 