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
        
        Path outputFile = tempOutputDir.resolve("api_output.json");
        String[] args = {
            testCodePath.toString(),
            "api",
            "-o", outputFile.toString()
        };
        
        // 运行 API 提取
        assertDoesNotThrow(() -> Main.main(args), "API 提取过程不应抛出异常");
        
        // 验证输出文件存在
        assertTrue(Files.exists(outputFile), "API 提取输出文件应该存在");
        
        // 解析并验证 JSON 内容
        String jsonContent = Files.readString(outputFile);
        assertFalse(jsonContent.trim().isEmpty(), "API 提取输出不应为空");
        
        List<Map<String, Object>> apis = objectMapper.readValue(jsonContent, 
            new TypeReference<List<Map<String, Object>>>() {});
        
        assertNotNull(apis, "API 列表不应为 null");
        assertTrue(apis.size() > 0, "应该提取到至少一个 API");
        
        // 验证 API 结构
        Map<String, Object> firstApi = apis.get(0);
        assertTrue(firstApi.containsKey("controller_name"), "API 应包含 controller_name");
        assertTrue(firstApi.containsKey("method_name"), "API 应包含 method_name");
        assertTrue(firstApi.containsKey("code_pos"), "API 应包含 code_pos");
        
        System.out.println("✓ 成功提取 " + apis.size() + " 个 API");
        
        // 验证特定 API 是否被正确识别
        boolean foundUserController = apis.stream()
            .anyMatch(api -> "UserController".equals(api.get("controller_name")));
        assertTrue(foundUserController, "应该找到 UserController 中的 API");
        
        boolean foundProductController = apis.stream()
            .anyMatch(api -> "ProductController".equals(api.get("controller_name")));
        assertTrue(foundProductController, "应该找到 ProductController 中的 API");
    }
    
    @Test
    @Order(2)
    @DisplayName("测试引用查找功能")
    void testReferenceFinding() throws Exception {
        System.out.println("\n=== 测试引用查找功能 ===");
        
        String[] testSymbols = {"createUser", "userService", "emailService", "sendWelcomeEmail"};
        
        for (String symbol : testSymbols) {
            System.out.println("查找符号: " + symbol);
            
            Path outputFile = tempOutputDir.resolve("reference_" + symbol + ".json");
            String[] args = {
                testCodePath.toString(),
                "reference",
                "-s", symbol,
                "-o", outputFile.toString()
            };
            
            // 运行引用查找
            assertDoesNotThrow(() -> Main.main(args), 
                "引用查找过程不应抛出异常: " + symbol);
            
            // 验证输出文件存在
            assertTrue(Files.exists(outputFile), 
                "引用查找输出文件应该存在: " + symbol);
            
            // 解析并验证 JSON 内容
            String jsonContent = Files.readString(outputFile);
            if (!jsonContent.trim().isEmpty()) {
                List<Map<String, Object>> references = objectMapper.readValue(jsonContent, 
                    new TypeReference<List<Map<String, Object>>>() {});
                
                assertNotNull(references, "引用列表不应为 null: " + symbol);
                
                if (!references.isEmpty()) {
                    // 验证引用结构
                    Map<String, Object> firstRef = references.get(0);
                    assertTrue(firstRef.containsKey("reference_type"), 
                        "引用应包含 reference_type: " + symbol);
                    assertTrue(firstRef.containsKey("code_pos"), 
                        "引用应包含 code_pos: " + symbol);
                    
                    System.out.println("  ✓ 找到 " + references.size() + " 个引用");
                } else {
                    System.out.println("  - 未找到引用（可能正常）");
                }
            } else {
                System.out.println("  - 输出为空（可能正常）");
            }
        }
    }
    
    @Test
    @Order(3)
    @DisplayName("测试调用图分析功能")
    void testCallGraphAnalysis() throws Exception {
        System.out.println("\n=== 测试调用图分析功能 ===");
        
        Path outputFile = tempOutputDir.resolve("callgraph_output.json");
        String[] args = {
            testCodePath.toString(),
            "callgraph",
            "-o", outputFile.toString()
        };
        
        // 运行调用图分析
        assertDoesNotThrow(() -> Main.main(args), "调用图分析过程不应抛出异常");
        
        // 验证输出文件存在
        assertTrue(Files.exists(outputFile), "调用图分析输出文件应该存在");
        
        // 解析并验证 JSON 内容
        String jsonContent = Files.readString(outputFile);
        assertFalse(jsonContent.trim().isEmpty(), "调用图分析输出不应为空");
        
        Map<String, Map<String, Object>> callGraph = objectMapper.readValue(jsonContent, 
            new TypeReference<Map<String, Map<String, Object>>>() {});
        
        assertNotNull(callGraph, "调用图不应为 null");
        assertTrue(callGraph.size() > 0, "应该分析到至少一个方法");
        
        System.out.println("✓ 成功分析 " + callGraph.size() + " 个方法的调用关系");
        
        // 验证调用图结构
        for (Map.Entry<String, Map<String, Object>> entry : callGraph.entrySet()) {
            Map<String, Object> methodData = entry.getValue();
            assertTrue(methodData.containsKey("method_signature"), 
                "方法数据应包含 method_signature");
            assertTrue(methodData.containsKey("class_name"), 
                "方法数据应包含 class_name");
            assertTrue(methodData.containsKey("callers"), 
                "方法数据应包含 callers");
            assertTrue(methodData.containsKey("callees"), 
                "方法数据应包含 callees");
        }
        
        // 验证特定方法的调用关系
        boolean foundUserServiceMethods = callGraph.keySet().stream()
            .anyMatch(key -> key.contains("UserService"));
        assertTrue(foundUserServiceMethods, "应该找到 UserService 中的方法");
        
        boolean foundEmailServiceMethods = callGraph.keySet().stream()
            .anyMatch(key -> key.contains("EmailService"));
        assertTrue(foundEmailServiceMethods, "应该找到 EmailService 中的方法");
        
        // 验证调用关系的合理性
        long totalCallRelations = callGraph.values().stream()
            .mapToLong(method -> {
                List<?> callees = (List<?>) method.get("callees");
                return callees != null ? callees.size() : 0;
            })
            .sum();
        
        System.out.println("✓ 总调用关系数: " + totalCallRelations);
        assertTrue(totalCallRelations > 0, "应该存在调用关系");
    }

    
    /**
     * 统计测试方法
     */
    private void printTestStatistics() {
        System.out.println("\n=== 测试统计 ===");
        System.out.println("测试代码文件数量: " + countJavaFiles(testCodePath));
        System.out.println("临时文件数量: " + countFiles(tempOutputDir));
    }
    
    private long countJavaFiles(Path dir) {
        try {
            return Files.walk(dir)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .count();
        } catch (IOException e) {
            return 0;
        }
    }
    
    private long countFiles(Path dir) {
        try {
            return Files.walk(dir)
                .filter(Files::isRegularFile)
                .count();
        } catch (IOException e) {
            return 0;
        }
    }
} 