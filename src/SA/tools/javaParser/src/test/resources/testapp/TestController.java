package com.example.test;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class TestController {

    // Simple GET
    @GetMapping("/hello")
    public String sayHello() {
        return "Hello, World!";
    }

    // GET with path variable
    @GetMapping("/users/{userId}")
    public Map<String, String> getUser(@PathVariable String userId) {
        return Map.of("id", userId, "name", "Test User");
    }

    // POST with request body
    @PostMapping("/users")
    public Map<String, Object> createUser(@RequestBody Map<String, String> userData) {
        return Map.of("status", "created", "data", userData);
    }

    // PUT mapping
    @PutMapping("/items/{itemId}")
    public String updateItem(@PathVariable int itemId) {
        return "Updated item " + itemId;
    }

    // DELETE mapping
    @DeleteMapping("/items/{itemId}")
    public String deleteItem(@PathVariable int itemId) {
        return "Deleted item " + itemId;
    }
    
    // PATCH mapping
    @PatchMapping("/items/{itemId}")
    public String patchItem(@PathVariable int itemId) {
        return "Patched item " + itemId;
    }

    // RequestMapping with multiple paths and explicit method
    @RequestMapping(value = {"/legacy/endpoint", "/new/endpoint"}, method = RequestMethod.GET)
    public List<String> getLegacyData() {
        return List.of("data1", "data2");
    }

    // Mapping without a leading slash
    @GetMapping("status")
    public String getStatus() {
        return "OK";
    }
}
