package vaultiq.session.core.scheduler;

import org.springframework.scheduling.support.CronTrigger;

import java.time.Duration;
import java.util.Optional;

/**
 * Utility class for parsing cleanup strategy strings into actionable scheduling objects.
 * <p>
 * Supports parsing cron-based and interval-based cleanup strategies from string representations.
 * This class is not meant to be instantiated.
 * </p>
 */
public final class CleanupStrategyParser {

    /**
     * Prefix for cron-based cleanup strategies (e.g., "clean-at:(0 0 2 * * ?)").
     */
    public static final String CLEAN_AT_PREFIX = "clean-at:";

    /**
     * Prefix for interval-based cleanup strategies (e.g., "clean-interval:10M").
     */
    public static final String CLEAN_INTERVAL_PREFIX = "clean-interval:";

    // Private constructor to prevent instantiation
    private CleanupStrategyParser() {
        throw new AssertionError("Utility class - do not instantiate.");
    }

    /**
     * Parses a cleanup strategy string as a cron expression if it starts with the cron prefix.
     *
     * @param cleanupStrategy the cleanup strategy string (e.g., "clean-at:(0 0 2 * * ?)")
     * @return an Optional containing a CronTrigger if parsing is successful, or Optional.empty() otherwise
     */
    public static Optional<CronTrigger> parseCron(String cleanupStrategy) {
        if (cleanupStrategy != null && cleanupStrategy.startsWith(CLEAN_AT_PREFIX)) {
            String cron = cleanupStrategy.substring(CLEAN_AT_PREFIX.length()).replaceAll("[()]", "").trim();
            return Optional.of(new CronTrigger(cron));
        }
        return Optional.empty();
    }

    /**
     * Parses a cleanup strategy string as a duration if it starts with the interval prefix.
     *
     * @param cleanupStrategy the cleanup strategy string (e.g., "clean-interval:10M")
     * @return an Optional containing a Duration if parsing is successful, or Optional.empty() otherwise
     */
    public static Optional<Duration> parseDuration(String cleanupStrategy) {
        if (cleanupStrategy != null && cleanupStrategy.startsWith(CLEAN_INTERVAL_PREFIX)) {
            String durationStr = cleanupStrategy.substring(CLEAN_INTERVAL_PREFIX.length()).trim().toUpperCase();
            if (!durationStr.startsWith("PT")) {
                durationStr = "PT" + durationStr;
            }
            return Optional.of(Duration.parse(durationStr));
        }
        return Optional.empty();
    }
}
