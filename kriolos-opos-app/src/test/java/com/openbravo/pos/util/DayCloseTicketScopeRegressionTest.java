package com.openbravo.pos.util;

import java.util.Calendar;
import java.util.Date;

public final class DayCloseTicketScopeRegressionTest {

    private DayCloseTicketScopeRegressionTest() {
    }

    public static void main(String[] args) {
        Date today = new Date();
        Calendar tomorrowCalendar = Calendar.getInstance();
        tomorrowCalendar.setTime(today);
        tomorrowCalendar.add(Calendar.DAY_OF_MONTH, 1);

        String authorizedCashier = "regression-authorized-cashier";
        String otherCashier = "regression-other-cashier";

        DayCloseTicketScope.grantAllTicketsAccess(today, authorizedCashier);

        assertAccess("authorized cashier keeps access for the close date", true,
                DayCloseTicketScope.hasAllTicketsAccess(today, authorizedCashier));
        assertAccess("another cashier is not authorized", false,
                DayCloseTicketScope.hasAllTicketsAccess(today, otherCashier));
        assertAccess("authorization expires on the next day", false,
                DayCloseTicketScope.hasAllTicketsAccess(tomorrowCalendar.getTime(), authorizedCashier));

        System.out.println("PASS: daily ticket authorization is isolated by cashier and date.");
    }

    private static void assertAccess(String scenario, boolean expected, boolean actual) {
        if (expected != actual) {
            throw new AssertionError(scenario + ": expected " + expected + ", got " + actual);
        }
    }
}
