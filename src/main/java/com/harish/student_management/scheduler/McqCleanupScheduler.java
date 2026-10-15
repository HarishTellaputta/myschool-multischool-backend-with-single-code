
package com.harish.student_management.scheduler;

import com.harish.student_management.service.McqCleanupService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class McqCleanupScheduler {

    private final McqCleanupService cleanupService;

    public McqCleanupScheduler(McqCleanupService cleanupService) {
        this.cleanupService = cleanupService;
    }

    @Scheduled(cron = "0 0 12 * * *", zone = "Asia/Kolkata")
    public void cleanupPreviousDayMcqData() {
        cleanupService.deleteYesterdayPostedTests();
    }
}
