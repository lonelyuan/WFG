package SA.tool.analyzer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.utils.SourceRoot;
import SA.tool.model.ApiInfo;
import SA.tool.model.ControllerAnalysisResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ApiExtractor {
    private final SymbolAnalyzer symbolAnalyzer = new SymbolAnalyzer();
    
    public List<ApiInfo> extractApiInfo(String targetPath, Path outputDir) throws IOException {
        List<ApiInfo> apiInfos = new ArrayList<>();
        Map<String, ControllerAnalysisResult> controllerResults = new HashMap<>();
        
        Path rootPath = Paths.get(targetPath).normalize();
        SourceRoot sourceRoot = new SourceRoot(rootPath);
        List<ParseResult<CompilationUnit>> parseResults = sourceRoot.tryToParse("");

        for (ParseResult<CompilationUnit> parseResult : parseResults) {
            if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                CompilationUnit cu = parseResult.getResult().get();
                
                // 查找Controller类
                Optional<ClassOrInterfaceDeclaration> controllerClass = findControllerClass(cu);
                
                if (controllerClass.isPresent()) {
                    String controllerName = controllerClass.get().getNameAsString();
                    String filePath = getRelativeFilePath(cu, rootPath);
                    
                    // 使用符号分析器分析整个Controller
                    ControllerAnalysisResult controllerResult = symbolAnalyzer.analyzeController(cu, controllerName, filePath);
                    controllerResults.put(controllerName, controllerResult);
                    
                    // 添加API信息到总列表（保持向后兼容）
                    apiInfos.addAll(controllerResult.getApis());
                }
            }
        }
        
        // 如果指定了输出目录，按Controller分别保存JSON文件
        if (outputDir != null) {
            saveControllerResults(controllerResults, String.valueOf(outputDir));
        }
        
        return apiInfos;
    }
    
    private Optional<ClassOrInterfaceDeclaration> findControllerClass(CompilationUnit cu) {
        return cu.findFirst(ClassOrInterfaceDeclaration.class, clazz -> 
            clazz.isAnnotationPresent("RestController") || clazz.isAnnotationPresent("Controller"));
    }
    
    private String getRelativeFilePath(CompilationUnit cu, Path rootPath) {
        return cu.getStorage()
            .map(storage -> {
                String rootAbs = rootPath.toAbsolutePath().toString();
                String fileAbs = storage.getPath().toAbsolutePath().toString();
                if (fileAbs.startsWith(rootAbs)) {
                    String result = fileAbs.substring(rootAbs.length());
                    if (result.startsWith("\\") || result.startsWith("/")) {
                        return result.substring(1);
                    }
                    return result;
                }
                return fileAbs;
            })
            .orElse("Unknown");
    }
    
    private void saveControllerResults(Map<String, ControllerAnalysisResult> controllerResults, String outputDir) throws IOException {
        // 创建输出目录
        Path outputPath = Paths.get(outputDir, "data", "API");
        Files.createDirectories(outputPath);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        for (Map.Entry<String, ControllerAnalysisResult> entry : controllerResults.entrySet()) {
            String controllerName = entry.getKey();
            ControllerAnalysisResult result = entry.getValue();
            
            File outputFile = outputPath.resolve(controllerName + ".json").toFile();
            mapper.writeValue(outputFile, result);
            
            System.out.println("Saved controller analysis: " + outputFile.getAbsolutePath());
        }
    }
} 