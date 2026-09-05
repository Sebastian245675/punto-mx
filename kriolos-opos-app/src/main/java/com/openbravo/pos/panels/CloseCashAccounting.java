package com.openbravo.pos.panels;

import com.openbravo.pos.util.RoundUtils;

final class CloseCashAccounting {

    private CloseCashAccounting() {
    }

    static double cashSalesContribution(String paymentType, double amount) {
        if ("cash".equals(paymentType)) {
            return amount;
        }
        if ("cashrefund".equals(paymentType)) {
            return -Math.abs(amount);
        }
        return 0.0;
    }

    static double cashMovementContribution(String paymentType, double amount) {
        double cashSales = cashSalesContribution(paymentType, amount);
        if (cashSales != 0.0 || "cash".equals(paymentType) || "cashrefund".equals(paymentType)) {
            return cashSales;
        }
        if ("cashin".equals(paymentType)) {
            return Math.abs(amount);
        }
        if ("cashout".equals(paymentType)) {
            return -Math.abs(amount);
        }
        return 0.0;
    }

    static double positiveOutflow(double amount) {
        return Math.abs(amount);
    }

    static double expectedCash(double initialAmount, double netCashSales, double cashCreditPayments,
            double cashIn, double cashOut) {
        return initialAmount + netCashSales + cashCreditPayments + Math.abs(cashIn) - Math.abs(cashOut);
    }

    static double grossCashSales(double netCashSales, double cashReturns) {
        return netCashSales + Math.abs(cashReturns);
    }

    static double netSalesAfterReturns(double grossCashSales, double cardSales, double creditSales,
            double voucherSales, double transferSales, double chequeSales, double otherSales, double returns) {
        return grossCashSales + cardSales + creditSales + voucherSales + transferSales + chequeSales + otherSales
                - Math.abs(returns);
    }

    static double ticketTotal(double subtotal, double tax) {
        return Math.round((RoundUtils.round(subtotal) + RoundUtils.round(tax)) * 2.0) / 2.0;
    }

    static final class PaymentTotals {

        private double cash;
        private double card;
        private double credit;
        private double voucher;
        private double transfer;
        private double cheque;
        private double other;

        void add(String paymentType, double amount) {
            if ("cash".equals(paymentType) || "cashrefund".equals(paymentType)) {
                cash += cashSalesContribution(paymentType, amount);
            } else if ("card".equals(paymentType) || "magcard".equals(paymentType)
                    || "ccard".equals(paymentType)) {
                card += amount;
            } else if ("magcardrefund".equals(paymentType) || "ccardrefund".equals(paymentType)) {
                card -= Math.abs(amount);
            } else if ("debt".equals(paymentType)) {
                credit += amount;
            } else if ("voucher".equals(paymentType) || "voucherin".equals(paymentType)) {
                voucher += amount;
            } else if ("voucherout".equals(paymentType)) {
                voucher -= Math.abs(amount);
            } else if ("bank".equals(paymentType)) {
                transfer += amount;
            } else if ("bankrefund".equals(paymentType)) {
                transfer -= Math.abs(amount);
            } else if ("cheque".equals(paymentType) || "paper".equals(paymentType)) {
                cheque += amount;
            } else if ("chequerefund".equals(paymentType) || "paperout".equals(paymentType)) {
                cheque -= Math.abs(amount);
            } else if ("cashin".equals(paymentType) || "cashout".equals(paymentType)
                    || "debtpaid".equals(paymentType)) {
                return;
            } else {
                other += amount;
            }
        }

        double reconcileToSales(double authoritativeSales) {
            double adjustment = authoritativeSales - getSalesTotal();
            if (Math.abs(adjustment) >= 0.005) {
                cash += adjustment;
            }
            return adjustment;
        }

        double getSalesTotal() {
            return cash + card + credit + voucher + transfer + cheque + other;
        }

        double getCash() {
            return cash;
        }

        double getCard() {
            return card;
        }

        double getCredit() {
            return credit;
        }

        double getVoucher() {
            return voucher;
        }

        double getTransfer() {
            return transfer;
        }

        double getCheque() {
            return cheque;
        }

        double getOther() {
            return other;
        }
    }
}
