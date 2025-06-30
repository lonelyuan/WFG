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
import java.util.stream.Collectors;

public class DefinitionFinderVisitor extends VoidVisitorAdapter<List<DefinitionInfo>> {

    private final Path rootPath;
    private final String targetSymbol;
    private String fqnClassName;
    private String memberName;
    private boolean isFqn;
    private String currentFile;
    private String currentClassFqn;

    public DefinitionFinderVisitor(Path rootPath, String targetSymbol) {
        this.rootPath = rootPath;
        this.targetSymbol = targetSymbol;
        
        if (targetSymbol.contains(".")) {
            int lastDot = targetSymbol.lastIndexOf('.');
            this.fqnClassName = targetSymbol.substring(0, lastDot);
            this.memberName = targetSymbol.substring(lastDot + 1);
            this.isFqn = true;
        } else {
            this.isFqn = false;
        }
    }

    @Override
    public void visit(CompilationUnit n, List<DefinitionInfo> arg) {
        n.getStorage().ifPresent(storage -> {
            Path filePath = storage.getPath();
            try {
                if (rootPath.getRoot() != null && filePath.getRoot() != null && !rootPath.getRoot().equals(filePath.getRoot())) {
                    this.currentFile = filePath.toString();
                } else {
                    this.currentFile = rootPath.relativize(filePath).toString();
                }
            } catch (IllegalArgumentException e) {
                this.currentFile = filePath.toAbsolutePath().toString();
            }
        });
        
        if (isFqn) {
            String packageName = n.getPackageDeclaration().map(PackageDeclaration::getNameAsString).orElse("");
            boolean fileMatches = n.getPrimaryTypeName()
                .map(typeName -> (packageName.isEmpty() ? "" : packageName + ".") + typeName)
                .map(fqn -> fqn.equals(fqnClassName))
                .orElse(false);

            if (fileMatches) {
                super.visit(n, arg);
            }
        } else {
            super.visit(n, arg);
        }
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, List<DefinitionInfo> arg) {
        String oldClassFqn = currentClassFqn;
        String packageName = n.findCompilationUnit().flatMap(CompilationUnit::getPackageDeclaration)
                .map(PackageDeclaration::getNameAsString).orElse("");
        currentClassFqn = packageName.isEmpty() ? n.getNameAsString() : packageName + "." + n.getNameAsString();

        boolean nameMatches = isFqn ? currentClassFqn.equals(fqnClassName) && memberName == null : n.getNameAsString().equals(targetSymbol);
        
        if (nameMatches) {
            addDefinition(n, targetSymbol, n.isInterface() ? "INTERFACE" : "CLASS", getClassSignature(n), n.toString(), getModifiers(n), currentFile, arg);
        }

        super.visit(n, arg);
        currentClassFqn = oldClassFqn;
    }

    @Override
    public void visit(MethodDeclaration n, List<DefinitionInfo> arg) {
        boolean nameMatches = n.getNameAsString().equals(isFqn ? memberName : targetSymbol);
        boolean classMatches = !isFqn || (currentClassFqn != null && currentClassFqn.equals(fqnClassName));
        
        if (nameMatches && classMatches) {
            addDefinition(n, targetSymbol, "METHOD", getMethodSignature(n), n.toString(), getModifiers(n), currentClassFqn, arg);
        }
        super.visit(n, arg);
    }
    
    @Override
    public void visit(FieldDeclaration n, List<DefinitionInfo> arg) {
        for (VariableDeclarator var : n.getVariables()) {
            boolean nameMatches = var.getNameAsString().equals(isFqn ? memberName : targetSymbol);
            boolean classMatches = !isFqn || (currentClassFqn != null && currentClassFqn.equals(fqnClassName));

            if (nameMatches && classMatches) {
                addDefinition(n, targetSymbol, "FIELD", var.getTypeAsString() + " " + var.getNameAsString(), n.toString(), getModifiers(n), currentClassFqn, arg);
            }
        }
        super.visit(n, arg);
    }

