package com.sylvester.jobservice.service;


import com.sylvester.jobservice.dtos.JobDto;
import com.sylvester.jobservice.dtos.JobResponse;
import com.sylvester.jobservice.dtos.PostJobRequest;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface JobService {

    void postJob(PostJobRequest request, String ownerId);
    void updateJob(PostJobRequest request);
    void deleteJob(String id);

    JobResponse getJobById(String jobId,String header, String userEmail);

    Page<JobDto> findJobByTitle(String title, int page, int size);

    JobResponse getJob(String jobId);

    List<JobResponse> getJobsByCompanyId(String header);

    void closeJob(String id);

    Page<JobDto> getAllJobs(int page, int size);
}
