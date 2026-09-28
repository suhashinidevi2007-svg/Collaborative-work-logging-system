package com.cwls.controller;

import com.cwls.model.*;
import com.cwls.repository.*;
import com.cwls.service.CWLSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AppController {

    @Autowired
    private CWLSService service;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private WorkLogRepository workLogRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private ProjectRepository projectRepository;

    // In-memory system settings (configurable by Admin)
    private final Map<String, String> systemSettings = new HashMap<>(Map.of(
            "standardWorkHours", "8.0",
            "pointsPerHour", "10",
            "companyName", "CWLS Enterprise"
    ));

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "CWLS-Backend"));
    }

    // ==========================================
    // 0. ADMIN EMPLOYEE DIRECTORY & ROSTER
    // ==========================================
    @GetMapping("/admin/employees")
    public ResponseEntity<List<Map<String, Object>>> getAdminEmployeeList() {
        List<Employee> employees = employeeRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Employee emp : employees) {
            Map<String, Object> map = new HashMap<>();
            map.put("employeeId", emp.getEmployeeId());
            map.put("department", emp.getDepartment() != null ? emp.getDepartment() : "General");
            map.put("designation", emp.getDesignation() != null ? emp.getDesignation() : "Staff");
            map.put("points", emp.getPoints() != null ? emp.getPoints() : 0);
            if (emp.getUser() != null) {
                map.put("userId", emp.getUser().getUserId());
                map.put("name", emp.getUser().getName());
                map.put("email", emp.getUser().getEmail());
                map.put("username", emp.getUser().getUsername());
                map.put("role", emp.getUser().getRole());
            } else {
                map.put("name", "Emp #" + emp.getEmployeeId());
                map.put("email", "N/A");
                map.put("username", "user_" + emp.getEmployeeId());
                map.put("role", "EMPLOYEE");
            }
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    // ==========================================
    // 1. ATTENDANCE (Monitor & Clocking)
    // ==========================================
    @PostMapping("/attendance/check-in/{employeeId}")
    public ResponseEntity<?> checkIn(@PathVariable Long employeeId) {
        return ResponseEntity.ok(service.checkIn(employeeId));
    }

    @PostMapping("/attendance/check-out/{employeeId}")
    public ResponseEntity<?> checkOut(@PathVariable Long employeeId) {
        return ResponseEntity.ok(service.checkOut(employeeId));
    }

    @GetMapping("/admin/attendance/all")
    public ResponseEntity<List<Attendance>> getAllAttendance() {
        return ResponseEntity.ok(attendanceRepository.findAll());
    }

    @GetMapping("/attendance/history/{employeeId}")
    public ResponseEntity<List<Attendance>> getAttendanceHistory(@PathVariable Long employeeId) {
        return ResponseEntity.ok(attendanceRepository.findByEmployeeEmployeeId(employeeId));
    }

    // ==========================================
    // 2. WORK LOGS & VERIFICATION
    // ==========================================
    @PostMapping("/worklogs/submit/{employeeId}")
    public ResponseEntity<?> submitWorkLog(@PathVariable Long employeeId,
                                           @RequestParam String task,
                                           @RequestParam Double hours) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        WorkLog workLog = new WorkLog();
        workLog.setEmployee(employee);
        workLog.setTaskDescription(task);
        workLog.setHoursWorked(hours);
        workLog.setWorkDate(LocalDate.now());
        workLog.setStatus("PENDING");

        return ResponseEntity.ok(workLogRepository.save(workLog));
    }

    @GetMapping("/admin/worklogs/pending")
    public ResponseEntity<List<WorkLog>> getPendingWorkLogs() {
        return ResponseEntity.ok(workLogRepository.findByStatus("PENDING"));
    }

    @PutMapping("/admin/worklogs/approve/{workLogId}")
    public ResponseEntity<?> approveWorkLog(@PathVariable Long workLogId) {
        WorkLog workLog = workLogRepository.findById(workLogId)
                .orElseThrow(() -> new RuntimeException("Work log not found: " + workLogId));
        workLog.setStatus("APPROVED");
        WorkLog saved = workLogRepository.save(workLog);

        // Auto-calculate points on approval (hours * pointsPerHour)
        Employee emp = workLog.getEmployee();
        int rate = 10;
        try {
            rate = Integer.parseInt(systemSettings.getOrDefault("pointsPerHour", "10").trim());
        } catch (NumberFormatException ignored) {}

        double hoursWorked = workLog.getHoursWorked() != null ? workLog.getHoursWorked() : 0.0;
        int earned = (int) Math.round(hoursWorked * rate);
        emp.setPoints((emp.getPoints() != null ? emp.getPoints() : 0) + earned);
        employeeRepository.save(emp);

        return ResponseEntity.ok(saved);
    }

    // ==========================================
    // 3. LEAVE MANAGEMENT (Request Leave)
    // ==========================================
    @PostMapping("/leave/apply/{employeeId}")
    public ResponseEntity<?> applyLeave(@PathVariable Long employeeId,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                        @RequestParam String leaveType,
                                        @RequestParam String reason) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        LeaveRequest req = new LeaveRequest();
        req.setEmployee(employee);
        req.setStartDate(startDate);
        req.setEndDate(endDate);
        req.setLeaveType(leaveType);
        req.setReason(reason);
        req.setStatus("PENDING");

        return ResponseEntity.ok(leaveRequestRepository.save(req));
    }

    @GetMapping("/leave/my-leaves/{employeeId}")
    public ResponseEntity<List<LeaveRequest>> getMyLeaves(@PathVariable Long employeeId) {
        return ResponseEntity.ok(leaveRequestRepository.findByEmployeeEmployeeId(employeeId));
    }

    @GetMapping("/admin/leave/pending")
    public ResponseEntity<List<LeaveRequest>> getPendingLeaves() {
        return ResponseEntity.ok(leaveRequestRepository.findByStatus("PENDING"));
    }

    @PutMapping("/admin/leave/status/{leaveId}")
    public ResponseEntity<?> updateLeaveStatus(@PathVariable Long leaveId, @RequestParam String status) {
        LeaveRequest req = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave request not found: " + leaveId));
        req.setStatus(status.toUpperCase());
        return ResponseEntity.ok(leaveRequestRepository.save(req));
    }

    // ==========================================
    // 4. AWARD POINTS & CALCULATE POINTS
    // ==========================================
    @PostMapping("/admin/points/award/{employeeId}")
    public ResponseEntity<?> awardPoints(@PathVariable Long employeeId, @RequestParam Integer points) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));
        emp.setPoints((emp.getPoints() != null ? emp.getPoints() : 0) + (points != null ? points : 0));
        return ResponseEntity.ok(employeeRepository.save(emp));
    }

    // ==========================================
    // 5. MANAGE PROJECTS
    // ==========================================
    @GetMapping("/projects")
    public ResponseEntity<List<Project>> getAllProjects() {
        return ResponseEntity.ok(projectRepository.findAll());
    }

    @PostMapping("/admin/projects")
    public ResponseEntity<?> createProject(@RequestParam(required = false) String projectName,
                                           @RequestParam(required = false) String clientName,
                                           @RequestParam(required = false, defaultValue = "IN_PROGRESS") String status) {
        if (projectName == null || projectName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Project name cannot be empty"));
        }

        Project p = new Project();
        p.setProjectName(projectName.trim());
        p.setClientName(clientName != null && !clientName.trim().isEmpty() ? clientName.trim() : "Internal");
        p.setStatus(status != null && !status.trim().isEmpty() ? status.trim() : "IN_PROGRESS");
        return ResponseEntity.ok(projectRepository.save(p));
    }

    // ==========================================
    // 6. SYSTEM SETTINGS
    // ==========================================
    @GetMapping("/admin/settings")
    public ResponseEntity<Map<String, String>> getSettings() {
        return ResponseEntity.ok(systemSettings);
    }

    @PostMapping("/admin/settings")
    public ResponseEntity<?> updateSettings(@RequestParam(required = false) String standardWorkHours,
                                            @RequestParam(required = false) String pointsPerHour,
                                            @RequestParam(required = false) String companyName) {
        if (standardWorkHours != null && !standardWorkHours.trim().isEmpty()) {
            systemSettings.put("standardWorkHours", standardWorkHours.trim());
        }
        if (pointsPerHour != null && !pointsPerHour.trim().isEmpty()) {
            systemSettings.put("pointsPerHour", pointsPerHour.trim());
        }
        if (companyName != null && !companyName.trim().isEmpty()) {
            systemSettings.put("companyName", companyName.trim());
        }
        return ResponseEntity.ok(systemSettings);
    }

    // ==========================================
    // 7. PERFORMANCE, PRODUCTIVITY & SUMMARY REPORTS
    // ==========================================
    @GetMapping("/reports/summary/{employeeId}")
    public ResponseEntity<?> getEmployeeSummary(@PathVariable Long employeeId) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        List<Attendance> attendances = attendanceRepository.findByEmployeeEmployeeId(employeeId);
        List<WorkLog> workLogs = workLogRepository.findByEmployeeEmployeeId(employeeId);
        List<LeaveRequest> leaves = leaveRequestRepository.findByEmployeeEmployeeId(employeeId);

        double totalHours = workLogs.stream()
                .filter(w -> "APPROVED".equalsIgnoreCase(w.getStatus()))
                .mapToDouble(w -> w.getHoursWorked() != null ? w.getHoursWorked() : 0.0)
                .sum();

        long approvedLeaves = leaves.stream()
                .filter(l -> "APPROVED".equalsIgnoreCase(l.getStatus()))
                .count();

        double stdHours = 8.0;
        try {
            stdHours = Double.parseDouble(systemSettings.getOrDefault("standardWorkHours", "8.0").trim());
        } catch (NumberFormatException ignored) {}

        double expectedHours = attendances.isEmpty() ? stdHours : (attendances.size() * stdHours);
        double productivity = expectedHours > 0 ? ((totalHours / expectedHours) * 100.0) : 0.0;

        Map<String, Object> summary = new HashMap<>();
        summary.put("employeeId", emp.getEmployeeId());
        summary.put("name", emp.getUser() != null ? emp.getUser().getName() : "N/A");
        summary.put("email", emp.getUser() != null ? emp.getUser().getEmail() : "N/A");
        summary.put("department", emp.getDepartment());
        summary.put("designation", emp.getDesignation());
        summary.put("points", emp.getPoints() != null ? emp.getPoints() : 0);
        summary.put("productivityScore", Math.min(100.0, Math.round(productivity * 10.0) / 10.0));
        summary.put("totalDaysPresent", attendances.size());
        summary.put("totalApprovedHours", totalHours);
        summary.put("pendingWorkLogsCount", workLogs.stream().filter(w -> "PENDING".equalsIgnoreCase(w.getStatus())).count());
        summary.put("approvedLeavesCount", approvedLeaves);
        summary.put("pendingLeavesCount", leaves.stream().filter(l -> "PENDING".equalsIgnoreCase(l.getStatus())).count());

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/reports/all-summary")
    public ResponseEntity<List<Map<String, Object>>> getAllEmployeesSummary() {
        List<Employee> employees = employeeRepository.findAll();
        List<Map<String, Object>> summaries = new ArrayList<>();

        double stdHours = 8.0;
        try {
            stdHours = Double.parseDouble(systemSettings.getOrDefault("standardWorkHours", "8.0").trim());
        } catch (NumberFormatException ignored) {}

        for (Employee emp : employees) {
            List<Attendance> attendances = attendanceRepository.findByEmployeeEmployeeId(emp.getEmployeeId());
            List<WorkLog> workLogs = workLogRepository.findByEmployeeEmployeeId(emp.getEmployeeId());
            List<LeaveRequest> leaves = leaveRequestRepository.findByEmployeeEmployeeId(emp.getEmployeeId());

            double totalHours = workLogs.stream()
                    .filter(w -> "APPROVED".equalsIgnoreCase(w.getStatus()))
                    .mapToDouble(w -> w.getHoursWorked() != null ? w.getHoursWorked() : 0.0)
                    .sum();

            double expectedHours = attendances.isEmpty() ? stdHours : (attendances.size() * stdHours);
            double productivity = expectedHours > 0 ? ((totalHours / expectedHours) * 100.0) : 0.0;

            Map<String, Object> summary = new HashMap<>();
            summary.put("employeeId", emp.getEmployeeId());
            summary.put("name", emp.getUser() != null ? emp.getUser().getName() : "N/A");
            summary.put("department", emp.getDepartment());
            summary.put("designation", emp.getDesignation());
            summary.put("points", emp.getPoints() != null ? emp.getPoints() : 0);
            summary.put("productivityScore", Math.min(100.0, Math.round(productivity * 10.0) / 10.0));
            summary.put("totalDaysPresent", attendances.size());
            summary.put("totalApprovedHours", totalHours);
            summary.put("approvedLeavesCount", leaves.stream().filter(l -> "APPROVED".equalsIgnoreCase(l.getStatus())).count());
            summaries.add(summary);
        }

        return ResponseEntity.ok(summaries);
    }
}