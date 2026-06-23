package com.sylvester.applicationservice.rabbitMq;


import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Producer {

    private final RabbitTemplate rabbitTemplate;

    public void sendMessage(ApplicationEvent event) {
        rabbitTemplate.convertAndSend("application-exchange", "application-routing-key", event);
    }

    public void sendMessageToCompany(CompanyAlertEvent event) {
        rabbitTemplate.convertAndSend("alert-exchange", "alert-routing-key", event);
    }
}
