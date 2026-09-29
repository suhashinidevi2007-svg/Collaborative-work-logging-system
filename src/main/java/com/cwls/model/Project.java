package com.cwls.model;

import javax.persistence.*;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long projectId;

    @Column(nullable = false)
    private String projectName;

    private String clientName;
    private String status = "IN_PROGRESS"; // NOT_STARTED, IN_PROGRESS, COMPLETED
    private Integer progress = 0; // 0 - 100%
    private String deadline;
    private String priority = "MEDIUM"; // LOW, MEDIUM, HIGH, CRITICAL

    public Project() {}

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getProgress() { return progress != null ? progress : 0; }
    public void setProgress(Integer progress) { this.progress = progress; }

    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }

    public String getPriority() { return priority != null ? priority : "MEDIUM"; }
    public void setPriority(String priority) { this.priority = priority; }
}