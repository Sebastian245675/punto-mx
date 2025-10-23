//    KriolOS POS
//    Copyright (c) 2019-2023 KriolOS

package com.openbravo.pos.branches;

import com.openbravo.basic.BasicException;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.BeanFactoryApp;
import com.openbravo.pos.forms.BeanFactoryException;
import com.openbravo.pos.forms.JPanelView;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Font;

public class JPanelBranchesTest extends JPanel implements JPanelView, BeanFactoryApp {

    private static final long serialVersionUID = 1L;
    
    public JPanelBranchesTest() {
        System.out.println("*** JPanelBranchesTest - Constructor llamado ***");
        setLayout(new BorderLayout());
        JLabel label = new JLabel("PRUEBA: Panel de Sucursales funciona!", JLabel.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 24));
        add(label, BorderLayout.CENTER);
    }
    
    @Override
    public void init(AppView app) throws BeanFactoryException {
        System.out.println("*** JPanelBranchesTest - init() llamado ***");
    }
    
    @Override
    public Object getBean() {
        return this;
    }
    
    @Override
    public JComponent getComponent() {
        System.out.println("*** JPanelBranchesTest - getComponent() llamado ***");
        return this;
    }

    @Override
    public String getTitle() {
        System.out.println("*** JPanelBranchesTest - getTitle() llamado ***");
        return "PRUEBA SUCURSALES";
    }

    @Override
    public void activate() throws BasicException {
        System.out.println("*** JPanelBranchesTest - activate() llamado ***");
    }

    @Override
    public boolean deactivate() {
        return true;
    }
}
