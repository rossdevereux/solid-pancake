package com.voucher.core.repository;

import com.voucher.core.domain.DailyStats;
import com.voucher.core.domain.GlobalStats;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class VoucherStatsRepository {

    private final MongoTemplate mongoTemplate;

    public void incrementGenerationStats(String orgId, String date, int count) {
        // Update Daily Stats
        Query dailyQuery = new Query(Criteria.where("orgId").is(orgId).and("date").is(date));
        Update dailyUpdate = new Update().inc("totalGenerated", count);
        mongoTemplate.upsert(dailyQuery, dailyUpdate, DailyStats.class);

        // Update Global Stats
        Query globalQuery = new Query(Criteria.where("orgId").is(orgId));
        Update globalUpdate = new Update()
                .inc("totalVouchers", count)
                .inc("activeVouchers", count);
        mongoTemplate.upsert(globalQuery, globalUpdate, GlobalStats.class);
    }

    public void incrementRedemptionStats(String orgId, String date) {
        // Update Daily Stats
        Query dailyQuery = new Query(Criteria.where("orgId").is(orgId).and("date").is(date));
        Update dailyUpdate = new Update().inc("totalRedeemed", 1);
        mongoTemplate.upsert(dailyQuery, dailyUpdate, DailyStats.class);

        // Update Global Stats
        Query globalQuery = new Query(Criteria.where("orgId").is(orgId));
        Update globalUpdate = new Update()
                .inc("redeemedVouchers", 1)
                .inc("activeVouchers", -1);
        mongoTemplate.upsert(globalQuery, globalUpdate, GlobalStats.class);
    }

    public Optional<GlobalStats> findGlobalStatsByOrgId(String orgId) {
        return Optional.ofNullable(mongoTemplate.findOne(new Query(Criteria.where("orgId").is(orgId)), GlobalStats.class));
    }

    public long getRedemptionsToday(String orgId, String date) {
        DailyStats stats = mongoTemplate.findOne(new Query(Criteria.where("orgId").is(orgId).and("date").is(date)), DailyStats.class);
        return stats != null ? stats.getTotalRedeemed() : 0;
    }
}
