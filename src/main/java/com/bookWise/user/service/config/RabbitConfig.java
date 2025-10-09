package com.bookWise.user.service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

@Configuration
public class RabbitConfig {

    public static final String USER_QUEUE = "user.queue";
    public static final String USER_DLQ = "user.queue.dlq";
    public static final String USER_EXCHANGE = "user.exchange";
    public static final String DLX_EXCHANGE = "user.dlx";

    @Value("${spring.rabbitmq.host}")
    private String host;

    @Value("${spring.rabbitmq.port}")
    private int port;

    @Value("${spring.rabbitmq.username}")
    private String username;

    @Value("${spring.rabbitmq.password}")
    private String password;

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setRequestedHeartBeat(30);
        connectionFactory.setConnectionTimeout(30000);
        return connectionFactory;
    }

    @Bean
    public Exchange userExchange() {
        return ExchangeBuilder.topicExchange(USER_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    @Bean
    public Queue userQueue() {
        return QueueBuilder.durable(USER_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", USER_DLQ)
                .build();
    }

    @Bean
    public Queue userDlq() {
        return QueueBuilder.durable(USER_DLQ).build();
    }

    @Bean
    public Binding userBinding() {
        return BindingBuilder.bind(userQueue())
                .to(userExchange())
                .with("user.#")
                .noargs();
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(userDlq())
                .to(dlxExchange())
                .with(USER_DLQ);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setExchange(USER_EXCHANGE);
        rabbitTemplate.setDefaultReceiveQueue(USER_QUEUE);
        rabbitTemplate.setMandatory(true);

        rabbitTemplate.setConfirmCallback((correlation, ack, reason) -> {
            if (!ack) {
                System.err.println("Falha ao publicar mensagem: " + reason);
            }
        });

        return rabbitTemplate;
    }

    @Bean
    public RetryOperationsInterceptor retryInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(1000, 2.0, 5000)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }
}
