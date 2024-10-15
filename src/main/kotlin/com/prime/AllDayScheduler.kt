package com.prime

import jakarta.ejb.Singleton
import jakarta.ejb.Startup
import java.util.concurrent.TimeUnit

@Singleton
@Startup
class AllDayScheduler : GenericIntervalScheduler(TimeUnit.MINUTES.toMillis(60), "AllDayTask") {
    override fun performTask() {
        logger.info("Performing all-day task...")
        try {

        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            logger.warning("AllDayTask was interrupted")
        }
        logger.info("AllDayTask completed.")
    }
}