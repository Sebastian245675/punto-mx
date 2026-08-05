package com.openbravo.pos.printer.printer;

import com.openbravo.pos.printer.ticket.BasicTicket;
import com.openbravo.pos.printer.ticket.PrintItem;
import com.openbravo.pos.printer.ticket.TicketPrintLogger;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;

/**
 *
 * @author adrianromero
 */
public class PrintableBasicTicket implements Printable {

    private int imageable_width;
    private int imageable_height;
    private int imageable_x;
    private int imageable_y;
    private int columns = 42;
    private int fontSize = 7;

    private BasicTicket ticket;

    /**
     *
     * @param ticket
     * @param imageable_x
     * @param imageable_y
     * @param imageable_width
     * @param imageable_height
     */
    public PrintableBasicTicket(BasicTicket ticket, int imageable_x, int imageable_y, int imageable_width, int imageable_height) {
        this(ticket, imageable_x, imageable_y, imageable_width, imageable_height, 42, 7);
    }

    /**
     *
     * @param ticket
     * @param imageable_x
     * @param imageable_y
     * @param imageable_width
     * @param imageable_height
     * @param columns
     * @param fontSize
     */
    public PrintableBasicTicket(BasicTicket ticket, int imageable_x, int imageable_y, int imageable_width, int imageable_height, int columns, int fontSize) {
        this.ticket = ticket;
        this.imageable_x = imageable_x;
        this.imageable_y = imageable_y;
        this.imageable_width = imageable_width;
        this.imageable_height = imageable_height;
        this.columns = columns;
        this.fontSize = fontSize;
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {

        Graphics2D g2d = (Graphics2D) graphics;

        int line = 0;
        int currentpage = 0;
        int currentpagey = 0;
        boolean printed = false;

        g2d.translate(imageable_x, imageable_y);

        // Dinámicamente calcular la escala horizontal (scaleX) basada en el ancho configurado del papel
        double physicalWidth = imageable_width > 0 ? imageable_width : 262;
        // Usar factor base constante de ancho de carácter para que el incremento de tamaño de fuente
        // no se anule al escalar y la letra se imprima grande, nítida y legible idéntica a Eleventa.
        double expectedTextWidth = (columns > 0 ? columns : 42) * 4.2;
        double scaleX = physicalWidth / expectedTextWidth;
        
        // Limitar la escala a un rango razonable para evitar compresión excesiva
        if (scaleX < 0.85) {
            scaleX = 0.85;
        } else if (scaleX > 2.5) {
            scaleX = 2.5;
        }

        g2d.scale(scaleX, 1.0);

        // El ancho lógico para cálculos de alineación y centrado es el ancho real dividido por la escala
        int logicalWidth = (int) (physicalWidth / scaleX);

        // LOGGING: Registrar la configuración de impresión
        TicketPrintLogger.logPrintableTicket(imageable_x, imageable_y, imageable_width,
                imageable_height, columns, fontSize, scaleX, logicalWidth);

        java.util.List<PrintItem> commands = ticket.getCommands();

        while (line < commands.size()) {

            int itemheight = commands.get(line).getHeight();

            if (currentpagey + itemheight <= imageable_height) {
                currentpagey += itemheight;
            } else {
                currentpage ++;
                currentpagey = itemheight;
            }

            if (currentpage < pageIndex) {
                line ++;
            } else if (currentpage == pageIndex) {
                printed = true;
                commands.get(line).draw(g2d, 0, currentpagey - itemheight, logicalWidth);

                line ++;
            } else if (currentpage > pageIndex) {
                line ++;
            }
        }

        return printed
            ? Printable.PAGE_EXISTS
            : Printable.NO_SUCH_PAGE;
    }
}
