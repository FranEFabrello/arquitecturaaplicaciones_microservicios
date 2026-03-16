package com.uade.notification.config;

import com.uade.notification.event.ProductCreatedEvent;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Profile("rabbitmq")
public class RabbitMQConfig {

    public static final String EXCHANGE = "inventory.exchange";
    public static final String QUEUE = "product.created.queue";
    public static final String ROUTING_KEY = "product.created";

    @Bean
    public TopicExchange inventoryExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue productCreatedQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    public Binding productCreatedBinding(Queue productCreatedQueue, TopicExchange inventoryExchange) {
        return BindingBuilder.bind(productCreatedQueue).to(inventoryExchange).with(ROUTING_KEY);
    }

    /**
     * Mapea el TypeId del productor (inventory-service) a la clase local del consumidor.
     * Sin esto, Jackson intenta buscar com.uade.inventory.domain.event.ProductCreatedEvent
     * que no existe en el classpath de notification-service, causando ClassNotFoundException.
     */
    @Bean
    public DefaultClassMapper classMapper() {
        DefaultClassMapper classMapper = new DefaultClassMapper();
        Map<String, Class<?>> idClassMapping = new HashMap<>();
        // Mapeo: clase del productor → clase del consumidor
        idClassMapping.put(
                "com.uade.inventory.domain.event.ProductCreatedEvent",
                ProductCreatedEvent.class
        );
        classMapper.setIdClassMapping(idClassMapping);
        return classMapper;
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setClassMapper(classMapper());
        return converter;
    }
}
