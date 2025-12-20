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
package com.openbravo.pos.customers;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.ComboBoxValModel;
import com.openbravo.data.loader.SentenceList;
import com.openbravo.data.user.DirtyManager;
import com.openbravo.data.user.EditorRecord;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.BeanFactoryException;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.forms.DataLogicSystem;
import com.openbravo.pos.util.StringUtils;
import com.openbravo.pos.customers.PuntosDataLogic; // Sebastian - Importar lógica de puntos
import com.openbravo.pos.scripting.ScriptFactory;
import com.openbravo.pos.scripting.ScriptEngine;
import com.openbravo.pos.scripting.ScriptException;
import com.openbravo.pos.printer.TicketParser;
import com.openbravo.pos.printer.TicketPrinterException;
import com.openbravo.pos.ticket.TicketInfo;
import com.openbravo.pos.ticket.UserInfo;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.swing.JOptionPane;
import javax.swing.table.AbstractTableModel;
import com.openbravo.beans.JCalendarDialog;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import javax.swing.table.JTableHeader;
import org.netbeans.validation.api.builtin.stringvalidation.StringValidators;
// Sebastian - Importaciones para mejoras de diseño
import java.awt.Color;
import javax.swing.BorderFactory;
import java.awt.Dimension;
import javax.swing.UIManager;

/**
 *
 * @author Jack Gerrard
 */
public final class CustomersView extends com.openbravo.pos.panels.ValidationPanel implements EditorRecord {

    private final static System.Logger LOGGER = System.getLogger(CustomersView.class.getName());
    private static final long serialVersionUID = 1L;
    private String m_oId;

    private SentenceList m_sentcat;
    private ComboBoxValModel m_CategoryModel;

    private DirtyManager m_Dirty;
    private DataLogicSales dlSales;
    private DataLogicSystem dlSystem; // Sebastian - Para templates
    private PuntosDataLogic puntosLogic; // Sebastian - Lógica de puntos
    private TicketParser ticketParser; // Sebastian - Para imprimir tickets

    //HS updates to get last added Customer 06.03.2014
    private AppView appView;
    private CustomerInfo customerInfo;

    /**
     * Creates new form CustomersView
     *
     * @param app
     * @param dirty
     */
    public CustomersView(AppView app, DirtyManager dirty) {
        try {
            appView = app;
            dlSales = (DataLogicSales) app.getBean("com.openbravo.pos.forms.DataLogicSales");
            dlSystem = (DataLogicSystem) app.getBean("com.openbravo.pos.forms.DataLogicSystem");
            puntosLogic = new PuntosDataLogic(dlSales); // Sebastian - Inicializar lógica de puntos con dlSales
            ticketParser = new TicketParser(app.getDeviceTicket(), dlSystem); // Sebastian - Inicializar parser de tickets

            initComponents();
            // Sebastian - Aplicar mejoras de diseño modernas
            aplicarMejorasVisuales();
            // Sebastian - Actualizar template XML en BD
            actualizarTemplatePuntosRedimidosEnBD();

            m_sentcat = dlSales.getTaxCustCategoriesList();
            m_CategoryModel = new ComboBoxValModel();

            m_Dirty = dirty;
            m_jTaxID.getDocument().addDocumentListener(dirty);
            m_jSearchkey.getDocument().addDocumentListener(dirty);
            m_jName.getDocument().addDocumentListener(dirty);
            m_jCategory.addActionListener(dirty);
            m_jNotes.getDocument().addDocumentListener(dirty);
            txtMaxdebt.getDocument().addDocumentListener(dirty);
            txtCurdebt.getDocument().addDocumentListener(dirty);
            txtCurdate.getDocument().addDocumentListener(dirty);
            m_jVisible.addActionListener(dirty);
            m_jVip.addActionListener(dirty);
            txtDiscount.getDocument().addDocumentListener(dirty);

            txtFirstName.getDocument().addDocumentListener(dirty);
            txtLastName.getDocument().addDocumentListener(dirty);
            txtEmail.getDocument().addDocumentListener(dirty);
            txtPhone.getDocument().addDocumentListener(dirty);
            txtPhone2.getDocument().addDocumentListener(dirty);
            txtFax.getDocument().addDocumentListener(dirty);
            m_jImage.addPropertyChangeListener(dirty);

            txtAddress.getDocument().addDocumentListener(dirty);
            txtAddress2.getDocument().addDocumentListener(dirty);
            txtPostal.getDocument().addDocumentListener(dirty);
            txtCity.getDocument().addDocumentListener(dirty);
            txtRegion.getDocument().addDocumentListener(dirty);
            txtCountry.getDocument().addDocumentListener(dirty);

            m_jdate.getDocument().addDocumentListener(dirty);

            init();
            initValidator();
        } catch (BeanFactoryException ex) {
            LOGGER.log(System.Logger.Level.WARNING, "", ex);
        }
    }

    private void initValidator() {
        org.netbeans.validation.api.ui.ValidationGroup valGroup = getValidationGroup();
        valGroup.add(m_jSearchkey, StringValidators.REQUIRE_NON_EMPTY_STRING);
        valGroup.add(m_jName, StringValidators.REQUIRE_NON_EMPTY_STRING);
        valGroup.add(txtMaxdebt, StringValidators.REQUIRE_NON_NEGATIVE_NUMBER);
    }
    
    /**
     * Sebastian - Aplicar mejoras visuales modernas y sutiles
     */
    private void aplicarMejorasVisuales() {
        // Aplicar mejoras después de que todos los componentes estén inicializados
        javax.swing.SwingUtilities.invokeLater(() -> {
            mejorarFuentesYColores();
            mejorarBotones();
            mejorarCamposDeTexto();
            mejorarTablaYPestanas();
        });
    }
    
    /**
     * Sebastian - Mejorar fuentes y colores generales
     */
    private void mejorarFuentesYColores() {
        Font fuenteModerna = new Font("Segoe UI", Font.PLAIN, 12);
        Color colorPrimario = new Color(0, 123, 255);
        
        // Mejorar pestañas
        if (jTabbedPane1 != null) {
            jTabbedPane1.setFont(new Font("Segoe UI", Font.BOLD, 12));
            jTabbedPane1.setBackground(Color.WHITE);
            jTabbedPane1.setForeground(new Color(52, 58, 64));
        }
        
        // Aplicar fuente moderna a etiquetas
        aplicarFuenteAEtiquetas(fuenteModerna);
    }
    
    /**
     * Sebastian - Aplicar fuente moderna a etiquetas principales
     */
    private void aplicarFuenteAEtiquetas(Font fuente) {
        if (jLabel1 != null) jLabel1.setFont(fuente);
        if (jLabel2 != null) jLabel2.setFont(fuente);
        if (jLabel3 != null) jLabel3.setFont(fuente);
        if (jLabel4 != null) jLabel4.setFont(fuente);
        if (jLabel5 != null) jLabel5.setFont(fuente);
        if (jLabel6 != null) jLabel6.setFont(fuente);
        if (jLabel7 != null) jLabel7.setFont(fuente);
        if (jLabel8 != null) jLabel8.setFont(fuente);
        if (jLabel9 != null) jLabel9.setFont(fuente);
        if (jLabel10 != null) jLabel10.setFont(fuente);
        
        // Etiquetas de contacto
        if (jLabel13 != null) jLabel13.setFont(fuente);
        if (jLabel14 != null) jLabel14.setFont(fuente);
        if (jLabel15 != null) jLabel15.setFont(fuente);
        if (jLabel16 != null) jLabel16.setFont(fuente);
        if (jLabel17 != null) jLabel17.setFont(fuente);
        if (jLabel18 != null) jLabel18.setFont(fuente);
        if (jLabel19 != null) jLabel19.setFont(fuente);
        
        // Etiquetas de dirección
        if (jLabel20 != null) jLabel20.setFont(fuente);
        if (jLabel21 != null) jLabel21.setFont(fuente);
        if (jLabel22 != null) jLabel22.setFont(fuente);
        if (jLabel23 != null) jLabel23.setFont(fuente);
        if (jLabel24 != null) jLabel24.setFont(fuente);
        
        // Etiquetas de puntos
        if (jLabelPuntosActuales != null) jLabelPuntosActuales.setFont(new Font("Segoe UI", Font.BOLD, 12));
        if (jLabelAjustarPuntos != null) jLabelAjustarPuntos.setFont(new Font("Segoe UI", Font.BOLD, 12));
    }
    
    /**
     * Sebastian - Mejorar botones con colores modernos
     */
    private void mejorarBotones() {
        Color colorExito = new Color(40, 167, 69);
        Color colorPeligro = new Color(220, 53, 69);
        Color colorPrimario = new Color(0, 123, 255);
        Color colorSecundario = new Color(108, 117, 125);
        Color colorAdvertencia = new Color(255, 193, 7);
        
        // Botones principales
        mejorarBoton(jBtnCreateCard, colorExito, Color.WHITE);
        mejorarBoton(jBtnClearCard, colorPeligro, Color.WHITE);
        mejorarBoton(jBtnShowTrans, colorPrimario, Color.WHITE);
        mejorarBoton(webBtnMail, colorSecundario, Color.WHITE);
        mejorarBoton(m_jbtndate, new Color(23, 162, 184), Color.WHITE);
        
        // Botones de puntos
        mejorarBoton(btnAgregarPuntos, colorExito, Color.WHITE);
        mejorarBoton(btnQuitarPuntos, colorPeligro, Color.WHITE);
        mejorarBoton(btnActualizarPuntos, colorAdvertencia, new Color(52, 58, 64));
        if (btnImprimirCopiaPuntos != null) {
            mejorarBoton(btnImprimirCopiaPuntos, new Color(0, 123, 255), Color.WHITE); // Sebastian - Botón azul para imprimir copia
        }
    }
    
