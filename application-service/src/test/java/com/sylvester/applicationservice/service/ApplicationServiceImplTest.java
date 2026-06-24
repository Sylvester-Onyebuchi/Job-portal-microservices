package com.sylvester.applicationservice.service;

import com.sylvester.applicationservice.config.JobClient;
import com.sylvester.applicationservice.dtos.ApplicationResponse;
import com.sylvester.applicationservice.dtos.CvResponse;
import com.sylvester.applicationservice.dtos.EducationRequest;
import com.sylvester.applicationservice.dtos.ExperienceRequest;
import com.sylvester.applicationservice.dtos.JobResponse;
import com.sylvester.applicationservice.dtos.SubmitApplicationRequest;
import com.sylvester.applicationservice.entity.Application;
import com.sylvester.applicationservice.entity.ApplicationStatus;
import com.sylvester.applicationservice.entity.Education;
import com.sylvester.applicationservice.entity.Experience;
import com.sylvester.applicationservice.exceptions.AlreadyExistsException;
import com.sylvester.applicationservice.exceptions.NotFoundException;
import com.sylvester.applicationservice.rabbitMq.ApplicationEvent;
import com.sylvester.applicationservice.rabbitMq.CompanyAlertEvent;
import com.sylvester.applicationservice.rabbitMq.Producer;
import com.sylvester.applicationservice.repository.ApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private JobClient jobClient;

    @Mock
    private Producer producer;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    private SubmitApplicationRequest request;
    private MockMultipartFile file;
    private JobResponse job;
    private Application application;

    @BeforeEach
    void setUp() {
        request = new SubmitApplicationRequest(
                "job-id",
                "Sylvester Onah",
                "test@example.com",
                "+3858012345678",
                "I would like to apply for this role.",
                "https://test.example.com",
                "https://linkedin.example.com/in/test",
                "https://github.com/test",
                Set.of("Java", "Spring Boot"),
                Set.of("English", "Croatian"),
                Set.of(new EducationRequest("Zagreb University of Applied Science", "BSc", "Informatics", 2024, 2027)),
                Set.of(new ExperienceRequest("Infobip", "Backend Engineer", "Built APIs", 2025, 2026, "false"))
        );

        file = new MockMultipartFile(
                "cvFile",
                "sylvester-cv.pdf",
                "application/pdf",
                "pdf-content".getBytes()
        );

        job = new JobResponse(
                "job-id",
                "company-id",
                "Infobip Inc",
                "owner@test.example.com",
                "Backend Engineer",
                LocalDateTime.of(2026, 1, 2, 3, 4)
        );

        application = Application.builder()
                .id("application-id")
                .jobId("job-id")
                .candidateUserId("candidate-id")
                .companyId("company-id")
                .fullName("Sylvester Onah")
                .email("ada@example.com")
                .phoneNumber("+3858012345678")
                .coverLetter("I would like to apply for this role.")
                .portfolioUrl("https://test.example.com")
                .linkedInUrl("https://linkedin.example.com/in/test")
                .githubUrl("https://github.com/test")
                .skills(Set.of("Java", "Spring Boot"))
                .languages(Set.of("English", "Croatian"))
                .education(Set.of(new Education("Zagreb University of Applied Science ", "BSc", "Informatics", 2024, 2027)))
                .experience(Set.of(new Experience("Infobip Inc", "Backend Engineer", "Built APIs", 2025, 2026, false)))
                .status(ApplicationStatus.SUBMITTED)
                .cv("pdf-content".getBytes())
                .cvFilename("sylvester-cv.pdf")
                .cvContent("application/pdf")
                .appliedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void submitApplication_shouldSaveApplicationAndPublishEvents() throws IOException {
        given(jobClient.getJobById(request.jobId(), "Bearer token")).willReturn(job);
        given(applicationRepository.existsByJobIdAndCandidateUserId(job.getJobId(), "candidate-id"))
                .willReturn(false);
        given(applicationRepository.save(any(Application.class))).willReturn(application);

        applicationService.submitApplication(request, file, "candidate-id", "Bearer token");

        ArgumentCaptor<Application> applicationCaptor = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(applicationCaptor.capture());
        Application savedApplication = applicationCaptor.getValue();
        assertEquals(job.getJobId(), savedApplication.getJobId());
        assertEquals("candidate-id", savedApplication.getCandidateUserId());
        assertEquals(job.getCompanyId(), savedApplication.getCompanyId());
        assertEquals(request.fullName(), savedApplication.getFullName());
        assertEquals(request.email(), savedApplication.getEmail());
        assertEquals(request.phoneNumber(), savedApplication.getPhoneNumber());
        assertEquals(request.coverLetter(), savedApplication.getCoverLetter());
        assertEquals(request.portfolioUrl(), savedApplication.getPortfolioUrl());
        assertEquals(request.linkedInUrl(), savedApplication.getLinkedInUrl());
        assertEquals(request.githubUrl(), savedApplication.getGithubUrl());
        assertEquals(request.skills(), savedApplication.getSkills());
        assertEquals(request.languages(), savedApplication.getLanguages());
        assertEquals(ApplicationStatus.SUBMITTED, savedApplication.getStatus());
        assertEquals("sylvester-cv.pdf", savedApplication.getCvFilename());
        assertEquals("application/pdf", savedApplication.getCvContent());
        assertArrayEquals("pdf-content".getBytes(), savedApplication.getCv());
        assertNotNull(savedApplication.getAppliedAt());
        assertEquals(1, savedApplication.getEducation().size());
        assertEquals(1, savedApplication.getExperience().size());

        ArgumentCaptor<ApplicationEvent> applicationEventCaptor =
                ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(producer).sendMessage(applicationEventCaptor.capture());
        ApplicationEvent applicationEvent = applicationEventCaptor.getValue();
        assertEquals(application.getEmail(), applicationEvent.candidateEmail());
        assertEquals(application.getFullName(), applicationEvent.candidateFullName());
        assertEquals(job.getJobName(), applicationEvent.companyName());
        assertEquals(job.getCompanyName(), applicationEvent.jobName());

        ArgumentCaptor<CompanyAlertEvent> companyAlertCaptor =
                ArgumentCaptor.forClass(CompanyAlertEvent.class);
        verify(producer).sendMessageToCompany(companyAlertCaptor.capture());
        CompanyAlertEvent companyAlertEvent = companyAlertCaptor.getValue();
        assertEquals(job.getCompanyOwnerEmail(), companyAlertEvent.companyEmail());
        assertEquals(application.getEmail(), companyAlertEvent.candidateEmail());
        assertEquals(application.getFullName(), companyAlertEvent.candidateFullName());
        assertEquals(job.getJobName(), companyAlertEvent.jobName());
    }

    @Test
    void submitApplication_shouldThrowAlreadyExistsException_whenCandidateAlreadyApplied() {
        given(jobClient.getJobById(request.jobId(), "Bearer token")).willReturn(job);
        given(applicationRepository.existsByJobIdAndCandidateUserId(job.getJobId(), "candidate-id"))
                .willReturn(true);

        assertThrows(
                AlreadyExistsException.class,
                () -> applicationService.submitApplication(request, file, "candidate-id", "Bearer token")
        );

        verify(applicationRepository, never()).save(any(Application.class));
        verify(producer, never()).sendMessage(any(ApplicationEvent.class));
        verify(producer, never()).sendMessageToCompany(any(CompanyAlertEvent.class));
    }

    @Test
    void submitApplication_shouldPropagateIOException_whenCvCannotBeRead() throws IOException {
        MultipartFile brokenFile = org.mockito.Mockito.mock(MultipartFile.class);
        given(jobClient.getJobById(request.jobId(), "Bearer token")).willReturn(job);
        given(applicationRepository.existsByJobIdAndCandidateUserId(job.getJobId(), "candidate-id"))
                .willReturn(false);
        given(brokenFile.getBytes()).willThrow(new IOException("cannot read file"));

        assertThrows(
                IOException.class,
                () -> applicationService.submitApplication(request, brokenFile, "candidate-id", "Bearer token")
        );

        verify(applicationRepository, never()).save(any(Application.class));
        verify(producer, never()).sendMessage(any(ApplicationEvent.class));
        verify(producer, never()).sendMessageToCompany(any(CompanyAlertEvent.class));
    }

    @Test
    void deleteApplication_shouldDeleteApplication_whenCandidateOwnsIt() {
        given(applicationRepository.findApplicationById("application-id")).willReturn(Optional.of(application));

        applicationService.deleteApplication("application-id", "candidate-id");

        verify(applicationRepository, times(2)).delete(application);
    }

    @Test
    void deleteApplication_shouldThrowRuntimeException_whenCandidateDoesNotOwnIt() {
        given(applicationRepository.findApplicationById("application-id")).willReturn(Optional.of(application));

        assertThrows(
                RuntimeException.class,
                () -> applicationService.deleteApplication("application-id", "other-candidate-id")
        );

        verify(applicationRepository).delete(application);
    }

    @Test
    void deleteApplication_shouldThrowNoSuchElementException_whenApplicationDoesNotExist() {
        given(applicationRepository.findApplicationById("application-id")).willReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> applicationService.deleteApplication("application-id", "candidate-id")
        );

        verify(applicationRepository, never()).delete(any(Application.class));
    }

    @Test
    void findAllByCandidateUserId_shouldReturnMappedApplications() {
        given(applicationRepository.findApplicationByCandidateUserId("candidate-id"))
                .willReturn(Optional.of(application));
        given(applicationRepository.findAllByCandidateUserId("candidate-id")).willReturn(List.of(application));

        List<ApplicationResponse> responses = applicationService.findAllByCandidateUserId("candidate-id");

        assertEquals(1, responses.size());
        ApplicationResponse response = responses.getFirst();
        assertEquals(application.getId(), response.getId());
        assertEquals(application.getCandidateUserId(), response.getCandidateUserId());
        assertEquals(application.getFullName(), response.getFullName());
        assertEquals(application.getEmail(), response.getEmail());
        assertEquals(application.getGithubUrl(), response.getGithubLink());
        assertEquals(application.getExperience(), response.getExperience());
        assertEquals(application.getEducation(), response.getEducation());
    }

    @Test
    void findAllByCandidateUserId_shouldThrowNotFoundException_whenCandidateHasNoApplication() {
        given(applicationRepository.findApplicationByCandidateUserId("candidate-id")).willReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> applicationService.findAllByCandidateUserId("candidate-id")
        );
    }

    @Test
    void getCv_shouldReturnCvResponse() {
        given(applicationRepository.findApplicationByCandidateUserId("candidate-id"))
                .willReturn(Optional.of(application));

        CvResponse response = applicationService.getCv("candidate-id");

        assertEquals(application.getCvFilename(), response.getName());
        assertEquals(application.getCvContent(), response.getContentType());
        assertArrayEquals(application.getCv(), response.getImage());
    }

    @Test
    void getCv_shouldThrowNotFoundException_whenCandidateHasNoApplication() {
        given(applicationRepository.findApplicationByCandidateUserId("candidate-id")).willReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> applicationService.getCv("candidate-id"));
    }
}
