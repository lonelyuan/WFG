package SA.tool.analyzer;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import SA.tool.model.*;

import java.util.*;

public class SymbolAnalyzer {
    
    private static final Set<String> KNOWN_JAVA_TYPES = Set.of(
        "String", "Integer", "Long", "Double", "Float", "Boolean", "Character",
        "List", "Map", "Set", "HashMap", "ArrayList", "HashSet",
        "Object", "Class", "System", "Math", "Optional"
    );
    
    private static final Set<String> KNOWN_SPRING_TYPES = Set.of(
        "ResponseEntity", "HttpStatus", "RequestBody", "PathVariable", 
        "RequestParam", "RequestHeader", "RestController", "Controller",
        "GetMapping", "PostMapping", "PutMapping", "DeleteMapping", "RequestMapping"
    );
    
    private Set<String> projectImports = new HashSet<>();
    private Map<String, String> importMap = new HashMap<>();
    
    public ControllerAnalysisResult analyzeController(CompilationUnit cu, String controllerName, String filePath) {
        // 分析imports
        analyzeImports(cu);
        
        ControllerAnalysisResult result = new ControllerAnalysisResult(controllerName, filePath);
        
        // 找到Controller类
        Optional<ClassOrInterfaceDeclaration> controllerClass = cu.findFirst(ClassOrInterfaceDeclaration.class, 
            clazz -> isController(clazz) && clazz.getNameAsString().equals(controllerName));
        
        if (controllerClass.isPresent()) {
            // 分析类级别的符号
            Map<String, SymbolDefinition> classSymbols = analyzeClassLevelSymbols(controllerClass.get(), result);
            
            // 分析API方法
            ControllerMethodVisitor methodVisitor = new ControllerMethodVisitor(result, classSymbols);
            methodVisitor.visit(controllerClass.get(), null);
            
            // 更新元数据
            AnalysisMetadata metadata = result.getMetadata();
            metadata.setTotalApis(result.getApis().size());
            metadata.setTotalSymbols(result.getSymbolTable().size());
        }
        
        return result;
    }
    
    private void analyzeImports(CompilationUnit cu) {
        projectImports.clear();
        importMap.clear();
        
        for (ImportDeclaration importDecl : cu.getImports()) {
            String importName = importDecl.getNameAsString();
            projectImports.add(importName);
            
            // 简单类名到全限定名的映射
            String simpleName = importName.substring(importName.lastIndexOf('.') + 1);
            importMap.put(simpleName, importName);
        }
    }
    
    private Map<String, SymbolDefinition> analyzeClassLevelSymbols(ClassOrInterfaceDeclaration controllerClass, 
                                                                   ControllerAnalysisResult result) {
        Map<String, SymbolDefinition> classSymbols = new HashMap<>();
        Map<String, String> symbolKeyToId = new HashMap<>();
        
        // 分析字段
        for (FieldDeclaration field : controllerClass.getFields()) {
            for (VariableDeclarator var : field.getVariables()) {
                String fieldName = var.getNameAsString();
                String dataType = var.getType().asString();
                
                if (isCustomType(dataType)) {
                    String symbolKey = SymbolIdGenerator.generateSymbolKey(fieldName, "FIELD", dataType, "CLASS_LEVEL");
                    
                    if (!symbolKeyToId.containsKey(symbolKey)) {
                        String symbolId = SymbolIdGenerator.generateSymbolId(fieldName, "FIELD", controllerClass.getNameAsString());
                        
                        SymbolDefinition symbol = new SymbolDefinition(symbolId, fieldName, "FIELD", dataType, "CLASS_LEVEL");
                        
                        // 设置定义位置
                        int lineNumber = var.getBegin().map(pos -> pos.line).orElse(-1);
                        String codeSnippet = field.toString().trim();
                        symbol.setDefinition(new SymbolLocation(result.getFilePath(), lineNumber, codeSnippet));
                        
                        classSymbols.put(fieldName, symbol);
                        result.addSymbol(symbolId, symbol);
                        symbolKeyToId.put(symbolKey, symbolId);
                    }
                }
            }
        }
        
        return classSymbols;
    }
    
    private boolean isController(ClassOrInterfaceDeclaration clazz) {
        return clazz.isAnnotationPresent("RestController") || clazz.isAnnotationPresent("Controller");
    }
    
