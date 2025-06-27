package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.repository.UserRepository;
import com.example.demo.entity.User;
import com.example.demo.dto.UserCreateRequest;
import com.example.demo.dto.UserUpdateRequest;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ValidationService validationService;

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> findUserById(Long id) {
        return userRepository.findById(id);
    }

    public User createUser(UserCreateRequest request) {
        validationService.validateUserRequest(request);
        
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(hashPassword(request.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setActive(true);
        
        User savedUser = userRepository.save(user);
        emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getUsername());
        
        return savedUser;
    }

    public Optional<User> updateUser(Long id, UserUpdateRequest request) {
        return userRepository.findById(id).map(user -> {
            if (request.getUsername() != null) {
                user.setUsername(request.getUsername());
            }
            if (request.getEmail() != null) {
                user.setEmail(request.getEmail());
            }
            user.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(user);
        });
    }

    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<User> searchUsers(String keyword) {
        return userRepository.findByUsernameContainingOrEmailContaining(keyword, keyword);
    }

    public Optional<User> activateUser(Long id) {
        return userRepository.findById(id).map(user -> {
            user.setActive(true);
            user.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(user);
        });
    }

    public Optional<User> deactivateUser(Long id) {
        return userRepository.findById(id).map(user -> {
            user.setActive(false);
            user.setUpdatedAt(LocalDateTime.now());
            return userRepository.save(user);
        });
    }

    public boolean isEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean isUsernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    private String hashPassword(String password) {
        // 简化的密码哈希实现
        return "hashed_" + password;
    }

    public List<User> findActiveUsers() {
        return userRepository.findByActiveTrue();
    }

    public List<User> findUsersByEmailDomain(String domain) {
        return userRepository.findByEmailEndingWith("@" + domain);
    }

    public long countActiveUsers() {
        return userRepository.countByActiveTrue();
    }
} 