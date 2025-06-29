package SA.tool;

import SA.tool.analyzer.ApiExtractor;
import SA.tool.analyzer.ReferenceFinder;
import SA.tool.analyzer.CallGraphAnalyzer;
import SA.tool.analyzer.DefinitionFinder;
import SA.tool.model.ApiInfo;
import SA.tool.model.ReferenceInfo;
import SA.tool.model.CallGraphNode;
import SA.tool.model.DefinitionInfo;
import SA.tool.visualizer.CallGraphVisualizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
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
            switch (command.toUpperCase()) {
                case "API":
                    handleApiExtraction(projectPath, args);
                    break;
                case "REF":
                    handleReferenceFind(projectPath, args);
                    break;
                case "CG":
                    handleCallGraphAnalysis(projectPath, args);
                    break;
                case "DEF":
                    handleDefinitionFind(projectPath, args);
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
        if (outputPath != null) {
            Path outputDir = Paths.get(outputPath);
            extractor.extractApiInfo(projectPath, outputDir);
            System.out.println("Enhanced API analysis completed. Results saved to " + outputPath + "/data/API/");
        } else {
            List<ApiInfo> apiInfos = extractor.extractApiInfo(projectPath, null);
            writeOutput(apiInfos, null, "API info");
        }
    }
    
    private static void handleReferenceFind(String projectPath, String[] args) throws IOException {
        if (args.length < 4 || !args[2].equals("-s")) {
            System.err.println("Usage: java -jar <jar-file> <project-path> REF -s <symbol> [-f <file> -l <line>] [-o <output-file>]");
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
        String outputPath = null;
        String imageOutputPath = null;
        String layoutEngine = "DOT";
        boolean removeIsolated = true;
        String classFilter = null;
        boolean skipImage = false;
        
        // 解析参数
        for (int i = 2; i < args.length; i++) {
            switch (args[i]) {
                case "-o":
                    if (i + 1 < args.length) outputPath = args[++i];
                    break;
                case "-img":
                    if (i + 1 < args.length) imageOutputPath = args[++i];
                    break;
                case "-layout":
                    if (i + 1 < args.length) layoutEngine = args[++i].toUpperCase();
                    break;
                case "-filter":
                    if (i + 1 < args.length) classFilter = args[++i];
                    break;
                case "-keep-isolated":
                    removeIsolated = false;
                    break;
                case "-no-image":
                    skipImage = true;
                    break;
            }
        }

        CallGraphAnalyzer analyzer = new CallGraphAnalyzer();
        Map<String, CallGraphNode> callGraph = analyzer.buildCallGraph(projectPath);
        writeOutput(callGraph, outputPath, "Call graph");
        if (!skipImage) {
            if (imageOutputPath == null) {
                if (outputPath != null) {
                    imageOutputPath = outputPath.replaceAll("\\.(json|txt)$", "") + ".svg";
                    if (imageOutputPath.equals(outputPath)) {
                        imageOutputPath = outputPath + ".svg";
                    }
                } else {
                    imageOutputPath = "call_graph.svg";
                }
            }
            try {
                CallGraphVisualizer visualizer = new CallGraphVisualizer(callGraph);
                if (classFilter != null) {
                    String[] filters = classFilter.split(",");
                    for (String filter : filters) {
                        visualizer.addClassFilter(filter.trim());
                    }
                }
                CallGraphVisualizer.LayoutEngine layout;
                try {
                    layout = CallGraphVisualizer.LayoutEngine.valueOf(layoutEngine);
                } catch (IllegalArgumentException e) {
                    System.err.println("Unknown layout engine: " + layoutEngine + ", using DOT");
                    layout = CallGraphVisualizer.LayoutEngine.DOT;
                }
                visualizer.visualize(imageOutputPath, layout, removeIsolated);
            } catch (Exception e) {
                System.err.println("Warning: Failed to generate visualization: " + e.getMessage());
            }
        }
    }
    
    private static void handleDefinitionFind(String projectPath, String[] args) throws IOException {
        if (args.length < 4 || !args[2].equals("-s")) {
            System.err.println("Usage: java -jar <jar-file> <project-path> DEF -s <symbol> [-o <output-file>]");
            System.exit(1);
            return;
        }
        
        String symbolName = args[3];
        String outputPath = getOutputPath(args, 4);
        DefinitionFinder finder = new DefinitionFinder();
        List<DefinitionInfo> definitions = finder.findDefinitions(projectPath, symbolName);
        writeOutput(definitions, outputPath, "Definitions");
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
        System.err.println("  java -jar <jar-file> <project-path> API [-o <output-file>]");
        System.err.println("  java -jar <jar-file> <project-path> REF -s <symbol> [-f <file> -l <line>] [-o <output-file>]");
        System.err.println("  java -jar <jar-file> <project-path> CG [-o <output-file>] [-img <image-file>] [-layout <engine>] [-filter <classes>] [-keep-isolated] [-no-image]");
        System.err.println("  java -jar <jar-file> <project-path> DEF -s <symbol> [-o <output-file>]");
    }
} 