package SA.tool.visitor;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InterfaceImplementorVisitor extends VoidVisitorAdapter<Map<String, List<String>>> {

    private Map<String, String> importMap = new HashMap<>();
    private String currentPackage = "";

    @Override
    public void visit(PackageDeclaration n, Map<String, List<String>> arg) {
        currentPackage = n.getNameAsString();
        super.visit(n, arg);
    }
    
    @Override
    public void visit(ImportDeclaration n, Map<String, List<String>> arg) {
        String importName = n.getNameAsString();
        importMap.put(importName.substring(importName.lastIndexOf('.') + 1), importName);
        super.visit(n, arg);
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Map<String, List<String>> interfaceImplementors) {
        if (!n.isInterface()) {
            String implClassName = currentPackage.isEmpty() ? n.getNameAsString() : currentPackage + "." + n.getNameAsString();

            for (ClassOrInterfaceType implementedType : n.getImplementedTypes()) {
                String interfaceName = implementedType.getNameWithScope();
                String interfaceFqn = importMap.getOrDefault(interfaceName, currentPackage + "." + interfaceName);

                interfaceImplementors.computeIfAbsent(interfaceFqn, k -> new ArrayList<>()).add(implClassName);
            }
        }
        super.visit(n, interfaceImplementors);
    }
} 