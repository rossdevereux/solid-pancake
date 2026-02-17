package com.voucher.api.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String VOUCHER_GEN_QUEUE = "voucher.generation.queue";
    public static final String VOUCHER_GEN_EXCHANGE = "voucher.generation.exchange";
    public static final String VOUCHER_GEN_ROUTING_KEY = "voucher.generation.routingKey";

    @Bean
    public Queue voucherGenerationQueue() {
        return new Queue(VOUCHER_GEN_QUEUE, true);
    }

    @Bean
    public DirectExchange voucherGenerationExchange() {
        return new DirectExchange(VOUCHER_GEN_EXCHANGE);
    }

    @Bean
    public Binding binding(Queue voucherGenerationQueue, DirectExchange voucherGenerationExchange) {
        return BindingBuilder.bind(voucherGenerationQueue).to(voucherGenerationExchange).with(VOUCHER_GEN_ROUTING_KEY);
    }

    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
