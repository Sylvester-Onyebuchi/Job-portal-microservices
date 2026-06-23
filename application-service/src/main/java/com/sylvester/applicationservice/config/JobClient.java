package com.sylvester.applicationservice.config;


import com.sylvester.applicationservice.dtos.JobResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;



@FeignClient(name = "job-service")
public interface JobClient {
    @GetMapping("/api/v1/jobs/job/{jobId}")
    JobResponse getJobById(@PathVariable String jobId, @RequestHeader("Authorization") String header);

}
