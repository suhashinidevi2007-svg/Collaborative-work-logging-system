package com.cwls.controller;

import com.cwls.model.Employee;
import com.cwls.model.User;
import com.cwls.repository.EmployeeRepository;
import com.cwls.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username,
                                   @RequestParam String password,
                                   HttpSession session) {
        Employee employee = employeeRepository.findAll().stream()
                .filter(e -> e.getUser() != null &&
                        username.equals(e.getUser().getUsername()) &&
                        passwordEncoder.matches(password, e.getUser().getPassword()))
                .findFirst()
                .orElse(null);

        if (employee == null) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        User user = employee.getUser();
        session.setAttribute("USER_ID", user.getUserId());
        session.setAttribute("EMP_ID", employee.getEmployeeId());
        session.setAttribute("ROLE", user.getRole());

        Map<String, Object> response = new HashMap<>();
        response.put("employeeId", employee.getEmployeeId());
        response.put("userId", user.getUserId());
        response.put("username", user.getUsername());
        response.put("name", user.getName());
        response.put("role", user.getRole());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam String username,
                                     @RequestParam String password,
                                     @RequestParam String name,
                                     @RequestParam String email,
                                     @RequestParam(defaultValue = "Engineering") String department,
                                     @RequestParam(defaultValue = "Developer") String designation) {
        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username already exists"));
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password)); // BCrypt hash generated here
        user.setName(name);
        user.setEmail(email);
        user.setRole("EMPLOYEE");
        user = userRepository.save(user);

        Employee employee = new Employee();
        employee.setUser(user);
        employee.setDepartment(department);
        employee.setDesignation(designation);
        employee.setStatus("ACTIVE");
        employeeRepository.save(employee);

        return ResponseEntity.ok(Map.of("message", "User registered successfully with hashed password"));
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        Object empId = session.getAttribute("EMP_ID");
        if (empId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("employeeId", empId);
        response.put("role", session.getAttribute("ROLE"));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}