    @Override
    public void visit(VariableDeclarationExpr n, List<DefinitionInfo> arg) {
        if (!isFqn) {
            for (VariableDeclarator var : n.getVariables()) {
                if (var.getNameAsString().equals(targetSymbol)) {
                     addDefinition(n, targetSymbol, "VARIABLE", var.getTypeAsString() + " " + var.getNameAsString(), n.toString(), "", currentClassFqn, arg);
                }
            }
        }
        super.visit(n, arg);
    }

    @Override
    public void visit(Parameter n, List<DefinitionInfo> arg) {
        if (!isFqn) {
            if (n.getNameAsString().equals(targetSymbol)) {
                 addDefinition(n, targetSymbol, "PARAMETER", n.getTypeAsString() + " " + n.getNameAsString(), n.toString(), "", currentClassFqn, arg);
            }
        }
        super.visit(n, arg);
    }
    
    @Override
    public void visit(EnumDeclaration n, List<DefinitionInfo> arg) {
        String oldClassFqn = currentClassFqn;
        String packageName = n.findCompilationUnit().flatMap(CompilationUnit::getPackageDeclaration)
                .map(PackageDeclaration::getNameAsString).orElse("");
        currentClassFqn = packageName.isEmpty() ? n.getNameAsString() : packageName + "." + n.getNameAsString();
        
        boolean nameMatches = isFqn ? currentClassFqn.equals(fqnClassName) && memberName == null : n.getNameAsString().equals(targetSymbol);

        if (nameMatches) {
             addDefinition(n, targetSymbol, "ENUM", "enum " + n.getNameAsString(), n.toString(), getModifiers(n), currentFile, arg);
        }
        
        super.visit(n, arg);
        currentClassFqn = oldClassFqn;
    }

    @Override
    public void visit(EnumConstantDeclaration n, List<DefinitionInfo> arg) {
        boolean nameMatches = n.getNameAsString().equals(isFqn ? memberName : targetSymbol);
        boolean classMatches = !isFqn || (currentClassFqn != null && currentClassFqn.equals(fqnClassName));

        if (nameMatches && classMatches) {
            addDefinition(n, targetSymbol, "ENUM_CONSTANT", n.getNameAsString(), n.toString(), "", currentClassFqn, arg);
        }
        super.visit(n, arg);
    }

    private void addDefinition(Node node, String name, String type, String signature, String code, String modifiers, String scope, List<DefinitionInfo> arg) {
        DefinitionInfo definition = new DefinitionInfo();
        definition.setSymbolName(name);
        definition.setDefinitionType(type);
        definition.setCodePos(getCodePosition(node));
        definition.setDefinitionCode(code);
        definition.setSignature(signature);
        definition.setModifiers(modifiers);
        definition.setScope(scope);
        arg.add(definition);
    }

    private String getCodePosition(Node n) {
        return n.getRange()
                .map(range -> currentFile + ":L" + range.begin.line + "-L" + range.end.line)
                .orElse(currentFile + ":unknown");
    }

    private String getClassSignature(ClassOrInterfaceDeclaration n) {
        StringBuilder sb = new StringBuilder();
        n.getModifiers().forEach(mod -> sb.append(mod.getKeyword().asString()).append(" "));
        sb.append(n.isInterface() ? "interface " : "class ");
        sb.append(n.getNameAsString());
        if (!n.getTypeParameters().isEmpty()) {
            sb.append("<").append(n.getTypeParameters().stream().map(Object::toString).collect(Collectors.joining(", "))).append(">");
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
            .collect(Collectors.joining(", "));
    }

    private String getModifiers(NodeWithModifiers<?> n) {
        return n.getModifiers().stream()
            .map(mod -> mod.getKeyword().asString())
            .collect(Collectors.joining(" "));
    }
} 