package SA.tool.analyzer;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.utils.SourceRoot;
import SA.tool.model.CallGraphNode;
import SA.tool.visitor.CallGraphVisitor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CallGraphAnalyzer {
    
    public Map<String, CallGraphNode> buildCallGraph(String targetPath) throws IOException {
        Map<String, CallGraphNode> callGraph = new HashMap<>();
        Path rootPath = Paths.get(targetPath).normalize();

        SourceRoot sourceRoot = new SourceRoot(rootPath);
        List<ParseResult<CompilationUnit>> parseResults = sourceRoot.tryToParse("");

        // 第一遍：收集所有方法定义
        for (ParseResult<CompilationUnit> parseResult : parseResults) {
            if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                CompilationUnit cu = parseResult.getResult().get();
                CallGraphVisitor visitor = new CallGraphVisitor(rootPath, true); // 第一遍扫描
                visitor.visit(cu, callGraph);
            }
        }

        // 第二遍：分析方法调用关系
        for (ParseResult<CompilationUnit> parseResult : parseResults) {
            if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                CompilationUnit cu = parseResult.getResult().get();
                CallGraphVisitor visitor = new CallGraphVisitor(rootPath, false); // 第二遍扫描
                visitor.visit(cu, callGraph);
            }
        }

        return callGraph;
    }
    
    public CallGraphNode analyzeMethod(String targetPath, String methodSignature) throws IOException {
        Map<String, CallGraphNode> callGraph = buildCallGraph(targetPath);
        return callGraph.get(methodSignature);
    }
} 