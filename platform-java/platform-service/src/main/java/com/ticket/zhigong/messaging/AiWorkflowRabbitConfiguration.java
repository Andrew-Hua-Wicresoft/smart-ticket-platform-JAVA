package com.ticket.zhigong.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiWorkflowRabbitConfiguration {

    @Bean
    public Declarables aiWorkflowDeclarables() {
        TopicExchange ticketEventsExchange = new TopicExchange(AiWorkflowMessaging.TICKET_EVENTS_EXCHANGE, true, false);
        DirectExchange deadLetterExchange = new DirectExchange(AiWorkflowMessaging.PLATFORM_DLX_EXCHANGE, true, false);

        Queue aiAnalysisQueue = QueueBuilder.durable(AiWorkflowMessaging.AI_ANALYSIS_QUEUE)
                .deadLetterExchange(AiWorkflowMessaging.PLATFORM_DLX_EXCHANGE)
                .build();
        Queue kbDraftQueue = QueueBuilder.durable(AiWorkflowMessaging.KB_DRAFT_QUEUE)
                .deadLetterExchange(AiWorkflowMessaging.PLATFORM_DLX_EXCHANGE)
                .build();
        Queue notificationDispatchQueue = QueueBuilder.durable(AiWorkflowMessaging.NOTIFICATION_DISPATCH_QUEUE)
                .deadLetterExchange(AiWorkflowMessaging.PLATFORM_DLX_EXCHANGE)
                .build();

        Binding aiAnalysisBinding = BindingBuilder.bind(aiAnalysisQueue)
                .to(ticketEventsExchange)
                .with(AiWorkflowMessaging.ROUTING_TICKET_CREATED);
        Binding kbDraftBinding = BindingBuilder.bind(kbDraftQueue)
                .to(ticketEventsExchange)
                .with(AiWorkflowMessaging.ROUTING_TICKET_RESOLVED);
        Binding notificationDispatchBinding = BindingBuilder.bind(notificationDispatchQueue)
                .to(ticketEventsExchange)
                .with(AiWorkflowMessaging.ROUTING_KNOWLEDGE_PUBLISHED);

        return new Declarables(
                ticketEventsExchange,
                deadLetterExchange,
                aiAnalysisQueue,
                kbDraftQueue,
                notificationDispatchQueue,
                aiAnalysisBinding,
                kbDraftBinding,
                notificationDispatchBinding
        );
    }

    @Bean
    public Jackson2JsonMessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        rabbitTemplate.setBeforePublishPostProcessors(message -> {
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            return message;
        });
        return rabbitTemplate;
    }
}
