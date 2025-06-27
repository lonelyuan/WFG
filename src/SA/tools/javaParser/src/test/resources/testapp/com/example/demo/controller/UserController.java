package com.example.test;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * 高级控制器 - 测试复杂的Spring注解和API提取
 */
@RestController
@RequestMapping("/api/v2/advanced")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {
    
    private UserService userService;
    private EmailService emailService;
    
    public UserController() {
        this.userService = new UserService();
        this.emailService = new EmailService();
    }
    
    /**
     * 复杂的用户创建API - 测试多个注解和参数
     */
    @PostMapping(value = "/users", 
                 consumes = "application/json", 
                 produces = "application/json")
    public ResponseEntity<Map<String, Object>> createUserAdvanced(
            @RequestBody Map<String, String> userData,
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "user") String role) {
        
        // 调用服务层方法
        String userId = userService.createUser(
            userData.get("username"), 
            userData.get("email"), 
            role
        );
        
        Map<String, Object> response = new HashMap<>();
        if (userId != null && !userId.startsWith("Invalid")) {
            response.put("success", true);
            response.put("userId", userId);
            response.put("message", "User created successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            response.put("success", false);
            response.put("error", userId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    /**
     * 用户查询API - 支持多种查询参数
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> getUserDetails(
            @PathVariable String userId,
            @RequestParam(required = false) String fields,
            @RequestParam(defaultValue = "false") boolean includeStats) {
        
        Map<String, Object> userInfo = userService.getUserInfo(userId);
        if (userInfo == null) {
            return ResponseEntity.notFound().build();
        }
        
        if (includeStats) {
            addUserStatistics(userInfo, userId);
        }
        
        if (fields != null) {
            userInfo = filterFields(userInfo, fields);
        }
        
        return ResponseEntity.ok(userInfo);
    }
    
    /**
     * 批量用户操作 - 测试复杂的请求体处理
     */
    @PutMapping("/users/batch")
    public ResponseEntity<Map<String, Object>> batchUpdateUsers(
            @RequestBody Map<String, Map<String, String>> batchUpdates,
            @RequestHeader(value = "X-Batch-Size", defaultValue = "100") int batchSize) {
        
        Map<String, Object> result = new HashMap<>();
        List<String> successIds = new ArrayList<>();
        List<String> failedIds = new ArrayList<>();
        
        int processed = 0;
        for (Map.Entry<String, Map<String, String>> entry : batchUpdates.entrySet()) {
            if (processed >= batchSize) break;
            
            String userId = entry.getKey();
            Map<String, String> updates = entry.getValue();
            
            boolean updated = userService.updateUser(userId, updates);
            if (updated) {
                successIds.add(userId);
            } else {
                failedIds.add(userId);
            }
            processed++;
        }
        
        result.put("processed", processed);
        result.put("successful", successIds);
        result.put("failed", failedIds);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 用户搜索API - 复杂的查询条件
     */
    @GetMapping("/users/search")
    public ResponseEntity<List<Map<String, Object>>> searchUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "username") String sortBy) {
        
        Map<String, Object> conditions = buildSearchConditions(username, email);
        DatabaseService dbService = new DatabaseService();
        List<Map<String, Object>> users = dbService.findUsers(conditions);
        
        // 分页和排序（简化实现）
        List<Map<String, Object>> result = paginateResults(users, page, size);
        result = sortResults(result, sortBy);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 用户删除API - 支持软删除和硬删除
     */
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Map<String, String>> deleteUserAdvanced(
            @PathVariable String userId,
            @RequestParam(defaultValue = "soft") String deleteType,
            @RequestParam(defaultValue = "false") boolean sendNotification) {
        
        boolean deleted;
        if ("hard".equals(deleteType)) {
            deleted = userService.deleteUser(userId);
        } else {
            // 软删除（标记为删除状态）
            Map<String, String> updates = new HashMap<>();
            updates.put("status", "deleted");
            deleted = userService.updateUser(userId, updates);
        }
        
        Map<String, String> response = new HashMap<>();
        if (deleted) {
            response.put("message", "User " + deleteType + " deleted successfully");
            
            if (sendNotification) {
                emailService.sendNotification(userId, "Your account has been deleted");
            }
            
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "Failed to delete user");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
    
    /**
     * 文件上传API - 测试多部分表单数据
     */
    @PostMapping(value = "/users/{userId}/avatar", 
                 consumes = "multipart/form-data")
    public ResponseEntity<Map<String, String>> uploadUserAvatar(
            @PathVariable String userId,
            @RequestParam("file") String fileName,
            @RequestParam(defaultValue = "100x100") String size) {
        
        // 模拟文件上传处理
        String avatarUrl = processAvatarUpload(fileName, size);
        
        // 更新用户头像信息
        Map<String, String> updates = new HashMap<>();
        updates.put("avatar_url", avatarUrl);
        boolean updated = userService.updateUser(userId, updates);
        
        Map<String, String> response = new HashMap<>();
        if (updated) {
            response.put("avatar_url", avatarUrl);
            response.put("message", "Avatar uploaded successfully");
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "Failed to update user avatar");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 用户统计API - 返回用户相关统计信息
     */
    @GetMapping("/users/{userId}/stats")
    public ResponseEntity<Map<String, Object>> getUserStatistics(
            @PathVariable String userId,
            @RequestParam(required = false) String period) {
        
        Map<String, Object> stats = calculateUserStatistics(userId, period);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 导出用户数据API - 支持多种格式
     */
    @GetMapping(value = "/users/export", 
                produces = {"application/json", "text/csv", "application/xml"})
    public ResponseEntity<String> exportUsers(
            @RequestParam(defaultValue = "json") String format,
            @RequestParam(required = false) String filter) {
        
        DatabaseService dbService = new DatabaseService();
        List<Map<String, Object>> users = dbService.getAllUsers();
        
        String exportData = formatExportData(users, format);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=users." + format)
                .body(exportData);
    }
    
    // 私有辅助方法 - 测试内部方法调用
    private void addUserStatistics(Map<String, Object> userInfo, String userId) {
        Map<String, Object> stats = calculateUserStatistics(userId, "all");
        userInfo.put("statistics", stats);
    }
    
    private Map<String, Object> filterFields(Map<String, Object> data, String fields) {
        Map<String, Object> filtered = new HashMap<>();
        String[] fieldArray = fields.split(",");
        
        for (String field : fieldArray) {
            if (data.containsKey(field.trim())) {
                filtered.put(field.trim(), data.get(field.trim()));
            }
        }
        
        return filtered;
    }
    
    private Map<String, Object> buildSearchConditions(String username, String email) {
        Map<String, Object> conditions = new HashMap<>();
        if (username != null && !username.isEmpty()) {
            conditions.put("username", username);
        }
        if (email != null && !email.isEmpty()) {
            conditions.put("email", email);
        }
        return conditions;
    }
    
    private List<Map<String, Object>> paginateResults(List<Map<String, Object>> data, int page, int size) {
        int start = page * size;
        int end = Math.min(start + size, data.size());
        
        if (start >= data.size()) {
            return new ArrayList<>();
        }
        
        return data.subList(start, end);
    }
    
    private List<Map<String, Object>> sortResults(List<Map<String, Object>> data, String sortBy) {
        // 简化的排序实现
        data.sort((a, b) -> {
            Object aValue = a.get(sortBy);
            Object bValue = b.get(sortBy);
            if (aValue == null) return 1;
            if (bValue == null) return -1;
            return aValue.toString().compareTo(bValue.toString());
        });
        return data;
    }
    
    private String processAvatarUpload(String fileName, String size) {
        // 模拟文件处理
        return "/uploads/avatars/" + fileName + "_" + size;
    }
    
    private Map<String, Object> calculateUserStatistics(String userId, String period) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("login_count", 42);
        stats.put("last_login", System.currentTimeMillis());
        stats.put("messages_sent", 156);
        stats.put("period", period);
        return stats;
    }
    
    private String formatExportData(List<Map<String, Object>> users, String format) {
        switch (format.toLowerCase()) {
            case "csv":
                return convertToCsv(users);
            case "xml":
                return convertToXml(users);
            default:
                return convertToJson(users);
        }
    }
    
    private String convertToCsv(List<Map<String, Object>> users) {
        StringBuilder csv = new StringBuilder();
        csv.append("id,username,email,created_at\n");
        for (Map<String, Object> user : users) {
            csv.append(user.get("id")).append(",")
               .append(user.get("username")).append(",")
               .append(user.get("email")).append(",")
               .append(user.get("created_at")).append("\n");
        }
        return csv.toString();
    }
    
    private String convertToXml(List<Map<String, Object>> users) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\"?>\n<users>\n");
        for (Map<String, Object> user : users) {
            xml.append("  <user>\n");
            user.forEach((key, value) -> 
                xml.append("    <").append(key).append(">")
                   .append(value).append("</").append(key).append(">\n"));
            xml.append("  </user>\n");
        }
        xml.append("</users>");
        return xml.toString();
    }
    
    private String convertToJson(List<Map<String, Object>> users) {
        // 简化的JSON转换
        return users.toString();
    }
} 