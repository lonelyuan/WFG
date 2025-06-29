package SA.tool.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ControllerAnalysisResult {
    @JsonProperty("controller_name")
    private String controllerName;
    
    @JsonProperty("file_path")
    private String filePath;
    
    @JsonProperty("apis")
    private List<ApiInfo> apis = new ArrayList<>();
    
    @JsonProperty("symbol_table")
    private Map<String, SymbolDefinition> symbolTable = new HashMap<>();
    
    @JsonProperty("metadata")
    private AnalysisMetadata metadata;

    // Constructors
    public ControllerAnalysisResult() {}
    
    public ControllerAnalysisResult(String controllerName, String filePath) {
        this.controllerName = controllerName;
        this.filePath = filePath;
        this.metadata = new AnalysisMetadata();
    }

    // Getters and Setters
    public String getControllerName() { return controllerName; }
    public void setControllerName(String controllerName) { this.controllerName = controllerName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public List<ApiInfo> getApis() { return apis; }
    public void setApis(List<ApiInfo> apis) { this.apis = apis; }
    
    public void addApi(ApiInfo api) { this.apis.add(api); }

    public Map<String, SymbolDefinition> getSymbolTable() { return symbolTable; }
    public void setSymbolTable(Map<String, SymbolDefinition> symbolTable) { this.symbolTable = symbolTable; }
    
    public void addSymbol(String symbolId, SymbolDefinition symbol) {
        this.symbolTable.put(symbolId, symbol);
    }
    
    public SymbolDefinition getSymbol(String symbolId) {
        return this.symbolTable.get(symbolId);
    }

    public AnalysisMetadata getMetadata() { return metadata; }
    public void setMetadata(AnalysisMetadata metadata) { this.metadata = metadata; }
} 