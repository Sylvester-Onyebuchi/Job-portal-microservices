package com.sylvester.notificationservice;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public Queue verificationQueue() {
        return new Queue("verificationQueue");
    }

    @Bean
    public Queue resetQueue() {
        return new Queue("resetQueue");
    }

    @Bean
    public Queue applicationQueue() {
        return new Queue("application-queue");
    }

    @Bean
    public Queue alertQueue() {
        return new Queue("alert-queue");
    }

    @Bean
    public TopicExchange verificationExchange() {
        return new TopicExchange("verificationExchange");
    }

    @Bean
    public TopicExchange resetExchange() {
        return new TopicExchange("resetExchange");
    }
    @Bean
    public TopicExchange applicationExchange() {
        return new TopicExchange("application-exchange");
    }

    @Bean
    public TopicExchange alertExchange() {
        return new TopicExchange("alert-exchange");
    }


    @Bean
    public Binding verificationBinding() {
        return BindingBuilder
                .bind(verificationQueue())
                .to(verificationExchange())
                .with("verificationKey");

    }

    @Bean
    public Binding resetBinding() {
        return BindingBuilder
                .bind(resetQueue())
                .to(resetExchange())
                .with("resetKey");
    }

    @Bean
    public Binding applicationBinding() {
        return BindingBuilder
                .bind(applicationQueue()).
                to(applicationExchange())
                .with("application-routing-key");
    }

    @Bean
    public Binding alertBinding() {
        return BindingBuilder
                .bind(alertQueue()).
                to(alertExchange())
                .with("alert-routing-key");
    }

    @Bean
    public MessageConverter verificationMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
