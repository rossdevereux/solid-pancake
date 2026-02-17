package com.voucher.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.voucher.admin", "com.voucher.core"})
@EnableMongoRepositories(basePackages = "com.voucher.core.repository")
public class VoucherAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(VoucherAdminApplication.class, args);
    }
}
