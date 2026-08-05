package com.openbravo.pos.inventory;

import com.openbravo.basic.BasicException;
import com.openbravo.data.user.SaveProvider;
import com.openbravo.pos.supabase.SupabaseServiceManager;
import com.openbravo.pos.supabase.SupabaseServiceREST;
import com.openbravo.pos.forms.AppConfig;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * SaveProvider para categorías que sincroniza con Supabase
 */
public class CategorySaveProviderWithSync implements SaveProvider<Object[]> {

    private static final Logger LOGGER = Logger.getLogger(CategorySaveProviderWithSync.class.getName());
    private final SaveProvider<Object[]> baseSaveProvider;

    public CategorySaveProviderWithSync(SaveProvider<Object[]> baseSaveProvider, CategoriesPanel categoriesPanel) {
        this.baseSaveProvider = baseSaveProvider;
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
        return baseSaveProvider.deleteData(value);
    }

    @Override
    public int insertData(Object[] value) throws BasicException {
        int result = baseSaveProvider.insertData(value);
        if (result > 0) {
            syncToSupabaseAsync(value);
        }
        return result;
    }

    @Override
    public int updateData(Object[] value) throws BasicException {
        int result = baseSaveProvider.updateData(value);
        if (result > 0) {
            syncToSupabaseAsync(value);
        }
        return result;
    }

    private void syncToSupabaseAsync(Object[] value) {
        new Thread(() -> {
            try {
                // value[0] = ID, value[1] = NAME, value[2] = PARENTID
                Map<String, Object> map = new HashMap<>();
                if (value.length > 0 && value[0] != null)
                    map.put("id", value[0].toString());
                if (value.length > 1 && value[1] != null)
                    map.put("nombre", value[1].toString());
                if (value.length > 2 && value[2] != null)
                    map.put("categoriapadre", value[2] != null ? value[2].toString() : null);

                SupabaseServiceManager manager = SupabaseServiceManager.getInstance();
                AppConfig config = new AppConfig(null);
                config.load();
                manager.initialize(config);
                SupabaseServiceREST supabase = manager.getService();

                supabase.syncData("categorias", Collections.singletonList(map));
                LOGGER.info("Categoría sincronizada con Supabase: " + map.get("nombre"));
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error sincronizando categoría", e);
            }
        }).start();
    }
}
