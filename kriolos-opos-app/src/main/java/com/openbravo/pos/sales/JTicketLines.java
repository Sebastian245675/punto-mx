//    KriolOS POS
//    Copyright (c) 2019-2023 KriolOS
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package com.openbravo.pos.sales;

import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.scripting.ScriptEngine;
import com.openbravo.pos.scripting.ScriptException;
import com.openbravo.pos.scripting.ScriptFactory;
import com.openbravo.pos.ticket.TicketLineInfo;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author JG uniCenta
 */
public class JTicketLines extends javax.swing.JPanel {

    private static final Logger logger = Logger.getLogger("com.openbravo.pos.sales.JTicketLines");

    private static SAXParser m_sp = null;

    private final TicketTableModel m_jTableModel;
    private Boolean sendStatus;
    private DeleteLineCallback deleteLineCallback;
    private int hoveredRow = -1;

    /**
     * Creates new form JLinesTicket
     *
     * @param ticketline
     */
    public JTicketLines(String ticketline) {
        logger.log(Level.FINEST, "Creating ticketline from: " + ticketline);
        initComponents();

        ColumnTicket[] acolumns = new ColumnTicket[0];

        if (ticketline != null) {
            try {
                if (m_sp == null) {
                    SAXParserFactory spf = SAXParserFactory.newInstance();
                    m_sp = spf.newSAXParser();
                }
                ColumnsHandler columnshandler = new ColumnsHandler();
                m_sp.parse(new InputSource(new StringReader(ticketline)), columnshandler);
                acolumns = columnshandler.getColumns();

            } catch (ParserConfigurationException ePC) {
                logger.log(Level.WARNING, "exception.parserconfig" + ticketline, ePC);
            } catch (SAXException eSAX) {
                logger.log(Level.WARNING, "exception.xmlfile" + ticketline, eSAX);
            } catch (IOException eIO) {
                logger.log(Level.WARNING, "exception.iofile" + ticketline, eIO);
            }
        }

        m_jTableModel = new TicketTableModel(acolumns);
        m_jTicketTable.setModel(m_jTableModel);

        //m_jTicketTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        TableColumnModel jColumns = m_jTicketTable.getColumnModel();
        TicketCellRenderer defaultRenderer = new TicketCellRenderer(acolumns);
        
        for (int i = 0; i < acolumns.length; i++) {
            jColumns.getColumn(i).setPreferredWidth(acolumns[i].width);
            jColumns.getColumn(i).setResizable(false);
            
            // Configurar renderizador específico para cada columna
            jColumns.getColumn(i).setCellRenderer(defaultRenderer);
        }

        m_jScrollTableTicket.getVerticalScrollBar().setPreferredSize(new Dimension(35, 35));

        // set font for headers
        Font f = new Font("Arial", Font.BOLD, 14);
        JTableHeader header = m_jTicketTable.getTableHeader();
        header.setFont(f);
        
        // Configurar renderer personalizado para el header que pinte fondo azul claro en columnas de código de barras y precio
        header.setDefaultRenderer(new HeaderCellRenderer(acolumns));

        m_jTicketTable.getTableHeader().setReorderingAllowed(true);
        m_jTicketTable.setAutoCreateRowSorter(true);
        m_jTicketTable.setDefaultRenderer(Object.class, defaultRenderer);

        m_jTicketTable.setRowHeight(40);
        m_jTicketTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Configurar colores estilo Eleventa
        m_jTicketTable.setBackground(java.awt.Color.WHITE);
        m_jTicketTable.setSelectionBackground(new java.awt.Color(91, 192, 222)); // Azul claro de Eleventa
        m_jTicketTable.setSelectionForeground(java.awt.Color.WHITE); // Texto blanco cuando está seleccionado
        m_jTicketTable.setGridColor(new java.awt.Color(220, 220, 220)); // Grid gris claro
        
        // Configurar el fondo del viewport para que las columnas de código de barras y precio se extiendan hasta abajo
        m_jTicketTable.setFillsViewportHeight(true);
        m_jTicketTable.setOpaque(false); // Hacer la tabla transparente para que se vea el fondo del viewport
        m_jTicketTable.setShowGrid(false); // Ocultar grid para mejor visualización
        
        // Reemplazar el viewport por defecto con uno personalizado que pinte el fondo de las columnas
        ColumnBackgroundViewport customViewport = new ColumnBackgroundViewport(m_jTicketTable, acolumns, defaultRenderer);
        customViewport.setBackground(java.awt.Color.WHITE);
        customViewport.setOpaque(true);
        customViewport.setView(m_jTicketTable);
        m_jScrollTableTicket.setViewport(customViewport);

        m_jTableModel.clear();
        
        // Configurar listeners para eliminar con Delete al pasar el mouse
        setupDeleteOnHover();
    }
    
