package com.borrowit.view.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtils {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private DateTimeUtils() {
    }

    public static String format(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(FORMATTER);
    }

    public static String remaining(LocalDateTime dueDate) {
        if (dueDate == null) {
            return "";
        }

        Duration duration = Duration.between(LocalDateTime.now(), dueDate);
        if (duration.isZero()) {
            return "Due now";
        }

        boolean overdue = duration.isNegative();
        Duration absoluteDuration = overdue ? duration.negated() : duration;
        String text = formatDuration(absoluteDuration);
        return overdue ? "Overdue by " + text : text + " left";
    }

    private static String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.minusDays(days).toHours();
        long minutes = duration.minusDays(days).minusHours(hours).toMinutes();

        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return Math.max(minutes, 1) + "m";
    }
}
