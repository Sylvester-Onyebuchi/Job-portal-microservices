package com.sylvester.companyservice.rabbitMq;

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
        return new Queue("createQueue");
    }
    @Bean
    public TopicExchange createExchange() {
        return new TopicExchange("createExchange");
    }
    @Bean
    public Binding createBinding() {
        return BindingBuilder
                .bind(createQueue()).
                to(createExchange())
                .with("createKey");
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
