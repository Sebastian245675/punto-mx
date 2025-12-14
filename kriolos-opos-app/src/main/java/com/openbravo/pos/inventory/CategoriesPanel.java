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

package com.openbravo.pos.inventory;

import com.openbravo.data.loader.ComparatorCreator;
import com.openbravo.data.loader.TableDefinition;
import com.openbravo.data.loader.Vectorer;
import com.openbravo.data.user.EditorRecord;
import com.openbravo.data.user.ListProvider;
import com.openbravo.data.user.ListProviderCreator;
import com.openbravo.data.user.DefaultSaveProvider;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.panels.JPanelTable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;

/**
 *
 * @author adrianromero
 */
public class CategoriesPanel extends JPanelTable {

    private static final long serialVersionUID = 1L;
    
    private TableDefinition tcategories;
    private CategoriesEditor jeditor;
    private JTextField searchField;
    
    /** Creates a new instance of JPanelCategories */
    public CategoriesPanel() {        
    }

    /**
     *
     */
    @Override
    protected void init() {   
        DataLogicSales dlSales = (DataLogicSales) app.getBean("com.openbravo.pos.forms.DataLogicSales");           
        tcategories = dlSales.getTableCategories();
        jeditor = new CategoriesEditor(app, dirty);    
    }
    
    /**
     *
     * @return
     */
    @Override
    public ListProvider getListProvider() {
        return new ListProviderCreator(tcategories);
    }
    
    /**
     *
     * @return
     */
    @Override
    public DefaultSaveProvider getSaveProvider() {
        return new DefaultSaveProvider(tcategories);      
    }
    
    /**
     *
     * @return
     */
    @Override
    public Vectorer getVectorer() {
        return tcategories.getVectorerBasic(new int[]{1});
    }
    
    /**
     *
     * @return
     */
    @Override
    public ComparatorCreator getComparatorCreator() {
        return tcategories.getComparatorCreator(new int[]{1});
    }
    
    /**
     *
     * @return
     */
    @Override
    public ListCellRenderer getListCellRenderer() {
        return new DepartamentoRenderer();
    }
    
    /**
     *
     * @return
     */
    @Override
    public EditorRecord getEditor() {
        return jeditor;
    }
    
    /**
     * Panel de filtro personalizado con título "DEPARTAMENTOS" y campo de búsqueda
     */
    @Override
    public Component getFilter() {
        JPanel filterPanel = new JPanel(new BorderLayout());
        filterPanel.setBackground(new Color(240, 240, 240));
        filterPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        filterPanel.setPreferredSize(new Dimension(300, 80));
        
        // Título "DEPARTAMENTOS" en amarillo
        JLabel titleLabel = new JLabel("DEPARTAMENTOS");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(new Color(255, 200, 0)); // Amarillo
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        filterPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Panel para el campo de búsqueda
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        searchPanel.setOpaque(false);
        
        // Campo de búsqueda con placeholder
        searchField = new JTextField(20);
        searchField.setFont(new Font("Arial", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 30, 5, 5)
        ));
        searchField.putClientProperty("JTextField.placeholderText", "Buscar...");
        searchField.setPreferredSize(new Dimension(250, 30));
        
        // Icono de búsqueda (usando un símbolo de texto por ahora)
        JLabel searchIcon = new JLabel("🔍");
        searchIcon.setFont(new Font("Arial", Font.PLAIN, 14));
        searchIcon.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        searchPanel.add(searchIcon);
        searchPanel.add(searchField);
        
        filterPanel.add(searchPanel, BorderLayout.CENTER);
        
        return filterPanel;
    }
    
    /**
     *
     * @return
     */
    @Override
    public String getTitle() {
        return AppLocal.getIntString("Menu.Categories");
    }        
}