    private boolean isCustomType(String typeName) {
        // 移除泛型部分
        String baseType = typeName.split("<")[0];
        
        // 检查是否为已知类型
        if (KNOWN_JAVA_TYPES.contains(baseType) || KNOWN_SPRING_TYPES.contains(baseType)) {
            return false;
        }
        
        // 检查是否为基本类型
        if (baseType.equals("int") || baseType.equals("long") || baseType.equals("double") || 
            baseType.equals("float") || baseType.equals("boolean") || baseType.equals("char") ||
            baseType.equals("byte") || baseType.equals("short") || baseType.equals("void")) {
            return false;
        }
        
        // 检查是否为标准库包
        String fullType = importMap.getOrDefault(baseType, baseType);
        if (fullType.startsWith("java.") || fullType.startsWith("javax.") || 
            fullType.startsWith("org.springframework.")) {
            return false;
        }
        
        return true;
    }
    
    private class ControllerMethodVisitor extends VoidVisitorAdapter<Void> {
        private final ControllerAnalysisResult result;
        private final Map<String, SymbolDefinition> classSymbols;
        private final Map<String, String> symbolKeyToId = new HashMap<>();
        
        private String currentMethodName;
        
        public ControllerMethodVisitor(ControllerAnalysisResult result, Map<String, SymbolDefinition> classSymbols) {
            this.result = result;
            this.classSymbols = classSymbols;
        }
        
        @Override
        public void visit(MethodDeclaration method, Void arg) {
            if (isApiMethod(method)) {
                this.currentMethodName = method.getNameAsString();
                
                // 创建API信息
                ApiInfo apiInfo = createApiInfo(method, result.getControllerName(), result.getFilePath());
                
                // 分析方法中的符号
                MethodSymbolVisitor symbolVisitor = new MethodSymbolVisitor();
                symbolVisitor.visit(method, null);
                
                result.addApi(apiInfo);
            }
        }
        
        private boolean isApiMethod(MethodDeclaration method) {
            return method.isAnnotationPresent("GetMapping") || method.isAnnotationPresent("PostMapping") ||
                   method.isAnnotationPresent("PutMapping") || method.isAnnotationPresent("DeleteMapping") ||
                   method.isAnnotationPresent("PatchMapping") || method.isAnnotationPresent("RequestMapping");
        }
        
        private ApiInfo createApiInfo(MethodDeclaration method, String controllerName, String filePath) {
            ApiInfo apiInfo = new ApiInfo();
            apiInfo.setControllerName(controllerName);
            apiInfo.setMethodName(method.getNameAsString());
            
            // 设置代码位置
            int startLine = method.getBegin().map(pos -> pos.line).orElse(-1);
            int endLine = method.getEnd().map(pos -> pos.line).orElse(-1);
            apiInfo.setCodePos(String.format("%s:L%d-L%d", filePath, startLine, endLine));
            
            // 创建HttpRequest（简化版本）
            HttpRequest httpRequest = new HttpRequest();
            httpRequest.setMethod("GET"); // 简化处理
            httpRequest.setPath("/" + method.getNameAsString()); // 简化处理
            apiInfo.setReq(httpRequest);
            
            return apiInfo;
        }
        
        private class MethodSymbolVisitor extends VoidVisitorAdapter<Void> {
            
            @Override
            public void visit(FieldAccessExpr expr, Void arg) {
                if (expr.getScope() instanceof NameExpr) {
                    String scopeName = ((NameExpr) expr.getScope()).getNameAsString();
                    String fieldName = expr.getNameAsString();
                    
                    // 检查是否为类成员变量
                    if (classSymbols.containsKey(scopeName)) {
                        SymbolDefinition classSymbol = classSymbols.get(scopeName);
                        
                        // 添加使用上下文
                        int usageLine = expr.getBegin().map(pos -> pos.line).orElse(-1);
                        SymbolUsage usage = new SymbolUsage(currentMethodName, "FIELD_ACCESS", usageLine, expr.toString());
                        classSymbol.addUsageContext(usage);
                        
                        // 添加到API的引用列表
                        addSymbolReference(classSymbol.getSymbolId());
                        
                        // 分析方法调用
                        analyzeMethodCall(scopeName, fieldName, classSymbol.getDataType(), expr);
                    }
                }
                super.visit(expr, arg);
            }
            
