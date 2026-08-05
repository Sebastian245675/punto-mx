/*
 * Copyright (C) 2022 KriolOS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.openbravo.pos.printer.screen;

import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppProperties;
import com.openbravo.pos.printer.DevicePrinter;
import com.openbravo.pos.printer.ticket.BasicTicket;
import com.openbravo.pos.printer.ticket.BasicTicketForScreen;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.JScrollPane;

/**
 *
 * @author JG uniCenta
 */
public class DevicePrinterPanel extends javax.swing.JPanel implements DevicePrinter {

    private final String m_sName;

    private final JTicketContainer m_jTicketContainer;
    private BasicTicket m_ticketcurrent;
    private JTicket m_lastTicket;

    private String fontName = "Arial";
    private int fontSize = 16;
    private boolean fontBold = true;
    private int columns = 42;
    private boolean normalTotals = false;
    private int paperWidth = 262;
    private int paperHeight = 546;

    /** Creates new form JPrinterScreen2 */
    public DevicePrinterPanel() {
        this(null, null);
    }

    public DevicePrinterPanel(AppProperties props, String printerParam2) {
        initComponents();

        m_sName = AppLocal.getIntString("printer.screen");

        m_ticketcurrent = null;

        if (props != null) {
            String param = (printerParam2 == null || printerParam2.equals("") || printerParam2.equals("true")) ? "receipt" : "standard";

            String fontNameProp = props.getProperty("paper." + param + ".fontname");
            if (fontNameProp == null) {
                fontNameProp = props.getProperty("paper.receipt.fontname");
            }
            this.fontName = (fontNameProp != null) ? fontNameProp : "Arial";

            String fontSizeProp = props.getProperty("paper." + param + ".fontsize");
            if (fontSizeProp == null) {
                fontSizeProp = props.getProperty("paper.receipt.fontsize");
            }
            try {
                this.fontSize = Integer.parseInt(fontSizeProp != null ? fontSizeProp : "16");
            } catch (Exception ex) {
                this.fontSize = 16;
            }

            String fontBoldProp = props.getProperty("paper." + param + ".fontbold");
            if (fontBoldProp == null) {
                fontBoldProp = props.getProperty("paper.receipt.fontbold");
            }
            this.fontBold = Boolean.parseBoolean(fontBoldProp != null ? fontBoldProp : "true");

            String columnsProp = props.getProperty("paper." + param + ".columns");
            if (columnsProp == null) {
                columnsProp = props.getProperty("paper.receipt.columns");
            }
            try {
                this.columns = Integer.parseInt(columnsProp != null ? columnsProp : "42");
            } catch (Exception ex) {
                this.columns = 42;
            }

            String widthProp = props.getProperty("paper." + param + ".width");
            try {
                this.paperWidth = Integer.parseInt(widthProp != null ? widthProp : "262");
            } catch (Exception ex) {
                this.paperWidth = 262;
            }

            String heightProp = props.getProperty("paper." + param + ".height");
            try {
                this.paperHeight = Integer.parseInt(heightProp != null ? heightProp : "546");
            } catch (Exception ex) {
                this.paperHeight = 546;
            }

            String normalTotalsProp = props.getProperty("paper." + param + ".normaltotals");
            if (normalTotalsProp == null) {
                normalTotalsProp = props.getProperty("paper.receipt.normaltotals");
            }
            this.normalTotals = Boolean.parseBoolean(normalTotalsProp != null ? normalTotalsProp : "false");
        }

        m_jTicketContainer = new JTicketContainer();
        m_jScrollView.setViewportView(m_jTicketContainer);
        m_jScrollView.getVerticalScrollBar().setValue(0);
        m_jScrollView.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    }

    /**
     *
     * @return
     */
    @Override
    public String getPrinterName() {
        return m_sName;
    }

    /**
     *
     */
    @Override
    public void printLogo() {
    }

    /**
     *
     * @return
     */
    @Override
    public String getPrinterDescription() {
        return null;
    }

    /**
     *
     * @return
     */
    @Override
    public JComponent getPrinterComponent() {
        return this;
    }

    /**
     *
     */
    @Override
    public void reset() {
        m_ticketcurrent = null;
        m_jTicketContainer.removeAllTickets();
        m_jTicketContainer.repaint();
    }

    @Override
    public void beginReceipt() {
        m_ticketcurrent = new BasicTicketForScreen(fontName, fontSize, fontBold, normalTotals);
    }

    /**
     *
     * @param image
     */
    @Override
    public void printImage(BufferedImage image) {
        m_ticketcurrent.printImage(image);
    }

    /**
     *
     * @param type
     * @param position
     * @param code
     */
    @Override
    public void printBarCode(String type, String position, String code) {
        m_ticketcurrent.printBarCode(type, position, code);
    }

    /**
     *
     * @param iTextSize
     */
    @Override
    public void beginLine(int iTextSize) {
        m_ticketcurrent.beginLine(iTextSize);
    }

    /**
     *
     * @param iStyle
     * @param sText
     */
    @Override
    public void printText(int iStyle, String sText) {
        m_ticketcurrent.printText(iStyle, sText);
    }

    /**
     *
     */
    @Override
    public void endLine() {
        m_ticketcurrent.endLine();
    }

    /**
     *
     */
    @Override
    public void endReceipt() {
        double screenScale = 12.0 / 7.0;
        int screenFontSize = (int) Math.round(fontSize * screenScale);
        m_lastTicket = new JTicket(m_ticketcurrent, columns, paperWidth, screenFontSize);
        m_jTicketContainer.addTicket(m_lastTicket);
        m_ticketcurrent = null;
    }

    /**
     * Marca el último ticket mostrado con el sello de CANCELADO.
     * 
     * @param cancelled true para mostrar el sello, false para ocultarlo
     */
    public void setCancelled(boolean cancelled) {
        if (m_lastTicket != null) {
            m_lastTicket.setCancelled(cancelled);
        }
    }

    /**
     *
     */
    @Override
    public void openDrawer() {
        Toolkit.getDefaultToolkit().beep();
        try {
            javax.print.PrintService defaultPrinter = javax.print.PrintServiceLookup.lookupDefaultPrintService();
            if (defaultPrinter != null) {
                byte[] openDrawerBytes = new byte[]{
                    0x1B, 0x70, 0x00, 0x19, (byte) 0xFA,
                    0x1B, 0x70, 0x01, 0x19, (byte) 0xFA
                };
                javax.print.DocPrintJob job = defaultPrinter.createPrintJob();
                javax.print.Doc doc = new javax.print.SimpleDoc(openDrawerBytes, javax.print.DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
                job.print(doc, null);
            }
        } catch (Exception ex) {
            // Silencioso en modo vista previa por pantalla
        }
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        m_jScrollView = new javax.swing.JScrollPane();

        setLayout(new java.awt.BorderLayout());

        m_jScrollView.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        add(m_jScrollView, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane m_jScrollView;
    // End of variables declaration//GEN-END:variables

}
