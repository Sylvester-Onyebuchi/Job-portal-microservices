package com.sylvester.applicationservice.service;

import com.sylvester.applicationservice.dtos.ApplicationResponse;
import com.sylvester.applicationservice.dtos.CvResponse;
import com.sylvester.applicationservice.dtos.SubmitApplicationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public interface ApplicationService {

    void submitApplication(SubmitApplicationRequest request, MultipartFile file, String senderId, String header) throws IOException;
    void deleteApplication(String id, String senderId);

    @Transactional
    List<ApplicationResponse> findAllByCandidateUserId(String candidateUserId);

    CvResponse getCv(String id);
}
