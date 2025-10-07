/*
 * CONNECTING POS - Panel de Información Modernizado
 */
package com.openbravo.pos.forms;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;

/**
 * Panel de Información Ultra Moderno por Sebastian
 * @author Sebastian
 */
public class JCopyRightPanel extends javax.swing.JPanel {

    private static final long serialVersionUID = 1L;
    private static final String SEBASTIAN_SITE = "https://github.com/sebastian-dev";
    
    // Colores modernos premium
    private static final Color MODERN_BLUE = new Color(74, 144, 226);
    private static final Color MODERN_LIGHT = new Color(248, 249, 250);
    private static final Color MODERN_DARK = new Color(33, 37, 41);
    private static final Color MODERN_GRAY = new Color(108, 117, 125);
    private static final Color MODERN_SUCCESS = new Color(40, 167, 69);
    
    /**
     * Creates new form JCopyRightPanel
     */
    public JCopyRightPanel() {
        initComponents();
        setupModernDesign();
    }
    
    private void setupModernDesign() {
        // Configurar el fondo moderno
        setBackground(MODERN_LIGHT);
        
        // Crear contenido moderno y atractivo
        copyRightLabel.setText("<html><div style='text-align: center; padding: 40px;'>"
                + "<h1 style='color: #4080FF; font-size: 28px; margin-bottom: 20px;'>🚀 CONNECTING POS</h1>"
                + "<p style='color: #2D2D2D; font-size: 18px; margin-bottom: 15px;'><strong>Sistema de Punto de Venta Moderno</strong></p>"
                + "<p style='color: #6C757D; font-size: 14px; margin-bottom: 25px;'>Desarrollado con tecnología avanzada para tu negocio</p>"
                
                + "<div style='background: #FFFFFF; padding: 25px; border-radius: 10px; margin: 20px 0; box-shadow: 0 2px 10px rgba(0,0,0,0.1);'>"
                + "<h2 style='color: #4080FF; font-size: 20px; margin-bottom: 15px;'>✨ Características Principales</h2>"
                + "<ul style='color: #2D2D2D; font-size: 14px; text-align: left; list-style: none; padding: 0;'>"
                + "<li style='margin: 8px 0;'>🎯 <strong>Interfaz Moderna:</strong> Diseño intuitivo y responsivo</li>"
                + "<li style='margin: 8px 0;'>⚡ <strong>Alto Rendimiento:</strong> Procesamiento rápido de transacciones</li>"
                + "<li style='margin: 8px 0;'>🔒 <strong>Seguridad Avanzada:</strong> Protección de datos empresariales</li>"
                + "<li style='margin: 8px 0;'>📊 <strong>Reportes Inteligentes:</strong> Análisis detallado de ventas</li>"
                + "<li style='margin: 8px 0;'>🌐 <strong>Conectividad:</strong> Sincronización en tiempo real</li>"
                + "</ul>"
                + "</div>"
                
                + "<div style='background: linear-gradient(135deg, #4080FF, #6496FF); padding: 20px; border-radius: 10px; margin: 20px 0;'>"
                + "<h3 style='color: white; font-size: 16px; margin-bottom: 10px;'>👨‍💻 Desarrollado por Sebastian</h3>"
                + "<p style='color: #E3F2FD; font-size: 13px;'>Tecnología de vanguardia para impulsar tu empresa</p>"
                + "</div>"
                
                + "<div style='margin-top: 30px; padding: 15px; background: #F8F9FA; border-radius: 8px;'>"
                + "<p style='color: #6C757D; font-size: 12px; margin: 5px 0;'>🔧 <strong>Versión:</strong> " + AppLocal.APP_VERSION + "</p>"
                + "<p style='color: #6C757D; font-size: 12px; margin: 5px 0;'>📅 <strong>Última actualización:</strong> Octubre 2025</p>"
                + "<p style='color: #6C757D; font-size: 12px; margin: 5px 0;'>🏢 <strong>Para:</strong> Empresas modernas y dinámicas</p>"
                + "</div>"
                
                + "<div style='margin-top: 25px;'>"
                + "<p style='color: #4080FF; font-size: 14px; font-weight: bold;'>¡Gracias por elegir CONNECTING POS! 🎉</p>"
                + "</div>"
                
                + "</div></html>"
        );

        // Aplicar estilo moderno al label
        copyRightLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        copyRightLabel.setAlignmentX(0.5F);
        copyRightLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        copyRightLabel.setMaximumSize(new java.awt.Dimension(900, 1200));
        copyRightLabel.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        copyRightLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // 🎨 EFECTOS VISUALES ULTRA MODERNOS
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        // Gradiente de fondo premium
        GradientPaint backgroundGradient = new GradientPaint(
            0, 0, new Color(248, 249, 250),
            0, getHeight(), new Color(239, 243, 248)
        );
        g2d.setPaint(backgroundGradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        
        // Efectos de luz sutil en las esquinas
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
        
        // Luz superior izquierda
        RadialGradientPaint lightGradient1 = new RadialGradientPaint(
            0, 0, 200,
            new float[]{0f, 1f},
            new Color[]{new Color(74, 144, 226, 50), new Color(74, 144, 226, 0)}
        );
        g2d.setPaint(lightGradient1);
        g2d.fillOval(-100, -100, 200, 200);
        
        // Luz inferior derecha
        RadialGradientPaint lightGradient2 = new RadialGradientPaint(
            getWidth(), getHeight(), 150,
            new float[]{0f, 1f},
            new Color[]{new Color(40, 167, 69, 30), new Color(40, 167, 69, 0)}
        );
        g2d.setPaint(lightGradient2);
        g2d.fillOval(getWidth() - 75, getHeight() - 75, 150, 150);
        
        g2d.dispose();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        copyRightLabel = new javax.swing.JLabel();

        // 🎨 CONFIGURACIÓN MODERNA DEL LABEL
        copyRightLabel.setFont(new java.awt.Font("Segoe UI", 0, 14));
        copyRightLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        copyRightLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        copyRightLabel.setAlignmentX(0.5F);
        copyRightLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        copyRightLabel.setMaximumSize(new java.awt.Dimension(900, 1200));
        copyRightLabel.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        copyRightLabel.setBackground(MODERN_LIGHT);
        copyRightLabel.setOpaque(true);

        // 🏗️ LAYOUT MODERNO CON MÁS ESPACIO
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        setBackground(MODERN_LIGHT);
        
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 600, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(20, 20, 20)
                    .addComponent(copyRightLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 560, Short.MAX_VALUE)
                    .addGap(20, 20, 20)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 500, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(20, 20, 20)
                    .addComponent(copyRightLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 460, Short.MAX_VALUE)
                    .addGap(20, 20, 20)))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel copyRightLabel;
    // End of variables declaration//GEN-END:variables
}
