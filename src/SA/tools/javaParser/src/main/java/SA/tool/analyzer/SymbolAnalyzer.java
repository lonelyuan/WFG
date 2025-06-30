package SA.tool.analyzer;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import SA.tool.model.ApiInfo;
import SA.tool.model.ControllerAnalysisResult;
import SA.tool.model.HttpRequest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SymbolAnalyzer {

    public ControllerAnalysisResult analyzeController(CompilationUnit cu, String controllerName, String filePath) {
        ControllerAnalysisResult result = new ControllerAnalysisResult(controllerName, filePath);
        
        Optional<ClassOrInterfaceDeclaration> controllerClassOpt = cu.findFirst(ClassOrInterfaceDeclaration.class,
                clazz -> clazz.isAnnotationPresent("RestController") || clazz.isAnnotationPresent("Controller"));

        if (controllerClassOpt.isPresent()) {
            ClassOrInterfaceDeclaration controllerClass = controllerClassOpt.get();
            ControllerMethodVisitor methodVisitor = new ControllerMethodVisitor(result, controllerClass, cu);
            methodVisitor.visit(controllerClass, null);
        }

        return result;
    }

    private static class ControllerMethodVisitor extends VoidVisitorAdapter<Void> {
        private final ControllerAnalysisResult result;
        private final ClassOrInterfaceDeclaration controllerClass;
        private final CompilationUnit cu;
        private final Map<String, String> importMap = new HashMap<>();
        private final Map<String, String> classFields = new HashMap<>(); // field name -> type

        private static final Set<String> API_MAPPING_ANNOTATIONS = new HashSet<>(Arrays.asList(
            "GetMapping", "PostMapping", "PutMapping", "DeleteMapping", "RequestMapping", "PatchMapping"
        ));

        public ControllerMethodVisitor(ControllerAnalysisResult result, ClassOrInterfaceDeclaration controllerClass, CompilationUnit cu) {
            this.result = result;
            this.controllerClass = controllerClass;
            this.cu = cu;
            buildImportMap();
            buildClassFields();
        }

        private void buildImportMap() {
            for (ImportDeclaration importDeclaration : cu.getImports()) {
                String name = importDeclaration.getNameAsString();
                importMap.put(name.substring(name.lastIndexOf('.') + 1), name);
            }
        }

        private void buildClassFields() {
            for (FieldDeclaration field : controllerClass.getFields()) {
                for (VariableDeclarator variable : field.getVariables()) {
                    classFields.put(variable.getNameAsString(), variable.getTypeAsString());
                }
            }
        }

        @Override
        public void visit(MethodDeclaration method, Void arg) {
            if (isApiMethod(method)) {
                ApiInfo apiInfo = createApiInfo(method, result.getControllerName(), result.getFilePath());
                
                // Simplified analysis
                method.findAll(MethodCallExpr.class).forEach(call -> {
                    call.getScope().ifPresent(scope -> {
                        if (scope instanceof NameExpr) {
                            String varName = ((NameExpr) scope).getNameAsString();
                            if (classFields.containsKey(varName)) {
                                String type = classFields.get(varName);
                                String fqn = importMap.getOrDefault(type, type);
                                apiInfo.addReference(fqn + "." + call.getNameAsString());
                            }
                        }
                    });
                });

                method.findAll(ObjectCreationExpr.class).forEach(creation -> {
                    String type = creation.getType().getNameAsString();
                    String fqn = importMap.getOrDefault(type, type);
                    apiInfo.addReference(fqn);
                });

                result.addApi(apiInfo);
            }
        }

        private boolean isApiMethod(MethodDeclaration method) {
            return method.getAnnotations().stream()
                       .anyMatch(a -> API_MAPPING_ANNOTATIONS.contains(a.getNameAsString()));
        }
        
        private ApiInfo createApiInfo(MethodDeclaration method, String controllerName, String filePath) {
            ApiInfo apiInfo = new ApiInfo();
            apiInfo.setControllerName(controllerName);
            apiInfo.setMethodName(method.getNameAsString());

            int startLine = method.getBegin().map(pos -> pos.line).orElse(-1);
            int endLine = method.getEnd().map(pos -> pos.line).orElse(-1);
            apiInfo.setCodePos(String.format("%s:L%d-L%d", filePath, startLine, endLine));

            HttpRequest httpRequest = new HttpRequest();
            
            // Analyze annotations for path, method, etc.
            method.getAnnotations().stream()
                .filter(a -> API_MAPPING_ANNOTATIONS.contains(a.getNameAsString()))
                .findFirst()
                .ifPresent(annotation -> {
                    // Set HTTP method
                    String annotationName = annotation.getNameAsString();
                    if (annotationName.endsWith("Mapping")) {
                        httpRequest.setMethod(annotationName.replace("Mapping", "").toUpperCase());
                    }
                    if (annotationName.equals("RequestMapping")) {
                        httpRequest.setMethod("ANY"); // Default or could be more specific
                    }

                    // Extract path
                    extractAnnotationValue(annotation, "value")
                        .or(() -> extractAnnotationValue(annotation, "path"))
                        .ifPresent(httpRequest::setPath);
                });

            // Analyze parameters for @RequestParam and @RequestBody
            for (Parameter parameter : method.getParameters()) {
                // Query parameters
                parameter.getAnnotationByName("RequestParam").ifPresent(annotation -> {
                    String paramName = extractAnnotationValue(annotation, "value")
                                         .orElse(parameter.getNameAsString());
                    httpRequest.addQueryParam(paramName, parameter.getTypeAsString());
                });

                // Request body
                parameter.getAnnotationByName("RequestBody").ifPresent(annotation -> {
                    String bodyType = parameter.getTypeAsString();
                    String fqn = importMap.getOrDefault(bodyType, bodyType);
                    httpRequest.setBody(Map.of("type", fqn));
                });
            }

            apiInfo.setReq(httpRequest);

            return apiInfo;
        }

        private Optional<String> extractAnnotationValue(AnnotationExpr annotation, String key) {
            if (annotation instanceof SingleMemberAnnotationExpr) {
                Expression memberValue = ((SingleMemberAnnotationExpr) annotation).getMemberValue();
                if (memberValue instanceof StringLiteralExpr) {
                    return Optional.of(((StringLiteralExpr) memberValue).asString());
                }
            } else if (annotation instanceof NormalAnnotationExpr) {
                for (MemberValuePair pair : ((NormalAnnotationExpr) annotation).getPairs()) {
                    if (pair.getNameAsString().equals(key)) {
                        Expression valueExpr = pair.getValue();
                        if (valueExpr instanceof StringLiteralExpr) {
                            return Optional.of(((StringLiteralExpr) valueExpr).asString());
                        }
                    }
                }
            }
            return Optional.empty();
        }
    }
} 