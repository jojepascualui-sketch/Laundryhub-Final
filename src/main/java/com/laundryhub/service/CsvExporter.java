package com.laundryhub.service;

import com.laundryhub.model.Order;
import com.laundryhub.util.MoneyFormatter;
import com.laundryhub.util.TimeUtils;
import java.util.List;

/**
 * Builds the "Export CSV" file of the Sales reports screen (A5). Pure Java, unit tested.
 * Amounts are plain numbers (e.g. 410.00) so spreadsheets can add them up.
 */
public final class CsvExporter {

    private static final String[] HEADER = {
        "Order no", "Date", "Customer", "Service", "Kg", "Fabric soap", "Payment", "Payment status",
        "Order status", "Laundry", "Soap add-on", "Delivery fee", "Total"
    };
    private static final String NEWLINE = "\r\n";

    private CsvExporter() {
    }

    /**
     * @param orders orders to export
     * @return CSV text with a header row (UTF-8 BOM so Excel shows ₱ and ñ correctly)
     */
    public static String ordersToCsv(List<Order> orders) {
        StringBuilder sb = new StringBuilder("﻿");
        appendRow(sb, HEADER);
        for (Order o : orders) {
            appendRow(sb, new String[] {
                o.getOrderNo(),
                TimeUtils.manilaShortLabel(o.getCreatedAt()),
                o.getCustomerName(),
                o.getServiceType().getDisplayName(),
                Integer.toString(o.getKg()),
                o.getSoapName(),
                o.getPaymentLabel(),
                o.getPaymentStatus().name(),
                o.getStatus().getLabel(),
                amount(o.getLaundryCost()),
                amount(o.getSoapCost()),
                amount(o.getDeliveryFee()),
                amount(o.getTotal())
            });
        }
        return sb.toString();
    }

    /**
     * @param value a cell value
     * @return the value quoted when it contains a comma, quote or line break (RFC 4180)
     */
    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes = value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
        // Cells starting with = + - @ could run as formulas in Excel; prefix them with a quote.
        String safe = !value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
        return needsQuotes ? "\"" + safe.replace("\"", "\"\"") + "\"" : safe;
    }

    private static String amount(long centavos) {
        String formatted = MoneyFormatter.format(centavos);
        return formatted.substring(formatted.indexOf('₱') + 1).replace(",", "");
    }

    private static void appendRow(StringBuilder sb, String[] cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(cells[i]));
        }
        sb.append(NEWLINE);
    }
}
