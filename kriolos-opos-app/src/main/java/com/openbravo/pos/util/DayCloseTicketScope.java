package com.openbravo.pos.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public final class DayCloseTicketScope {

    private static final System.Logger LOGGER = System.getLogger(DayCloseTicketScope.class.getName());
    private static final Preferences PREFERENCES = Preferences.userRoot()
            .node("com/openbravo/pos/dayClose");
    private static final String COMPLETED_DATE_KEY = "completedDate";
    private static final String AUTHORIZED_DATE_KEY = "authorizedDate";
    private static final String AUTHORIZED_USERS_KEY = "authorizedUsers";
    private static volatile String completedDate;
    private static volatile String authorizedDate;
    private static volatile Set<String> authorizedUsers = Collections.emptySet();

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

    public static synchronized void grantAllTicketsAccess(Date date, String userId) {
        String normalizedUserId = normalizeUserId(userId);
        if (normalizedUserId == null) {
            return;
        }

        String dateValue = toLocalDate(date).toString();
        Set<String> users = loadAuthorizedUsers(dateValue);
        users.add(normalizedUserId);
        authorizedDate = dateValue;
        authorizedUsers = Collections.unmodifiableSet(new HashSet<>(users));

        try {
            PREFERENCES.put(AUTHORIZED_DATE_KEY, dateValue);
            PREFERENCES.put(AUTHORIZED_USERS_KEY, encodeUsers(users));
            PREFERENCES.flush();
        } catch (SecurityException | BackingStoreException exception) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "No se pudo guardar la autorización para consultar tickets del día: "
                            + exception.getMessage());
        }
    }

    public static synchronized boolean hasAllTicketsAccess(Date date, String userId) {
        String normalizedUserId = normalizeUserId(userId);
        if (normalizedUserId == null) {
            return false;
        }

        String dateValue = toLocalDate(date).toString();
        return loadAuthorizedUsers(dateValue).contains(normalizedUserId);
    }

    private static Set<String> loadAuthorizedUsers(String dateValue) {
        if (dateValue.equals(authorizedDate)) {
            return new HashSet<>(authorizedUsers);
        }

        try {
            String storedDate = PREFERENCES.get(AUTHORIZED_DATE_KEY, "");
            Set<String> users = dateValue.equals(storedDate)
                    ? decodeUsers(PREFERENCES.get(AUTHORIZED_USERS_KEY, ""))
                    : new HashSet<>();
            authorizedDate = dateValue;
            authorizedUsers = Collections.unmodifiableSet(new HashSet<>(users));
            return users;
        } catch (SecurityException exception) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "No se pudo consultar la autorización para tickets del día: "
                            + exception.getMessage());
            authorizedDate = dateValue;
            authorizedUsers = Collections.emptySet();
            return new HashSet<>();
        }
    }

    private static String encodeUsers(Set<String> users) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        StringBuilder encoded = new StringBuilder();
        for (String userId : users) {
            if (encoded.length() > 0) {
                encoded.append(',');
            }
            encoded.append(encoder.encodeToString(userId.getBytes(StandardCharsets.UTF_8)));
        }
        return encoded.toString();
    }

    private static Set<String> decodeUsers(String encodedUsers) {
        Set<String> users = new HashSet<>();
        if (encodedUsers == null || encodedUsers.isEmpty()) {
            return users;
        }

        Base64.Decoder decoder = Base64.getUrlDecoder();
        for (String encodedUser : encodedUsers.split(",")) {
            try {
                String userId = new String(decoder.decode(encodedUser), StandardCharsets.UTF_8);
                String normalizedUserId = normalizeUserId(userId);
                if (normalizedUserId != null) {
                    users.add(normalizedUserId);
                }
            } catch (IllegalArgumentException exception) {
                LOGGER.log(System.Logger.Level.WARNING,
                        "Se ignoró una autorización de tickets inválida: " + exception.getMessage());
            }
        }
        return users;
    }

    private static String normalizeUserId(String userId) {
        if (userId == null) {
            return null;
        }
        String normalizedUserId = userId.trim();
        return normalizedUserId.isEmpty() ? null : normalizedUserId;
    }

    private static LocalDate toLocalDate(Date date) {
        Instant instant = date != null ? date.toInstant() : Instant.now();
        return instant.atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
