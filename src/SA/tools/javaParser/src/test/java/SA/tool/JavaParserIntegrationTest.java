package SA.tool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * JavaParser 集成测试
 * 在 Maven 测试阶段自动运行，验证所有功能
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JavaParserIntegrationTest {
    
    private static Path testCodePath;
    private static Path tempOutputDir;
    private static ObjectMapper objectMapper;
    
    @BeforeAll
    static void setup() throws IOException {
        testCodePath = Paths.get("src/test/resources/testapp").toAbsolutePath();
        tempOutputDir = Files.createTempDirectory("javaparser-test-");
        objectMapper = new ObjectMapper();
        
        System.out.println("测试环境初始化:");
        System.out.println("  测试代码路径: " + testCodePath);
        System.out.println("  临时输出目录: " + tempOutputDir);

        assertTrue(Files.exists(testCodePath), "测试代码目录不存在: " + testCodePath);
        assertTrue(Files.isDirectory(testCodePath), "测试代码路径不是目录: " + testCodePath);
    }
    
    @AfterAll
    static void cleanup() throws IOException {
        // 清理临时文件
        if (tempOutputDir != null && Files.exists(tempOutputDir)) {
            Files.walk(tempOutputDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }
    
    @Test
    @Order(1)
    @DisplayName("测试 API 提取功能")
    void testApiExtraction() throws Exception {
        System.out.println("\n=== 测试 API 提取功能 ===");
        // 使用测试代码路径作为输出目录，这样JSON文件会保存在测试源码目录下
        String[] args = {
            testCodePath.toString(),
            "API",
            "-o", testCodePath.toString()
        };
        assertDoesNotThrow(() -> Main.main(args), "API 提取过程不应抛出异常");
        
        // 检查生成的Controller分析文件
        Path apiOutputDir = testCodePath.resolve("data").resolve("API");
        assertTrue(Files.exists(apiOutputDir), "API输出目录应该存在");
        assertTrue(Files.isDirectory(apiOutputDir), "API输出路径应该是目录");
        
        // 检查具体的Controller文件
        Path userControllerFile = apiOutputDir.resolve("UserController.json");
        Path productControllerFile = apiOutputDir.resolve("ProductController.json");
        
        assertTrue(Files.exists(userControllerFile) || Files.exists(productControllerFile), 
            "至少应该存在一个Controller分析文件");
        
        if (Files.exists(userControllerFile)) {
            System.out.println("\n--- UserController 分析结果 ---");
            String userControllerContent = Files.readString(userControllerFile);
            System.out.println(userControllerContent);
            
            // 验证重构后的分析结果结构
            Map<String, Object> userControllerAnalysis = objectMapper.readValue(userControllerContent, 
                new TypeReference<Map<String, Object>>() {});
            
            assertTrue(userControllerAnalysis.containsKey("controller_name"), "应包含 controller_name");
            assertTrue(userControllerAnalysis.containsKey("apis"), "应包含 apis");
            assertFalse(userControllerAnalysis.containsKey("symbol_table"), "不应再包含 symbol_table");
            assertFalse(userControllerAnalysis.containsKey("metadata"), "不应再包含 metadata");
            
            // 验证APIs包含references字段
            List<Map<String, Object>> apis = (List<Map<String, Object>>) userControllerAnalysis.get("apis");
            if (!apis.isEmpty()) {
                Map<String, Object> firstApi = apis.get(0);
                assertTrue(firstApi.containsKey("controller_name"), "API应包含 controller_name");
                assertTrue(firstApi.containsKey("method_name"), "API应包含 method_name");
                assertTrue(firstApi.containsKey("code_pos"), "API应包含 code_pos");
                assertTrue(firstApi.containsKey("references"), "API应包含 references 字段");
                Object refs = firstApi.get("references");
                assertTrue(refs instanceof List, "references 应该是一个列表");

                System.out.println("✓ API包含符号引用: " + firstApi.get("references"));
                System.out.println("✓ 成功提取 " + apis.size() + " 个 API");
            }
        }
        
        if (Files.exists(productControllerFile)) {
            System.out.println("\n--- ProductController 分析结果 ---");
            String productControllerContent = Files.readString(productControllerFile);
            System.out.println(productControllerContent);
            
            Map<String, Object> productControllerAnalysis = objectMapper.readValue(productControllerContent, 
                new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> apis = (List<Map<String, Object>>) productControllerAnalysis.get("apis");
            if (!apis.isEmpty()) {
                System.out.println("✓ ProductController 包含 " + apis.size() + " 个 API");
            }
        }
        
        System.out.println("✓ API分析功能测试完成，结果已保存到测试源码目录");
    }
    
    @Test
    @Order(2)
    @DisplayName("测试引用查找功能")
    void testReferenceFinding() throws Exception {
        System.out.println("\n=== 测试引用查找功能 ===");
        String[] testSymbols = {"createUser", "userService", "emailService", "sendWelcomeEmail", "UserService", "EmailService"};
        
        for (String symbol : testSymbols) {
            System.out.println("\n--- 查找符号: " + symbol + " ---");
            Path outputFile = tempOutputDir.resolve("reference_" + symbol + ".json");
            String[] args = {
                testCodePath.toString(),
                "REF",
                "-s", symbol,
                "-o", outputFile.toString()
            };

            assertDoesNotThrow(() -> Main.main(args), "引用查找过程不应抛出异常: " + symbol);
            assertTrue(Files.exists(outputFile), "引用查找输出文件应该存在: " + symbol);
            String jsonContent = Files.readString(outputFile);
            System.out.println("完整输出:");
            System.out.println(jsonContent.isEmpty() ? "(空输出)" : jsonContent);
            
            if (!jsonContent.trim().isEmpty()) {
                List<Map<String, Object>> references = objectMapper.readValue(jsonContent, 
                    new TypeReference<List<Map<String, Object>>>() {});
                assertNotNull(references, "引用列表不应为 null: " + symbol);
            } else {
                System.out.println("- 输出为空（可能正常）");
            }
        }
    }
    

    
    @Test
    @Order(3)
    @DisplayName("测试符号定义查找功能")
    void testDefinitionFinding() throws Exception {
        System.out.println("\n=== 测试符号定义查找功能 ===");
        String[] testSymbols = {"UserService", "EmailService", "createUser", "sendWelcomeEmail", "User", "UserRepository"};
        for (String symbol : testSymbols) {
            System.out.println("\n--- 查找符号定义: " + symbol + " ---");
            Path outputFile = tempOutputDir.resolve("definition_" + symbol + ".json");
            String[] args = {
                testCodePath.toString(),
                "DEF",
                "-s", symbol,
                "-o", outputFile.toString()
            };
            assertDoesNotThrow(() -> Main.main(args),  "定义查找过程不应抛出异常: " + symbol);
            assertTrue(Files.exists(outputFile), "定义查找输出文件应该存在: " + symbol);
            String jsonContent = Files.readString(outputFile);
            System.out.println("完整输出:");
            System.out.println(jsonContent.isEmpty() ? "(空输出)" : jsonContent);
            
            if (!jsonContent.trim().isEmpty()) {
                List<Map<String, Object>> definitions = objectMapper.readValue(jsonContent, 
                    new TypeReference<List<Map<String, Object>>>() {});
                assertNotNull(definitions, "定义列表不应为 null: " + symbol);
                
                if (!definitions.isEmpty()) {
                    System.out.println("\n定义详细信息:");
                    for (int i = 0; i < definitions.size(); i++) {
                        Map<String, Object> def = definitions.get(i);
                        System.out.println(String.format("  定义 #%d:", i + 1));
                        String defCode = (String) def.get("definition_code");
                        if (defCode != null) {
                            String displayCode = defCode.length() > 200 ? 
                                defCode.substring(0, 200) + "...(截断)" : defCode;
                            System.out.println("    定义代码: " + displayCode);
                        }
                    }
                    
                    // 验证定义结构
                    Map<String, Object> firstDef = definitions.get(0);
                    assertTrue(firstDef.containsKey("definition_type"), "定义应包含 definition_type: " + symbol);
                    assertTrue(firstDef.containsKey("code_pos"), "定义应包含 code_pos: " + symbol);
                    assertTrue(firstDef.containsKey("signature"), "定义应包含 signature: " + symbol);
                    System.out.println("✓ 找到 " + definitions.size() + " 个定义");
                } else {
                    System.out.println("- 未找到定义（可能正常）");
                }
            } else {
                System.out.println("- 输出为空（可能正常）");
            }
        }
    }

    @Test
    @Order(4)
    @DisplayName("测试调用图分析与可视化功能")
    void testCallGraphVisualization() throws Exception {
        System.out.println("\n=== 测试调用图分析与可视化功能 ===");
        Path jsonOutputFile = testCodePath.resolve("call_graph.json");
        Path svgOutputFile = testCodePath.resolve("call_graph.svg");
        String[] args = {
            testCodePath.toString(),
            "CG",
            "-o", jsonOutputFile.toString(),
            "-img", svgOutputFile.toString()
        };
        assertDoesNotThrow(() -> Main.main(args), "调用图分析不应抛出异常");
        String jsonContent = Files.readString(jsonOutputFile);
        assertFalse(jsonContent.trim().isEmpty(), "调用图分析输出不应为空");
        Map<String, Map<String, Object>> callGraph = objectMapper.readValue(jsonContent,
            new TypeReference<Map<String, Map<String, Object>>>() {});
        assertNotNull(callGraph, "调用图不应为 null");
        assertFalse(callGraph.isEmpty(), "应该分析到至少一个方法");
        System.out.println("=== 调用图统计信息 ===");
        System.out.println("总方法数: " + callGraph.size());
        long totalCallRelations = callGraph.values().stream()
            .mapToLong(method -> {
                List<?> callees = (List<?>) method.get("callees");
                return callees != null ? callees.size() : 0;
            })
            .sum();

        System.out.println("总调用关系数: " + totalCallRelations);

        // 统计各类方法数量
        Map<String, Integer> classMethodCount = new HashMap<>();
        for (Map<String, Object> methodData : callGraph.values()) {
            String className = (String) methodData.get("class_name");
            classMethodCount.put(className, classMethodCount.getOrDefault(className, 0) + 1);
        }
        System.out.println("各类方法统计:");
        classMethodCount.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(entry ->
                System.out.println("  " + entry.getKey() + ": " + entry.getValue() + " 个方法"));
        System.out.println("✓ 调用图生成成功，文件保存在测试源代码目录下");
    }
} 