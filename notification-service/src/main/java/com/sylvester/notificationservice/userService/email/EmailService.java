package com.sylvester.notificationservice.userService.email;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${spring.mail.username}")
    private String sender;

    private final JavaMailSender mailSender;

    @Async
    public void sendVerificationEmail(String email, String firstname, String lastname, String code){

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(email);
        message.setSubject("Verification Email");
        message.setText("Dear " + firstname + " "+ lastname+ "! ,To verify your account, use the code below\nYour Verification Code is " + code);
        message.setSentDate(new java.util.Date());
        mailSender.send(message);

    }

    @Async
    public void sendResetPasswordEmail(String email, String firstname, String token){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(email);
        message.setSubject("Reset Password");
        message.setText("Dear "+firstname+"!, Below you can find a token you can use to reset your password.\nPassword token: "+token);
        message.setSentDate(new java.util.Date());
        mailSender.send(message);
    }

}
