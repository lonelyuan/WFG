package SA.tool.visitor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import SA.tool.model.CallGraphNode;

import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

public class CallGraphVisitor extends VoidVisitorAdapter<Map<String, CallGraphNode>> {
    
    private final Path rootPath;
    private final boolean isFirstPass;
    private String currentClassName = "";
    private String currentMethodSignature = "";

    public CallGraphVisitor(Path rootPath, boolean isFirstPass) {
        this.rootPath = rootPath;
        this.isFirstPass = isFirstPass;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Map<String, CallGraphNode> arg) {
        String previousClassName = currentClassName;
        currentClassName = n.getNameAsString();
        super.visit(n, arg);
        currentClassName = previousClassName;
    }

    @Override
    public void visit(MethodDeclaration n, Map<String, CallGraphNode> arg) {
        String methodSignature = buildMethodSignature(n);
        String previousMethodSignature = currentMethodSignature;
        currentMethodSignature = methodSignature;

        if (isFirstPass) {
            // 第一遍：收集方法定义
            if (!arg.containsKey(methodSignature)) {
                CallGraphNode node = new CallGraphNode();
                node.setMethodSignature(methodSignature);
                node.setClassName(currentClassName);
                node.setMethodName(n.getNameAsString());
                
                // 设置代码位置
                String relativePath = getRelativePath(n);
                int startLine = n.getBegin().map(pos -> pos.line).orElse(-1);
                int endLine = n.getEnd().map(pos -> pos.line).orElse(-1);
                node.setCodePos(String.format("%s:L%d-L%d", relativePath, startLine, endLine));
                
                arg.put(methodSignature, node);
            }
        }
        
        super.visit(n, arg);
        currentMethodSignature = previousMethodSignature;
    }

    @Override
    public void visit(MethodCallExpr n, Map<String, CallGraphNode> arg) {
        if (!isFirstPass && !currentMethodSignature.isEmpty()) {
            // 第二遍：分析方法调用关系
            String calledMethodName = n.getNameAsString();
            
            // 简化的方法匹配：只根据方法名匹配
            // 实际实现中需要更复杂的类型解析
            for (String signature : arg.keySet()) {
                if (signature.contains("." + calledMethodName + "(")) {
                    CallGraphNode callerNode = arg.get(currentMethodSignature);
                    CallGraphNode calleeNode = arg.get(signature);
                    
                    if (callerNode != null && calleeNode != null) {
                        // 添加调用关系
                        if (!callerNode.getCallees().contains(signature)) {
                            callerNode.getCallees().add(signature);
                        }
                        if (!calleeNode.getCallers().contains(currentMethodSignature)) {
                            calleeNode.getCallers().add(currentMethodSignature);
                        }
                        
                        // 添加调用位置信息
                        CallGraphNode.CallSite callSite = new CallGraphNode.CallSite();
                        callSite.setTargetMethod(signature);
                        
                        String relativePath = getRelativePath(n);
                        int line = n.getBegin().map(pos -> pos.line).orElse(-1);
                        int column = n.getBegin().map(pos -> pos.column).orElse(-1);
                        callSite.setCodePos(String.format("%s:L%d:C%d", relativePath, line, column));
                        callSite.setCallType("DIRECT"); // 简化实现
                        
                        callerNode.getCallSites().add(callSite);
                    }
                    break; // 找到第一个匹配的方法即停止
                }
            }
        }
        super.visit(n, arg);
    }

    private String buildMethodSignature(MethodDeclaration method) {
        String parameters = method.getParameters().stream()
                .map(param -> param.getType().toString())
                .collect(Collectors.joining(", "));
        
        return String.format("%s.%s(%s)", 
                currentClassName, 
                method.getNameAsString(), 
                parameters);
    }

    private String getRelativePath(com.github.javaparser.ast.Node node) {
        return node.findCompilationUnit().flatMap(CompilationUnit::getStorage)
                .map(s -> {
                    String rootAbs = rootPath.toAbsolutePath().toString();
                    String fileAbs = s.getPath().toAbsolutePath().toString();
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
} 