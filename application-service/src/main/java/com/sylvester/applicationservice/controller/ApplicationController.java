package com.sylvester.applicationservice.controller;


import com.sylvester.applicationservice.dtos.ApplicationResponse;
import com.sylvester.applicationservice.dtos.CvResponse;
import com.sylvester.applicationservice.dtos.SubmitApplicationRequest;
import com.sylvester.applicationservice.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/applications")
public class ApplicationController {

    private final ApplicationService applicationService;


    @PostMapping(value = "/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> submitApplication(
                                               @AuthenticationPrincipal Jwt jwt,
                                               @RequestPart("application")SubmitApplicationRequest request,
                                               @RequestPart("cvFile") MultipartFile cv,
                                               @RequestHeader("Authorization") String header ) throws IOException {
        String candidateUserId = jwt.getClaim("userId");
        applicationService.submitApplication(request,cv,candidateUserId,header);
        return new ResponseEntity<>(HttpStatus.OK);

    }

    @GetMapping("/candidate")
    public ResponseEntity<List<ApplicationResponse>> getCandidateApplications(@AuthenticationPrincipal Jwt jwt){
        String candidateUserId = jwt.getClaim("userId");
        List<ApplicationResponse> response = applicationService.findAllByCandidateUserId(candidateUserId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<?> deleteApplication(@PathVariable String id, @AuthenticationPrincipal Jwt jwt){
        String candidateUserId = jwt.getClaim("userId");
        applicationService.deleteApplication(id,candidateUserId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/cv")
    public ResponseEntity<byte[]> getCv(@AuthenticationPrincipal Jwt jwt){
        String candidateUserId = jwt.getClaim("userId");
        CvResponse cvResponse = applicationService.getCv(candidateUserId);
        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(
                                cvResponse.getContentType()
                        )
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" +
                                cvResponse.getName() +
                                "\""
                )
                .body(cvResponse.getImage());
    }
}
