package SA.tool.analyzer;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.utils.SourceRoot;
import SA.tool.model.ApiInfo;
import SA.tool.visitor.ApiExtractorVisitor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ApiExtractor {
    public List<ApiInfo> extractApiInfo(String targetPath) throws IOException {
        List<ApiInfo> apiInfos = new ArrayList<>();
        Path rootPath = Paths.get(targetPath).normalize();

        // Directly treat the target path as a source root, which is more robust
        SourceRoot sourceRoot = new SourceRoot(rootPath);
        List<ParseResult<CompilationUnit>> parseResults = sourceRoot.tryToParse(""); // This will parse all .java files recursively

        for (ParseResult<CompilationUnit> parseResult : parseResults) {
            if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                CompilationUnit cu = parseResult.getResult().get();
                ApiExtractorVisitor visitor = new ApiExtractorVisitor(rootPath);
                visitor.visit(cu, apiInfos);
            }
        }
        return apiInfos;
    }
} 