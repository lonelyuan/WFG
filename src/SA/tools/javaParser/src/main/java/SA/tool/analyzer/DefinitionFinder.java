package SA.tool.analyzer;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.utils.SourceRoot;
import SA.tool.model.DefinitionInfo;
import SA.tool.visitor.DefinitionFinderVisitor;
import SA.tool.visitor.InterfaceImplementorVisitor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class DefinitionFinder {
    
    public List<DefinitionInfo> findDefinitions(String targetPath, String symbolName) throws IOException {
        Path rootPath = Paths.get(targetPath).normalize();
        SourceRoot sourceRoot = new SourceRoot(rootPath);
        List<ParseResult<CompilationUnit>> parseResults = sourceRoot.tryToParse("");

        // Step 1: Build the interface-to-implementation map
        Map<String, List<String>> interfaceImplementors = new HashMap<>();
        InterfaceImplementorVisitor implementorVisitor = new InterfaceImplementorVisitor();
        for (ParseResult<CompilationUnit> parseResult : parseResults) {
            parseResult.ifSuccessful(cu -> implementorVisitor.visit(cu, interfaceImplementors));
        }

        // Step 2: Find all possible definitions
        List<DefinitionInfo> allDefinitions = new ArrayList<>();
        DefinitionFinderVisitor initialVisitor = new DefinitionFinderVisitor(rootPath, symbolName);
        for (ParseResult<CompilationUnit> parseResult : parseResults) {
            parseResult.ifSuccessful(cu -> initialVisitor.visit(cu, allDefinitions));
        }
        
        // Step 3: Post-process to find implementation if only an interface method is found
        if (allDefinitions.size() == 1 && "METHOD".equals(allDefinitions.get(0).getDefinitionType())) {
            DefinitionInfo interfaceDef = allDefinitions.get(0);
            String interfaceFqn = interfaceDef.getScope();
            
            if (interfaceFqn != null && interfaceImplementors.containsKey(interfaceFqn)) {
                List<String> implementorClasses = interfaceImplementors.get(interfaceFqn);
                String methodName = symbolName.substring(symbolName.lastIndexOf('.') + 1);
                
                List<DefinitionInfo> implementationDefs = new ArrayList<>();
                for (String implClassFqn : implementorClasses) {
                     // Create a new targeted search for the implementation
                     DefinitionFinderVisitor implVisitor = new DefinitionFinderVisitor(rootPath, implClassFqn + "." + methodName);
                     for (ParseResult<CompilationUnit> parseResult : parseResults) {
                         parseResult.ifSuccessful(cu -> implVisitor.visit(cu, implementationDefs));
                     }
                }

                if (!implementationDefs.isEmpty()) {
                    return implementationDefs; // Return implementations instead
                }
            }
        }

        return allDefinitions;
    }
} 