    /**
     * Sebastian - Mejorar un botón individual
     */
    private void mejorarBoton(javax.swing.JButton boton, Color colorFondo, Color colorTexto) {
        if (boton != null) {
            boton.setBackground(colorFondo);
            boton.setForeground(colorTexto);
            boton.setFont(new Font("Segoe UI", Font.BOLD, 11));
            boton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(colorFondo.darker(), 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
            ));
            boton.setFocusPainted(false);
        }
    }
    
    /**
     * Sebastian - Mejorar campos de texto
     */
    private void mejorarCamposDeTexto() {
        Color bordeCampo = new Color(206, 212, 218);
        Font fuenteCampo = new Font("Segoe UI", Font.PLAIN, 11);
        
        // Aplicar mejoras a campos principales
        mejorarCampoTexto(m_jTaxID, fuenteCampo, bordeCampo);
        mejorarCampoTexto(m_jSearchkey, fuenteCampo, bordeCampo);
        mejorarCampoTexto(m_jName, fuenteCampo, bordeCampo);
        mejorarCampoTexto(jcard, fuenteCampo, bordeCampo);
        mejorarCampoTexto(txtMaxdebt, fuenteCampo, bordeCampo);
        mejorarCampoTexto(txtCurdebt, fuenteCampo, bordeCampo);
        mejorarCampoTexto(txtCurdate, fuenteCampo, bordeCampo);
        mejorarCampoTexto(txtDiscount, fuenteCampo, bordeCampo);
        mejorarCampoTexto(m_jdate, fuenteCampo, bordeCampo);
        
        // Campos de contacto
        mejorarCampoTexto(txtFirstName, fuenteCampo, bordeCampo);
        mejorarCampoTexto(txtLastName, fuenteCampo, bordeCampo);
        mejorarCampoTexto(txtEmail, fuenteCampo, bordeCampo);
        mejorarCampoTexto(txtPhone, fuenteCampo, bordeCampo);
        mejorarCampoTexto(txtPhone2, fuenteCampo, bordeCampo);
        mejorarCampoTexto(txtFax, fuenteCampo, bordeCampo);
        
        // Campos de dirección
        mejorarCampoTexto(txtAddress, fuenteCampo, bordeCampo);
        mejorarCampoTexto(txtAddress2, fuenteCampo, bordeCampo);
        mejorarCampoTexto(txtPostal, fuenteCampo, bordeCampo);
        mejorarCampoTexto(txtCity, fuenteCampo, bordeCampo);
        mejorarCampoTexto(txtRegion, fuenteCampo, bordeCampo);
        mejorarCampoTexto(txtCountry, fuenteCampo, bordeCampo);
        
        // Campos de puntos
        mejorarCampoTexto(txtPuntosActuales, new Font("Segoe UI", Font.BOLD, 12), new Color(40, 167, 69));
        mejorarCampoTexto(txtAjustarPuntos, fuenteCampo, bordeCampo);
        
        // Área de notas
        if (m_jNotes != null) {
            m_jNotes.setFont(fuenteCampo);
            m_jNotes.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bordeCampo, 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
            ));
        }
    }
    
    /**
     * Sebastian - Mejorar un campo de texto individual
     */
    private void mejorarCampoTexto(javax.swing.JTextField campo, Font fuente, Color borde) {
        if (campo != null) {
            campo.setFont(fuente);
            campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borde, 1),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
            ));
        }
    }
    
    /**
     * Sebastian - Mejorar tabla y pestañas
     */
    private void mejorarTablaYPestanas() {
        // Mejorar tabla si existe
        if (jTableCustomerTransactions != null) {
            jTableCustomerTransactions.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            jTableCustomerTransactions.setRowHeight(28);
            jTableCustomerTransactions.setGridColor(new Color(233, 236, 239));
            jTableCustomerTransactions.setSelectionBackground(new Color(0, 123, 255, 30));
        }
        
        // Mejorar paneles de pestañas con espaciado
        mejorarPanelPestana(jPanelGeneral);
        mejorarPanelPestana(jPanel1);
        mejorarPanelPestana(jPanel2);
        mejorarPanelPestana(jPanel3);
        mejorarPanelPestana(jPanel4);
        mejorarPanelPestana(jPanel5);
        mejorarPanelPestana(jPanelPuntaje);
    }
    
    /**
     * Sebastian - Mejorar un panel de pestaña individual
     */
    private void mejorarPanelPestana(javax.swing.JPanel panel) {
        if (panel != null) {
            panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        }
    }

    private void init() {
        writeValueEOF();
    }

    /**
     * Instantiate object
     *
     * @throws BasicException
     */
    public void activate() throws BasicException {

        List a = m_sentcat.list();
        a.add(0, null); // The null item
        m_CategoryModel = new ComboBoxValModel(a);
        m_jCategory.setModel(m_CategoryModel);
        String cId = null;
    }

    /**
     * Refresh object
     */
    @Override
    public void refresh() {
        jLblTranCount.setText(null);
    }

    /**
     * Write EOF
     */
    @Override
    public void writeValueEOF() {
        m_oId = null;
        m_jTaxID.setText(null);
        m_jSearchkey.setText(null);
        m_jName.setText(null);
        m_CategoryModel.setSelectedKey(null);
        m_jNotes.setText(null);

        txtMaxdebt.setText(null);
        txtCurdebt.setText(null);
        txtCurdate.setText(null);
        m_jVisible.setSelected(false);
        m_jVip.setSelected(false);
        txtDiscount.setText(null);
        jcard.setText(null);

        txtFirstName.setText(null);
        txtLastName.setText(null);
        txtEmail.setText(null);
        txtPhone.setText(null);
        txtPhone2.setText(null);
        txtFax.setText(null);
        m_jImage.setImage(null);

        txtAddress.setText(null);
        txtAddress2.setText(null);
        txtPostal.setText(null);
        txtCity.setText(null);
        txtRegion.setText(null);
        txtCountry.setText(null);

        m_jdate.setText(null);
        
        // Sebastian - Limpiar campos de puntos
        txtPuntosActuales.setText("0");
        txtAjustarPuntos.setText("0");

        m_jTaxID.setEnabled(false);
        m_jSearchkey.setEnabled(false);
        m_jName.setEnabled(false);
        m_jCategory.setEnabled(false);
        m_jNotes.setEnabled(false);
        txtMaxdebt.setEnabled(false);
        txtCurdebt.setEnabled(false);
        txtCurdate.setEnabled(false);
        m_jVisible.setEnabled(false);
        m_jVip.setEnabled(false);
        txtDiscount.setEnabled(false);
        jcard.setEnabled(false);

        txtFirstName.setEnabled(false);
        txtLastName.setEnabled(false);
        txtEmail.setEnabled(false);
        txtPhone.setEnabled(false);
        txtPhone2.setEnabled(false);
        txtFax.setEnabled(false);
        m_jImage.setEnabled(false);

        txtAddress.setEnabled(false);
        txtAddress2.setEnabled(false);
        txtPostal.setEnabled(false);
        txtCity.setEnabled(false);
        txtRegion.setEnabled(false);
        txtCountry.setEnabled(false);

        jBtnCreateCard.setEnabled(false);
        jBtnClearCard.setEnabled(false);

        m_jdate.setEnabled(false);
        m_jbtndate.setEnabled(false);

        jTableCustomerTransactions.setEnabled(false);
        
        // Sebastian - Deshabilitar campos de puntos
        txtAjustarPuntos.setEnabled(false);
        btnAgregarPuntos.setEnabled(false);
        btnQuitarPuntos.setEnabled(false);
        btnActualizarPuntos.setEnabled(false);
        if (btnImprimirCopiaPuntos != null) {
            btnImprimirCopiaPuntos.setEnabled(false);
        }

        repaint();
        refresh();
    }

    @Override
    public void writeValueInsert() {

        m_oId = null;
        m_jTaxID.setText(null);
        m_jSearchkey.setText(null);
        m_jName.setText(null);
        txtPhone.setText(null);
        txtEmail.setText(null);

        m_CategoryModel.setSelectedKey(null);
        m_jNotes.setText(null);
        txtMaxdebt.setText(null);
        txtCurdebt.setText(null);
        txtCurdate.setText(null);
        m_jVisible.setSelected(true);
        m_jVip.setSelected(false);
        txtDiscount.setText(null);
        jcard.setText(null);

        txtFirstName.setText(null);
        txtLastName.setText(null);
        txtPhone2.setText(null);
        txtFax.setText(null);
        m_jImage.setImage(null);

        txtAddress.setText(null);
        txtAddress2.setText(null);
        txtPostal.setText(null);
        txtCity.setText(null);
        txtRegion.setText(null);
        txtCountry.setText(null);

        m_jdate.setText(null);
        
        // Sebastian - Inicializar puntos para cliente nuevo
        txtPuntosActuales.setText("0");
        txtAjustarPuntos.setText("0");

        m_jTaxID.setEnabled(true);
        m_jSearchkey.setEnabled(true);
        m_jName.setEnabled(true);
        m_jCategory.setEnabled(true);
        m_jNotes.setEnabled(true);
        txtMaxdebt.setEnabled(true);
        txtCurdebt.setEnabled(true);
        txtCurdate.setEnabled(true);
        m_jVisible.setEnabled(true);
        m_jVip.setEnabled(true);
        txtDiscount.setEnabled(true);
        jcard.setEnabled(true);

        txtFirstName.setEnabled(true);
        txtLastName.setEnabled(true);
        txtEmail.setEnabled(true);
        webBtnMail.setEnabled(true);
        txtPhone.setEnabled(true);
        txtPhone2.setEnabled(true);
        txtFax.setEnabled(true);
        m_jImage.setEnabled(true);

        txtAddress.setEnabled(true);
        txtAddress2.setEnabled(true);
        txtPostal.setEnabled(true);
        txtCity.setEnabled(true);
        txtRegion.setEnabled(true);
        txtCountry.setEnabled(true);

        jBtnCreateCard.setEnabled(true);
        jBtnClearCard.setEnabled(true);

        jTableCustomerTransactions.setEnabled(false);

        m_jdate.setEnabled(true);
        m_jbtndate.setEnabled(true);
        
        // Sebastian - Habilitar campos de puntos para cliente nuevo
        txtAjustarPuntos.setEnabled(true);
        btnAgregarPuntos.setEnabled(true);
        btnQuitarPuntos.setEnabled(true);
        btnActualizarPuntos.setEnabled(true);
        if (btnImprimirCopiaPuntos != null && m_oId != null) {
            try {
                // Habilitar si hay historial de puntos redimidos
                java.util.List<PuntosDataLogic.PuntosRedimidos> historial = puntosLogic.getHistorialPuntosRedimidos(m_oId);
                btnImprimirCopiaPuntos.setEnabled(historial != null && !historial.isEmpty());
            } catch (Exception e) {
                btnImprimirCopiaPuntos.setEnabled(false);
            }
        }

        repaint();
        refresh();
    }

    /**
     * Delete from object
     *
     * @param value
     */
    @Override
    public void writeValueDelete(Object value) {

        setValues(value);

        m_jTaxID.setEnabled(false);
        m_jSearchkey.setEnabled(false);
        m_jName.setEnabled(false);
        m_jNotes.setEnabled(false);
        txtMaxdebt.setEnabled(false);
        txtCurdebt.setEnabled(false);
        txtCurdate.setEnabled(false);
        m_jVisible.setEnabled(false);
        m_jVip.setEnabled(false);
        txtDiscount.setEnabled(false);
        jcard.setEnabled(false);

        txtFirstName.setEnabled(false);
        txtLastName.setEnabled(false);
        txtEmail.setEnabled(false);
        webBtnMail.setEnabled(false);
        txtPhone.setEnabled(false);
        txtPhone2.setEnabled(false);
        txtFax.setEnabled(false);
        m_jImage.setEnabled(true);

        txtAddress.setEnabled(false);
        txtAddress2.setEnabled(false);
        txtPostal.setEnabled(false);
        txtCity.setEnabled(false);
        txtRegion.setEnabled(false);
        txtCountry.setEnabled(false);

        m_jCategory.setEnabled(false);

        jBtnCreateCard.setEnabled(false);
        jBtnClearCard.setEnabled(false);

        jTableCustomerTransactions.setEnabled(false);

        m_jdate.setEnabled(false);
        m_jbtndate.setEnabled(false);

        repaint();
        refresh();
    }
    
    private void setValues(Object value){
        Object[] customer = (Object[]) value;
        m_oId = (String)customer[0];
        m_jSearchkey.setText((String) customer[1]);
        m_jTaxID.setText((String) customer[2]);
        m_jName.setText((String) customer[3]);
        m_CategoryModel.setSelectedKey(customer[4]);
        jcard.setText((String) customer[5]);
        txtMaxdebt.setText(Formats.CURRENCY.formatValue((Double)customer[6]));
        txtAddress.setText(Formats.STRING.formatValue((String)customer[7]));
        txtAddress2.setText(Formats.STRING.formatValue((String)customer[8]));
        txtPostal.setText(Formats.STRING.formatValue((String)customer[9]));
        txtCity.setText(Formats.STRING.formatValue((String)customer[10]));
        txtRegion.setText(Formats.STRING.formatValue((String)customer[11]));
        txtCountry.setText(Formats.STRING.formatValue((String)customer[12]));
        txtFirstName.setText(Formats.STRING.formatValue((String)customer[13]));
        txtLastName.setText(Formats.STRING.formatValue((String)customer[14]));
        txtEmail.setText(Formats.STRING.formatValue((String)customer[15]));
        txtPhone.setText(Formats.STRING.formatValue((String)customer[16]));
        txtPhone2.setText(Formats.STRING.formatValue((String)customer[17]));
        txtFax.setText(Formats.STRING.formatValue((String)customer[18]));
        m_jNotes.setText((String) customer[19]);
        m_jVisible.setSelected(((Boolean) customer[20]));
        txtCurdate.setText(Formats.DATE.formatValue((Date)customer[21]));
        txtCurdebt.setText(Formats.CURRENCY.formatValue((Double)customer[22]));
        m_jImage.setImage((BufferedImage) customer[23]);
        m_jVip.setSelected(((Boolean) customer[24]));
        txtDiscount.setText(Formats.DOUBLE.formatValue((Double)customer[25]));
        m_jdate.setText(Formats.DATE.formatValue((Date)customer[26]));
        
        // Sebastian - Actualizar puntos cuando se carga un cliente
        actualizarPuntosVisuales();
    }

    /**
     * Edit object
     *
     * @param value
     */
    @Override
    public void writeValueEdit(Object value) {
        setValues(value);

        m_jTaxID.setEnabled(true);
        m_jSearchkey.setEnabled(true);
        m_jName.setEnabled(true);
        m_jNotes.setEnabled(true);
        txtMaxdebt.setEnabled(true);
        txtCurdebt.setEnabled(true);
        txtCurdate.setEnabled(true);
        m_jVisible.setEnabled(true);
        m_jVip.setEnabled(true);
        txtDiscount.setEnabled(true);
        jcard.setEnabled(true);

        txtFirstName.setEnabled(true);
        txtLastName.setEnabled(true);
        txtEmail.setEnabled(true);
        webBtnMail.setEnabled(true);
        txtPhone.setEnabled(true);
        txtPhone2.setEnabled(true);
        txtFax.setEnabled(true);
        m_jImage.setEnabled(true);

        txtAddress.setEnabled(true);
        txtAddress2.setEnabled(true);
        txtPostal.setEnabled(true);
        txtCity.setEnabled(true);
        txtRegion.setEnabled(true);
        txtCountry.setEnabled(true);

        m_jCategory.setEnabled(true);

        m_jdate.setEnabled(true);
        m_jbtndate.setEnabled(true);

        jBtnCreateCard.setEnabled(true);
        jBtnClearCard.setEnabled(true);
        
        // Sebastian - Habilitar campos de puntos para cliente existente
        txtAjustarPuntos.setEnabled(true);
        btnAgregarPuntos.setEnabled(true);
        btnQuitarPuntos.setEnabled(true);
        btnActualizarPuntos.setEnabled(true);
        if (btnImprimirCopiaPuntos != null && m_oId != null) {
            try {
                // Habilitar si hay historial de puntos redimidos
                java.util.List<PuntosDataLogic.PuntosRedimidos> historial = puntosLogic.getHistorialPuntosRedimidos(m_oId);
                btnImprimirCopiaPuntos.setEnabled(historial != null && !historial.isEmpty());
            } catch (Exception e) {
                btnImprimirCopiaPuntos.setEnabled(false);
            }
        }

        jTableCustomerTransactions.setVisible(false);
        jTableCustomerTransactions.setEnabled(true);
        resetTranxTable();

        jTableCustomerTransactions.repaint();
        repaint();
        refresh();
    }

    public void resetTranxTable() {

        jTableCustomerTransactions.getColumnModel().getColumn(0).setPreferredWidth(60);
        jTableCustomerTransactions.getColumnModel().getColumn(1).setPreferredWidth(100);
        jTableCustomerTransactions.getColumnModel().getColumn(2).setPreferredWidth(250);
        jTableCustomerTransactions.getColumnModel().getColumn(3).setPreferredWidth(50);
        jTableCustomerTransactions.getColumnModel().getColumn(4).setPreferredWidth(70);
        jTableCustomerTransactions.getColumnModel().getColumn(5).setPreferredWidth(60); // Sebastian - Columna de puntos

        // Configurar header con estilo moderno
        JTableHeader header = jTableCustomerTransactions.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(new Color(248, 249, 250));
        header.setForeground(new Color(52, 58, 64));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 35));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(233, 236, 239)));

        // Configurar tabla con mejor apariencia
        jTableCustomerTransactions.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        jTableCustomerTransactions.setRowHeight(30);
        jTableCustomerTransactions.setGridColor(new Color(233, 236, 239));
        jTableCustomerTransactions.setSelectionBackground(new Color(0, 123, 255, 25));
        jTableCustomerTransactions.setSelectionForeground(new Color(33, 37, 41));
        jTableCustomerTransactions.setShowGrid(true);
        jTableCustomerTransactions.setIntercellSpacing(new Dimension(1, 1));
        
        // Alternar colores de filas para mejor legibilidad
        jTableCustomerTransactions.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (!isSelected) {
                    if (row % 2 == 0) {
                        c.setBackground(Color.WHITE);
                    } else {
                        c.setBackground(new Color(248, 249, 250));
                    }
                }
                
                // Alinear números a la derecha
                if (column == 3 || column == 4 || column == 5) { // Cantidad, Total, Puntos
                    ((javax.swing.table.DefaultTableCellRenderer) c).setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
                    c.setFont(new Font("Segoe UI", Font.BOLD, 11));
                } else {
                    ((javax.swing.table.DefaultTableCellRenderer) c).setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
                }
                
                return c;
            }
        });

        jTableCustomerTransactions.getTableHeader().setReorderingAllowed(true);
        jTableCustomerTransactions.setAutoCreateRowSorter(true);
        jTableCustomerTransactions.repaint();
    }

    /**
     * Create object
     *
     * @return
     * @throws BasicException
     */
    @Override
    public Object createValue() throws BasicException {

        Object[] customer = new Object[27];

        customer[0] = m_oId == null ? UUID.randomUUID().toString() : m_oId;
        customer[1] = m_jSearchkey.getText();
        customer[2] = m_jTaxID.getText();
        customer[3] = m_jName.getText();
        customer[4] = m_CategoryModel.getSelectedKey();
        customer[5] = Formats.STRING.parseValue(jcard.getText()); // Format to manage NULL values
        customer[6] = Formats.CURRENCY.parseValue(txtMaxdebt.getText(), 0.0);
        customer[7] = Formats.STRING.parseValue(txtAddress.getText());
        customer[8] = Formats.STRING.parseValue(txtAddress2.getText());
        customer[9] = Formats.STRING.parseValue(txtPostal.getText());
        customer[10] = Formats.STRING.parseValue(txtCity.getText());
        customer[11] = Formats.STRING.parseValue(txtRegion.getText());
        customer[12] = Formats.STRING.parseValue(txtCountry.getText());
        customer[13] = Formats.STRING.parseValue(txtFirstName.getText());
        customer[14] = Formats.STRING.parseValue(txtLastName.getText());
        customer[15] = Formats.STRING.parseValue(txtEmail.getText());
        customer[16] = Formats.STRING.parseValue(txtPhone.getText());
        customer[17] = Formats.STRING.parseValue(txtPhone2.getText());
        customer[18] = Formats.STRING.parseValue(txtFax.getText());
        customer[19] = m_jNotes.getText();
        customer[20] = m_jVisible.isSelected();
        customer[21] = Formats.TIMESTAMP.parseValue(txtCurdate.getText()); // not saved
        customer[22] = Formats.CURRENCY.parseValue(txtCurdebt.getText()); // not saved
        customer[23] = m_jImage.getImage();
        customer[24] = m_jVip.isSelected();
        customer[25] = Formats.CURRENCY.parseValue(txtDiscount.getText(), 0.0);
        customer[26] = Formats.TIMESTAMP.parseValue(m_jdate.getText());

        return customer;
    }

    @Override
    public Component getComponent() {
        return this;
    }

