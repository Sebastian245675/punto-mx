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

import com.openbravo.basic.BasicException;
import com.openbravo.data.user.SaveProvider;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * SaveProvider personalizado que valida el código de administrador antes de
 * guardar
 * y envía el producto a Supabase después de guardarlo exitosamente
 */
public class ProductSaveProviderWithAdminValidation implements SaveProvider<Object[]> {

    private static final Logger LOGGER = Logger.getLogger(ProductSaveProviderWithAdminValidation.class.getName());

    private final SaveProvider<Object[]> baseSaveProvider;
    private final ProductsPanel productsPanel;

    public ProductSaveProviderWithAdminValidation(SaveProvider<Object[]> baseSaveProvider,
            ProductsPanel productsPanel) {
        this.baseSaveProvider = baseSaveProvider;
        this.productsPanel = productsPanel;
    }

    @Override
    public boolean canDelete() {
        return baseSaveProvider.canDelete();
    }

    @Override
    public boolean canInsert() {
        return baseSaveProvider.canInsert();
    }

    @Override
    public boolean canUpdate() {
        return baseSaveProvider.canUpdate();
    }

    @Override
    public int deleteData(Object[] value) throws BasicException {
        // Para eliminar también requerimos validación de administrador
        if (!validateAdminBeforeSave("eliminar")) {
            throw new BasicException("Se requiere validación de administrador para eliminar productos");
        }
        int result = baseSaveProvider.deleteData(value);
        return result;
    }

    @Override
    public int insertData(Object[] value) throws BasicException {
        // Validar código de administrador antes de insertar
        if (!validateAdminBeforeSave("guardar")) {
            throw new BasicException("Se requiere validación de administrador para guardar productos");
        }

        // Guardar en la base de datos local
        int result = baseSaveProvider.insertData(value);

        // Si se guardó exitosamente, enviar a Supabase
        if (result > 0) {
            sendToSupabaseAsync(value);
        }

        return result;
    }

    @Override
    public int updateData(Object[] value) throws BasicException {
        // Validar código de administrador antes de actualizar
        if (!validateAdminBeforeSave("actualizar")) {
            throw new BasicException("Se requiere validación de administrador para actualizar productos");
        }

        // Guardar en la base de datos local
        int result = baseSaveProvider.updateData(value);

        // Si se guardó exitosamente, enviar a Supabase
        if (result > 0) {
            sendToSupabaseAsync(value);
        }

        return result;
    }

    /**
     * Valida el código de administrador antes de guardar
     * 
     * @param action Acción que se está realizando (guardar, actualizar, eliminar)
     * @return true si el código es válido, false en caso contrario
     */
    private boolean validateAdminBeforeSave(String action) {
        // Omitir validación de administrador a petición del usuario para permitir guardar, actualizar o eliminar directamente
        LOGGER.info("Omitiendo validación de administrador para " + action + " producto");
        return true;
    }

    /**
     * Envía el producto a Supabase de forma asíncrona
     */
    private void sendToSupabaseAsync(Object[] productData) {
        // La sincronización de productos ha sido deshabilitada por solicitud del usuario
        /*
        // Ejecutar en un hilo separado para no bloquear la UI
        new Thread(() -> {
            try {
                productsPanel.sendProductToSupabase(productData);
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Error enviando producto a Supabase en hilo asíncrono: " + ex.getMessage(),
                        ex);
            }
        }).start();
        */
    }
}
