package com.cwls.model;

import javax.persistence.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long attendanceId;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    private LocalDate date;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private Double totalHours;
    private String status;

    public void clockIn() {
        this.date = LocalDate.now();
        this.checkInTime = LocalTime.now();
        this.status = "PRESENT";
    }

    public void clockOut() {
        this.checkOutTime = LocalTime.now();
        if (this.checkInTime != null) {
            long minutes = Duration.between(this.checkInTime, this.checkOutTime).toMinutes();
            this.totalHours = Math.round((minutes / 60.0) * 100.0) / 100.0;
        }
    }

    public Long getAttendanceId() { return attendanceId; }
    public void setAttendanceId(Long attendanceId) { this.attendanceId = attendanceId; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalTime checkInTime) { this.checkInTime = checkInTime; }
    public LocalTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalTime checkOutTime) { this.checkOutTime = checkOutTime; }
    public Double getTotalHours() { return totalHours; }
    public void setTotalHours(Double totalHours) { this.totalHours = totalHours; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}