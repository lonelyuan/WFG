package SA.tool.analyzer;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.utils.SourceRoot;
import SA.tool.model.ReferenceInfo;
import SA.tool.visitor.ReferenceFinderVisitor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ReferenceFinder {
    
    public List<ReferenceInfo> findReferences(String targetPath, String symbolName, String targetFile, int targetLine) throws IOException {
        List<ReferenceInfo> references = new ArrayList<>();
        Path rootPath = Paths.get(targetPath).normalize();

        SourceRoot sourceRoot = new SourceRoot(rootPath);
        List<ParseResult<CompilationUnit>> parseResults = sourceRoot.tryToParse("");

        for (ParseResult<CompilationUnit> parseResult : parseResults) {
            if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                CompilationUnit cu = parseResult.getResult().get();
                ReferenceFinderVisitor visitor = new ReferenceFinderVisitor(rootPath, symbolName, targetFile, targetLine);
                visitor.visit(cu, references);
            }
        }
        return references;
    }
    
    public List<ReferenceInfo> findReferences(String targetPath, String symbolName) throws IOException {
        return findReferences(targetPath, symbolName, null, -1);
    }
} 