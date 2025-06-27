package SA.tool.visitor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import SA.tool.model.ReferenceInfo;

import java.nio.file.Path;
import java.util.List;

public class ReferenceFinderVisitor extends VoidVisitorAdapter<List<ReferenceInfo>> {
    
    private final Path rootPath;
    private final String symbolName;
    private final String targetFile;
    private final int targetLine;
    private String currentClassName = "";
    private String currentMethodName = "";

    public ReferenceFinderVisitor(Path rootPath, String symbolName, String targetFile, int targetLine) {
        this.rootPath = rootPath;
        this.symbolName = symbolName;
        this.targetFile = targetFile;
        this.targetLine = targetLine;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, List<ReferenceInfo> arg) {
        String previousClassName = currentClassName;
        currentClassName = n.getNameAsString();
        super.visit(n, arg);
        currentClassName = previousClassName;
    }

    @Override
    public void visit(MethodDeclaration n, List<ReferenceInfo> arg) {
        String previousMethodName = currentMethodName;
        currentMethodName = n.getNameAsString();
        super.visit(n, arg);
        currentMethodName = previousMethodName;
    }

    @Override
    public void visit(MethodCallExpr n, List<ReferenceInfo> arg) {
        if (n.getNameAsString().equals(symbolName)) {
            addReference(n, "METHOD_CALL", arg);
        }
        super.visit(n, arg);
    }

    @Override
    public void visit(FieldAccessExpr n, List<ReferenceInfo> arg) {
        if (n.getNameAsString().equals(symbolName)) {
            addReference(n, "FIELD_ACCESS", arg);
        }
        super.visit(n, arg);
    }

    @Override
    public void visit(NameExpr n, List<ReferenceInfo> arg) {
        if (n.getNameAsString().equals(symbolName)) {
            addReference(n, "VARIABLE_ACCESS", arg);
        }
        super.visit(n, arg);
    }

    @Override
    public void visit(ClassOrInterfaceType n, List<ReferenceInfo> arg) {
        if (n.getNameAsString().equals(symbolName)) {
            addReference(n.getNameAsExpression(), "TYPE_REFERENCE", arg);
        }
        super.visit(n, arg);
    }

    private void addReference(Expression expr, String referenceType, List<ReferenceInfo> references) {
        if (!shouldIncludeReference(expr)) {
            return;
        }

        ReferenceInfo refInfo = new ReferenceInfo();
        refInfo.setSymbolName(symbolName);
        refInfo.setReferenceType(referenceType);
        
        // 设置代码位置
        String relativePath = getRelativePath(expr);
        int line = expr.getBegin().map(pos -> pos.line).orElse(-1);
        int column = expr.getBegin().map(pos -> pos.column).orElse(-1);
        refInfo.setCodePos(String.format("%s:L%d:C%d", relativePath, line, column));
        
        // 设置上下文信息
        String context = String.format("%s.%s", currentClassName, currentMethodName);
        refInfo.setContext(context);
        
        // 获取行内容
        String lineContent = getLineContent(expr);
        refInfo.setLineContent(lineContent);
        
        references.add(refInfo);
    }

    private boolean shouldIncludeReference(Expression expr) {
        if (targetFile == null || targetLine == -1) {
            return true; // 没有指定位置，包含所有引用
        }
        
        String relativePath = getRelativePath(expr);
        int line = expr.getBegin().map(pos -> pos.line).orElse(-1);
        
        // 如果指定了文件和行号，只查找该位置的符号定义的引用
        return relativePath.contains(targetFile) && line == targetLine;
    }

    private String getRelativePath(Expression expr) {
        return expr.findCompilationUnit().flatMap(CompilationUnit::getStorage)
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

    private String getLineContent(Expression expr) {
        // 简化实现：返回表达式的字符串表示
        // 实际实现中可以读取源文件获取完整行内容
        return expr.toString();
    }
} 