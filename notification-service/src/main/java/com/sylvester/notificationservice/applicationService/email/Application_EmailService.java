package com.sylvester.notificationservice.applicationService.email;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Application_EmailService {

    @Value("${spring.mail.username}")
    private String sender;

    private final JavaMailSender mailSender;

    @Async
    public void sendApplicationSuccessEmail(String email, String name,String jobName, String companyName) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(email);
        message.setSubject("Application Submission for the role "+jobName+" at "+companyName);
        message.setText("Dear " + name + "!,\nWe have received your job application for the role " + jobName+
                " in our company.\nWe will access your application and give you a feedback as soon as possible.\n"+
        "Best regard.\n\n"+companyName);
        message.setSentDate(new java.util.Date());
        mailSender.send(message);

    }

    @Async
    public void sendCompanyAlertEmail(String companyEmail, String candidateEmail, String candidateName, String jobName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(companyEmail);
        message.setSubject("Received a new job application for the role " + jobName);
        message.setText("This is to notify you that a candidate by name "+candidateName+", applied for the role "+jobName+" in your company."+
        "The candidate email is "+candidateEmail
        +"\n\nDo not reply to this email");
        message.setSentDate(new java.util.Date());
        mailSender.send(message);
    }

}
