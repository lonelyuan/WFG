package SA.tool.analyzer;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.utils.SourceRoot;
import SA.tool.model.DefinitionInfo;
import SA.tool.visitor.DefinitionFinderVisitor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class DefinitionFinder {
    
    public List<DefinitionInfo> findDefinitions(String targetPath, String symbolName) throws IOException {
        List<DefinitionInfo> definitions = new ArrayList<>();
        Path rootPath = Paths.get(targetPath).normalize();

        SourceRoot sourceRoot = new SourceRoot(rootPath);
        List<ParseResult<CompilationUnit>> parseResults = sourceRoot.tryToParse("");

        for (ParseResult<CompilationUnit> parseResult : parseResults) {
            if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                CompilationUnit cu = parseResult.getResult().get();
                DefinitionFinderVisitor visitor = new DefinitionFinderVisitor(rootPath, symbolName);
                visitor.visit(cu, definitions);
            }
        }
        return definitions;
    }
} 