            @Override
            public void visit(MethodCallExpr expr, Void arg) {
                if (expr.getScope().isPresent() && expr.getScope().get() instanceof NameExpr) {
                    String scopeName = ((NameExpr) expr.getScope().get()).getNameAsString();
                    String methodName = expr.getNameAsString();
                    
                    if (classSymbols.containsKey(scopeName)) {
                        SymbolDefinition classSymbol = classSymbols.get(scopeName);
                        analyzeMethodCall(scopeName, methodName, classSymbol.getDataType(), expr);
                    }
                }
                super.visit(expr, arg);
            }
            
            @Override
            public void visit(ObjectCreationExpr expr, Void arg) {
                String typeName = expr.getType().asString();
                if (isCustomType(typeName)) {
                    String symbolKey = SymbolIdGenerator.generateSymbolKey(typeName, "CLASS", typeName, "EXTERNAL");
                    
                    if (!symbolKeyToId.containsKey(symbolKey)) {
                        String symbolId = SymbolIdGenerator.generateSymbolId(typeName, "CLASS", currentMethodName);
                        
                        SymbolDefinition symbol = new SymbolDefinition(symbolId, typeName, "CLASS", typeName, "EXTERNAL");
                        
                        int usageLine = expr.getBegin().map(pos -> pos.line).orElse(-1);
                        SymbolUsage usage = new SymbolUsage(currentMethodName, "INSTANTIATION", usageLine, expr.toString());
                        symbol.addUsageContext(usage);
                        
                        result.addSymbol(symbolId, symbol);
                        symbolKeyToId.put(symbolKey, symbolId);
                        addSymbolReference(symbolId);
                    } else {
                        // 添加使用上下文到现有符号
                        String symbolId = symbolKeyToId.get(symbolKey);
                        SymbolDefinition symbol = result.getSymbol(symbolId);
                        if (symbol != null) {
                            int usageLine = expr.getBegin().map(pos -> pos.line).orElse(-1);
                            SymbolUsage usage = new SymbolUsage(currentMethodName, "INSTANTIATION", usageLine, expr.toString());
                            symbol.addUsageContext(usage);
                            addSymbolReference(symbolId);
                        }
                    }
                }
                super.visit(expr, arg);
            }
            
            private void analyzeMethodCall(String scopeName, String methodName, String scopeType, Expression expr) {
                String symbolKey = SymbolIdGenerator.generateSymbolKey(methodName, "METHOD", "Unknown", "EXTERNAL");
                
                if (!symbolKeyToId.containsKey(symbolKey)) {
                    String symbolId = SymbolIdGenerator.generateSymbolId(methodName, "METHOD", scopeType);
                    
                    SymbolDefinition symbol = new SymbolDefinition(symbolId, methodName, "METHOD", "Unknown", "EXTERNAL");
                    
                    int usageLine = expr.getBegin().map(pos -> pos.line).orElse(-1);
                    SymbolUsage usage = new SymbolUsage(currentMethodName, "METHOD_CALL", usageLine, expr.toString());
                    symbol.addUsageContext(usage);
                    
                    result.addSymbol(symbolId, symbol);
                    symbolKeyToId.put(symbolKey, symbolId);
                    addSymbolReference(symbolId);
                } else {
                    // 添加使用上下文到现有符号
                    String symbolId = symbolKeyToId.get(symbolKey);
                    SymbolDefinition symbol = result.getSymbol(symbolId);
                    if (symbol != null) {
                        int usageLine = expr.getBegin().map(pos -> pos.line).orElse(-1);
                        SymbolUsage usage = new SymbolUsage(currentMethodName, "METHOD_CALL", usageLine, expr.toString());
                        symbol.addUsageContext(usage);
                        addSymbolReference(symbolId);
                    }
                }
            }
            
            private void addSymbolReference(String symbolId) {
                // 获取当前正在分析的API
                if (!result.getApis().isEmpty()) {
                    ApiInfo currentApi = result.getApis().get(result.getApis().size() - 1);
                    if (!currentApi.getReferences().contains(symbolId)) {
                        currentApi.addReference(symbolId);
                    }
                }
            }
        }
    }
} 