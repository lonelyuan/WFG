package SA.tool.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AnalysisMetadata {
    @JsonProperty("total_apis")
    private int totalApis;
    
    @JsonProperty("total_symbols")
    private int totalSymbols;
    
    @JsonProperty("analysis_timestamp")
    private String analysisTimestamp;
    
    @JsonProperty("unresolved_symbols")
    private List<String> unresolvedSymbols = new ArrayList<>();

    // Constructors
    public AnalysisMetadata() {
        this.analysisTimestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT);
    }

    // Getters and Setters
    public int getTotalApis() { return totalApis; }
    public void setTotalApis(int totalApis) { this.totalApis = totalApis; }

    public int getTotalSymbols() { return totalSymbols; }
    public void setTotalSymbols(int totalSymbols) { this.totalSymbols = totalSymbols; }

    public String getAnalysisTimestamp() { return analysisTimestamp; }
    public void setAnalysisTimestamp(String analysisTimestamp) { this.analysisTimestamp = analysisTimestamp; }

    public List<String> getUnresolvedSymbols() { return unresolvedSymbols; }
    public void setUnresolvedSymbols(List<String> unresolvedSymbols) { this.unresolvedSymbols = unresolvedSymbols; }
    
    public void addUnresolvedSymbol(String symbol) {
        if (!this.unresolvedSymbols.contains(symbol)) {
            this.unresolvedSymbols.add(symbol);
        }
    }
} 