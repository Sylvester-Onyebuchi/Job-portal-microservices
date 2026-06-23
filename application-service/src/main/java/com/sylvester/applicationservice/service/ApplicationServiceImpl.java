package com.sylvester.applicationservice.service;

import com.sylvester.applicationservice.config.JobClient;
import com.sylvester.applicationservice.dtos.*;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobClient jobClient;
    private final Producer producer;

    @Override
    @Transactional
    public void submitApplication(SubmitApplicationRequest request,
                                  MultipartFile file,
                                  String candidateUserId,
                                  String header) throws IOException {

        JobResponse job = jobClient.getJobById(request.jobId(), header);

        log.info("Job ID: " + job.getJobId());
        log.info("Company ID: " + job.getCompanyId());



        boolean alreadyApplied = applicationRepository.existsByJobIdAndCandidateUserId(job.getJobId(), candidateUserId);
        if (alreadyApplied) {
            throw new AlreadyExistsException("You have already applied for this job");
        }

        Application newApplication = Application.builder()
                .jobId(job.getJobId())
                .candidateUserId(candidateUserId)
                .companyId(job.getCompanyId())
                .coverLetter(request.coverLetter())
                .email(request.email())
                .cv(file.getBytes())
                .cvFilename(file.getOriginalFilename())
                .cvContent(file.getContentType())
                .fullName(request.fullName())
                .githubUrl(request.githubUrl())
                .linkedInUrl(request.linkedInUrl())
                .languages(request.languages())
                .phoneNumber(request.phoneNumber())
                .skills(request.skills())
                .portfolioUrl(request.portfolioUrl())
                .education(request.education().stream().map(this::getEducation)
                        .collect(Collectors.toSet()))
                .experience(request.experience().stream().map(this::getExperience)
                .collect(Collectors.toSet()))
                .appliedAt(LocalDateTime.now())
                .status(ApplicationStatus.SUBMITTED)
                .build();
        var saved = applicationRepository.save(newApplication);
        log.info("Application has been submitted successfully");
        ApplicationEvent event = new ApplicationEvent(saved.getEmail(), saved.getFullName(), job.getJobName(), job.getCompanyName());
        producer.sendMessage(event);
        log.info("Application has been queued");


        CompanyAlertEvent companyAlertEvent = new CompanyAlertEvent(job.getCompanyOwnerEmail(), saved.getEmail(), saved.getFullName(), job.getJobName());
        producer.sendMessageToCompany(companyAlertEvent);
        log.info("Alert has been queued");



    }

    private Education getEducation(EducationRequest request){
        Education education = new Education();
        education.setSchool(request.school());
        education.setDegree(request.degree());
        education.setFieldOfStudy(request.fieldOfStudy());
        education.setStartYear(request.startYear());
        education.setEndYear(request.endYear());
        return education;
    }

    private Experience getExperience(ExperienceRequest request){
        Experience experience = new Experience();
        experience.setCompany(request.company());
        experience.setPosition(request.position());
        experience.setDescription(request.description());
        experience.setStartYear(request.startYear());
        experience.setEndYear(request.endYear());
        return experience;
    }

    @Override
    @Transactional
    public void deleteApplication(String id, String senderId) {

        var application = applicationRepository.findApplicationById(id).orElseThrow();
        applicationRepository.delete(application);

        if (!application.getCandidateUserId().equals(senderId)) {
            throw new RuntimeException("You are not permitted to delete this application");
        }

        applicationRepository.delete(application);

    }


    @Override
    public List<ApplicationResponse> findAllByCandidateUserId(String candidateUserId) {

        applicationRepository.findApplicationByCandidateUserId(candidateUserId).orElseThrow(
                () -> new NotFoundException("Application with id " + candidateUserId + " not found")
        );

        List<Application> applications = applicationRepository.findAllByCandidateUserId(candidateUserId);
        return applications.stream().map(application -> {
            ApplicationResponse response = new ApplicationResponse();
            response.setId(application.getId());
            response.setCandidateUserId(application.getCandidateUserId());
            response.setFullName(application.getFullName());
            response.setEmail(application.getEmail());
            response.setGithubLink(application.getGithubUrl());
            response.setExperience(application.getExperience());
            response.setEducation(application.getEducation());
            return  response;

        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CvResponse getCv(String id) {
        var application = applicationRepository.findApplicationByCandidateUserId(id).orElseThrow(
                () -> new NotFoundException("Application with id " + id + " not found")
        );

        CvResponse response = new CvResponse();
        response.setName(application.getCvFilename());
        response.setContentType(application.getCvContent());
        response.setImage(application.getCv());
        return response;
    }
}
