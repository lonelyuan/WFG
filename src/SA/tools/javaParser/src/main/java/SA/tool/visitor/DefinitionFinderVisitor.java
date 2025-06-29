package SA.tool.visitor;

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import SA.tool.model.DefinitionInfo;

import java.nio.file.Path;
import java.util.List;

public class DefinitionFinderVisitor extends VoidVisitorAdapter<List<DefinitionInfo>> {
    
    private final Path rootPath;
    private final String targetSymbol;
    private String currentFile;
    private String currentClass;
    private String currentMethod;

    public DefinitionFinderVisitor(Path rootPath, String targetSymbol) {
        this.rootPath = rootPath;
        this.targetSymbol = targetSymbol;
    }

    @Override
    public void visit(CompilationUnit n, List<DefinitionInfo> arg) {
        this.currentFile = rootPath.relativize(n.getStorage().get().getPath()).toString();
        super.visit(n, arg);
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, List<DefinitionInfo> arg) {
        String oldClass = currentClass;
        currentClass = n.getNameAsString();
        
        if (n.getNameAsString().equals(targetSymbol)) {
            DefinitionInfo definition = new DefinitionInfo();
            definition.setSymbolName(targetSymbol);
            definition.setDefinitionType(n.isInterface() ? "INTERFACE" : "CLASS");
            definition.setCodePos(getCodePosition(n));
            definition.setDefinitionCode(n.toString());
            definition.setSignature(getClassSignature(n));
            definition.setModifiers(getModifiers(n));
            definition.setScope(currentFile);
            arg.add(definition);
        }
        
        super.visit(n, arg);
        currentClass = oldClass;
    }

    @Override
    public void visit(MethodDeclaration n, List<DefinitionInfo> arg) {
        String oldMethod = currentMethod;
        currentMethod = n.getNameAsString();
        
        if (n.getNameAsString().equals(targetSymbol)) {
            DefinitionInfo definition = new DefinitionInfo();
            definition.setSymbolName(targetSymbol);
            definition.setDefinitionType("METHOD");
            definition.setCodePos(getCodePosition(n));
            definition.setDefinitionCode(n.toString());
            definition.setSignature(getMethodSignature(n));
            definition.setModifiers(getModifiers(n));
            definition.setScope(currentClass);
            definition.setReturnType(n.getType().asString());
            definition.setParameters(getParametersString(n));
            arg.add(definition);
        }
        
        super.visit(n, arg);
        currentMethod = oldMethod;
    }

    @Override
    public void visit(FieldDeclaration n, List<DefinitionInfo> arg) {
        for (VariableDeclarator var : n.getVariables()) {
            if (var.getNameAsString().equals(targetSymbol)) {
                DefinitionInfo definition = new DefinitionInfo();
                definition.setSymbolName(targetSymbol);
                definition.setDefinitionType("FIELD");
                definition.setCodePos(getCodePosition(n));
                definition.setDefinitionCode(n.toString());
                definition.setSignature(var.getTypeAsString() + " " + var.getNameAsString());
                definition.setModifiers(getModifiers(n));
                definition.setScope(currentClass);
                definition.setReturnType(var.getTypeAsString());
                arg.add(definition);
            }
        }
        super.visit(n, arg);
    }

    @Override
    public void visit(VariableDeclarationExpr n, List<DefinitionInfo> arg) {
        for (VariableDeclarator var : n.getVariables()) {
            if (var.getNameAsString().equals(targetSymbol)) {
                DefinitionInfo definition = new DefinitionInfo();
                definition.setSymbolName(targetSymbol);
                definition.setDefinitionType("VARIABLE");
                definition.setCodePos(getCodePosition(n));
                definition.setDefinitionCode(n.toString());
                definition.setSignature(var.getTypeAsString() + " " + var.getNameAsString());
                definition.setScope(currentMethod != null ? currentMethod : currentClass);
                definition.setReturnType(var.getTypeAsString());
                arg.add(definition);
            }
        }
        super.visit(n, arg);
    }

    @Override
    public void visit(Parameter n, List<DefinitionInfo> arg) {
        if (n.getNameAsString().equals(targetSymbol)) {
            DefinitionInfo definition = new DefinitionInfo();
            definition.setSymbolName(targetSymbol);
            definition.setDefinitionType("PARAMETER");
            definition.setCodePos(getCodePosition(n));
            definition.setDefinitionCode(n.toString());
            definition.setSignature(n.getTypeAsString() + " " + n.getNameAsString());
            definition.setScope(currentMethod);
            definition.setReturnType(n.getTypeAsString());
            arg.add(definition);
        }
        super.visit(n, arg);
    }

    @Override
    public void visit(EnumDeclaration n, List<DefinitionInfo> arg) {
        String oldClass = currentClass;
        currentClass = n.getNameAsString();
        
        if (n.getNameAsString().equals(targetSymbol)) {
            DefinitionInfo definition = new DefinitionInfo();
            definition.setSymbolName(targetSymbol);
            definition.setDefinitionType("ENUM");
            definition.setCodePos(getCodePosition(n));
            definition.setDefinitionCode(n.toString());
            definition.setSignature("enum " + n.getNameAsString());
            definition.setModifiers(getModifiers(n));
            definition.setScope(currentFile);
            arg.add(definition);
        }
        
        super.visit(n, arg);
        currentClass = oldClass;
    }

    private String getCodePosition(Node n) {
        if (n.getRange().isPresent()) {
            var range = n.getRange().get();
            return currentFile + ":L" + range.begin.line + "-L" + range.end.line;
        }
        return currentFile + ":unknown";
    }

    private String getClassSignature(ClassOrInterfaceDeclaration n) {
        StringBuilder sb = new StringBuilder();
        if (!n.getModifiers().isEmpty()) {
            sb.append(n.getModifiers().toString()).append(" ");
        }
        sb.append(n.isInterface() ? "interface " : "class ");
        sb.append(n.getNameAsString());
        if (!n.getTypeParameters().isEmpty()) {
            sb.append("<").append(n.getTypeParameters().toString()).append(">");
        }
        return sb.toString();
    }

    private String getMethodSignature(MethodDeclaration n) {
        StringBuilder sb = new StringBuilder();
        sb.append(n.getType().asString()).append(" ");
        sb.append(n.getNameAsString());
        sb.append("(");
        sb.append(getParametersString(n));
        sb.append(")");
        return sb.toString();
    }

    private String getParametersString(MethodDeclaration n) {
        return n.getParameters().stream()
            .map(p -> p.getType().asString() + " " + p.getNameAsString())
            .reduce((a, b) -> a + ", " + b)
            .orElse("");
    }

    private String getModifiers(NodeWithModifiers<?> n) {
        return n.getModifiers().stream()
            .map(Object::toString)
            .reduce((a, b) -> a + " " + b)
            .orElse("");
    }
} 