package com.sylvester.applicationservice.rabbitMq;

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
    public Queue createQueue() {
        return new Queue("application-queue");
    }

    @Bean
    public TopicExchange createExchange() {
        return new TopicExchange("application-exchange");
    }

    @Bean
    public Queue alertQueue() {
        return new Queue("alert-queue");
    }

    @Bean
    public TopicExchange alertExchange() {
        return new TopicExchange("alert-exchange");
    }

    @Bean
    public Binding createBinding() {
        return BindingBuilder
                .bind(createQueue()).
                to(createExchange())
                .with("application-routing-key");
    }

    @Bean
    public Binding alertBinding() {
        return BindingBuilder
                .bind(createQueue()).
                to(createExchange())
                .with("alert-routing-key");
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
