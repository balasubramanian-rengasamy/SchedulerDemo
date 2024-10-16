package com.prime

import jakarta.ejb.Schedule
import jakarta.ejb.Singleton
import jakarta.ejb.Startup
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

@Singleton
@Startup
class JobScheduler {

    private val logger: Logger = Logger.getLogger(JobScheduler::class.java.name)
    private var jobRunning: Boolean = false // Volatile is not necessary

    @Schedule(minute = "*/2", hour = "*", persistent = false)
    fun scheduledJob() {
        if (jobRunning) {
            logger.warning("Previous job is still running, skipping this execution.")
            return
        }

        jobRunning = true

        try {
            performJob()
        } finally {
            jobRunning = false
        }
    }

    private fun performJob() {
        // Your job logic here
        logger.info("Job started.")

        // Simulate job processing
        try {
            TimeUnit.MINUTES.sleep(3) // Simulate job duration
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        logger.info("Job completed.")
    }
}