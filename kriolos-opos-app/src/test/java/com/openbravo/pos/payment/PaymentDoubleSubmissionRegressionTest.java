package com.openbravo.pos.payment;

import com.openbravo.basic.BasicException;
import com.openbravo.pos.customers.CustomerInfoExt;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.ticket.TicketInfo;
import com.openbravo.pos.ticket.TicketLineInfo;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

public final class PaymentDoubleSubmissionRegressionTest {

    private static final int STRESS_SALE_COUNT = 600;

    private PaymentDoubleSubmissionRegressionTest() {
    }

    public static void main(String[] args) throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("This Swing regression test requires a graphical environment.");
        }

        Method paymentAction = JPaymentSelect.class.getDeclaredMethod(
                "m_jButtonOKActionPerformed", ActionEvent.class);
        paymentAction.setAccessible(true);
        Method validator = DataLogicSales.class.getDeclaredMethod("validateTicketPaymentTotal", TicketInfo.class);
        validator.setAccessible(true);

        for (int saleNumber = 1; saleNumber <= STRESS_SALE_COUNT; saleNumber++) {
            double total = (((saleNumber * 37) % 400) + 1) * 0.5;
            double tendered = Math.ceil(total / 20.0) * 20.0;
            runStressSale(saleNumber, total, tendered, paymentAction, validator);
        }

        verifyCashCalculations();
        verifyPersistenceGuard();
        System.out.println("PASS: " + STRESS_SALE_COUNT
                + " sales completed without duplicate payments or change inconsistencies.");
    }

    private static void runStressSale(int saleNumber, double total, double tendered, Method paymentAction,
            Method validator) throws Exception {
        TestPaymentSelect[] dialogHolder = new TestPaymentSelect[1];
        FakeCashPayment payment = new FakeCashPayment(total, tendered);

        SwingUtilities.invokeAndWait(() -> {
            try {
                TestPaymentSelect dialog = new TestPaymentSelect();
                dialogHolder[0] = dialog;
                setField(dialog, "m_aPaymentInfo", new PaymentInfoList());
                setDoubleField(dialog, "m_dTotal", total);
                JTabbedPane tabs = (JTabbedPane) getField(dialog, "m_jTabPayment");
                tabs.removeAll();
                tabs.addTab("Efectivo", payment);
                ActionEvent event = new ActionEvent(dialog, ActionEvent.ACTION_PERFORMED, "stress-payment");
                int duplicateAttempts = 2 + (saleNumber % 7);
                for (int attempt = 0; attempt < duplicateAttempts; attempt++) {
                    paymentAction.invoke(dialog, event);
                }
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(ex);
            }
        });

        TestPaymentSelect dialog = dialogHolder[0];
        waitForPayment(dialog);

        if (payment.executionCount.get() != 1) {
            throw new AssertionError("Sale " + saleNumber + " executed payment "
                    + payment.executionCount.get() + " times.");
        }
        if (dialog.getSelectedPayments().size() != 1) {
            throw new AssertionError("Sale " + saleNumber + " stored "
                    + dialog.getSelectedPayments().size() + " payments.");
        }
        assertAmount("sale " + saleNumber + " payment total", total, dialog.getTotal());
        assertAmount("sale " + saleNumber + " tendered", tendered, dialog.getPaidTotal());

        TicketInfo ticket = createTicket(TicketInfo.RECEIPT_NORMAL, total);
        ticket.setPayments(dialog.getSelectedPayments());
        validator.invoke(null, ticket);
        assertAmount("sale " + saleNumber + " cash received", tendered, ticket.getCashReceived());
        assertAmount("sale " + saleNumber + " cash change", tendered - total, ticket.getCashChange());

        SwingUtilities.invokeAndWait(dialog::dispose);
    }

    private static void verifyCashCalculations() {
        TicketInfo validTicket = createTicket(TicketInfo.RECEIPT_NORMAL, 55.0);
        validTicket.setPayments(List.of(new PaymentInfoCash(55.0, 110.0)));
        assertAmount("valid cash received", 110.0, validTicket.getCashReceived());
        assertAmount("valid cash change", 55.0, validTicket.getCashChange());

        TicketInfo oldCorruptedTicket = createTicket(TicketInfo.RECEIPT_NORMAL, 55.0);
        oldCorruptedTicket.setPayments(List.of(new PaymentInfoCash(110.0, 200.0)));
        assertAmount("corrupted cash received fallback", 110.0, oldCorruptedTicket.getCashReceived());
        assertAmount("corrupted cash change", 55.0, oldCorruptedTicket.getCashChange());

        TicketInfo mixedTicket = createTicket(TicketInfo.RECEIPT_NORMAL, 100.0);
        mixedTicket.setPayments(List.of(
                new PaymentInfoCash(60.0, 100.0),
                new PaymentInfoTicket(40.0, "magcard")));
        assertAmount("mixed cash received", 100.0, mixedTicket.getCashReceived());
        assertAmount("mixed cash change", 40.0, mixedTicket.getCashChange());
    }

    private static void verifyPersistenceGuard() throws Exception {
        Method validator = DataLogicSales.class.getDeclaredMethod("validateTicketPaymentTotal", TicketInfo.class);
        validator.setAccessible(true);

        TicketInfo validTicket = createTicket(TicketInfo.RECEIPT_NORMAL, 55.0);
        validTicket.setPayments(List.of(new PaymentInfoCash(55.0, 110.0)));
        validator.invoke(null, validTicket);

        TicketInfo invalidTicket = createTicket(TicketInfo.RECEIPT_NORMAL, 55.0);
        invalidTicket.setPayments(List.of(new PaymentInfoCash(110.0, 200.0)));
        try {
            validator.invoke(null, invalidTicket);
            throw new AssertionError("Persistence guard accepted a $110 payment for a $55 ticket.");
        } catch (InvocationTargetException ex) {
            if (!(ex.getCause() instanceof BasicException)) {
                throw ex;
            }
        }
    }

    private static TicketInfo createTicket(int type, double total) {
        TicketInfo ticket = new TicketInfo();
        ticket.setTicketType(type);
        ticket.setLines(List.of(new TicketLineInfo("test-product", 1.0, total, null)));
        return ticket;
    }

    private static void assertAmount(String label, double expected, double actual) {
        if (Math.abs(expected - actual) > 0.001) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private static void waitForPayment(TestPaymentSelect dialog) throws Exception {
        for (int attempt = 0; attempt < 200; attempt++) {
            SwingUtilities.invokeAndWait(() -> {
            });
            if (dialog.getSelectedPayments().size() == 1) {
                return;
            }
            Thread.sleep(10L);
        }
        throw new AssertionError("Payment worker did not finish.");
    }

    private static Object getField(Object target, String fieldName) throws ReflectiveOperationException {
        Field field = JPaymentSelect.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = JPaymentSelect.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void setDoubleField(Object target, String fieldName, double value)
            throws ReflectiveOperationException {
        Field field = JPaymentSelect.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setDouble(target, value);
    }

    private static final class FakeCashPayment extends JPanel implements JPaymentInterface {

        private static final long serialVersionUID = 1L;
        private final AtomicInteger executionCount = new AtomicInteger();
        private final double total;
        private final double tendered;

        private FakeCashPayment(double total, double tendered) {
            this.total = total;
            this.tendered = tendered;
        }

        @Override
        public void activate(CustomerInfoExt customer, double total, String transactionId) {
        }

        @Override
        public PaymentInfo executePayment() {
            executionCount.incrementAndGet();
            return new PaymentInfoCash(total, tendered);
        }

        @Override
        public Component getComponent() {
            return this;
        }
    }

    private static final class TestPaymentSelect extends JPaymentSelect {

        private static final long serialVersionUID = 1L;

        private TestPaymentSelect() {
            super((JFrame) null, false, ComponentOrientation.LEFT_TO_RIGHT);
        }

        @Override
        protected void addTabs() {
        }

        @Override
        protected void setStatusPanel(boolean positive, boolean complete) {
        }

        @Override
        protected PaymentInfo getDefaultPayment(double total) {
            return new PaymentInfoCash(total, total);
        }
    }

}
