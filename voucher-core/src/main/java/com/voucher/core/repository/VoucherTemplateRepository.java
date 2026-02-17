package com.voucher.core.repository;

import com.voucher.core.domain.VoucherTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherTemplateRepository extends MongoRepository<VoucherTemplate, String> {
    List<VoucherTemplate> findByOrgId(String orgId);
    Optional<VoucherTemplate> findByIdAndOrgId(String id, String orgId);
}
