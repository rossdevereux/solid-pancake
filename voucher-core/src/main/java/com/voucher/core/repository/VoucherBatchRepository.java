package com.voucher.core.repository;

import com.voucher.core.domain.VoucherBatch;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherBatchRepository extends MongoRepository<VoucherBatch, String> {
    List<VoucherBatch> findByOrgId(String orgId);
    java.util.Optional<VoucherBatch> findByIdAndOrgId(String id, String orgId);

    List<VoucherBatch> findByTemplateIdAndOrgId(String templateId, String orgId);

    long countByStatusAndOrgId(VoucherBatch.BatchStatus status, String orgId);
}
