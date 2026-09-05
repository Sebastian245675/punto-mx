package com.openbravo.pos.panels;

import com.openbravo.pos.scripting.ScriptEngine;
import com.openbravo.pos.scripting.ScriptFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;

public final class DayCloseTicketRegressionTest {

    private DayCloseTicketRegressionTest() {
    }

    public static void main(String[] args) throws Exception {
        verifyReferenceScenario();
        verifyMixedPaymentsScenario();
        verifyPaymentClassification();
        verifyExpectedCashWithoutInitialFund();
        verifyDayCloseAuthorizationRules();
        verifyTemplateStructure();
        System.out.println("PASS: corte del dia conciliado con desglose por turnos.");
    }

    private static void verifyReferenceScenario() {
        double netCash = 14514.0;
        double returns = 140.0;
        double grossCash = CloseCashAccounting.grossCashSales(netCash, returns);
        assertAmount("efectivo bruto de referencia", 14654.0, grossCash);
        assertAmount("ventas netas de referencia", 14514.0,
                CloseCashAccounting.netSalesAfterReturns(grossCash, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, returns));
    }

    private static void verifyMixedPaymentsScenario() {
        double grossCash = 1000.0;
        double card = 300.0;
        double credit = 100.0;
        double vouchers = 50.0;
        double transfers = 225.0;
        double cheques = 75.0;
        double other = 25.0;
        double returns = 125.0;
        assertAmount("ventas mixtas netas", 1650.0,
                CloseCashAccounting.netSalesAfterReturns(grossCash, card, credit, vouchers,
                        transfers, cheques, other, returns));
    }

    private static void verifyPaymentClassification() {
        CloseCashAccounting.PaymentTotals totals = new CloseCashAccounting.PaymentTotals();
        totals.add("cash", 900.0);
        totals.add("magcard", 300.0);
        totals.add("debt", 100.0);
        totals.add("voucher", 50.0);
        totals.add("bank", 225.0);
        totals.add("cheque", 75.0);
        totals.add("slip", 25.0);
        assertAmount("transferencias separadas", 225.0, totals.getTransfer());
        assertAmount("cheques separados", 75.0, totals.getCheque());
        assertAmount("otros sin transferencia ni cheque", 25.0, totals.getOther());
        assertAmount("total clasificado", 1675.0, totals.getSalesTotal());
    }

    private static void verifyExpectedCashWithoutInitialFund() {
        assertAmount("efectivo esperado sin fondo inicial", 1115.0,
                CloseCashAccounting.expectedCash(0.0, 1000.0, 100.0, 50.0, 35.0));
    }

    private static void verifyDayCloseAuthorizationRules() {
        requireCondition("el administrador debe entrar directamente",
                JPanelCloseMoney.canSkipDayClosePassword("1", true));
        requireCondition("el rol ADMIN debe entrar directamente",
                JPanelCloseMoney.canSkipDayClosePassword("ADMIN", true));
        requireCondition("una cajera siempre debe usar contrasena administrativa",
                !JPanelCloseMoney.canSkipDayClosePassword("3", true));
        requireCondition("un rol personalizado siempre debe usar contrasena administrativa",
                !JPanelCloseMoney.canSkipDayClosePassword("cajera", true));
        requireCondition("sin permiso no debe existir acceso directo",
                !JPanelCloseMoney.canSkipDayClosePassword("1", false));
    }

    private static void verifyTemplateStructure() throws Exception {
        String resource = "/com/openbravo/pos/templates/Printer.CloseCash.xml";
        try (InputStream input = DayCloseTicketRegressionTest.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new AssertionError("No se encontro " + resource);
            }
            String template = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            require(template, "REALIZADO:");
            require(template, "VENTAS TOTALES:");
            require(template, "DEV. DE VENTAS:");
            require(template, "TOTAL VENTAS:");
            require(template, "== VENTAS POR TURNO ==");
            require(template, "TOTAL TURNOS:");
            require(template, "== VENTAS POR DEPTO ==");
            forbid(template, "DEV. YA RESTADAS:");
            verifyRenderedTemplate(template);
        }
    }

    private static void verifyRenderedTemplate(String template) throws Exception {
        ScriptEngine script = ScriptFactory.getScriptEngine(ScriptFactory.VELOCITY);
        script.put("isDayClose", Boolean.TRUE);
        script.put("allShifts", List.of(new ShiftSummaryStub("TURNO #2 Juan", "$14,514.00")));
        script.put("dayCloseDate", "20/AGO/2026");
        script.put("dayClosePerformedAt", "20/AGO/2026 6:28 PM");
        script.put("totalDaySales", "$14,514.00");
        script.put("totalDayProfit", "$6,067.38");
        script.put("totalDayReceiptCount", 483);
        script.put("totalDayCashGross", "$14,654.00");
        script.put("totalDayCardSales", "$0.00");
        script.put("totalDayCreditSales", "$0.00");
        script.put("totalDayVoucherSales", "$0.00");
        script.put("totalDayTransferSales", "$0.00");
        script.put("totalDayChequeSales", "$0.00");
        script.put("showTotalDayOtherSales", Boolean.FALSE);
        script.put("totalDayOtherSales", "$0.00");
        script.put("totalDayReturns", "$140.00");
        script.put("dayDeptLines", List.of(Map.of(
                "compactName", "TORTILLA MOST.",
                "sales", "$14,514.00")));
        script.put("totalDayDepartmentSales", "$14,514.00");
        script.put("totalDayCreditPay", "$0.00");
        script.put("totalDayCashIn", "$0.00");
        script.put("totalDayCashOut", "$0.00");
        script.put("totalDayCashTotal", "$14,514.00");

        String rendered = String.valueOf(script.eval(template));
        forbid(rendered, "$totalDay");
        forbid(rendered, "$shift");
        require(rendered, "TURNO #2 Juan");
        require(rendered, "TOTAL TURNOS:");
        forbid(rendered, "VTAS EFEC. NETAS:");
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(rendered.getBytes(StandardCharsets.UTF_8)));
    }

    private static void require(String text, String expected) {
        if (!text.contains(expected)) {
            throw new AssertionError("Falta en la plantilla: " + expected);
        }
    }

    private static void forbid(String text, String forbidden) {
        if (text.contains(forbidden)) {
            throw new AssertionError("Texto repetido o ambiguo en la plantilla: " + forbidden);
        }
    }

    private static void assertAmount(String label, double expected, double actual) {
        if (Math.abs(expected - actual) > 0.001) {
            throw new AssertionError(label + ": esperado " + expected + ", obtenido " + actual);
        }
    }

    private static void requireCondition(String label, boolean condition) {
        if (!condition) {
            throw new AssertionError(label);
        }
    }

    public static final class ShiftSummaryStub {
        private final String label;
        private final String total;

        ShiftSummaryStub(String label, String total) {
            this.label = label;
            this.total = total;
        }

        public String printCompactLabel() {
            return label;
        }

        public String printTotalSales() {
            return total;
        }
    }
}
