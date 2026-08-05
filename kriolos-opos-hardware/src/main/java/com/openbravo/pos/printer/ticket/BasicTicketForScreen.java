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
        // Scale the printer font size up to screen size (base printer size is 7, base
        // screen size is 12)
        double screenScale = 12.0 / 7.0;
        int screenFontSize = (int) Math.round(fontSize * screenScale);

        this.baseFont = new Font(fontName, style, screenFontSize)
                .deriveFont(AffineTransform.getScaleInstance(1.65, 1.40));
        this.fontHeight = (int) Math.round(screenFontSize * 1.7);
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
