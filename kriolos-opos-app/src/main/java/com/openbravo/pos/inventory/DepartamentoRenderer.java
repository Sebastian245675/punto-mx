//    KriolOS POS
//    Copyright (c) 2019-2023 KriolOS
//
package com.openbravo.pos.inventory;

import com.openbravo.format.Formats;
import com.openbravo.pos.resources.ImageResources;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JList;

/**
 * Renderer personalizado para la lista de departamentos
 * Muestra iconos de carpeta y resalta la selección en azul
 */
public class DepartamentoRenderer extends DefaultListCellRenderer {
    
    private final Icon folderIcon;
    
    public DepartamentoRenderer() {
        // Usar el icono de categoría como icono de carpeta
        folderIcon = ImageResources.ICON_CATEGORY.getIcon(20, 20);
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, 
            int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, null, index, isSelected, cellHasFocus);
        
        setIcon(folderIcon);
        
        // Extraer el nombre del departamento del array (el nombre está en el índice 1)
        String name = "";
        if (value != null && value instanceof Object[]) {
            Object[] cat = (Object[]) value;
            if (cat.length > 1 && cat[1] != null) {
                name = Formats.STRING.formatValue((String) cat[1]);
            }
        } else if (value != null) {
            name = value.toString();
        }
        
        setText(name);
        setFont(new Font("Arial", Font.PLAIN, 14));
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        
        if (isSelected) {
            setBackground(new Color(66, 165, 245)); // Azul para selección
            setForeground(Color.WHITE);
        } else {
            setBackground(Color.WHITE);
            setForeground(Color.BLACK);
        }
        
        return this;
    }
}

