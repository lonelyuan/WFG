package SA.tool;

import SA.tool.analyzer.ApiExtractor;
import SA.tool.model.ApiInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String targetPath;
        String outputPath = null;

        if (args.length == 1) {
            targetPath = args[0];
        } else if (args.length >= 3 && args[1].equals("-o")) {
            targetPath = args[0];
            outputPath = args[2];
        } else {
            System.err.println("Usage: java -jar <jar-file> <path-to-java-project> [-o <output-file>]");
            System.exit(1);
            return;
        }

        ApiExtractor extractor = new ApiExtractor();

        try {
            List<ApiInfo> apiInfos = extractor.extractApiInfo(targetPath);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

            if (outputPath != null) {
                try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputPath), StandardCharsets.UTF_8)) {
                    objectMapper.writeValue(writer, apiInfos);
                }
                System.out.println("API info successfully written to " + outputPath);
            } else {
                String jsonOutput = objectMapper.writeValueAsString(apiInfos);
                System.out.println(jsonOutput);
            }

        } catch (IOException e) {
            System.err.println("Error processing the project: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
} 