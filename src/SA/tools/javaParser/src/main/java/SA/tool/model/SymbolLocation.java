package SA.tool.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SymbolLocation {
    @JsonProperty("file_path")
    private String filePath;
    
    @JsonProperty("line_number")
    private int lineNumber;
    
    @JsonProperty("code_snippet")
    private String codeSnippet;

    // Constructors
    public SymbolLocation() {}
    
    public SymbolLocation(String filePath, int lineNumber, String codeSnippet) {
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.codeSnippet = codeSnippet;
    }

    // Getters and Setters
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }

    public String getCodeSnippet() { return codeSnippet; }
    public void setCodeSnippet(String codeSnippet) { this.codeSnippet = codeSnippet; }
} 