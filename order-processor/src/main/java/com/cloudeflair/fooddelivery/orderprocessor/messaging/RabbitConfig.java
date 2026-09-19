package com.cloudeflair.fooddelivery.orderprocessor.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // Declara os mesmos recursos (idempotente): garante que existem mesmo que
    // o worker suba antes do Order Service.
    @Bean
    TopicExchange ordersExchange() {
        return new TopicExchange(OrderMessaging.EXCHANGE);
    }

    @Bean
    Queue orderCreatedQueue() {
        return QueueBuilder.durable(OrderMessaging.QUEUE).build();
    }

    @Bean
    Binding orderCreatedBinding(Queue orderCreatedQueue, TopicExchange ordersExchange) {
        return BindingBuilder.bind(orderCreatedQueue).to(ordersExchange).with(OrderMessaging.ROUTING_KEY);
    }

    /**
     * Jackson 3 (Spring AMQP 4). Usa o tipo do parametro do @RabbitListener (INFERRED)
     * em vez do header __TypeId__ do produtor, para que cada servico tenha sua propria
     * classe de evento em pacotes diferentes sem quebrar a desserializacao.
     *
     * Como ha um unico bean MessageConverter, o Spring Boot o injeta automaticamente
     * no listener container factory (nao precisamos declarar a factory manualmente).
     */
    @Bean
    MessageConverter jsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();
        typeMapper.setTypePrecedence(JacksonJavaTypeMapper.TypePrecedence.INFERRED);
        typeMapper.setTrustedPackages("*");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
