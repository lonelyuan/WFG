package SA.tool.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class ControllerAnalysisResult {
    @JsonProperty("controller_name")
    private String controllerName;
    
    @JsonProperty("file_path")
    private String filePath;
    
    @JsonProperty("apis")
    private List<ApiInfo> apis = new ArrayList<>();

    // Constructors
    public ControllerAnalysisResult() {}
    
    public ControllerAnalysisResult(String controllerName, String filePath) {
        this.controllerName = controllerName;
        this.filePath = filePath;
    }

    // Getters and Setters
    public String getControllerName() { return controllerName; }
    public void setControllerName(String controllerName) { this.controllerName = controllerName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public List<ApiInfo> getApis() { return apis; }
    public void setApis(List<ApiInfo> apis) { this.apis = apis; }
    
    public void addApi(ApiInfo api) { this.apis.add(api); }
} 