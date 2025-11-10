package com.ai.roleplay.controller;

import com.ai.roleplay.model.User;
import com.ai.roleplay.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> request, HttpSession session) {
        try {
            String username = request.get("username");
            String email = request.get("email");
            String password = request.get("password");

            if (username == null || email == null || password == null) {
                return ResponseEntity.badRequest().body("用户名、邮箱和密码不能为空");
            }

            User user = userService.registerUser(username, email, password);

            // 注册成功后自动登录
            session.setAttribute("user_id", String.valueOf(user.getId()));
            session.setAttribute("username", user.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "注册成功");
            response.put("user", user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> request, HttpSession session) {
        try {
            String username = request.get("username");
            String password = request.get("password");

            if (username == null || password == null) {
                return ResponseEntity.badRequest().body("用户名和密码不能为空");
            }

            return userService.findByUsername(username)
                    .map(user -> {
                        if (userService.checkPassword(user, password)) {
                            // 登录成功，设置session
                            session.setAttribute("user_id", String.valueOf(user.getId()));
                            session.setAttribute("username", user.getUsername());

                            Map<String, Object> response = new HashMap<>();
                            response.put("message", "登录成功");
                            response.put("user", user);
                            return ResponseEntity.ok(response);
                        } else {
                            return ResponseEntity.badRequest().body("密码错误");
                        }
                    })
                    .orElse(ResponseEntity.badRequest().body("用户不存在"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(HttpSession session) {
        session.invalidate();
        Map<String, String> response = new HashMap<>();
        response.put("message", "登出成功");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        String userId = (String) session.getAttribute("user_id");
        String username = (String) session.getAttribute("username");

        if (userId == null || username == null) {
            return ResponseEntity.badRequest().body("用户未登录");
        }

        Map<String, String> user = new HashMap<>();
        user.put("id", userId);
        user.put("username", username);

        return ResponseEntity.ok(user);
    }
}