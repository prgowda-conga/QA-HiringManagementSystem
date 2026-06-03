package com.hiring.response;

/**
 * POJO for Application API response body.
 */
public class ApplicationResponse {

    private int id;
    private String appliedAt;
    private String coverLetter;
    private String status;
    private UserResponse candidate;
    private JobResponse job;
    private JobResponse appliedJob;

    public ApplicationResponse() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(String appliedAt) {
        this.appliedAt = appliedAt;
    }

    public String getCoverLetter() { return coverLetter; }
    public void setCoverLetter(String coverLetter) { this.coverLetter = coverLetter; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }


    public UserResponse getCandidate() {
        return candidate;
    }

    public void setCandidate(UserResponse candidate) {
        this.candidate = candidate;
    }

    public JobResponse getJob() {
        return job;
    }

    public void setJob(JobResponse job) {
        this.job = job;
    }

    public JobResponse getAppliedJob() {
        return appliedJob;
    }

    public void setAppliedJob(JobResponse appliedJob) {
        this.appliedJob = appliedJob;
    }
}

