package com.voucher.api.repository;

import com.voucher.api.domain.VoucherBatch;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoucherBatchRepository extends MongoRepository<VoucherBatch, String> {
    List<VoucherBatch> findByOrgId(String orgId);

    java.util.Optional<VoucherBatch> findByIdAndOrgId(String id, String orgId);

    List<VoucherBatch> findByTemplateIdAndOrgId(String templateId, String orgId);

    long countByStatusAndOrgId(VoucherBatch.BatchStatus status, String orgId);
}
