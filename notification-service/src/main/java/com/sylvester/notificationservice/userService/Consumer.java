package com.sylvester.notificationservice.userService;


import com.sylvester.notificationservice.userService.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class Consumer {

    private final EmailService emailService;


    @RabbitListener(queues = "verificationQueue")
    public void sendVerificationEmail(RegisterEvent event) {

        emailService.sendVerificationEmail(event.email(), event.firstname(), event.lastname(), event.code());
        log.info("Verification email sent to " + event.email());
    }

    @RabbitListener(queues = "resetQueue")
    public void sendResetPasswordEmail(ResetPasswordEvent event) {
        emailService.sendResetPasswordEmail(event.email(), event.firstname(), event.token());
        log.info("Reset email sent to " + event.email());
    }


}
