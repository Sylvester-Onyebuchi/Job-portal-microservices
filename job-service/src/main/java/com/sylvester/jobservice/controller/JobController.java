package com.sylvester.jobservice.controller;


import com.sylvester.jobservice.dtos.JobDto;
import com.sylvester.jobservice.dtos.PostJobRequest;
import com.sylvester.jobservice.dtos.JobResponse;
import com.sylvester.jobservice.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/jobs")
public class JobController {
    private final JobService jobService;

    @PostMapping("/post-job")
    public ResponseEntity<?>  postJob(@Valid @RequestBody PostJobRequest postJobRequest,
                                      @RequestHeader("Authorization") String header) {

        jobService.postJob(postJobRequest, header);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/company")
    public ResponseEntity<List<JobResponse>> getCompanyJobs(@RequestHeader("Authorization") String header) {
        List<JobResponse> response = jobService.getJobsByCompanyId(header);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("job/{id}")
    public ResponseEntity<JobResponse> getJobById(@PathVariable String id, @RequestHeader("Authorization") String header,
                                                  @AuthenticationPrincipal Jwt jwt) {
        String userEmail = jwt.getClaimAsString("email");
        JobResponse response = jobService.getJobById(id, header, userEmail);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<JobDto>> searchJobs(
            @RequestParam String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                jobService.findJobByTitle(title, page, size)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJobById(@PathVariable String id){
       JobResponse response = jobService.getJob(id);
       return new ResponseEntity<>(response, HttpStatus.OK);

    }

    @GetMapping
    public ResponseEntity<Page<JobDto>> getAllJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(jobService.getAllJobs(page, size));
    }

   @DeleteMapping("/job/{id}/delete")
    public ResponseEntity<?> deleteJob(@PathVariable String id) {
        jobService.deleteJob(id);
        return new ResponseEntity<>(HttpStatus.OK);
   }

}
