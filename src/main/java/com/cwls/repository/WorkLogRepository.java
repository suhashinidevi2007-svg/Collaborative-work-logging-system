package com.cwls.repository;

import com.cwls.model.WorkLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkLogRepository extends JpaRepository<WorkLog, Long> {
    List<WorkLog> findByStatus(String status);
    List<WorkLog> findByEmployeeEmployeeId(Long employeeId);
}