package com.cwls.repository;

import com.cwls.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployeeEmployeeId(Long employeeId);
    List<LeaveRequest> findByStatus(String status);
}