    /**
     * Interfaz para callback de eliminación de línea
     */
    public interface DeleteLineCallback {
        void onDeleteLine(int rowIndex);
    }
    
    /**
     * Establece el callback para eliminar líneas
     * @param callback El callback que se llamará cuando se presione Delete sobre una fila
     */
    public void setDeleteLineCallback(DeleteLineCallback callback) {
        this.deleteLineCallback = callback;
    }
    
    /**
     * Configura los listeners de mouse y teclado para eliminar líneas
     * cuando se pasa el mouse sobre una fila y se presiona Delete
     */
    private void setupDeleteOnHover() {
        // Hacer la tabla focusable para recibir eventos de teclado
        m_jTicketTable.setFocusable(true);
        m_jTicketTable.setRequestFocusEnabled(true);
        
        // Listener de mouse para detectar cuando el mouse está sobre una fila
        m_jTicketTable.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point point = e.getPoint();
                int row = m_jTicketTable.rowAtPoint(point);
                if (row >= 0 && row < m_jTableModel.getRowCount()) {
                    hoveredRow = row;
                    // Solicitar foco cuando el mouse está sobre una fila para poder usar Delete
                    if (!m_jTicketTable.hasFocus()) {
                        m_jTicketTable.requestFocusInWindow();
                    }
                } else {
                    hoveredRow = -1;
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                hoveredRow = -1;
            }
        });
        
        // Listener de teclado para detectar cuando se presiona Delete
        // Se agrega tanto a la tabla como al panel para capturar el evento en ambos casos
        KeyAdapter deleteKeyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    int rowToDelete = -1;
                    
                    // Si hay una fila bajo el mouse, usar esa
                    if (hoveredRow >= 0 && hoveredRow < m_jTableModel.getRowCount()) {
                        rowToDelete = hoveredRow;
                    } 
                    // Si no, usar la fila seleccionada
                    else {
                        int selectedRow = m_jTicketTable.getSelectedRow();
                        if (selectedRow >= 0 && selectedRow < m_jTableModel.getRowCount()) {
                            rowToDelete = selectedRow;
                        }
                    }
                    
                    // Si hay una fila válida para eliminar, llamar al callback
                    if (rowToDelete >= 0 && deleteLineCallback != null) {
                        deleteLineCallback.onDeleteLine(rowToDelete);
                        e.consume(); // Consumir el evento para evitar comportamiento por defecto
                    }
                }
            }
        };
        
        m_jTicketTable.addKeyListener(deleteKeyListener);
        // También agregar al panel para capturar eventos cuando la tabla no tiene foco
        this.addKeyListener(deleteKeyListener);
        this.setFocusable(true);
    }

    /**
     *
     * @param l
     */
    public void addListSelectionListener(ListSelectionListener l) {
        m_jTicketTable.getSelectionModel().addListSelectionListener(l);
    }

    /**
     *
     * @param l
     */
    public void removeListSelectionListener(ListSelectionListener l) {
        m_jTicketTable.getSelectionModel().removeListSelectionListener(l);
    }
    
    public void addTableModelListener(TableModelListener listener){
        m_jTicketTable.getModel().addTableModelListener(listener);
    }
    
    public void removeTableModelListener(TableModelListener listener){
        m_jTicketTable.getModel().removeTableModelListener(listener);
    }

    /**
     *
     */
    public void clearTicketLines() {
        m_jTableModel.clear();
    }

    /**
     *
     * @param index
     * @param oLine
     */
    public void setTicketLine(int index, TicketLineInfo oLine) {
        m_jTableModel.setRow(index, oLine);
    }

    /**
     *
     * @param oLine
     */
    public void addTicketLine(TicketLineInfo oLine) {

        m_jTableModel.addRow(oLine);

        setSelectedIndex(m_jTableModel.getRowCount() - 1);
    }

    /**
     *
     * @param index
     * @param oLine
     */
    public void insertTicketLine(int index, TicketLineInfo oLine) {

        m_jTableModel.insertRow(index, oLine);

        setSelectedIndex(index);
    }

    /**
     *
     * @param i
     */
    public void removeTicketLine(int i) {

        m_jTableModel.removeRow(i);

        if (i >= m_jTableModel.getRowCount()) {
            i = m_jTableModel.getRowCount() - 1;
        }

        if ((i >= 0) && (i < m_jTableModel.getRowCount())) {
            setSelectedIndex(i);
        }
    }

    /**
     *
     * @param i
     */
    public void setSelectedIndex(int i) {

        m_jTicketTable.getSelectionModel().setSelectionInterval(i, i);

        Rectangle oRect = m_jTicketTable.getCellRect(i, 0, true);
        m_jTicketTable.scrollRectToVisible(oRect);
    }

    /**
     *
     * @return
     */
    public int getSelectedIndex() {
        return m_jTicketTable.getSelectionModel().getMinSelectionIndex(); // solo sera uno, luego no importa...
    }

    /**
     *
     */
    public void selectionDown() {

        int i = m_jTicketTable.getSelectionModel().getMaxSelectionIndex();
        if (i < 0) {
            i = 0;
        } else {
            i++;
            if (i >= m_jTableModel.getRowCount()) {
                i = m_jTableModel.getRowCount() - 1;
            }
        }

        if ((i >= 0) && (i < m_jTableModel.getRowCount())) {
            setSelectedIndex(i);
        }
    }

    /**
     *
     */
    public void selectionUp() {

        int i = m_jTicketTable.getSelectionModel().getMinSelectionIndex();
        if (i < 0) {
            i = m_jTableModel.getRowCount() - 1; // No hay ninguna seleccionada
        } else {
            i--;
            if (i < 0) {
                i = 0;
            }
        }

        if ((i >= 0) && (i < m_jTableModel.getRowCount())) {
            setSelectedIndex(i);
        }
    }

    public void setTicketTableFont(Font f) {
        this.m_jTicketTable.getTableHeader().setFont(f);
        this.m_jTicketTable.setFont(f);
    }

    private static class TicketCellRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        private final ColumnTicket[] m_acolumns;

        public TicketCellRenderer(ColumnTicket[] acolumns) {
            m_acolumns = acolumns;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            JLabel aux = (JLabel) super.getTableCellRendererComponent(table, value,
                    isSelected, hasFocus, row, column);
            aux.setVerticalAlignment(javax.swing.SwingConstants.TOP);
            aux.setHorizontalAlignment(m_acolumns[column].align);
            // Usar fuente Arial como Eleventa
            aux.setFont(new Font("Arial", Font.PLAIN, 14));
            
            // Identificar nombre de la columna traducida
            String columnName = com.openbravo.pos.forms.AppLocal.getIntString(m_acolumns[column].name);
            String columnKey = m_acolumns[column].name;
            
            // Verificar si es columna de código de barras o precio
            boolean isBarcodeColumn = columnKey != null && (
                columnKey.contains("barcode") || 
                columnKey.contains("code") ||
                columnName.toLowerCase().contains("código de barras") ||
                columnName.toLowerCase().contains("codigo de barras")
            );
            boolean isPriceColumn = columnKey != null && (
                columnKey.contains("price") ||
                columnName.toLowerCase().contains("precio")
            );
            
            // Asegurarse de que el componente sea opaco para que se vea el fondo
            aux.setOpaque(true);
            
            // Colores estilo Eleventa
            if (isSelected) {
                aux.setBackground(new java.awt.Color(91, 192, 222)); // Azul claro de Eleventa
                aux.setForeground(java.awt.Color.WHITE); // Texto blanco
            } else {
                // Aplicar fondo azul claro a columnas de código de barras y precio (siempre, incluso en celdas vacías)
                if (isBarcodeColumn || isPriceColumn) {
                    aux.setBackground(new java.awt.Color(220, 235, 245)); // Azul claro suave
                    aux.setForeground(java.awt.Color.BLACK);
                } else {
                    aux.setBackground(java.awt.Color.WHITE);
                    aux.setForeground(java.awt.Color.BLACK);
                }
            }

            return aux;
        }
        
        /**
         * Método para obtener el color de fondo de una columna específica
         */
        public java.awt.Color getColumnBackgroundColor(int column) {
            if (column >= 0 && column < m_acolumns.length) {
                String columnName = com.openbravo.pos.forms.AppLocal.getIntString(m_acolumns[column].name);
                String columnKey = m_acolumns[column].name;
                
                boolean isBarcodeColumn = columnKey != null && (
                    columnKey.contains("barcode") || 
                    columnKey.contains("code") ||
                    columnName.toLowerCase().contains("código de barras") ||
                    columnName.toLowerCase().contains("codigo de barras")
                );
                boolean isPriceColumn = columnKey != null && (
                    columnKey.contains("price") ||
                    columnName.toLowerCase().contains("precio")
                );
                
                if (isBarcodeColumn || isPriceColumn) {
                    return new java.awt.Color(220, 235, 245);
                }
            }
            return java.awt.Color.WHITE;
        }
    }

    private static class TicketCellRendererSent extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        private final ColumnTicket[] m_acolumns;

        public TicketCellRendererSent(ColumnTicket[] acolumns) {
            m_acolumns = acolumns;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            JLabel aux = (JLabel) super.getTableCellRendererComponent(table,
                    value, isSelected, hasFocus, row, column);

            aux.setVerticalAlignment(javax.swing.SwingConstants.TOP);
            aux.setHorizontalAlignment(m_acolumns[column].align);
            Font fName = aux.getFont();
            aux.setFont(new Font(fName.getName(), Font.PLAIN, 12));
            aux.setBackground(Color.yellow);
            return aux;
        }
    }

    private static class TicketTableModel extends AbstractTableModel {

        private final ColumnTicket[] m_acolumns;
        private final ArrayList m_rows = new ArrayList();

        public TicketTableModel(ColumnTicket[] acolumns) {
            m_acolumns = acolumns;
        }

        @Override
        public int getRowCount() {
            return m_rows.size();
        }

        @Override
        public int getColumnCount() {
            return m_acolumns.length;
        }

        @Override
        public String getColumnName(int column) {
            return AppLocal.getIntString(m_acolumns[column].name);
            // return m_acolumns[column].name;
        }

        @Override
        public Object getValueAt(int row, int column) {
            return ((String[]) m_rows.get(row))[column];
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        public void clear() {
            int old = getRowCount();
            if (old > 0) {
                m_rows.clear();
                fireTableRowsDeleted(0, old - 1);
            }
        }

        public void setRow(int index, TicketLineInfo oLine) {

            String[] row = (String[]) m_rows.get(index);
            for (int i = 0; i < m_acolumns.length; i++) {
                try {
                    ScriptEngine script = ScriptFactory.getScriptEngine(ScriptFactory.VELOCITY);
                    script.put("ticketline", oLine);
                    row[i] = script.eval(m_acolumns[i].value).toString();
                } catch (ScriptException e) {
                    row[i] = null;
                }
                fireTableCellUpdated(index, i);
            }
        }

        public void addRow(TicketLineInfo oLine) {

            insertRow(m_rows.size(), oLine);
        }

        public void insertRow(int index, TicketLineInfo oLine) {

            String[] row = new String[m_acolumns.length];
            for (int i = 0; i < m_acolumns.length; i++) {
                try {
                    ScriptEngine script = ScriptFactory.getScriptEngine(ScriptFactory.VELOCITY);
                    script.put("ticketline", oLine);
                    row[i] = script.eval(m_acolumns[i].value).toString();
                } catch (ScriptException e) {
                    row[i] = null;
                }
            }

            m_rows.add(index, row);
            fireTableRowsInserted(index, index);
        }

        public void removeRow(int row) {
            m_rows.remove(row);
            fireTableRowsDeleted(row, row);
        }
    }

    private static class ColumnsHandler extends DefaultHandler {

        private ArrayList m_columns = null;

        public ColumnTicket[] getColumns() {
            return (ColumnTicket[]) m_columns.toArray(new ColumnTicket[m_columns.size()]);
        }

        @Override
        public void startDocument() throws SAXException {
            m_columns = new ArrayList();
        }

        @Override
        public void endDocument() throws SAXException {
        }

        @Override
        public void startElement(String uri, String localName, String qName,
                Attributes attributes) throws SAXException {
            if ("column".equals(qName)) {
                ColumnTicket c = new ColumnTicket();
                c.name = attributes.getValue("name");
                c.width = Integer.parseInt(attributes.getValue("width"));
                String sAlign = attributes.getValue("align");
                switch (sAlign) {
                    case "right":
                        c.align = javax.swing.SwingConstants.RIGHT;
                        break;
                    case "center":
                        c.align = javax.swing.SwingConstants.CENTER;
                        break;
                    default:
                        c.align = javax.swing.SwingConstants.LEFT;
                        break;
                }
                c.value = attributes.getValue("value");
                m_columns.add(c);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
        }
    }

    /**
     *
     * @param state
     */
    public void setSendStatus(Boolean state) {
        sendStatus = state;
    }

    private static class ColumnTicket {

        public String name;
        public int width;
        public int align;
        public String value;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        m_jScrollTableTicket = new javax.swing.JScrollPane();
        m_jTicketTable = new javax.swing.JTable();

        setLayout(new java.awt.BorderLayout());

        m_jScrollTableTicket.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        m_jScrollTableTicket.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        m_jScrollTableTicket.setFont(new java.awt.Font("Segoe UI", 0, 16)); // Fuente moderna

        m_jTicketTable.setFont(new java.awt.Font("Segoe UI", 0, 20)); // Fuente moderna y números grandes
        m_jTicketTable.setFocusable(false);
        m_jTicketTable.setIntercellSpacing(new java.awt.Dimension(0, 1));
        m_jTicketTable.setRequestFocusEnabled(false);
        m_jTicketTable.setShowVerticalLines(false);
        // Configurar fondo blanco para la tabla
        m_jTicketTable.setBackground(java.awt.Color.WHITE);
        m_jScrollTableTicket.getViewport().setBackground(java.awt.Color.WHITE);
        m_jScrollTableTicket.setBackground(java.awt.Color.WHITE);
        m_jScrollTableTicket.setViewportView(m_jTicketTable);

        add(m_jScrollTableTicket, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane m_jScrollTableTicket;
    private javax.swing.JTable m_jTicketTable;
    // End of variables declaration//GEN-END:variables

    /**
     * Renderer personalizado para el header de la tabla que pinta fondo azul claro
     * en las columnas de código de barras y precio
     */
    private static class HeaderCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        private final ColumnTicket[] columns;
        
        public HeaderCellRenderer(ColumnTicket[] columns) {
            this.columns = columns;
            setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            setFont(new Font("Arial", Font.BOLD, 14));
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (column >= 0 && column < columns.length) {
                String columnName = com.openbravo.pos.forms.AppLocal.getIntString(columns[column].name);
                String columnKey = columns[column].name;
                
                boolean isBarcodeColumn = columnKey != null && (
                    columnKey.contains("barcode") || 
                    columnKey.contains("code") ||
                    (columnName != null && columnName.toLowerCase().contains("código de barras")) ||
                    (columnName != null && columnName.toLowerCase().contains("codigo de barras"))
                );
                boolean isPriceColumn = columnKey != null && (
                    columnKey.contains("price") ||
                    (columnName != null && columnName.toLowerCase().contains("precio"))
                );
                
                // Aplicar fondo azul claro a columnas de código de barras y precio
                if (isBarcodeColumn || isPriceColumn) {
                    setBackground(new java.awt.Color(220, 235, 245)); // Azul claro suave
                    setForeground(java.awt.Color.BLACK);
                } else {
                    setBackground(java.awt.Color.WHITE);
                    setForeground(java.awt.Color.BLACK);
                }
            }
            
            setOpaque(true);
            return this;
        }
    }
    
    /**
     * Viewport personalizado que pinta el fondo de las columnas de código de barras y precio hasta abajo
     */
    private static class ColumnBackgroundViewport extends javax.swing.JViewport {
        private static final long serialVersionUID = 1L;
        private final JTable table;
        private final ColumnTicket[] columns;
        private final TicketCellRenderer renderer;
        
        public ColumnBackgroundViewport(JTable table, ColumnTicket[] columns, TicketCellRenderer renderer) {
            this.table = table;
            this.columns = columns;
            this.renderer = renderer;
        }
        
        @Override
        public void paint(java.awt.Graphics g) {
            // Pintar primero el fondo blanco completo
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());
            
            // Pintar el fondo azul claro de las columnas de código de barras y precio (siempre, desde arriba hasta abajo)
            // Esto se hace ANTES de pintar la tabla para que siempre esté visible, incluso cuando está vacía
            // El fondo se pintará uniformemente de arriba a abajo en toda la altura del viewport
            if (table != null && columns != null) {
                paintColumnBackgrounds(g);
            }
            
            // Luego pintar el contenido normal (tabla) - las celdas mantendrán su color gracias al renderizador
            super.paint(g);
        }
        
        
        /**
         * Pinta el fondo de las columnas de código de barras y precio desde arriba hasta abajo
         * Siempre pinta el fondo completo, incluso cuando la tabla está vacía
         */
        private void paintColumnBackgrounds(java.awt.Graphics g) {
            if (table == null || columns == null) return;
            
            java.awt.Point viewPosition = getViewPosition();
            int viewportHeight = getHeight();
            
            // Pintar el fondo completo de cada columna desde arriba hasta abajo (incluyendo cuando está vacía)
            int x = -viewPosition.x;
            for (int i = 0; i < columns.length && i < table.getColumnCount(); i++) {
                int columnWidth = table.getColumnModel().getColumn(i).getWidth();
                
                // Verificar si es columna de código de barras o precio
                String columnName = com.openbravo.pos.forms.AppLocal.getIntString(columns[i].name);
                String columnKey = columns[i].name;
                
                boolean isBarcodeColumn = columnKey != null && (
                    columnKey.contains("barcode") || 
                    columnKey.contains("code") ||
                    (columnName != null && columnName.toLowerCase().contains("código de barras")) ||
                    (columnName != null && columnName.toLowerCase().contains("codigo de barras"))
                );
                boolean isPriceColumn = columnKey != null && (
                    columnKey.contains("price") ||
                    (columnName != null && columnName.toLowerCase().contains("precio"))
                );
                
                // Pintar el fondo completo de la columna desde arriba hasta abajo (siempre, incluso vacía)
                if (isBarcodeColumn || isPriceColumn) {
                    g.setColor(new java.awt.Color(220, 235, 245)); // Azul claro suave
                    int paintX = Math.max(0, x);
                    int paintWidth = Math.min(columnWidth - (paintX - x), getWidth() - paintX);
                    if (paintWidth > 0) {
                        // Pintar desde el inicio del viewport (0) hasta el final (viewportHeight)
                        // Esto asegura que el fondo azul se vea en toda la altura, uniforme de arriba a abajo
                        g.fillRect(paintX, 0, paintWidth, viewportHeight);
                    }
                }
                
                x += columnWidth;
            }
        }
        
    }

}
