package SA.tool;

import SA.tool.analyzer.ApiExtractor;
import SA.tool.analyzer.ReferenceFinder;
import SA.tool.analyzer.CallGraphAnalyzer;
import SA.tool.model.ApiInfo;
import SA.tool.model.ReferenceInfo;
import SA.tool.model.CallGraphNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            System.exit(1);
            return;
        }

        String projectPath = args[0];
        String command = args[1];
        
        try {
            switch (command.toLowerCase()) {
                case "api":
                    handleApiExtraction(projectPath, args);
                    break;
                case "reference":
                    handleReferenceFind(projectPath, args);
                    break;
                case "callgraph":
                    handleCallGraphAnalysis(projectPath, args);
                    break;
                default:
                    System.err.println("Unknown command: " + command);
                    printUsage();
                    System.exit(1);
            }
        } catch (IOException e) {
            System.err.println("Error processing the project: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void handleApiExtraction(String projectPath, String[] args) throws IOException {
        String outputPath = getOutputPath(args, 2);
        
        ApiExtractor extractor = new ApiExtractor();
        List<ApiInfo> apiInfos = extractor.extractApiInfo(projectPath);
        
        writeOutput(apiInfos, outputPath, "API info");
    }
    
    private static void handleReferenceFind(String projectPath, String[] args) throws IOException {
        if (args.length < 4 || !args[2].equals("-s")) {
            System.err.println("Usage: java -jar <jar-file> <project-path> reference -s <symbol> [-f <file> -l <line>] [-o <output-file>]");
            System.exit(1);
            return;
        }
        
        String symbolName = args[3];
        String targetFile = null;
        int targetLine = -1;
        String outputPath = null;
        
        // 解析可选参数
        for (int i = 4; i < args.length; i++) {
            switch (args[i]) {
                case "-f":
                    if (i + 1 < args.length) targetFile = args[++i];
                    break;
                case "-l":
                    if (i + 1 < args.length) {
                        try {
                            targetLine = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid line number: " + args[i]);
                            System.exit(1);
                        }
                    }
                    break;
                case "-o":
                    if (i + 1 < args.length) outputPath = args[++i];
                    break;
            }
        }
        
        ReferenceFinder finder = new ReferenceFinder();
        List<ReferenceInfo> references = finder.findReferences(projectPath, symbolName, targetFile, targetLine);
        
        writeOutput(references, outputPath, "References");
    }
    
    private static void handleCallGraphAnalysis(String projectPath, String[] args) throws IOException {
        String outputPath = getOutputPath(args, 2);
        
        CallGraphAnalyzer analyzer = new CallGraphAnalyzer();
        Map<String, CallGraphNode> callGraph = analyzer.buildCallGraph(projectPath);
        
        writeOutput(callGraph, outputPath, "Call graph");
    }
    
    private static String getOutputPath(String[] args, int startIndex) {
        for (int i = startIndex; i < args.length - 1; i++) {
            if ("-o".equals(args[i])) {
                return args[i + 1];
            }
        }
        return null;
    }
    
    private static void writeOutput(Object data, String outputPath, String description) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        if (outputPath != null) {
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputPath), StandardCharsets.UTF_8)) {
                objectMapper.writeValue(writer, data);
            }
            System.out.println(description + " successfully written to " + outputPath);
        } else {
            String jsonOutput = objectMapper.writeValueAsString(data);
            System.out.println(jsonOutput);
        }
    }
    
    private static void printUsage() {
        System.err.println("Usage:");
        System.err.println("  java -jar <jar-file> <project-path> api [-o <output-file>]");
        System.err.println("  java -jar <jar-file> <project-path> reference -s <symbol> [-f <file> -l <line>] [-o <output-file>]");
        System.err.println("  java -jar <jar-file> <project-path> callgraph [-o <output-file>]");
        System.err.println();
        System.err.println("Commands:");
        System.err.println("  api        - Extract API information from Spring controllers");
        System.err.println("  reference  - Find all references to a specified symbol");
        System.err.println("  callgraph  - Build method call graph for the project");
    }
} 