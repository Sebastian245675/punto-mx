package com.openbravo.pos.sales;

public final class DailyTicketVisibilityRegressionTest {

    private DailyTicketVisibilityRegressionTest() {
    }

    public static void main(String[] args) {
        assertVisible("admin sees Juan's sale", true,
                JPanelTicket.canViewDailyTicket(true, false, "admin", "juan"));
        assertVisible("Juan sees Juan's sale", true,
                JPanelTicket.canViewDailyTicket(false, false, "juan", "juan"));
        assertVisible("Maria cannot see Juan's sale", false,
                JPanelTicket.canViewDailyTicket(false, false, "maria", "juan"));
        assertVisible("Juan cannot see Maria's sale", false,
                JPanelTicket.canViewDailyTicket(false, false, "juan", "maria"));
        assertVisible("authorized Juan sees Maria's sale after day close", true,
                JPanelTicket.canViewDailyTicket(false, true, "juan", "maria"));
        assertVisible("Maria remains isolated when only Juan was authorized", false,
                JPanelTicket.canViewDailyTicket(false, false, "maria", "juan"));

        System.out.println("PASS: day-close authorization expands only the authorized cashier's ticket scope.");
    }

    private static void assertVisible(String scenario, boolean expected, boolean actual) {
        if (expected != actual) {
            throw new AssertionError(scenario + ": expected " + expected + ", got " + actual);
        }
    }
}
