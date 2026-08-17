package com.openbravo.pos.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public final class DayCloseTicketScope {

    private static final System.Logger LOGGER = System.getLogger(DayCloseTicketScope.class.getName());
    private static final Preferences PREFERENCES = Preferences.userRoot()
            .node("com/openbravo/pos/dayClose");
    private static final String COMPLETED_DATE_KEY = "completedDate";
    private static volatile String completedDate;

    private DayCloseTicketScope() {
    }

    public static void markCompleted(Date date) {
        String dateValue = toLocalDate(date).toString();
        completedDate = dateValue;
        try {
            PREFERENCES.put(COMPLETED_DATE_KEY, dateValue);
            PREFERENCES.flush();
        } catch (SecurityException | BackingStoreException exception) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "No se pudo guardar la fecha del corte diario: " + exception.getMessage());
        }
    }

    public static boolean isCompleted(Date date) {
        String dateValue = toLocalDate(date).toString();
        if (dateValue.equals(completedDate)) {
            return true;
        }
        try {
            String storedDate = PREFERENCES.get(COMPLETED_DATE_KEY, "");
            if (dateValue.equals(storedDate)) {
                completedDate = storedDate;
                return true;
            }
        } catch (SecurityException exception) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "No se pudo consultar la fecha del corte diario: " + exception.getMessage());
        }
        return false;
    }

    private static LocalDate toLocalDate(Date date) {
        Instant instant = date != null ? date.toInstant() : Instant.now();
        return instant.atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
