package com.sylvester.jobservice.service;

import com.sylvester.jobservice.dtos.CompanyDto;
import com.sylvester.jobservice.dtos.JobResponse;
import com.sylvester.jobservice.dtos.PostJobRequest;
import com.sylvester.jobservice.entity.EmploymentType;
import com.sylvester.jobservice.entity.Job;
import com.sylvester.jobservice.entity.JobStatus;
import com.sylvester.jobservice.entity.WorkMode;
import com.sylvester.jobservice.exceptions.NotFoundException;
import com.sylvester.jobservice.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private CompanyClient companyClient;

    @InjectMocks
    private JobServiceImpl jobService;

    private PostJobRequest request;
    private CompanyDto company;
    private Job job;

    @BeforeEach
    void setUp() {
        request = new PostJobRequest(
                "Backend Engineer",
                "Request Location",
                "FULL_TIME",
                "REMOTE",
                "Build and maintain backend services",
                "Own APIs and service reliability",
                "Strong Java and Spring Boot experience",
                "Health insurance and remote work",
                "Work with a focused engineering team"
        );

        company = new CompanyDto(
                "company-id",
                "owner-id",
                "Acme Inc",
                "owner@test.example.com",
                "https://test.example.com",
                "Lagos",
                "A software company"
        );

        job = Job.builder()
                .id("job-id")
                .companyId("company-id")
                .title("Backend Engineer")
                .location("Zagreb")
                .employmentType(EmploymentType.FULL_TIME)
                .workMode(WorkMode.REMOTE)
                .description("Build and maintain backend services")
                .responsibilities("Own APIs and service reliability")
                .qualifications("Strong Java and Spring Boot experience")
                .benefits("Health insurance and remote work")
                .whyJoinUs("Work with a focused engineering team")
                .postedAt(LocalDateTime.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .status(JobStatus.RECRUITING)
                .build();
    }

    @Test
    void postJob_shouldCreateJobFromRequestAndCompany() {
        given(companyClient.getJobById("Bearer token")).willReturn(company);

        jobService.postJob(request, "Bearer token");

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(jobCaptor.capture());
        Job savedJob = jobCaptor.getValue();

        assertEquals(company.getId(), savedJob.getCompanyId());
        assertEquals(request.title(), savedJob.getTitle());
        assertEquals(company.getLocation(), savedJob.getLocation());
        assertEquals(EmploymentType.FULL_TIME, savedJob.getEmploymentType());
        assertEquals(WorkMode.REMOTE, savedJob.getWorkMode());
        assertEquals(request.description(), savedJob.getDescription());
        assertEquals(request.responsibilities(), savedJob.getResponsibilities());
        assertEquals(request.qualifications(), savedJob.getQualifications());
        assertEquals(request.benefits(), savedJob.getBenefits());
        assertEquals(request.whyJoinUs(), savedJob.getWhyJoinUs());
        assertEquals(JobStatus.RECRUITING, savedJob.getStatus());
        assertNotNull(savedJob.getPostedAt());
        assertNotNull(savedJob.getExpiresAt());
    }

    @Test
    void deleteJob_shouldDeleteExistingJob() {
        given(jobRepository.findById("job-id")).willReturn(Optional.of(job));

        jobService.deleteJob("job-id");

        verify(jobRepository).delete(job);
    }

    @Test
    void deleteJob_shouldThrowNotFoundException_whenJobDoesNotExist() {
        given(jobRepository.findById("job-id")).willReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> jobService.deleteJob("job-id"));

        verify(jobRepository, never()).delete(any(Job.class));
    }

    @Test
    void getJobById_shouldReturnJobResponse() {
        given(jobRepository.findById("job-id")).willReturn(Optional.of(job));
        given(companyClient.getJobById("Bearer token")).willReturn(company);

        JobResponse response = jobService.getJobById(
                "job-id",
                "Bearer token",
                "applicant@example.com"
        );

        assertEquals(job.getId(), response.getJobId());
        assertEquals(job.getCompanyId(), response.getCompanyId());
        assertEquals(job.getTitle(), response.getJobName());
        assertEquals(job.getPostedAt(), response.getPostedAt());
        assertEquals(company.getName(), response.getCompanyName());
        assertEquals("applicant@example.com", response.getCompanyOwnerEmail());
    }

    @Test
    void getJobById_shouldThrowNotFoundException_whenJobDoesNotExist() {
        given(jobRepository.findById("job-id")).willReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> jobService.getJobById("job-id", "Bearer token", "applicant@example.com")
        );

        verify(companyClient, never()).getJobById(any(String.class));
    }

    @Test
    void getJobsByCompanyId_shouldReturnJobsForCompany() {
        Job secondJob = Job.builder()
                .id("second-job-id")
                .companyId("company-id")
                .title("Frontend Engineer")
                .postedAt(LocalDateTime.now().minusDays(1))
                .build();
        given(companyClient.getJobById("Bearer token")).willReturn(company);
        given(jobRepository.findJobsByCompanyId(company.getId())).willReturn(List.of(job, secondJob));

        List<JobResponse> responses = jobService.getJobsByCompanyId("Bearer token");

        assertEquals(2, responses.size());
        assertEquals("job-id", responses.get(0).getJobId());
        assertEquals("company-id", responses.get(0).getCompanyId());
        assertEquals("Backend Engineer", responses.get(0).getJobName());
        assertEquals("Acme Inc", responses.get(0).getCompanyName());
        assertEquals("second-job-id", responses.get(1).getJobId());
        assertEquals("Frontend Engineer", responses.get(1).getJobName());
    }

    @Test
    void closeJob_shouldSetStatusToClosed() {
        given(jobRepository.findById("job-id")).willReturn(Optional.of(job));

        jobService.closeJob("job-id");

        assertEquals(JobStatus.CLOSED, job.getStatus());
        verify(jobRepository).save(job);
    }

    @Test
    void closeJob_shouldThrowNotFoundException_whenJobDoesNotExist() {
        given(jobRepository.findById("job-id")).willReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> jobService.closeJob("job-id"));

        verify(jobRepository, never()).save(any(Job.class));
    }
}
