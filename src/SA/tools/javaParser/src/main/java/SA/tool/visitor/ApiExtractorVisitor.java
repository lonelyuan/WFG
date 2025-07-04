package SA.tool.visitor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import SA.tool.model.ApiInfo;
import SA.tool.model.HttpRequest;
import com.github.javaparser.resolution.UnsolvedSymbolException;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ApiExtractorVisitor extends VoidVisitorAdapter<List<ApiInfo>> {

    private static final List<String> CONTROLLER_ANNOTATIONS = Arrays.asList(
            "RestController", "Controller"
    );

    private static final List<String> MAPPING_ANNOTATIONS = Arrays.asList(
            "RequestMapping", "GetMapping", "PostMapping", "PutMapping", "DeleteMapping", "PatchMapping"
    );

    private final Path rootPath;
    private List<String> classLevelPaths = new ArrayList<>();

    public ApiExtractorVisitor(Path rootPath) {
        this.rootPath = rootPath;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, List<ApiInfo> arg) {
        boolean isController = CONTROLLER_ANNOTATIONS.stream().anyMatch(n::isAnnotationPresent);
        if (isController) {
            this.classLevelPaths = extractPathsFromAnnotation(n.getAnnotationByName("RequestMapping"));
            super.visit(n, arg);
            this.classLevelPaths.clear();
        }
    }

    @Override
    public void visit(MethodDeclaration n, List<ApiInfo> arg) {
        for (String annotationName : MAPPING_ANNOTATIONS) {
            if (n.isAnnotationPresent(annotationName)) {
                Optional<AnnotationExpr> annotationOpt = n.getAnnotationByName(annotationName);
                if (annotationOpt.isPresent()) {
                    ApiInfo apiInfo = new ApiInfo();

                    // Set basic info
                    apiInfo.setControllerName(n.findAncestor(ClassOrInterfaceDeclaration.class).map(ClassOrInterfaceDeclaration::getNameAsString).orElse(""));
                    apiInfo.setMethodName(n.getNameAsString());

                    // Set code position
                    int endLine = n.getEnd().map(p -> p.line).orElse(-1);
                    int startLine = n.getBegin().map(p -> p.line).orElse(endLine);
                    if (n.getJavadocComment().isPresent()) {
                        startLine = n.getJavadocComment().get().getBegin().map(p -> p.line).orElse(startLine);
                    } else if (!n.getAnnotations().isEmpty()) {
                        int firstAnnotationLine = n.getAnnotations().stream()
                                .mapToInt(a -> a.getBegin().map(p -> p.line).orElse(Integer.MAX_VALUE))
                                .min()
                                .orElse(startLine);
                        startLine = Math.min(startLine, firstAnnotationLine);
                    }
                    final String relativePath = n.findCompilationUnit().flatMap(CompilationUnit::getStorage)
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
                    apiInfo.setCodePos(String.format("%s:L%d-L%d", relativePath.replace('\\', '/'), startLine, endLine));

                    // Create and set HttpRequest
                    HttpRequest httpRequest = new HttpRequest();
                    List<String> methodLevelPaths = extractPathsFromAnnotation(annotationOpt);
                    httpRequest.setPath(combinePaths(classLevelPaths, methodLevelPaths));
                    httpRequest.setMethod(getHttpMethod(annotationOpt.get()));
                    // (You would add logic here to parse @RequestBody, @RequestParam etc. and set it in httpRequest)
                    apiInfo.setReq(httpRequest);

                    // --- NEW: Resolve internal symbols ---
                    Set<String> references = new HashSet<>();

                    // 1. Resolve symbols within the method body (method calls and object creations)
                    n.getBody().ifPresent(body -> {
                        body.findAll(MethodCallExpr.class).forEach(call -> {
                            try {
                                references.add(call.resolve().getQualifiedSignature());
                            } catch (UnsolvedSymbolException | UnsupportedOperationException e) {
                                // Ignore unresolved symbols, common for JDK or external libs without full classpath
                            }
                        });
                        body.findAll(ObjectCreationExpr.class).forEach(creation -> {
                            try {
                                references.add(creation.resolve().getQualifiedSignature());
                            } catch (UnsolvedSymbolException | UnsupportedOperationException e) {
                                // Ignore unresolved symbols
                            }
                        });
                    });

                    // 2. Resolve class-level field types
                    n.findAncestor(ClassOrInterfaceDeclaration.class).ifPresent(cls -> {
                        cls.getFields().forEach(field -> {
                            field.getVariables().forEach(variable -> {
                                try {
                                    // Resolve the type of the field and get its fully qualified name
                                    references.add(variable.getType().resolve().asReferenceType().getQualifiedName());
                                } catch (UnsolvedSymbolException | UnsupportedOperationException | ClassCastException e) {
                                    // Ignore unresolved types or primitive types that cause ClassCastException
                                }
                            });
                        });
                    });

                    apiInfo.setReferences(new ArrayList<>(references));
                    // --- END NEW ---

                    arg.add(apiInfo);
                    break;
                }
            }
        }
    }

    private List<String> extractPathsFromAnnotation(Optional<AnnotationExpr> annotationOpt) {
        if (annotationOpt.isEmpty()) {
            return new ArrayList<>();
        }
        AnnotationExpr annotation = annotationOpt.get();

        if (annotation.isStringLiteralExpr()) {
            return new ArrayList<>(Collections.singletonList(annotation.asStringLiteralExpr().asString()));
        }
        if (annotation.isSingleMemberAnnotationExpr()) {
            Expression value = annotation.asSingleMemberAnnotationExpr().getMemberValue();
            return extractPathsFromExpression(value);
        }
        if (annotation.isNormalAnnotationExpr()) {
            return annotation.asNormalAnnotationExpr().getPairs().stream()
                    .filter(pair -> "value".equals(pair.getNameAsString()) || "path".equals(pair.getNameAsString()))
                    .findFirst()
                    .map(pair -> extractPathsFromExpression(pair.getValue()))
                    .orElse(new ArrayList<>());
        }
        return new ArrayList<>();
    }

    private List<String> extractPathsFromExpression(Expression expression) {
        if (expression.isStringLiteralExpr()) {
            return new ArrayList<>(Collections.singletonList(expression.asStringLiteralExpr().asString()));
        }
        if (expression.isArrayInitializerExpr()) {
            return expression.asArrayInitializerExpr().getValues().stream()
                    .filter(Expression::isStringLiteralExpr)
                    .map(val -> val.asStringLiteralExpr().asString())
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        return new ArrayList<>();
    }

    private String getHttpMethod(AnnotationExpr annotation) {
        String annotationName = annotation.getNameAsString();
        if ("RequestMapping".equals(annotationName)) {
            if (annotation.isNormalAnnotationExpr()) {
                return annotation.asNormalAnnotationExpr().getPairs().stream()
                        .filter(p -> "method".equals(p.getNameAsString()))
                        .findFirst()
                        .map(p -> p.getValue().toString().substring(p.getValue().toString().lastIndexOf('.') + 1))
                        .orElse("ANY");
            }
            return "ANY";
        }
        return annotationName.replace("Mapping", "").toUpperCase();
    }

    private String combinePaths(List<String> classPaths, List<String> methodPaths) {
        String classPath = classPaths.isEmpty() ? "" : classPaths.get(0);
        String methodPath = methodPaths.isEmpty() ? "" : methodPaths.get(0);

        String combined = Stream.of(classPath, methodPath)
                .map(p -> p.startsWith("/") ? p : "/" + p)
                .map(p -> p.endsWith("/") ? p.substring(0, p.length() - 1) : p)
                .collect(Collectors.joining());

        return combined.isEmpty() ? "/" : combined;
    }
} 