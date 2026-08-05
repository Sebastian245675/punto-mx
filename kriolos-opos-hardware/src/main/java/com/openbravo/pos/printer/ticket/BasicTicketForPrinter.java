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
import java.awt.geom.AffineTransform;

/**
 *
 * @author jaroslawwozniak
 * @author adrianromero
 *
 */
public class BasicTicketForPrinter extends BasicTicket {

    private final Font baseFont;
    private final int fontHeight;
    private final double imageScale;
    private final boolean normalTotals;

    public BasicTicketForPrinter() {
        this("Courier New", 7, false, false);
    }

    public BasicTicketForPrinter(String fontName, int fontSize, boolean fontBold, boolean normalTotals) {
        int style = fontBold ? Font.BOLD : Font.PLAIN;
        int effectiveSize = Math.max(fontSize, 11);
        this.baseFont = new Font(fontName, style, effectiveSize)
                .deriveFont(AffineTransform.getScaleInstance(1.45, 1.45));
        this.fontHeight = (int) Math.round(effectiveSize * 1.8);
        this.imageScale = 0.65;
        this.normalTotals = normalTotals;

        // LOGGING: Registrar la configuración del ticket
        TicketPrintLogger.logTicketCreated(fontName, fontSize, fontBold, normalTotals);
        TicketPrintLogger.logBaseFontCalculated(fontName, fontSize, effectiveSize, this.baseFont);
    }

    @Override
    protected Font getBaseFont() {
        return baseFont;
    }

    @Override
    protected int getFontHeight() {
        return fontHeight;
    }

    @Override
    protected double getImageScale() {
        return imageScale;
    }

    @Override
    protected boolean isNormalTotals() {
        return normalTotals;
    }
}