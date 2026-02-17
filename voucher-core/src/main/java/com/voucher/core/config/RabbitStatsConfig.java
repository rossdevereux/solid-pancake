package com.voucher.core.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitStatsConfig {

    public static final String VOUCHER_STATS_QUEUE = "voucher.stats.queue";
    public static final String VOUCHER_STATS_EXCHANGE = "voucher.stats.exchange";
    public static final String VOUCHER_STATS_ROUTING_KEY = "voucher.stats.routingKey";

    @Bean
    public Queue voucherStatsQueue() {
        return new Queue(VOUCHER_STATS_QUEUE, true);
    }

    @Bean
    public DirectExchange voucherStatsExchange() {
        return new DirectExchange(VOUCHER_STATS_EXCHANGE);
    }

    @Bean
    public Binding statsBinding(Queue voucherStatsQueue, DirectExchange voucherStatsExchange) {
        return BindingBuilder.bind(voucherStatsQueue).to(voucherStatsExchange).with(VOUCHER_STATS_ROUTING_KEY);
    }
}
