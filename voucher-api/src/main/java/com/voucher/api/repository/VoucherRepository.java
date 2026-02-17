package com.voucher.api.repository;

import com.voucher.api.domain.Voucher;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoucherRepository extends MongoRepository<Voucher, String> {
    java.util.List<Voucher> findByOrgId(String orgId);

    long countByOrgId(String orgId);

    Optional<Voucher> findByIdAndOrgId(String id, String orgId);

    Optional<Voucher> findByCodeHashAndOrgId(String codeHash, String orgId);

    long countByBatchIdAndOrgId(String batchId, String orgId);

    long countByStatusAndOrgId(Voucher.VoucherStatus status, String orgId);

    long countByOrgIdAndRedemptionsRedeemedAtAfter(String orgId, java.time.LocalDateTime date);
}
