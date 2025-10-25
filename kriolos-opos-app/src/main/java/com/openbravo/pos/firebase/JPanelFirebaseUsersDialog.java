//    KriolOS POS
//    Dialog to display Firebase roles/users
package com.openbravo.pos.firebase;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;

public class JPanelFirebaseUsersDialog extends JDialog {

    private JTable table;
    private DefaultTableModel model;
    private JButton btnCopyUsuario;

    public JPanelFirebaseUsersDialog(Window owner, List<Map<String, Object>> roles) {
        super(owner, "Usuarios Firebase", ModalityType.APPLICATION_MODAL);
        initComponents();
        loadRoles(roles);
        setSize(700, 400);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 8));

        String[] cols = {"ID", "Usuario", "Nombre", "Rol", "Tarjeta", "Otros Campos"};
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);

        JScrollPane sc = new JScrollPane(table);
        add(sc, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));

        btnCopyUsuario = new JButton("Copiar Usuario");
        btnCopyUsuario.addActionListener((ActionEvent e) -> copySelectedField());
        bottom.add(btnCopyUsuario);

        JButton btnClose = new JButton("Cerrar");
        btnClose.addActionListener(e -> dispose());
        bottom.add(btnClose);

        add(bottom, BorderLayout.SOUTH);
    }

    private void loadRoles(List<Map<String, Object>> roles) {
        model.setRowCount(0);
        if (roles == null) return;

        for (Map<String, Object> role : roles) {
            String id = safe(role.get("id"));
            String usuario = safe(role.get("usuario"));
            String nombre = safe(role.get("nombre"));
            String rol = safe(role.get("rol"));
            String tarjeta = safe(role.get("tarjeta"));

            // Collect other fields into a single string for display
            StringBuilder others = new StringBuilder();
            for (Map.Entry<String, Object> e : role.entrySet()) {
                String k = e.getKey();
                if (k.equals("id") || k.equals("usuario") || k.equals("nombre") || k.equals("rol") || k.equals("tarjeta")) continue;
                others.append(k).append(": ").append(safe(e.getValue())).append("; ");
            }

            model.addRow(new Object[]{id, usuario, nombre, rol, tarjeta, others.toString()});
        }
    }

    private String safe(Object o) {
        return o == null ? "" : o.toString();
    }

    private void copySelectedField() {
        int sel = table.getSelectedRow();
        if (sel == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione una fila primero.", "Seleccione", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Convert view index to model index
        int modelRow = table.convertRowIndexToModel(sel);

        // Get usuario field (column 1)
        String value = (String) model.getValueAt(modelRow, 1);

        if (value == null || value.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El campo usuario está vacío.", "Vacío", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Copy to clipboard
        StringSelection selStr = new StringSelection(value);
        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
        cb.setContents(selStr, null);

        JOptionPane.showMessageDialog(this, "Usuario copiado al portapapeles:\n" + value, "Copiado", JOptionPane.INFORMATION_MESSAGE);
    }
}
