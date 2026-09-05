package com.openbravo.pos.panels;

import com.openbravo.pos.scripting.ScriptEngine;
import com.openbravo.pos.scripting.ScriptFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;

public final class ShiftCloseTicketRegressionTest {

    private ShiftCloseTicketRegressionTest() {
    }

    public static void main(String[] args) throws Exception {
        verifyRefundScenario();
        verifyMixedPaymentsScenario();
        verifyTemplateStructure();
        System.out.println("PASS: corte de turno neto, conciliado y renderizado correctamente.");
    }

    private static void verifyRefundScenario() {
        double netCash = 599.0;
        double returns = 98.0;
        double grossCash = CloseCashAccounting.grossCashSales(netCash, returns);
        assertAmount("efectivo bruto del turno", 697.0, grossCash);
        assertAmount("ventas netas del turno", 599.0,
                CloseCashAccounting.netSalesAfterReturns(
                        grossCash, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, returns));
        assertAmount("efectivo esperado del turno", 699.0,
                CloseCashAccounting.expectedCash(100.0, netCash, 0.0, 0.0, 0.0));
    }

    private static void verifyMixedPaymentsScenario() {
        CloseCashAccounting.PaymentTotals totals = new CloseCashAccounting.PaymentTotals();
        totals.add("cash", 500.0);
        totals.add("card", 200.0);
        totals.add("bank", 100.0);
        totals.add("cheque", 50.0);
        totals.add("voucher", 25.0);
        totals.add("debt", 75.0);
        assertAmount("pagos mixtos del turno", 950.0, totals.getSalesTotal());

        double returns = 50.0;
        double grossCash = CloseCashAccounting.grossCashSales(totals.getCash(), returns);
        assertAmount("ventas mixtas netas del turno", 950.0,
                CloseCashAccounting.netSalesAfterReturns(
                        grossCash, totals.getCard(), totals.getCredit(), totals.getVoucher(),
                        totals.getTransfer(), totals.getCheque(), totals.getOther(), returns));
    }

    private static void verifyTemplateStructure() throws Exception {
        String resource = "/com/openbravo/pos/templates/Printer.CloseCash.xml";
        try (InputStream input = ShiftCloseTicketRegressionTest.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new AssertionError("No se encontro " + resource);
            }
            String template = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            require(template, "CIERRE DE TURNO");
            require(template, "$shiftCashGross");
            require(template, "DEV. DE VENTAS:");
            require(template, "VTAS EFEC. NETAS:");
            require(template, "$shiftExpectedCash");
            forbid(template, "${payments.printPaymentsTotal()}");
            verifyRenderedTemplate(template);
        }
    }

    private static void verifyRenderedTemplate(String template) throws Exception {
        ScriptEngine script = ScriptFactory.getScriptEngine(ScriptFactory.VELOCITY);
        script.put("isDayClose", Boolean.FALSE);
        script.put("allShifts", null);
        script.put("payments", new FakePayments());
        script.put("shiftTotalSales", "$599.00");
        script.put("shiftReceiptCount", 8);
        script.put("shiftCashGross", "$697.00");
        script.put("shiftCashNet", "$599.00");
        script.put("shiftCardSales", "$0.00");
        script.put("shiftCreditSales", "$0.00");
        script.put("shiftVoucherSales", "$0.00");
        script.put("shiftTransferSales", "$0.00");
        script.put("shiftChequeSales", "$0.00");
        script.put("showShiftOtherSales", Boolean.FALSE);
        script.put("shiftOtherSales", "$0.00");
        script.put("shiftReturns", "$98.00");
        script.put("shiftDeptLines", List.of(
                Map.of("compactName", "TORT. MOST.", "sales", "$300.00"),
                Map.of("compactName", "MASA MOSTRADOR", "sales", "$299.00")));
        script.put("shiftDepartmentSales", "$599.00");
        script.put("shiftInitialAmount", "$100.00");
        script.put("shiftCreditPay", "$0.00");
        script.put("shiftCashIn", "$0.00");
        script.put("shiftCashOut", "$0.00");
        script.put("shiftExpectedCash", "$699.00");

        String rendered = String.valueOf(script.eval(template));
        forbid(rendered, "$shift");
        require(rendered, "- $98.00");
        require(rendered, "$599.00");
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(rendered.getBytes(StandardCharsets.UTF_8)));
    }

    public static final class FakePayments {
        public String printUser() {
            return "cajera";
        }

        public String printSequence() {
            return "12";
        }

        public String printDateStart() {
            return "22/08/2026, 8:00 a.m.";
        }

        public String printDateEnd() {
            return "22/08/2026, 7:00 p.m.";
        }
    }

    private static void require(String text, String expected) {
        if (!text.contains(expected)) {
            throw new AssertionError("Falta en el corte de turno: " + expected);
        }
    }

    private static void forbid(String text, String forbidden) {
        if (text.contains(forbidden)) {
            throw new AssertionError("Texto antiguo o variable sin resolver: " + forbidden);
        }
    }

    private static void assertAmount(String label, double expected, double actual) {
        if (Math.abs(expected - actual) > 0.001) {
            throw new AssertionError(label + ": esperado " + expected + ", obtenido " + actual);
        }
    }
}
