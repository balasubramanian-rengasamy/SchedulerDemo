package com.prime

import jakarta.annotation.PostConstruct
import jakarta.annotation.Resource
import jakarta.ejb.SessionContext
import jakarta.ejb.Timeout
import jakarta.ejb.TimerConfig
import jakarta.ejb.TimerService
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.logging.Level
import java.util.logging.Logger

abstract class GenericIntervalScheduler(
    private val interval: Long,
    private val taskName: String,
    private val startTime: LocalTime? = null,
    private val endTime: LocalTime? = null
) {
    protected open val logger: Logger = Logger.getLogger(javaClass.name)

    @Resource
    private lateinit var timerService: TimerService

    @Resource
    private lateinit var sessionContext: SessionContext

    private var isRunning = false
    private var lastScheduledTime: Instant = Instant.now()
    private var taskStartTime: Instant? = null
    private var taskEndTime: Instant? = null
    private var nextScheduledTime: Instant? = null

    @PostConstruct
    fun init() {
        logger.info("GenericIntervalScheduler started for task: $taskName")
        scheduleNextRun()
    }

    @Timeout
    fun scheduledTask() {
        if (!isContainerActive()) {
            logger.warning("Container is not active. Skipping task execution and rescheduling: $taskName")
            scheduleNextRun()
            return
        }

        if (isWithinAllowedTimeRange()) {
            if (!isRunning) {
                try {
                    isRunning = true
                    taskStartTime = Instant.now()
                    logger.info("Starting scheduled task: $taskName")
                    performTask()
                    logger.info("Completed scheduled task: $taskName")
                } catch (e: Exception) {
                    logger.log(Level.SEVERE, "Error occurred while executing task: $taskName", e)
                } finally {
                    isRunning = false
                    taskEndTime = Instant.now()
                    scheduleNextRun()
                    printSummary()
                }
            } else {
                logger.warning("Previous task still running for: $taskName. Scheduling next run without executing.")
                scheduleNextRun()
                printSummary()
            }
        } else {
            logger.info("Task $taskName not executed as it's outside the allowed time range.")
            scheduleNextRun()
        }
    }

    private fun isContainerActive(): Boolean {
        return try {
            sessionContext.contextData // This will throw an exception if the container is not active
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun isWithinAllowedTimeRange(): Boolean {
        if (startTime == null || endTime == null) {
            return true // No time restrictions
        }

        val now = LocalTime.now()
        return now >= startTime && now <= endTime
    }

    private fun scheduleNextRun() {
        if (!isContainerActive()) {
            logger.warning("Container is not active. Skipping scheduling for task: $taskName")
            return
        }

        try {
            val now = Instant.now()
            val delay = calculateNextDelay(now)
            nextScheduledTime = now.plusMillis(delay)
            lastScheduledTime = now
            val timerConfig = TimerConfig().apply { isPersistent = false }
            timerService.createSingleActionTimer(delay, timerConfig)
            logger.info("Next execution of $taskName scheduled at: ${LocalDateTime.ofInstant(nextScheduledTime, ZoneId.systemDefault())}")
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed to schedule next run for task: $taskName", e)
        }
    }

    private fun calculateNextDelay(now: Instant): Long {
        val nextScheduledTime = lastScheduledTime.plusMillis(interval)
        var delay = Duration.between(now, nextScheduledTime).toMillis()

        if (delay < 0) {
            // If we're behind schedule, find the next interval
            val missedIntervals = (-delay / interval) + 1
            delay = interval * missedIntervals
        }

        if (startTime != null && endTime != null) {
            val nextRunTime = LocalDateTime.ofInstant(now.plusMillis(delay), ZoneId.systemDefault())
            if (nextRunTime.toLocalTime() > endTime) {
                // Next run would be outside allowed range, schedule for start time tomorrow
                val tomorrowStart = nextRunTime.plusDays(1).with(startTime)
                delay = Duration.between(now, tomorrowStart.atZone(ZoneId.systemDefault()).toInstant()).toMillis()
            } else if (nextRunTime.toLocalTime() < startTime) {
                // Next run would be before start time today, schedule for start time today
                val todayStart = nextRunTime.with(startTime)
                delay = Duration.between(now, todayStart.atZone(ZoneId.systemDefault()).toInstant()).toMillis()
            }
        }

        return delay
    }

    protected abstract fun performTask()

    private fun printSummary() {
        val formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH)
        val start = taskStartTime?.atZone(ZoneId.systemDefault())?.format(formatter) ?: "N/A"
        val end = taskEndTime?.atZone(ZoneId.systemDefault())?.format(formatter) ?: "N/A"
        val next = nextScheduledTime?.atZone(ZoneId.systemDefault())?.format(formatter) ?: "N/A"
        val duration = if (taskStartTime != null && taskEndTime != null) {
            Duration.between(taskStartTime, taskEndTime).seconds
        } else {
            null
        }
        logger.info("Task Summary: $taskName, startTime = $start, endTime = $end, duration = ${duration ?: "N/A"} seconds, nextScheduledTime = $next")
    }
}