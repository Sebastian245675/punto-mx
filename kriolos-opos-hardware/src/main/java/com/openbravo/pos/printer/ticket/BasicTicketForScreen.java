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

/**
 *
 * @author JG uniCenta
 */
public class BasicTicketForScreen extends BasicTicket {

    private final Font baseFont;
    private final int fontHeight;
    private final double imageScale;
    private final boolean normalTotals;

    public BasicTicketForScreen() {
        this("Courier New", 7, false, false);
    }

    public BasicTicketForScreen(String fontName, int fontSize, boolean fontBold, boolean normalTotals) {
        int style = fontBold ? Font.BOLD : Font.PLAIN;
        int screenFontSize = Math.max(10, Math.min(fontSize, 18));

        this.baseFont = new Font(fontName, style, screenFontSize);
        this.fontHeight = Math.max(14, (int) Math.round(screenFontSize * 1.35));
        this.imageScale = 1.0;
        this.normalTotals = normalTotals;

        TicketPrintLogger.logTicketCreated(fontName, fontSize, fontBold, normalTotals);
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
