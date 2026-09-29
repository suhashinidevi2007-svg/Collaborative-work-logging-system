package com.cwls.controller;

import com.cwls.model.*;
import com.cwls.repository.*;
import com.cwls.service.CWLSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
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

    /**
     * Helper to verify if the requester has ADMIN privileges.
     * Checks session attribute or X-User-Role header / request param.
     */
    private boolean checkIsAdmin(HttpSession session, String roleHeader, String roleParam) {
        if (session != null && "ADMIN".equalsIgnoreCase((String) session.getAttribute("ROLE"))) {
            return true;
        }
        if (roleHeader != null && "ADMIN".equalsIgnoreCase(roleHeader.trim())) {
            return true;
        }
        if (roleParam != null && "ADMIN".equalsIgnoreCase(roleParam.trim())) {
            return true;
        }
        return false;
    }

    // ==========================================
    // 0. ADMIN EMPLOYEE DIRECTORY & ROSTER
    // ==========================================
    @GetMapping("/admin/employees")
    public ResponseEntity<?> getAdminEmployeeList(@RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                                  @RequestParam(required = false) String role,
                                                  HttpSession session) {
        if (!checkIsAdmin(session, roleHeader, role)) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden: Admin privileges required"));
        }

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
    public ResponseEntity<?> getAllAttendance(@RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                              @RequestParam(required = false) String role,
                                              HttpSession session) {
        if (!checkIsAdmin(session, roleHeader, role)) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden: Admin privileges required"));
        }
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
                                           @RequestParam Double hours,
                                           @RequestParam(required = false, defaultValue = "MEDIUM") String priority,
                                           @RequestParam(required = false, defaultValue = "Development") String category) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        WorkLog workLog = new WorkLog();
        workLog.setEmployee(employee);
        workLog.setTaskDescription(task);
        workLog.setHoursWorked(hours);
        workLog.setWorkDate(LocalDate.now());
        workLog.setStatus("PENDING");
        workLog.setPriority(priority != null ? priority.toUpperCase() : "MEDIUM");
        workLog.setCategory(category != null ? category : "Development");

        return ResponseEntity.ok(workLogRepository.save(workLog));
    }

    @GetMapping("/worklogs/my-logs/{employeeId}")
    public ResponseEntity<List<WorkLog>> getMyWorkLogs(@PathVariable Long employeeId) {
        return ResponseEntity.ok(workLogRepository.findByEmployeeEmployeeId(employeeId));
    }

    @GetMapping("/admin/worklogs/pending")
    public ResponseEntity<?> getPendingWorkLogs(@RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                                @RequestParam(required = false) String role,
                                                HttpSession session) {
        if (!checkIsAdmin(session, roleHeader, role)) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden: Admin privileges required"));
        }
        return ResponseEntity.ok(workLogRepository.findByStatus("PENDING"));
    }

    @PutMapping("/admin/worklogs/approve/{workLogId}")
    public ResponseEntity<?> approveWorkLog(@PathVariable Long workLogId,
                                           @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                           @RequestParam(required = false) String role,
                                           HttpSession session) {
        if (!checkIsAdmin(session, roleHeader, role)) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden: Admin privileges required"));
        }

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

    @PutMapping("/admin/worklogs/reject/{workLogId}")
    public ResponseEntity<?> rejectWorkLog(@PathVariable Long workLogId,
                                           @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                           @RequestParam(required = false) String role,
                                           HttpSession session) {
        if (!checkIsAdmin(session, roleHeader, role)) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden: Admin privileges required"));
        }

        WorkLog workLog = workLogRepository.findById(workLogId)
                .orElseThrow(() -> new RuntimeException("Work log not found: " + workLogId));
        workLog.setStatus("REJECTED");
        return ResponseEntity.ok(workLogRepository.save(workLog));
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
    public ResponseEntity<?> getPendingLeaves(@RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                              @RequestParam(required = false) String role,
                                              HttpSession session) {
        if (!checkIsAdmin(session, roleHeader, role)) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden: Admin privileges required"));
        }
        return ResponseEntity.ok(leaveRequestRepository.findByStatus("PENDING"));
    }

    @PutMapping("/admin/leave/status/{leaveId}")
    public ResponseEntity<?> updateLeaveStatus(@PathVariable Long leaveId,
                                               @RequestParam String status,
                                               @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                               @RequestParam(required = false) String role,
                                               HttpSession session) {
        if (!checkIsAdmin(session, roleHeader, role)) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden: Admin privileges required"));
        }
        LeaveRequest req = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave request not found: " + leaveId));
        req.setStatus(status.toUpperCase());
        return ResponseEntity.ok(leaveRequestRepository.save(req));
    }

    // ==========================================
    // 4. AWARD POINTS & CALCULATE POINTS (ADMIN ONLY!)
    // ==========================================
    @PostMapping("/admin/points/award/{employeeId}")
    public ResponseEntity<?> awardPoints(@PathVariable Long employeeId,
                                         @RequestParam Integer points,
                                         @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                         @RequestParam(required = false) String role,
                                         HttpSession session) {
        // STRICT RBAC: Strictly restricted to Admin only!
        if (!checkIsAdmin(session, roleHeader, role)) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden: Only Admin can award points!"));
        }

        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));
        emp.setPoints((emp.getPoints() != null ? emp.getPoints() : 0) + (points != null ? points : 0));
        return ResponseEntity.ok(employeeRepository.save(emp));
    }

    // ==========================================
    // 5. MANAGE PROJECTS (ADMIN: FULL / EMPLOYEES: READ-ONLY)
    // ==========================================
    @GetMapping("/projects")
    public ResponseEntity<List<Project>> getAllProjects() {
        // Both Admins and Employees can view projects
        return ResponseEntity.ok(projectRepository.findAll());
    }

    @PostMapping("/admin/projects")
    public ResponseEntity<?> createProject(@RequestParam(required = false) String projectName,
                                           @RequestParam(required = false) String clientName,
                                           @RequestParam(required = false, defaultValue = "IN_PROGRESS") String status,
                                           @RequestParam(required = false, defaultValue = "0") Integer progress,
                                           @RequestParam(required = false) String deadline,
                                           @RequestParam(required = false, defaultValue = "MEDIUM") String priority,
                                           @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                           @RequestParam(required = false) String role,
                                           HttpSession session) {
        // STRICT RBAC: Project Creation is strictly for Admin only!
        if (!checkIsAdmin(session, roleHeader, role)) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden: Only Admin can manage projects!"));
        }

        if (projectName == null || projectName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Project name cannot be empty"));
        }

        Project p = new Project();
        p.setProjectName(projectName.trim());
        p.setClientName(clientName != null && !clientName.trim().isEmpty() ? clientName.trim() : "Internal");
        p.setStatus(status != null && !status.trim().isEmpty() ? status.trim() : "IN_PROGRESS");
        p.setProgress(progress != null ? Math.min(100, Math.max(0, progress)) : 0);
        p.setDeadline(deadline != null ? deadline.trim() : "TBD");
        p.setPriority(priority != null ? priority.toUpperCase() : "MEDIUM");
        return ResponseEntity.ok(projectRepository.save(p));
    }

    @PutMapping("/admin/projects/{projectId}")
    public ResponseEntity<?> updateProject(@PathVariable Long projectId,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) Integer progress,
                                           @RequestParam(required = false) String deadline,
                                           @RequestParam(required = false) String priority,
                                           @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                           @RequestParam(required = false) String role,
                                           HttpSession session) {
        if (!checkIsAdmin(session, roleHeader, role)) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden: Only Admin can update projects!"));
        }

        Project p = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

        if (status != null && !status.isBlank()) p.setStatus(status.trim());
        if (progress != null) p.setProgress(Math.min(100, Math.max(0, progress)));
        if (deadline != null && !deadline.isBlank()) p.setDeadline(deadline.trim());
        if (priority != null && !priority.isBlank()) p.setPriority(priority.toUpperCase());

        return ResponseEntity.ok(projectRepository.save(p));
    }

    @DeleteMapping("/admin/projects/{projectId}")
    public ResponseEntity<?> deleteProject(@PathVariable Long projectId,
                                           @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                           @RequestParam(required = false) String role,
                                           HttpSession session) {
        if (!checkIsAdmin(session, roleHeader, role)) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden: Only Admin can delete projects!"));
        }

        projectRepository.deleteById(projectId);
        return ResponseEntity.ok(Map.of("message", "Project deleted successfully"));
    }

    // ==========================================
    // 6. SYSTEM SETTINGS (ADMIN ONLY!)
    // ==========================================
    @GetMapping("/admin/settings")
    public ResponseEntity<?> getSettings(@RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                         @RequestParam(required = false) String role,
                                         HttpSession session) {
        if (!checkIsAdmin(session, roleHeader, role)) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden: Admin privileges required"));
        }
        return ResponseEntity.ok(systemSettings);
    }

    @PostMapping("/admin/settings")
    public ResponseEntity<?> updateSettings(@RequestParam(required = false) String standardWorkHours,
                                            @RequestParam(required = false) String pointsPerHour,
                                            @RequestParam(required = false) String companyName,
                                            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
                                            @RequestParam(required = false) String role,
                                            HttpSession session) {
        if (!checkIsAdmin(session, roleHeader, role)) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden: Admin privileges required"));
        }

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
    // 7. UNIQUE FEATURES: LEADERBOARD, STANDUP GENERATOR, CSV EXPORT
    // ==========================================
    @GetMapping("/leaderboard")
    public ResponseEntity<List<Map<String, Object>>> getLeaderboard() {
        List<Employee> employees = employeeRepository.findAll();
        employees.sort((a, b) -> Integer.compare(b.getPoints() != null ? b.getPoints() : 0, a.getPoints() != null ? a.getPoints() : 0));

        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        for (Employee emp : employees) {
            Map<String, Object> map = new HashMap<>();
            map.put("rank", rank);
            map.put("employeeId", emp.getEmployeeId());
            map.put("name", emp.getUser() != null ? emp.getUser().getName() : "Emp #" + emp.getEmployeeId());
            map.put("department", emp.getDepartment() != null ? emp.getDepartment() : "General");
            map.put("designation", emp.getDesignation() != null ? emp.getDesignation() : "Staff");
            map.put("points", emp.getPoints() != null ? emp.getPoints() : 0);

            int pts = emp.getPoints() != null ? emp.getPoints() : 0;
            String medal = rank == 1 ? "🥇 Champion" : rank == 2 ? "🥈 Silver" : rank == 3 ? "🥉 Bronze" : "⚡ Contributor";
            String badge = pts >= 100 ? "Diamond Star 💎" : pts >= 50 ? "Gold Achiever 🌟" : "Rising Star ✨";
            map.put("medal", medal);
            map.put("badge", badge);
            result.add(map);
            rank++;
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/standup/summary/{employeeId}")
    public ResponseEntity<Map<String, String>> generateDailyStandup(@PathVariable Long employeeId) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        List<WorkLog> logs = workLogRepository.findByEmployeeEmployeeId(employeeId);
        LocalDate today = LocalDate.now();

        List<WorkLog> todayLogs = logs.stream()
                .filter(l -> today.equals(l.getWorkDate()))
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("📋 *Daily Standup Summary - ").append(emp.getUser() != null ? emp.getUser().getName() : "Team Member").append("*\n\n");
        sb.append("✅ *Tasks Completed / In-Progress Today (").append(today).append("):*\n");

        if (todayLogs.isEmpty()) {
            sb.append("- No tasks logged yet for today. Ready for assignments.\n");
        } else {
            for (WorkLog l : todayLogs) {
                sb.append("• [").append(l.getCategory()).append("] ")
                  .append(l.getTaskDescription())
                  .append(" (").append(l.getHoursWorked()).append(" hrs | Priority: ").append(l.getPriority()).append(" | Status: ").append(l.getStatus()).append(")\n");
            }
        }

        double todayHours = todayLogs.stream().mapToDouble(w -> w.getHoursWorked() != null ? w.getHoursWorked() : 0.0).sum();
        sb.append("\n⏱ *Total Hours Logged Today:* ").append(todayHours).append(" hrs\n");
        sb.append("🏆 *Reward Points Earned:* ").append(emp.getPoints() != null ? emp.getPoints() : 0).append(" pts\n");
        sb.append("🚧 *Blockers:* None\n");

        return ResponseEntity.ok(Map.of("standupText", sb.toString(), "date", today.toString()));
    }

    @GetMapping("/reports/export-csv")
    public ResponseEntity<String> exportWorkLogsCsv() {
        List<WorkLog> logs = workLogRepository.findAll();
        StringBuilder csv = new StringBuilder();
        csv.append("WorkLogId,Date,Employee,Department,Category,Priority,Hours,Status,TaskDescription\n");
        for (WorkLog l : logs) {
            csv.append(l.getWorkLogId()).append(",")
               .append(l.getWorkDate()).append(",")
               .append("\"").append(l.getEmployee() != null && l.getEmployee().getUser() != null ? l.getEmployee().getUser().getName() : "N/A").append("\",")
               .append("\"").append(l.getEmployee() != null ? l.getEmployee().getDepartment() : "N/A").append("\",")
               .append("\"").append(l.getCategory() != null ? l.getCategory() : "Development").append("\",")
               .append("\"").append(l.getPriority() != null ? l.getPriority() : "MEDIUM").append("\",")
               .append(l.getHoursWorked() != null ? l.getHoursWorked() : 0.0).append(",")
               .append(l.getStatus()).append(",")
               .append("\"").append(l.getTaskDescription() != null ? l.getTaskDescription().replace("\"", "\"\"") : "").append("\"\n");
        }
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=\"cwls_worklogs.csv\"")
                .body(csv.toString());
    }

    // ==========================================
    // 8. PERFORMANCE & PRODUCTIVITY SUMMARY REPORTS
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