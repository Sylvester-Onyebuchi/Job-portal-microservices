package com.sylvester.jobservice.service;

import com.sylvester.jobservice.dtos.CompanyDto;
import com.sylvester.jobservice.dtos.JobDto;
import com.sylvester.jobservice.dtos.JobResponse;
import com.sylvester.jobservice.dtos.PostJobRequest;
import com.sylvester.jobservice.entity.EmploymentType;
import com.sylvester.jobservice.entity.Job;
import com.sylvester.jobservice.entity.JobStatus;
import com.sylvester.jobservice.entity.WorkMode;
import com.sylvester.jobservice.exceptions.AlreadyExistsException;
import com.sylvester.jobservice.exceptions.NotFoundException;
import com.sylvester.jobservice.repository.JobRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final CompanyClient companyClient;

@PostConstruct
public void init(){
    System.out.println(companyClient);
}

    @Override
    @Transactional
    public void postJob(PostJobRequest request, String header) {

        CompanyDto company = companyClient.getJobById(header);


       if (jobRepository.existsByCompanyIdAndTitleIgnoreCase(company.getId(), request.title())){
           throw new AlreadyExistsException("Job already exists");
       }
        var newJob = Job.builder()
                .companyId(company.getId())
                .title(request.title())
                .description(request.description())
                .benefits(request.benefits())
                .location(request.location())
                .employmentType(EmploymentType.valueOf(request.employmentType()))
                .workMode(WorkMode.valueOf(request.workMode()))
                .responsibilities(request.responsibilities())
                .qualifications(request.qualifications())
                .whyJoinUs(request.whyJoinUs())
                .postedAt(LocalDateTime.now())
                .expiresAt(Instant.now().plus(90, ChronoUnit.DAYS))
                .status(JobStatus.RECRUITING)
                .build();
        jobRepository.save(newJob);


    }

    @Override
    @Transactional
    public void updateJob(PostJobRequest request) {



    }

    @Override
    @Transactional
    public void deleteJob(String id) {
        var job = jobRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Job not found")
        );
        jobRepository.delete(job);

    }

    @Override
    @Transactional
    public JobResponse getJobById(String jobId, String header,String userEmail) {
        var job = jobRepository.findById(jobId).orElseThrow(
                () -> new NotFoundException("Job not found")
        );

        CompanyDto company = companyClient.getJobById(header);

        JobResponse jobResponse = new JobResponse();
        jobResponse.setCompanyId(job.getCompanyId());
        jobResponse.setJobId(job.getId());
        jobResponse.setJobName(job.getTitle());
        jobResponse.setPostedAt(job.getPostedAt());
        jobResponse.setCompanyName(company.getName());
        jobResponse.setCompanyOwnerEmail(userEmail);
        return jobResponse;


    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobDto> findJobByTitle(String title, int page, int size) {

        Pageable sortedPageable = PageRequest.of(
                page,
                size,
                Sort.by("postedAt").descending()
        );

        return jobRepository
                .findByTitleContainingIgnoreCase(title, sortedPageable)
                .map(this::toResponse);
    }

    @Override
    public JobResponse getJob(String jobId){
        var job = jobRepository.findById(jobId).orElseThrow(
                () -> new NotFoundException("Job not found")
        );
        JobResponse jobResponse = new JobResponse();
        jobResponse.setJobId(job.getId());
        jobResponse.setJobName(job.getTitle());
        jobResponse.setPostedAt(job.getPostedAt());
        return jobResponse;
    }

    @Override
    @Transactional
    public List<JobResponse> getJobsByCompanyId(String header) {
        CompanyDto company = companyClient.getJobById(header);
        List<Job> jobs = jobRepository.findJobsByCompanyId(company.getId());
        return jobs.stream().map(job -> {
            JobResponse jobResponse = new JobResponse();
            jobResponse.setJobId(job.getId());
            jobResponse.setCompanyId(job.getCompanyId());
            jobResponse.setJobName(job.getTitle());
            jobResponse.setPostedAt(job.getPostedAt());
            jobResponse.setCompanyName(company.getName());
            return jobResponse;
        }).toList();
    }





    @Override
    @Transactional
    public void closeJob(String id) {

        var job = jobRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Job not found")
        );
        job.setStatus(JobStatus.CLOSED);
        jobRepository.save(job);

    }

    @Override
    public Page<JobDto> getAllJobs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        return jobRepository.findAll(pageable)
                .map(this::toResponse);
    }


    private JobDto toResponse(Job job) {
        return new JobDto(
                job.getId(),
                job.getTitle(),
                job.getEmploymentType(),
                job.getWorkMode(),
                job.getLocation(),
                job.getDescription(),
                job.getResponsibilities(),
                job.getQualifications(),
                job.getBenefits(),
                job.getWhyJoinUs(),
                job.getPostedAt()
        );
    }




}
