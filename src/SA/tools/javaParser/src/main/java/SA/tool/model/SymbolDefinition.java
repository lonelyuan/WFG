package SA.tool.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class SymbolDefinition {
    @JsonProperty("symbol_id")
    private String symbolId;
    
    @JsonProperty("symbol_name")
    private String symbolName;
    
    @JsonProperty("symbol_type")
    private String symbolType; // FIELD, METHOD, CLASS, PARAMETER
    
    @JsonProperty("data_type")
    private String dataType;
    
    @JsonProperty("scope")
    private String scope; // CLASS_LEVEL, METHOD_LEVEL, EXTERNAL
    
    @JsonProperty("definition")
    private SymbolLocation definition;
    
    @JsonProperty("usage_contexts")
    private List<SymbolUsage> usageContexts = new ArrayList<>();

    // Constructors
    public SymbolDefinition() {}
    
    public SymbolDefinition(String symbolId, String symbolName, String symbolType, 
                           String dataType, String scope) {
        this.symbolId = symbolId;
        this.symbolName = symbolName;
        this.symbolType = symbolType;
        this.dataType = dataType;
        this.scope = scope;
    }

    // Getters and Setters
    public String getSymbolId() { return symbolId; }
    public void setSymbolId(String symbolId) { this.symbolId = symbolId; }

    public String getSymbolName() { return symbolName; }
    public void setSymbolName(String symbolName) { this.symbolName = symbolName; }

    public String getSymbolType() { return symbolType; }
    public void setSymbolType(String symbolType) { this.symbolType = symbolType; }

    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public SymbolLocation getDefinition() { return definition; }
    public void setDefinition(SymbolLocation definition) { this.definition = definition; }

    public List<SymbolUsage> getUsageContexts() { return usageContexts; }
    public void setUsageContexts(List<SymbolUsage> usageContexts) { this.usageContexts = usageContexts; }
    
    public void addUsageContext(SymbolUsage usage) {
        this.usageContexts.add(usage);
    }
} 