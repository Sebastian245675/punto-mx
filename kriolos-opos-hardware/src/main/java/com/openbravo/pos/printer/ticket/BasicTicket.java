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
package com.openbravo.pos.printer.ticket;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author JG uniCenta
 */
public abstract class BasicTicket implements PrintItem {

    protected java.util.List<PrintItem> printItems;
    protected PrintItemLine printItemLine;

    protected int m_iBodyHeight;

    public BasicTicket() {
        printItems = new ArrayList<>();
        printItemLine = null;
        m_iBodyHeight = 0;
    }


    protected abstract Font getBaseFont();
    protected abstract int getFontHeight();
    protected abstract double getImageScale();
    
    protected boolean isNormalTotals() {
        return false;
    }
    
    @Override
    public int getHeight() {
        return m_iBodyHeight;
    }

    @Override
    public void draw(Graphics2D g2d, int x, int y, int width) {

        int currenty = y;
        for (PrintItem pi : printItems) {
            pi.draw(g2d, x, currenty, width);
            currenty += pi.getHeight();
        }
    }

    public List<PrintItem> getCommands() {
        return printItems;
    }

    // INTERFACE FOR PRINTER 2
    public void printImage(BufferedImage image) {

        PrintItem pi = new PrintItemImage(image, getImageScale());
        printItems.add(pi);
        m_iBodyHeight += pi.getHeight();
    }

    public void printBarCode(String type, String position, String code) {

        PrintItem pi = new PrintItemBarcode(type, position, code, getImageScale());
        printItems.add(pi);
        m_iBodyHeight += pi.getHeight();
    }

    public void beginLine(int iTextSize) {
        int size = iTextSize;
        if (isNormalTotals() && size > 0) {
            size = 0;
        }
        // LOGGING: Este es el punto CLAVE donde normalTotals puede cancelar el tamaño grande
        TicketPrintLogger.logBeginLine(iTextSize, size, isNormalTotals());
        printItemLine = new PrintItemLine(size, getBaseFont(), getFontHeight());
    }

    public void printText(int iStyle, String sText) {
        if (printItemLine != null) {
            // LOGGING: Registrar cada texto que se agrega
            TicketPrintLogger.logPrintText(iStyle, sText);
            printItemLine.addText(iStyle, sText);
        }
    }

    public void endLine() {
        if (printItemLine != null) {
            // LOGGING: fin de línea
            TicketPrintLogger.logEndLine();
            printItems.add(printItemLine);
            m_iBodyHeight += printItemLine.getHeight();
            printItemLine = null;
        }
    }
}
