package com.cwls.config;

import com.cwls.model.Employee;
import com.cwls.model.User;
import com.cwls.repository.EmployeeRepository;
import com.cwls.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DataInitializer(UserRepository userRepository, EmployeeRepository employeeRepository) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public void run(String... args) {
        // Seed default Admin if not exists
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setName("System Administrator");
            admin.setEmail("admin@cwls.com");
            admin.setPhone("1234567890");
            admin.setRole("ADMIN");
            admin = userRepository.save(admin);

            Employee adminEmp = new Employee();
            adminEmp.setUser(admin);
            adminEmp.setDepartment("Management");
            adminEmp.setDesignation("Lead Administrator");
            adminEmp.setJoinDate(LocalDate.now());
            adminEmp.setPoints(100);
            adminEmp.setStatus("ACTIVE");
            adminEmp.setPhone("1234567890");
            employeeRepository.save(adminEmp);

            System.out.println(">>> [CWLS] Default Admin created: admin / admin123");
        }

        // Seed default Employee if not exists
        if (userRepository.findByUsername("employee").isEmpty()) {
            User empUser = new User();
            empUser.setUsername("employee");
            empUser.setPassword(passwordEncoder.encode("emp123"));
            empUser.setName("John Doe");
            empUser.setEmail("john@cwls.com");
            empUser.setPhone("9876543210");
            empUser.setRole("EMPLOYEE");
            empUser = userRepository.save(empUser);

            Employee emp = new Employee();
            emp.setUser(empUser);
            emp.setDepartment("Engineering");
            emp.setDesignation("Full Stack Developer");
            emp.setJoinDate(LocalDate.now());
            emp.setPoints(50);
            emp.setStatus("ACTIVE");
            emp.setPhone("9876543210");
            employeeRepository.save(emp);

            System.out.println(">>> [CWLS] Default Employee created: employee / emp123");
        }
    }
}
