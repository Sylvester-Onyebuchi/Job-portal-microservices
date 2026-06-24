package com.sylvester.jobservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sylvester.jobservice.dtos.JobDto;
import com.sylvester.jobservice.dtos.JobResponse;
import com.sylvester.jobservice.dtos.PostJobRequest;
import com.sylvester.jobservice.entity.EmploymentType;
import com.sylvester.jobservice.entity.WorkMode;
import com.sylvester.jobservice.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class JobControllerTest {

    @Mock
    private JobService jobService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private PostJobRequest request;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        request = new PostJobRequest(
                "Backend Engineer",
                "Zagreb",
                "FULL_TIME",
                "REMOTE",
                "Build and maintain backend services",
                "Own APIs and service reliability",
                "Strong Java and Spring Boot experience",
                "Health insurance and remote work",
                "Work with a focused engineering team"
        );

        jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("email", "applicant@example.com")
                .build();

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new JobController(jobService))
                .setValidator(validator)
                .setCustomArgumentResolvers(new JwtArgumentResolver(jwt))
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void postJob_shouldReturnCreatedAndCallServiceWithAuthorizationHeader() throws Exception {
        mockMvc.perform(post("/api/v1/jobs/post-job")
                        .header("Authorization", "Bearer token")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(jobService).postJob(request, "Bearer token");
    }

    @Test
    void postJob_shouldReturnBadRequest_whenPayloadIsInvalid() throws Exception {
        PostJobRequest invalidRequest = new PostJobRequest(
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "optional"
        );

        mockMvc.perform(post("/api/v1/jobs/post-job")
                        .header("Authorization", "Bearer token")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(jobService);
    }

    @Test
    void getCompanyJobs_shouldReturnJobsFromService() throws Exception {
        JobResponse firstJob = new JobResponse(
                "job-id",
                "company-id",
                "Backend Engineer",
                "Test Inc",
                null,
                LocalDateTime.of(2026, 1, 2, 3, 4)
        );
        JobResponse secondJob = new JobResponse(
                "second-job-id",
                "company-id",
                "Frontend Engineer",
                "Test Inc",
                null,
                LocalDateTime.of(2026, 1, 3, 3, 4)
        );
        given(jobService.getJobsByCompanyId("Bearer token")).willReturn(List.of(firstJob, secondJob));

        mockMvc.perform(get("/api/v1/jobs/company")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobId").value("job-id"))
                .andExpect(jsonPath("$[0].companyId").value("company-id"))
                .andExpect(jsonPath("$[0].jobName").value("Backend Engineer"))
                .andExpect(jsonPath("$[0].companyName").value("Test Inc"))
                .andExpect(jsonPath("$[1].jobId").value("second-job-id"))
                .andExpect(jsonPath("$[1].jobName").value("Frontend Engineer"));

        verify(jobService).getJobsByCompanyId("Bearer token");
    }

    @Test
    void getJobById_shouldReturnJobFromServiceAndPassJwtEmail() throws Exception {
        JobResponse response = new JobResponse(
                "job-id",
                "company-id",
                "Backend Engineer",
                "Test Inc",
                "applicant@example.com",
                LocalDateTime.of(2026, 1, 2, 3, 4)
        );
        given(jobService.getJobById("job-id", "Bearer token", "applicant@example.com"))
                .willReturn(response);

        mockMvc.perform(get("/api/v1/jobs/job/job-id")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value("job-id"))
                .andExpect(jsonPath("$.companyId").value("company-id"))
                .andExpect(jsonPath("$.jobName").value("Backend Engineer"))
                .andExpect(jsonPath("$.companyName").value("Test Inc"))
                .andExpect(jsonPath("$.companyOwnerEmail").value("applicant@example.com"));

        verify(jobService).getJobById("job-id", "Bearer token", "applicant@example.com");
    }

    @Test
    void getPublicJobById_shouldReturnJobFromService() throws Exception {
        JobResponse response = new JobResponse(
                "job-id",
                null,
                "Backend Engineer",
                null,
                null,
                LocalDateTime.of(2026, 1, 2, 3, 4)
        );
        given(jobService.getJob("job-id")).willReturn(response);

        mockMvc.perform(get("/api/v1/jobs/job-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value("job-id"))
                .andExpect(jsonPath("$.jobName").value("Backend Engineer"));

        verify(jobService).getJob("job-id");
    }

    @Test
    void searchJobs_shouldReturnPagedJobsFromService() throws Exception {
        JobDto jobDto = new JobDto(
                "job-id",
                "Backend Engineer",
                EmploymentType.FULL_TIME,
                WorkMode.REMOTE,
                "Zagreb",
                "Build services",
                "Own APIs",
                "Java",
                "Remote",
                "Good team",
                LocalDateTime.of(2026, 1, 2, 3, 4)
        );
        given(jobService.findJobByTitle("backend", 1, 5))
                .willReturn(new PageImpl<>(List.of(jobDto), PageRequest.of(1, 5), 1));

        mockMvc.perform(get("/api/v1/jobs/search")
                        .param("title", "backend")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("job-id"))
                .andExpect(jsonPath("$.content[0].title").value("Backend Engineer"));

        verify(jobService).findJobByTitle("backend", 1, 5);
    }

    @Test
    void getAllJobs_shouldReturnPagedJobsFromService() throws Exception {
        JobDto jobDto = new JobDto();
        jobDto.setId("job-id");
        jobDto.setTitle("Backend Engineer");
        given(jobService.getAllJobs(0, 10))
                .willReturn(new PageImpl<>(List.of(jobDto), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("job-id"))
                .andExpect(jsonPath("$.content[0].title").value("Backend Engineer"));

        verify(jobService).getAllJobs(0, 10);
    }

    @Test
    void deleteJob_shouldReturnOkAndCallService() throws Exception {
        mockMvc.perform(delete("/api/v1/jobs/job/job-id/delete"))
                .andExpect(status().isOk());

        verify(jobService).deleteJob("job-id");
    }

    private static class JwtArgumentResolver implements HandlerMethodArgumentResolver {

        private final Jwt jwt;

        private JwtArgumentResolver(Jwt jwt) {
            this.jwt = jwt;
        }

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                    && Jwt.class.isAssignableFrom(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                WebDataBinderFactory binderFactory
        ) {
            return jwt;
        }
    }
}