// JG 3 Oct 2013 - Customer Transaction List
// JG 10 April 2016 - Revision    
    private List<CustomerTransaction> getTransactionOfName(String cId) {

        List<CustomerTransaction> customerTransactionList = new ArrayList<>();
        try {
            customerTransactionList = dlSales.getCustomersTransactionList(cId);
            for (CustomerTransaction customerTransaction : customerTransactionList) {
                String customerId = customerTransaction.getCustomerId();
                if (!customerId.equals(cId)) {
                    customerTransactionList.remove(customerTransaction);
                }
            }
        } catch (BasicException ex) {
            LOGGER.log(System.Logger.Level.WARNING, "", ex);
        }

        txtCurdate.repaint();
        txtCurdebt.repaint();

        repaint();
        refresh();

        return customerTransactionList;
    }

    class TransactionTableModel extends AbstractTableModel {

        String tkt = AppLocal.getIntString("label.tblHeaderCol1");
        String dte = AppLocal.getIntString("label.tblHeaderCol2");
        String prd = AppLocal.getIntString("label.tblHeaderCol3");
        String qty = AppLocal.getIntString("label.tblHeaderCol4");
        String ttl = AppLocal.getIntString("label.tblHeaderCol5");
        String pts = "Puntos"; // Sebastian - Puntos otorgados

        List<CustomerTransaction> transactionList;
        String[] columnNames = {tkt, dte, prd, qty, ttl, pts};
        public Double Tamount;

        public TransactionTableModel(List<CustomerTransaction> list) {
            transactionList = list;
        }

        @Override
        public int getColumnCount() {
            return 6; // Sebastian - Agregada columna de puntos
        }

        @Override
        public int getRowCount() {
            return transactionList.size();
        }

        // this method is called to set the value of each cell
        @Override
        public Object getValueAt(int row, int column) {
            CustomerTransaction customerTransaction = transactionList.get(row);

            jTableCustomerTransactions.setRowHeight(25);

            switch (column) {

                case 0:
                    return customerTransaction.getTicketId();
                case 1:
                    Date transactionDate = customerTransaction.getTransactionDate();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                    String formattedDate = sdf.format(transactionDate);
                    return formattedDate;
                case 2:
                    return customerTransaction.getProductName();
                case 3:
                    return customerTransaction.getUnit();
                case 4:
                    Double amount = customerTransaction.getTotal();
                    DecimalFormat df = new DecimalFormat("#.##");
                    String formattedAmount = df.format(amount);
                    return formattedAmount;
                case 5:
                    // Sebastian - Puntos otorgados por esta transacción
                    Integer puntos = customerTransaction.getPuntos();
                    return puntos != null ? puntos.toString() : "0";
                default:
                    return "";

            }
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanelGeneral = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        m_jSearchkey = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        m_jName = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jcard = new javax.swing.JTextField();
        txtDiscount = new javax.swing.JTextField();
        jBtnCreateCard = new javax.swing.JButton();
        jLblDiscountpercent = new javax.swing.JLabel();
        jBtnClearCard = new javax.swing.JButton();
        m_jbtndate = new javax.swing.JButton();
        jLabel9 = new javax.swing.JLabel();
        m_jdate = new javax.swing.JTextField();
        m_jCategory = new javax.swing.JComboBox();
        jLabel10 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        m_jVisible = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        txtMaxdebt = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        txtCurdebt = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        txtCurdate = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        m_jTaxID = new javax.swing.JTextField();
        jLblVIP = new javax.swing.JLabel();
        m_jVip = new javax.swing.JCheckBox();
        jLblDiscount = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabel19 = new javax.swing.JLabel();
        txtFirstName = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        txtLastName = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        txtEmail = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        txtPhone = new javax.swing.JTextField();
        jLabel18 = new javax.swing.JLabel();
        txtPhone2 = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        txtFax = new javax.swing.JTextField();
        webBtnMail = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        txtAddress = new javax.swing.JTextField();
        jLabel20 = new javax.swing.JLabel();
        txtCountry = new javax.swing.JTextField();
        jLabel21 = new javax.swing.JLabel();
        txtAddress2 = new javax.swing.JTextField();
        jLabel22 = new javax.swing.JLabel();
        txtPostal = new javax.swing.JTextField();
        jLabel23 = new javax.swing.JLabel();
        txtCity = new javax.swing.JTextField();
        jLabel24 = new javax.swing.JLabel();
        txtRegion = new javax.swing.JTextField();
        jPanel4 = new javax.swing.JPanel();
        jBtnShowTrans = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTableCustomerTransactions = new javax.swing.JTable();
        jLblTranCount = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        m_jImage = new com.openbravo.data.gui.JImageEditor();
        jLabel34 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        m_jNotes = new javax.swing.JTextArea();
        
        // Sebastian - Inicializar componentes de puntaje
        jPanelPuntaje = new javax.swing.JPanel();
        jLabelPuntosActuales = new javax.swing.JLabel();
        txtPuntosActuales = new javax.swing.JTextField();
        jLabelAjustarPuntos = new javax.swing.JLabel();
        txtAjustarPuntos = new javax.swing.JTextField();
        btnAgregarPuntos = new javax.swing.JButton();
        btnQuitarPuntos = new javax.swing.JButton();
        btnActualizarPuntos = new javax.swing.JButton();
        btnImprimirCopiaPuntos = new javax.swing.JButton();

        setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        setPreferredSize(new java.awt.Dimension(700, 400));

        jTabbedPane1.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jTabbedPane1.setPreferredSize(new java.awt.Dimension(650, 300));

        jLabel8.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel8.setText(AppLocal.getIntString("label.searchkeym")); // NOI18N
        jLabel8.setPreferredSize(new java.awt.Dimension(100, 30));

        m_jSearchkey.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jSearchkey.setPreferredSize(new java.awt.Dimension(140, 30));

        jLabel3.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel3.setText(AppLocal.getIntString("label.namem")); // NOI18N
        jLabel3.setMaximumSize(new java.awt.Dimension(140, 25));
        jLabel3.setMinimumSize(new java.awt.Dimension(140, 25));
        jLabel3.setPreferredSize(new java.awt.Dimension(150, 30));

        m_jName.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jName.setPreferredSize(new java.awt.Dimension(410, 30));

        jLabel5.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel5.setText(AppLocal.getIntString("label.card")); // NOI18N
        jLabel5.setMaximumSize(new java.awt.Dimension(140, 25));
        jLabel5.setMinimumSize(new java.awt.Dimension(140, 25));
        jLabel5.setPreferredSize(new java.awt.Dimension(150, 30));

        jcard.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jcard.setPreferredSize(new java.awt.Dimension(150, 30));

        txtDiscount.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        txtDiscount.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        txtDiscount.setPreferredSize(new java.awt.Dimension(50, 30));

        jBtnCreateCard.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/encrypted.png"))); // NOI18N
        jBtnCreateCard.setToolTipText("Create Key");
        jBtnCreateCard.setMaximumSize(new java.awt.Dimension(64, 32));
        jBtnCreateCard.setMinimumSize(new java.awt.Dimension(64, 32));
        jBtnCreateCard.setPreferredSize(new java.awt.Dimension(40, 35));
        jBtnCreateCard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnCreateCardActionPerformed(evt);
            }
        });

        jLblDiscountpercent.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLblDiscountpercent.setText("%");
        jLblDiscountpercent.setPreferredSize(new java.awt.Dimension(15, 30));

        jBtnClearCard.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/fileclose.png"))); // NOI18N
        jBtnClearCard.setToolTipText("Clear Key");
        jBtnClearCard.setMaximumSize(new java.awt.Dimension(64, 32));
        jBtnClearCard.setMinimumSize(new java.awt.Dimension(64, 32));
        jBtnClearCard.setPreferredSize(new java.awt.Dimension(40, 35));
        jBtnClearCard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnClearCardActionPerformed(evt);
            }
        });

        m_jbtndate.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/date.png"))); // NOI18N
        m_jbtndate.setToolTipText("Open Calendar");
        m_jbtndate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jbtndateActionPerformed(evt);
            }
        });

        jLabel9.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel9.setText(AppLocal.getIntString("label.custtaxcategory")); // NOI18N
        jLabel9.setToolTipText(AppLocal.getIntString("label.custtaxcategory")); // NOI18N
        jLabel9.setMaximumSize(new java.awt.Dimension(140, 25));
        jLabel9.setMinimumSize(new java.awt.Dimension(140, 25));
        jLabel9.setPreferredSize(new java.awt.Dimension(150, 30));

        m_jdate.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jdate.setPreferredSize(new java.awt.Dimension(130, 30));

        m_jCategory.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jCategory.setPreferredSize(new java.awt.Dimension(180, 30));

        jLabel10.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("pos_messages"); // NOI18N
        jLabel10.setText(bundle.getString("label.customerdate")); // NOI18N
        jLabel10.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        jLabel10.setPreferredSize(new java.awt.Dimension(130, 30));

        jLabel4.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel4.setText(AppLocal.getIntString("label.visible")); // NOI18N
        jLabel4.setMaximumSize(new java.awt.Dimension(140, 25));
        jLabel4.setMinimumSize(new java.awt.Dimension(140, 25));
        jLabel4.setPreferredSize(new java.awt.Dimension(150, 30));

        m_jVisible.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jVisible.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        m_jVisible.setPreferredSize(new java.awt.Dimension(30, 30));

        jLabel1.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText(AppLocal.getIntString("label.maxdebt")); // NOI18N
        jLabel1.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        jLabel1.setMaximumSize(new java.awt.Dimension(140, 25));
        jLabel1.setMinimumSize(new java.awt.Dimension(120, 25));
        jLabel1.setPreferredSize(new java.awt.Dimension(130, 30));

        txtMaxdebt.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        txtMaxdebt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtMaxdebt.setPreferredSize(new java.awt.Dimension(130, 30));

        jLabel2.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText(AppLocal.getIntString("label.curdebt")); // NOI18N
        jLabel2.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        jLabel2.setMaximumSize(new java.awt.Dimension(140, 25));
        jLabel2.setMinimumSize(new java.awt.Dimension(120, 25));
        jLabel2.setPreferredSize(new java.awt.Dimension(130, 30));

        txtCurdebt.setEditable(false);
        txtCurdebt.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        txtCurdebt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtCurdebt.setPreferredSize(new java.awt.Dimension(130, 30));

        jLabel6.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel6.setText(AppLocal.getIntString("label.curdate")); // NOI18N
        jLabel6.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        jLabel6.setPreferredSize(new java.awt.Dimension(130, 30));

        txtCurdate.setEditable(false);
        txtCurdate.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        txtCurdate.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtCurdate.setPreferredSize(new java.awt.Dimension(130, 30));

        jLabel7.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel7.setText(AppLocal.getIntString("label.taxid")); // NOI18N
        jLabel7.setMaximumSize(new java.awt.Dimension(150, 30));
        jLabel7.setMinimumSize(new java.awt.Dimension(140, 25));
        jLabel7.setPreferredSize(new java.awt.Dimension(150, 30));
        jLabel7.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel7MouseClicked(evt);
            }
        });

        m_jTaxID.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jTaxID.setPreferredSize(new java.awt.Dimension(150, 30));

        jLblVIP.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLblVIP.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLblVIP.setText(AppLocal.getIntString("label.vip")); // NOI18N
        jLblVIP.setPreferredSize(new java.awt.Dimension(50, 30));

        m_jVip.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jVip.setForeground(new java.awt.Color(0, 188, 243));
        m_jVip.setPreferredSize(new java.awt.Dimension(21, 30));
        m_jVip.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                m_jVipnone(evt);
            }
        });
        m_jVip.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jVipActionPerformed(evt);
            }
        });

        jLblDiscount.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLblDiscount.setText(AppLocal.getIntString("label.discount")); // NOI18N
        jLblDiscount.setPreferredSize(new java.awt.Dimension(70, 30));

        javax.swing.GroupLayout jPanelGeneralLayout = new javax.swing.GroupLayout(jPanelGeneral);
        jPanelGeneral.setLayout(jPanelGeneralLayout);
        jPanelGeneralLayout.setHorizontalGroup(
            jPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelGeneralLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(txtMaxdebt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLblDiscount, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelGeneralLayout.createSequentialGroup()
                        .addGroup(jPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanelGeneralLayout.createSequentialGroup()
                                .addGroup(jPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanelGeneralLayout.createSequentialGroup()
                                        .addComponent(m_jdate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(m_jbtndate, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(txtCurdate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(txtCurdebt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(jPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanelGeneralLayout.createSequentialGroup()
                                    .addGroup(jPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanelGeneralLayout.createSequentialGroup()
                                            .addComponent(txtDiscount, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(jLblDiscountpercent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addGroup(jPanelGeneralLayout.createSequentialGroup()
                                            .addComponent(jcard, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addGap(30, 30, 30)))
                                    .addComponent(jBtnCreateCard, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGap(18, 18, 18)
                                    .addComponent(jBtnClearCard, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addComponent(m_jName, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 409, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(m_jCategory, javax.swing.GroupLayout.PREFERRED_SIZE, 281, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanelGeneralLayout.createSequentialGroup()
                        .addGroup(jPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(m_jTaxID, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(m_jSearchkey, javax.swing.GroupLayout.PREFERRED_SIZE, 210, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanelGeneralLayout.createSequentialGroup()
                                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(m_jVisible, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanelGeneralLayout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(jLblVIP, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(m_jVip, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(83, 83, 83))))
        );
        jPanelGeneralLayout.setVerticalGroup(
            jPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelGeneralLayout.createSequentialGroup()
                .addContainerGap(13, Short.MAX_VALUE)
                .addGroup(jPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(m_jTaxID, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(m_jVisible, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(m_jSearchkey, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(m_jVip, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLblVIP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(m_jName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jBtnCreateCard, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jcard, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBtnClearCard, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLblDiscountpercent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLblDiscount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(txtDiscount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanelGeneralLayout.createSequentialGroup()
                        .addGroup(jPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(m_jCategory, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanelGeneralLayout.createSequentialGroup()
                                .addGroup(jPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(txtCurdate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtMaxdebt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(jPanelGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelGeneralLayout.createSequentialGroup()
                                    .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(m_jdate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addComponent(m_jbtndate, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(jPanelGeneralLayout.createSequentialGroup()
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtCurdebt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        jTabbedPane1.addTab(AppLocal.getIntString("label.general"), jPanelGeneral); // NOI18N

        jPanel1.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N

        jLabel19.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel19.setText(AppLocal.getIntString("label.firstname")); // NOI18N
        jLabel19.setAlignmentX(0.5F);
        jLabel19.setPreferredSize(new java.awt.Dimension(150, 30));

        txtFirstName.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        txtFirstName.setPreferredSize(new java.awt.Dimension(300, 30));

        jLabel15.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel15.setText(AppLocal.getIntString("label.lastname")); // NOI18N
        jLabel15.setPreferredSize(new java.awt.Dimension(150, 30));

        txtLastName.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        txtLastName.setPreferredSize(new java.awt.Dimension(300, 30));

        jLabel16.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel16.setText(AppLocal.getIntString("label.email")); // NOI18N
        jLabel16.setPreferredSize(new java.awt.Dimension(150, 30));

        txtEmail.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        txtEmail.setPreferredSize(new java.awt.Dimension(300, 30));

        jLabel17.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel17.setText(AppLocal.getIntString("label.phone")); // NOI18N
        jLabel17.setPreferredSize(new java.awt.Dimension(150, 30));

        txtPhone.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        txtPhone.setPreferredSize(new java.awt.Dimension(300, 30));

        jLabel18.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel18.setText(AppLocal.getIntString("label.phone2")); // NOI18N
        jLabel18.setPreferredSize(new java.awt.Dimension(150, 30));

        txtPhone2.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        txtPhone2.setPreferredSize(new java.awt.Dimension(300, 30));

        jLabel14.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel14.setText(AppLocal.getIntString("label.fax")); // NOI18N
        jLabel14.setPreferredSize(new java.awt.Dimension(150, 30));

        txtFax.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        txtFax.setPreferredSize(new java.awt.Dimension(300, 30));

        webBtnMail.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        webBtnMail.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/mail24.png"))); // NOI18N
        webBtnMail.setText(bundle.getString("button.email")); // NOI18N
        webBtnMail.setPreferredSize(new java.awt.Dimension(90, 30));
        webBtnMail.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                webBtnMailActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txtLastName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel14, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txtFax, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel16, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtPhone, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtEmail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtPhone2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel19, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txtFirstName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(webBtnMail, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(90, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel19, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtFirstName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtLastName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel16, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtEmail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(webBtnMail, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtPhone, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtPhone2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtFax, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel14, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(57, 57, 57))
        );

        jTabbedPane1.addTab(AppLocal.getIntString("label.contact"), jPanel1); // NOI18N

        jPanel2.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N

        jLabel13.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel13.setText(AppLocal.getIntString("label.address")); // NOI18N
        jLabel13.setPreferredSize(new java.awt.Dimension(150, 30));

        txtAddress.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        txtAddress.setPreferredSize(new java.awt.Dimension(300, 30));

        jLabel20.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel20.setText(AppLocal.getIntString("label.country")); // NOI18N
        jLabel20.setPreferredSize(new java.awt.Dimension(150, 30));

        txtCountry.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        txtCountry.setPreferredSize(new java.awt.Dimension(300, 30));

        jLabel21.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel21.setText(AppLocal.getIntString("label.address2")); // NOI18N
        jLabel21.setPreferredSize(new java.awt.Dimension(150, 30));

        txtAddress2.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        txtAddress2.setPreferredSize(new java.awt.Dimension(300, 30));

        jLabel22.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel22.setText(AppLocal.getIntString("label.postal")); // NOI18N
        jLabel22.setPreferredSize(new java.awt.Dimension(110, 30));

        txtPostal.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        txtPostal.setPreferredSize(new java.awt.Dimension(0, 30));

        jLabel23.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel23.setText(AppLocal.getIntString("label.city")); // NOI18N
        jLabel23.setPreferredSize(new java.awt.Dimension(150, 30));

        txtCity.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        txtCity.setPreferredSize(new java.awt.Dimension(300, 30));

        jLabel24.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel24.setText(AppLocal.getIntString("label.region")); // NOI18N
        jLabel24.setPreferredSize(new java.awt.Dimension(150, 30));

        txtRegion.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        txtRegion.setPreferredSize(new java.awt.Dimension(300, 30));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel20, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel24, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel23, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel21, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel13, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel22, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtPostal, javax.swing.GroupLayout.PREFERRED_SIZE, 89, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(txtCity, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(txtAddress, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(txtAddress2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(txtRegion, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(txtCountry, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGap(113, 113, 113))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtAddress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel21, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtAddress2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel23, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtCity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel24, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtRegion, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel22, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtPostal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel20, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtCountry, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jTabbedPane1.addTab(AppLocal.getIntString("label.locationaddress"), jPanel2); // NOI18N

        jPanel4.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jPanel4.setPreferredSize(new java.awt.Dimension(535, 0));

        jBtnShowTrans.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jBtnShowTrans.setText(bundle.getString("button.CustomerTrans")); // NOI18N
        jBtnShowTrans.setToolTipText("");
        jBtnShowTrans.setPreferredSize(new java.awt.Dimension(140, 30));
        jBtnShowTrans.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnShowTransActionPerformed(evt);
            }
        });

        jScrollPane3.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jScrollPane3.setPreferredSize(new java.awt.Dimension(620, 500));

        jTableCustomerTransactions.setAutoCreateRowSorter(true);
        jTableCustomerTransactions.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jTableCustomerTransactions.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "TicketID", "Date", "Product", "Qty", "Total", "Puntos"
            }
        ));
        jTableCustomerTransactions.setGridColor(new java.awt.Color(102, 204, 255));
        jTableCustomerTransactions.setOpaque(false);
        jTableCustomerTransactions.setPreferredSize(new java.awt.Dimension(375, 500));
        jTableCustomerTransactions.setRowHeight(25);
        jScrollPane3.setViewportView(jTableCustomerTransactions);

        jLblTranCount.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLblTranCount.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLblTranCount.setOpaque(true);
        jLblTranCount.setPreferredSize(new java.awt.Dimension(50, 30));

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jBtnShowTrans, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLblTranCount, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(424, 424, 424))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLblTranCount, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jBtnShowTrans, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab(bundle.getString("label.Transactions"), jPanel4); // NOI18N

        m_jImage.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jImage.setPreferredSize(new java.awt.Dimension(300, 250));

        jLabel34.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel34.setText(bundle.getString("label.imagesize")); // NOI18N
        jLabel34.setPreferredSize(new java.awt.Dimension(500, 30));

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel34, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(m_jImage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(m_jImage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel34, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jTabbedPane1.addTab(bundle.getString("label.photo"), jPanel5); // NOI18N

        jPanel3.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N

        m_jNotes.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jNotes.setPreferredSize(new java.awt.Dimension(0, 0));
        jScrollPane1.setViewportView(m_jNotes);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 458, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 286, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.addTab(AppLocal.getIntString("label.notes"), jPanel3); // NOI18N

        // Sebastian - Configurar pestaña de puntaje
        jPanelPuntaje.setFont(new java.awt.Font("Arial", 0, 12));
        
        jLabelPuntosActuales.setFont(new java.awt.Font("Arial", 0, 14));
        jLabelPuntosActuales.setText("Puntos Actuales:");
        jLabelPuntosActuales.setPreferredSize(new java.awt.Dimension(120, 30));
        
        txtPuntosActuales.setFont(new java.awt.Font("Arial", 0, 14));
        txtPuntosActuales.setEditable(false);
        txtPuntosActuales.setBackground(new java.awt.Color(240, 240, 240));
        txtPuntosActuales.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtPuntosActuales.setPreferredSize(new java.awt.Dimension(100, 30));
        
        jLabelAjustarPuntos.setFont(new java.awt.Font("Arial", 0, 14));
        jLabelAjustarPuntos.setText("Ajustar Puntos:");
        jLabelAjustarPuntos.setPreferredSize(new java.awt.Dimension(120, 30));
        
        txtAjustarPuntos.setFont(new java.awt.Font("Arial", 0, 14));
        txtAjustarPuntos.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtAjustarPuntos.setPreferredSize(new java.awt.Dimension(100, 30));
        txtAjustarPuntos.setText("0");
        
        btnAgregarPuntos.setFont(new java.awt.Font("Arial", 0, 12));
        btnAgregarPuntos.setText("Agregar");
        btnAgregarPuntos.setPreferredSize(new java.awt.Dimension(80, 30));
        btnAgregarPuntos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAgregarPuntosActionPerformed(evt);
            }
        });
        
        btnQuitarPuntos.setFont(new java.awt.Font("Arial", 0, 12));
        btnQuitarPuntos.setText("Quitar");
        btnQuitarPuntos.setPreferredSize(new java.awt.Dimension(80, 30));
        btnQuitarPuntos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnQuitarPuntosActionPerformed(evt);
            }
        });
        
        btnActualizarPuntos.setFont(new java.awt.Font("Arial", 0, 12));
        btnActualizarPuntos.setText("Actualizar");
        btnActualizarPuntos.setPreferredSize(new java.awt.Dimension(90, 30));
        btnActualizarPuntos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnActualizarPuntosActionPerformed(evt);
            }
        });
        
        btnImprimirCopiaPuntos.setFont(new java.awt.Font("Arial", 0, 12));
        btnImprimirCopiaPuntos.setText("Imprimir Copia");
        btnImprimirCopiaPuntos.setPreferredSize(new java.awt.Dimension(120, 30));
        btnImprimirCopiaPuntos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnImprimirCopiaPuntosActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelPuntajeLayout = new javax.swing.GroupLayout(jPanelPuntaje);
        jPanelPuntaje.setLayout(jPanelPuntajeLayout);
        jPanelPuntajeLayout.setHorizontalGroup(
            jPanelPuntajeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelPuntajeLayout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addGroup(jPanelPuntajeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelPuntajeLayout.createSequentialGroup()
                        .addComponent(jLabelPuntosActuales, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtPuntosActuales, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnActualizarPuntos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelPuntajeLayout.createSequentialGroup()
                        .addComponent(jLabelAjustarPuntos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtAjustarPuntos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(btnAgregarPuntos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnQuitarPuntos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelPuntajeLayout.createSequentialGroup()
                        .addGap(30, 30, 30)
                        .addComponent(btnImprimirCopiaPuntos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelPuntajeLayout.setVerticalGroup(
            jPanelPuntajeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelPuntajeLayout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addGroup(jPanelPuntajeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelPuntosActuales, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtPuntosActuales, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnActualizarPuntos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(20, 20, 20)
                .addGroup(jPanelPuntajeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelAjustarPuntos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtAjustarPuntos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnAgregarPuntos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnQuitarPuntos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(20, 20, 20)
                .addComponent(btnImprimirCopiaPuntos, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Puntaje", jPanelPuntaje); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 659, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 339, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jBtnCreateCardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnCreateCardActionPerformed
        if (JOptionPane.showConfirmDialog(this,
                AppLocal.getIntString("message.cardnew"),
                AppLocal.getIntString("title.editor"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
            jcard.setText("c" + StringUtils.getCardNumber());
            m_Dirty.setDirty(true);
        }
    }//GEN-LAST:event_jBtnCreateCardActionPerformed

    private void jBtnClearCardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnClearCardActionPerformed
        if (JOptionPane.showConfirmDialog(this,
                AppLocal.getIntString("message.cardremove"),
                AppLocal.getIntString("title.editor"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
            jcard.setText(null);
            m_Dirty.setDirty(true);
        }
    }//GEN-LAST:event_jBtnClearCardActionPerformed

    private void webBtnMailActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_webBtnMailActionPerformed

        if (!"".equals(txtEmail.getText())) {
            Desktop desktop;

            if (Desktop.isDesktopSupported()
                    && (desktop = Desktop.getDesktop()).isSupported(Desktop.Action.MAIL)) {
                URI mailto = null;
                try {
                    mailto = new URI("mailto:"
                            + txtEmail.getText());
                } catch (URISyntaxException ex) {
                    LOGGER.log(System.Logger.Level.WARNING, "", ex);
                }
                try {
                    desktop.mail(mailto);
                } catch (IOException ex) {
                    LOGGER.log(System.Logger.Level.WARNING, "", ex);
                }
            } else {
                JOptionPane.showMessageDialog(this,
                        AppLocal.getIntString("message.email"),
                        "Email", JOptionPane.INFORMATION_MESSAGE);
            }
        }

    }//GEN-LAST:event_webBtnMailActionPerformed

    private void jBtnShowTransActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnShowTransActionPerformed

        if (m_oId != null) {
            String cId = m_oId.toString();
            TransactionTableModel transactionModel = new TransactionTableModel(getTransactionOfName(cId));
            jTableCustomerTransactions.setModel(transactionModel);
            if (transactionModel.getRowCount() > 0) {
                jTableCustomerTransactions.setVisible(true);
                String TranCount = String.valueOf(transactionModel.getRowCount());
                jLblTranCount.setText(TranCount + " for " + m_jName.getText());
            } else {
                jTableCustomerTransactions.setVisible(false);
                JOptionPane.showMessageDialog(null,
                        AppLocal.getIntString("message.nocustomertranx"),
                        AppLocal.getIntString("label.Transactions"),
                        JOptionPane.INFORMATION_MESSAGE);
            }
            resetTranxTable();
        } else {
            LOGGER.log(System.Logger.Level.DEBUG, "Customer ID is null");
            Toolkit.getDefaultToolkit().beep();
        }
    }//GEN-LAST:event_jBtnShowTransActionPerformed

    private void m_jVipnone(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_m_jVipnone

    }//GEN-LAST:event_m_jVipnone

    private void m_jVipActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jVipActionPerformed

//        AbstractButton abstractButton = (AbstractButton)evt.getSource();
//        boolean selected = abstractButton.getModel().isSelected();
//        jLblDiscountpercent.setVisible(selected);
//        txtDiscount.setVisible(selected);
//        jLblDiscount.setVisible(selected);

    }//GEN-LAST:event_m_jVipActionPerformed

    private void m_jbtndateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jbtndateActionPerformed

        Date date;
        try {
            date = (Date) Formats.TIMESTAMP.parseValue(m_jdate.getText());
        } catch (BasicException e) {
            date = null;
        }
        date = JCalendarDialog.showCalendarTime(this, date);
        if (date != null) {
            m_jdate.setText(Formats.TIMESTAMP.formatValue(date));
        }
    }//GEN-LAST:event_m_jbtndateActionPerformed

    private void jLabel7MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel7MouseClicked

        if (evt.getClickCount() == 2) {
            String uuidString = m_oId.toString();
            StringSelection stringSelection = new StringSelection(uuidString);
            Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
            clpbrd.setContents(stringSelection, null);

            JOptionPane.showMessageDialog(null,
                    AppLocal.getIntString("message.uuidcopy"));
        }
    }//GEN-LAST:event_jLabel7MouseClicked

    // Sebastian - Métodos para manejar eventos de puntos
    private void btnActualizarPuntosActionPerformed(java.awt.event.ActionEvent evt) {
        actualizarPuntosVisuales();
    }

    private void btnAgregarPuntosActionPerformed(java.awt.event.ActionEvent evt) {
        if (m_oId != null && !txtAjustarPuntos.getText().trim().isEmpty()) {
            try {
                int puntosAAgregar = Integer.parseInt(txtAjustarPuntos.getText().trim());
                if (puntosAAgregar > 0) {
                    puntosLogic.agregarPuntos(m_oId, puntosAAgregar);
                    actualizarPuntosVisuales();
                    txtAjustarPuntos.setText("0");
                    JOptionPane.showMessageDialog(this, 
                        "Se agregaron " + puntosAAgregar + " puntos exitosamente.", 
                        "Puntos Agregados", 
                        JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "Ingrese una cantidad positiva de puntos.", 
                        "Error", 
                        JOptionPane.WARNING_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, 
                    "Ingrese un número válido.", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            } catch (BasicException ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error al agregar puntos: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void btnQuitarPuntosActionPerformed(java.awt.event.ActionEvent evt) {
        if (m_oId != null && !txtAjustarPuntos.getText().trim().isEmpty()) {
            try {
                int puntosAQuitar = Integer.parseInt(txtAjustarPuntos.getText().trim());
                if (puntosAQuitar > 0) {
                    int puntosActuales = puntosLogic.obtenerPuntos(m_oId);
                    if (puntosActuales >= puntosAQuitar) {
                        // Sebastian - Confirmar antes de redimir puntos
                        String nombreCliente = m_jName.getText().trim();
                        int confirmacion = JOptionPane.showConfirmDialog(this, 
                            "¿Confirmar redención de " + puntosAQuitar + " puntos?\n\n" +
                            "Cliente: " + nombreCliente + "\n" +
                            "Puntos actuales: " + puntosActuales + "\n" +
                            "Puntos después: " + (puntosActuales - puntosAQuitar) + "\n\n" +
                            "Se imprimirá un ticket con la información de redención.",
                            "Confirmar Redención de Puntos",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE);
                        
                        if (confirmacion != JOptionPane.YES_OPTION) {
                            return; // El usuario canceló
                        }
                        
                        // Redimir los puntos
                        puntosLogic.actualizarPuntos(m_oId, puntosActuales - puntosAQuitar);
                        actualizarPuntosVisuales();
                        
                        // Sebastian - Registrar en historial de puntos redimidos
                        puntosLogic.registrarPuntosRedimidos(m_oId, nombreCliente, puntosAQuitar);
                        
                        // Habilitar botón de reimpresión
                        if (btnImprimirCopiaPuntos != null) {
                            btnImprimirCopiaPuntos.setEnabled(true);
                        }
                        
                        // Imprimir ticket de puntos redimidos
                        imprimirTicketPuntosRedimidos(nombreCliente, puntosAQuitar, false);
                        
                        txtAjustarPuntos.setText("0");
                        JOptionPane.showMessageDialog(this, 
                            "Se redimieron " + puntosAQuitar + " puntos exitosamente.", 
                            "Puntos Redimidos", 
                            JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, 
                            "No se pueden redimir más puntos de los disponibles (" + puntosActuales + ").", 
                            "Error", 
                            JOptionPane.WARNING_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "Ingrese una cantidad positiva de puntos.", 
                        "Error", 
                        JOptionPane.WARNING_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, 
                    "Ingrese un número válido.", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            } catch (BasicException ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error al redimir puntos: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                LOGGER.log(System.Logger.Level.WARNING, "Error al imprimir ticket de puntos redimidos: ", ex);
                JOptionPane.showMessageDialog(this, 
                    "Puntos redimidos, pero hubo un error al imprimir el ticket: " + ex.getMessage(), 
                    "Advertencia", 
                    JOptionPane.WARNING_MESSAGE);
            }
        }
    }
    
    /**
     * Sebastian - Imprime un ticket de puntos redimidos
     * @param nombreCliente Nombre del cliente que redimió los puntos
     * @param puntosRedimidos Cantidad de puntos redimidos
     * @param esCopia true si es una copia del ticket, false si es el original
     */
    private void imprimirTicketPuntosRedimidos(String nombreCliente, int puntosRedimidos, boolean esCopia) {
        try {
            // Sebastian - Forzar actualización del template antes de usarlo
            actualizarTemplatePuntosRedimidosEnBD();
            
            // Obtener el template actualizado desde la base de datos
            String template = dlSystem.getResourceAsXML("Printer.PuntosRedimidos");
            
            if (template == null || template.trim().isEmpty()) {
                throw new RuntimeException("No se pudo cargar el template Printer.PuntosRedimidos");
            }
            
            // Crear un TicketInfo mínimo para el template
            TicketInfo ticket = new TicketInfo();
            ticket.setDate(new java.util.Date());
            
            // Convertir AppUser a UserInfo
            com.openbravo.pos.forms.AppUser appUser = appView.getAppUserView().getUser();
            if (appUser != null) {
                UserInfo userInfo = new UserInfo(appUser.getId(), appUser.getName());
                ticket.setUser(userInfo);
            }
            
            // Crear script de Velocity
            ScriptEngine script = ScriptFactory.getScriptEngine(ScriptFactory.VELOCITY);
            script.put("ticket", ticket);
            script.put("customerName", nombreCliente != null ? nombreCliente : "");
            script.put("puntosRedimidos", puntosRedimidos);
            script.put("esCopia", esCopia);
            
            // Evaluar el template y imprimir
            String ticketText = script.eval(template).toString();
            ticketParser.printTicket(ticketText, ticket);
            
        } catch (Exception ex) {
            LOGGER.log(System.Logger.Level.WARNING, "Error imprimiendo ticket de puntos redimidos: ", ex);
            throw new RuntimeException("Error al imprimir ticket: " + ex.getMessage(), ex);
        }
    }
    
    /**
     * Sebastian - Actualiza el template Printer.PuntosRedimidos en la base de datos desde el archivo XML
     */
    private void actualizarTemplatePuntosRedimidosEnBD() {
        try {
            // Leer el archivo XML desde el classpath
            java.io.InputStream is = getClass().getResourceAsStream("/com/openbravo/pos/templates/Printer.PuntosRedimidos.xml");
            if (is == null) {
                LOGGER.log(System.Logger.Level.WARNING, "No se pudo encontrar el archivo Printer.PuntosRedimidos.xml en el classpath");
                return;
            }
            
            // Leer todo el contenido del archivo
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            byte[] templateContent = baos.toByteArray();
            is.close();
            baos.close();
            
            // Verificar que el contenido no esté vacío
            if (templateContent == null || templateContent.length == 0) {
                LOGGER.log(System.Logger.Level.WARNING, "El archivo Printer.PuntosRedimidos.xml está vacío");
                return;
            }
            
            // Verificar que el contenido contiene el texto corregido (sin la sintaxis problemática)
            String templateStr = new String(templateContent, java.nio.charset.StandardCharsets.UTF_8);
            if (templateStr.contains("!\"\"}")) {
                LOGGER.log(System.Logger.Level.WARNING, "El template todavía contiene la sintaxis problemática. Verificando archivo fuente...");
                return; // No actualizar si todavía tiene el error
            }
            
            // Sebastian - Eliminar el recurso existente primero para asegurar que se actualice correctamente
            try {
                // Acceder a la sesión usando reflexión
                java.lang.reflect.Field sessionField = dlSystem.getClass().getDeclaredField("session");
                sessionField.setAccessible(true);
                com.openbravo.data.loader.Session session = (com.openbravo.data.loader.Session) sessionField.get(dlSystem);
                
                // Eliminar el recurso existente
                com.openbravo.data.loader.SentenceExec deleteResource = new com.openbravo.data.loader.PreparedSentenceExec(
                    session,
                    "DELETE FROM resources WHERE NAME = ?",
                    com.openbravo.data.loader.SerializerWriteString.INSTANCE
                );
                deleteResource.exec("Printer.PuntosRedimidos");
                LOGGER.log(System.Logger.Level.INFO, "Recurso Printer.PuntosRedimidos eliminado de la BD antes de actualizar");
            } catch (Exception e) {
                // Si no existe o hay error, no hay problema, continuamos con el UPDATE/INSERT
                LOGGER.log(System.Logger.Level.DEBUG, "No se pudo eliminar el recurso existente (continuando con UPDATE): " + e.getMessage());
            }
            
            // Insertar el template actualizado en la base de datos
            // Tipo 0 = texto/XML - setResource hace UPDATE si existe, INSERT si no existe
            dlSystem.setResource("Printer.PuntosRedimidos", 0, templateContent);
            LOGGER.log(System.Logger.Level.INFO, "Template Printer.PuntosRedimidos actualizado en la base de datos (" + templateContent.length + " bytes)");
            
        } catch (java.io.IOException e) {
            LOGGER.log(System.Logger.Level.WARNING, "Error leyendo el archivo Printer.PuntosRedimidos.xml: " + e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.ERROR, "Error actualizando template Printer.PuntosRedimidos en BD: " + e.getMessage(), e);
        }
    }
    
    /**
     * Sebastian - Reimprime el último ticket de puntos redimidos o muestra historial
     */
    private void btnImprimirCopiaPuntosActionPerformed(java.awt.event.ActionEvent evt) {
        if (m_oId == null) {
            JOptionPane.showMessageDialog(this, 
                "Seleccione un cliente primero.", 
                "Información", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        try {
            // Obtener historial de puntos redimidos del cliente
            java.util.List<PuntosDataLogic.PuntosRedimidos> historial = puntosLogic.getHistorialPuntosRedimidos(m_oId);
            
            if (historial == null || historial.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "No hay tickets de puntos redimidos para este cliente.", 
                    "Información", 
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            
            // Crear diálogo para seleccionar qué ticket reimprimir
            String[] opciones = new String[historial.size()];
            for (int i = 0; i < historial.size(); i++) {
                PuntosDataLogic.PuntosRedimidos redimido = historial.get(i);
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
                opciones[i] = sdf.format(redimido.getFecha()) + " - " + redimido.getPuntosRedimidos() + " puntos";
            }
            
            String seleccionado = (String) JOptionPane.showInputDialog(
                this,
                "Seleccione el ticket a reimprimir:",
                "Reimprimir Ticket de Puntos Redimidos",
                JOptionPane.QUESTION_MESSAGE,
                null,
                opciones,
                opciones[0]
            );
            
            if (seleccionado != null) {
                // Encontrar el índice seleccionado
                int indice = java.util.Arrays.asList(opciones).indexOf(seleccionado);
                if (indice >= 0 && indice < historial.size()) {
                    PuntosDataLogic.PuntosRedimidos redimido = historial.get(indice);
                    imprimirTicketPuntosRedimidos(redimido.getNombreCliente(), redimido.getPuntosRedimidos(), true);
                    JOptionPane.showMessageDialog(this, 
                        "Copia del ticket de puntos redimidos impresa exitosamente.", 
                        "Copia Impresa", 
                        JOptionPane.INFORMATION_MESSAGE);
                }
            }
            
        } catch (Exception ex) {
            LOGGER.log(System.Logger.Level.WARNING, "Error al obtener historial de puntos redimidos: ", ex);
            JOptionPane.showMessageDialog(this, 
                "Error al obtener historial: " + ex.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    // Método auxiliar para actualizar la visualización de puntos
    private void actualizarPuntosVisuales() {
        if (m_oId != null) {
            try {
                int puntos = puntosLogic.obtenerPuntos(m_oId);
                txtPuntosActuales.setText(String.valueOf(puntos));
            } catch (BasicException ex) {
                txtPuntosActuales.setText("Error");
                LOGGER.log(System.Logger.Level.WARNING, "Error al obtener puntos: ", ex);
            }
        } else {
            txtPuntosActuales.setText("0");
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jBtnClearCard;
    private javax.swing.JButton jBtnCreateCard;
    private javax.swing.JButton jBtnShowTrans;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLblDiscount;
    private javax.swing.JLabel jLblDiscountpercent;
    private javax.swing.JLabel jLblTranCount;
    private javax.swing.JLabel jLblVIP;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanelGeneral;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTableCustomerTransactions;
    private javax.swing.JTextField jcard;
    private javax.swing.JComboBox m_jCategory;
    private com.openbravo.data.gui.JImageEditor m_jImage;
    private javax.swing.JTextField m_jName;
    private javax.swing.JTextArea m_jNotes;
    private javax.swing.JTextField m_jSearchkey;
    private javax.swing.JTextField m_jTaxID;
    private javax.swing.JCheckBox m_jVip;
    private javax.swing.JCheckBox m_jVisible;
    private javax.swing.JButton m_jbtndate;
    private javax.swing.JTextField m_jdate;
    private javax.swing.JTextField txtAddress;
    private javax.swing.JTextField txtAddress2;
    private javax.swing.JTextField txtCity;
    private javax.swing.JTextField txtCountry;
    private javax.swing.JTextField txtCurdate;
    private javax.swing.JTextField txtCurdebt;
    private javax.swing.JTextField txtDiscount;
    private javax.swing.JTextField txtEmail;
    private javax.swing.JTextField txtFax;
    private javax.swing.JTextField txtFirstName;
    private javax.swing.JTextField txtLastName;
    private javax.swing.JTextField txtMaxdebt;
    private javax.swing.JTextField txtPhone;
    private javax.swing.JTextField txtPhone2;
    private javax.swing.JTextField txtPostal;
    private javax.swing.JTextField txtRegion;
    private javax.swing.JButton webBtnMail;
    
    // Sebastian - Componentes para la pestaña de puntaje
    private javax.swing.JPanel jPanelPuntaje;
    private javax.swing.JLabel jLabelPuntosActuales;
    private javax.swing.JTextField txtPuntosActuales;
    private javax.swing.JLabel jLabelAjustarPuntos;
    private javax.swing.JTextField txtAjustarPuntos;
    private javax.swing.JButton btnAgregarPuntos;
    private javax.swing.JButton btnQuitarPuntos;
    private javax.swing.JButton btnActualizarPuntos;
    private javax.swing.JButton btnImprimirCopiaPuntos; // Sebastian - Botón para reimprimir ticket de puntos
    // End of variables declaration//GEN-END:variables

}
