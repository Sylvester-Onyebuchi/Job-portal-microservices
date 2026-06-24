package com.sylvester.applicationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sylvester.applicationservice.dtos.ApplicationResponse;
import com.sylvester.applicationservice.dtos.CvResponse;
import com.sylvester.applicationservice.dtos.EducationRequest;
import com.sylvester.applicationservice.dtos.ExperienceRequest;
import com.sylvester.applicationservice.dtos.SubmitApplicationRequest;
import com.sylvester.applicationservice.service.ApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ApplicationControllerTest {

    @Mock
    private ApplicationService applicationService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Jwt jwt;
    private SubmitApplicationRequest request;

    @BeforeEach
    void setUp() {
        jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("userId", "candidate-id")
                .build();

        request = new SubmitApplicationRequest(
                "job-id",
                "Ada Lovelace",
                "ada@example.com",
                "+2348012345678",
                "I would like to apply for this role.",
                "https://ada.example.com",
                "https://linkedin.example.com/in/ada",
                "https://github.com/ada",
                Set.of("Java", "Spring Boot"),
                Set.of("English", "French"),
                Set.of(new EducationRequest("University of Lagos", "BSc", "Computer Science", 2016, 2020)),
                Set.of(new ExperienceRequest("Acme Inc", "Backend Engineer", "Built APIs", 2021, 2024, "false"))
        );

        mockMvc = MockMvcBuilders
                .standaloneSetup(new ApplicationController(applicationService))
                .setCustomArgumentResolvers(new JwtArgumentResolver(jwt))
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void submitApplication_shouldReturnOkAndCallService() throws Exception {
        MockMultipartFile applicationPart = new MockMultipartFile(
                "application",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );
        MockMultipartFile cvPart = new MockMultipartFile(
                "cvFile",
                "ada-cv.pdf",
                "application/pdf",
                "pdf-content".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/applications/apply")
                        .file(applicationPart)
                        .file(cvPart)
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk());

        verify(applicationService).submitApplication(
                request,
                cvPart,
                "candidate-id",
                "Bearer token"
        );
    }

    @Test
    void submitApplication_shouldReturnBadRequest_whenCvPartIsMissing() throws Exception {
        MockMultipartFile applicationPart = new MockMultipartFile(
                "application",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );

        mockMvc.perform(multipart("/api/v1/applications/apply")
                        .file(applicationPart)
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(applicationService);
    }

    @Test
    void getCandidateApplications_shouldReturnApplicationsFromService() throws Exception {
        ApplicationResponse response = new ApplicationResponse();
        response.setId("application-id");
        response.setCandidateUserId("candidate-id");
        response.setFullName("Ada Lovelace");
        response.setEmail("ada@example.com");
        response.setPhoneNumber("+2348012345678");
        response.setGithubLink("https://github.com/ada");

        given(applicationService.findAllByCandidateUserId("candidate-id"))
                .willReturn(List.of(response));

        mockMvc.perform(get("/api/v1/applications/candidate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("application-id"))
                .andExpect(jsonPath("$[0].candidateUserId").value("candidate-id"))
                .andExpect(jsonPath("$[0].fullName").value("Ada Lovelace"))
                .andExpect(jsonPath("$[0].email").value("ada@example.com"))
                .andExpect(jsonPath("$[0].phoneNumber").value("+2348012345678"))
                .andExpect(jsonPath("$[0].githubLink").value("https://github.com/ada"));

        verify(applicationService).findAllByCandidateUserId("candidate-id");
    }

    @Test
    void deleteApplication_shouldReturnOkAndCallService() throws Exception {
        mockMvc.perform(delete("/api/v1/applications/application-id/delete"))
                .andExpect(status().isOk());

        verify(applicationService).deleteApplication("application-id", "candidate-id");
    }

    @Test
    void getCv_shouldReturnFileBytesWithDownloadHeaders() throws Exception {
        byte[] cvBytes = "pdf-content".getBytes();
        CvResponse cvResponse = new CvResponse("ada-cv.pdf", "application/pdf", cvBytes);
        given(applicationService.getCv("candidate-id")).willReturn(cvResponse);

        mockMvc.perform(get("/api/v1/applications/cv"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"ada-cv.pdf\""
                ))
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes(cvBytes));

        verify(applicationService).getCv("candidate-id");
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
