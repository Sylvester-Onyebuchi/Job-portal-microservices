package com.sylvester.notificationservice.applicationService;

import com.sylvester.notificationservice.applicationService.email.Application_EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ApplicationServiceConsumer {

    private final Application_EmailService applicationEmailService;

    @RabbitListener(queues = "application-queue")
    public void sendEmailToCandidate(ApplicationEvent event) {
        applicationEmailService.sendApplicationSuccessEmail(event.candidateEmail(), event.candidateFullName(),
                event.companyName(),  event.jobName());
        log.debug("Sent email to  {} for Job  Application Successfully", event.candidateEmail());
    }

    @RabbitListener(queues = "alert-queue")
    public void sendEmailToCompany(CompanyAlertEvent event) {
        applicationEmailService.sendCompanyAlertEmail(event.companyEmail(), event.candidateEmail(),
                event.candidateFullName(),  event.jobName());
        log.debug("Email for the new role applied sent successfully to  {}", event.companyEmail());
    }
}
