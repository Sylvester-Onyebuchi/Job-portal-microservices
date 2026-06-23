package com.sylvester.companyservice.rabbitMq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Producer {

    private final RabbitTemplate rabbitTemplate;

    public void sendMessage(CreateCompanyEvent event) {
        rabbitTemplate.convertAndSend("createExchange", "createKey", event);
    }
}
