package com.wecureit.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wecureit.dto.response.DoctorReferralStat;
import com.wecureit.dto.response.ReferralOverviewStats;
import com.wecureit.dto.response.ReferralTrendPoint;
import com.wecureit.dto.response.SpecialityStat;
import com.wecureit.entity.Referral;
import com.wecureit.repository.ReferralRepository;

@Service
@Transactional(readOnly = true)
public class AdminReferralService {

    private final ReferralRepository referralRepository;

    public AdminReferralService(ReferralRepository referralRepository) {
        this.referralRepository = referralRepository;
    }

    public ReferralOverviewStats getOverviewStats() {
        List<Referral> all = referralRepository.findAllWithDetails();

        int total = all.size();
        int pending = 0, accepted = 0, completed = 0, cancelled = 0;
        long totalResponseHours = 0;
        int responseCount = 0;
        Map<String, Integer> specCounts = new HashMap<>();

        for (Referral r : all) {
            switch (r.getStatus()) {
                case "PENDING"   -> pending++;
                case "ACCEPTED"  -> accepted++;
                case "COMPLETED" -> completed++;
                case "CANCELLED" -> cancelled++;
            }
            if (("ACCEPTED".equals(r.getStatus()) || "COMPLETED".equals(r.getStatus()))
                    && r.getCreatedAt() != null && r.getUpdatedAt() != null) {
                totalResponseHours += ChronoUnit.HOURS.between(r.getCreatedAt(), r.getUpdatedAt());
                responseCount++;
            }
            if (r.getSpeciality() != null) {
                specCounts.merge(r.getSpeciality().getSpecialityName(), 1, (a, b) -> a + b);
            }
        }

        String topSpec = specCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        ReferralOverviewStats stats = new ReferralOverviewStats();
        stats.setTotalReferrals(total);
        stats.setPendingCount(pending);
        stats.setAcceptedCount(accepted);
        stats.setCompletedCount(completed);
        stats.setCancelledCount(cancelled);
        stats.setCompletionRate(total > 0 ? Math.round((double) completed / total * 1000.0) / 10.0 : 0);
        stats.setAvgResponseTimeHours(responseCount > 0 ? Math.round((double) totalResponseHours / responseCount * 10.0) / 10.0 : 0);
        stats.setTopSpeciality(topSpec);
        return stats;
    }

    public List<ReferralTrendPoint> getTrends(int days) {
        List<Referral> all = referralRepository.findAllWithDetails();
        LocalDateTime from = LocalDateTime.now().minusDays(days).truncatedTo(ChronoUnit.DAYS);

        Map<String, Integer> byDate = new HashMap<>();
        for (Referral r : all) {
            if (r.getCreatedAt() != null && r.getCreatedAt().isAfter(from)) {
                String date = r.getCreatedAt().toLocalDate().toString();
                byDate.merge(date, 1, (a, b) -> a + b);
            }
        }

        List<ReferralTrendPoint> result = new ArrayList<>();
        for (int i = days; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).toString();
            ReferralTrendPoint pt = new ReferralTrendPoint();
            pt.setDate(date);
            pt.setCount(byDate.getOrDefault(date, 0));
            result.add(pt);
        }
        return result;
    }

    public List<DoctorReferralStat> getDoctorStats() {
        List<Referral> all = referralRepository.findAllWithDetails();
        Map<UUID, DoctorReferralStat> map = new HashMap<>();

        for (Referral r : all) {
            if (r.getFromDoctor() != null) {
                UUID id = r.getFromDoctor().getId();
                DoctorReferralStat s = map.computeIfAbsent(id, k -> {
                    DoctorReferralStat st = new DoctorReferralStat();
                    st.setDoctorId(k.toString());
                    st.setDoctorName(r.getFromDoctor().getName());
                    return st;
                });
                s.setOutgoingCount(s.getOutgoingCount() + 1);
            }
            if (r.getToDoctor() != null) {
                UUID id = r.getToDoctor().getId();
                DoctorReferralStat s = map.computeIfAbsent(id, k -> {
                    DoctorReferralStat st = new DoctorReferralStat();
                    st.setDoctorId(k.toString());
                    st.setDoctorName(r.getToDoctor().getName());
                    return st;
                });
                s.setIncomingCount(s.getIncomingCount() + 1);
            }
        }

        return map.values().stream()
                .sorted((a, b) -> (b.getOutgoingCount() + b.getIncomingCount())
                        - (a.getOutgoingCount() + a.getIncomingCount()))
                .limit(10)
                .toList();
    }

    public List<SpecialityStat> getSpecialityStats() {
        List<Referral> all = referralRepository.findAllWithDetails();
        Map<String, SpecialityStat> map = new HashMap<>();

        for (Referral r : all) {
            if (r.getSpeciality() == null) continue;
            String code = r.getSpeciality().getSpecialityCode();
            SpecialityStat s = map.computeIfAbsent(code, k -> {
                SpecialityStat st = new SpecialityStat();
                st.setSpecialityCode(k);
                st.setSpecialityName(r.getSpeciality().getSpecialityName());
                return st;
            });
            s.setTotalCount(s.getTotalCount() + 1);
            if ("COMPLETED".equals(r.getStatus())) {
                s.setCompletedCount(s.getCompletedCount() + 1);
            }
        }

        map.values().forEach(s -> s.setCompletionRate(
                s.getTotalCount() > 0
                        ? Math.round((double) s.getCompletedCount() / s.getTotalCount() * 1000.0) / 10.0
                        : 0));

        return map.values().stream()
                .sorted((a, b) -> b.getTotalCount() - a.getTotalCount())
                .toList();
    }
}
