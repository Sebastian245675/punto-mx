import java.io.File;
import java.nio.file.Files;
import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.Session;
import com.openbravo.pos.forms.AppConfig;
import com.openbravo.pos.forms.DataLogicSystem;
import com.openbravo.pos.util.AltEncrypter;

/**
 * Script para actualizar el template Printer.Ticket en la base de datos
 * Elimina el logo y traduce todo al español
 */
public class UpdateTicketTemplate {
    
    public static void main(String[] args) {
        try {
            // Cargar configuración
            AppConfig config = AppConfig.getInstance();
            config.load();
            
            String db_user = config.getProperty("db.user");
            String db_url = config.getProperty("db.URL") + config.getProperty("db.schema");
            String db_password = config.getProperty("db.password");
            
            if (db_password != null && db_password.startsWith("crypt:")) {
                AltEncrypter cypher = new AltEncrypter("cypherkey" + db_user);
                db_password = cypher.decrypt(db_password.substring(6));
            }
            
            // Lista de templates a actualizar en la base de datos
            String[] templates = {
                "Printer.Ticket",
                "Printer.Ticket2",
                "Printer.TicketPreview",
                "Printer.CloseCash",
                "Printer.PartialCash",
                "Printer.ReprintTicket",
                "Printer.PrintLastTicket"
            };

            for (String tName : templates) {
                File tFile = new File("kriolos-opos-app/src/main/resources/com/openbravo/pos/templates/" + tName + ".xml");
                if (tFile.exists()) {
                    byte[] content = Files.readAllBytes(tFile.toPath());
                    try {
                        Session session = new Session(db_url, db_user, db_password);
                        session.begin();
                        DataLogicSystem dlSystem = new DataLogicSystem();
                        dlSystem.init(session);
                        dlSystem.setResource(tName, 0, content);
                        session.commit();
                        session.close();
                        System.out.println("  ✓ Template " + tName + " actualizado exitosamente en la BD.");
                    } catch (Exception ex) {
                        System.err.println("  ✗ Error al actualizar " + tName + ": " + ex.getMessage());
                    }
                }
            }
            
            System.out.println("\n✓ ¡PROCESO DE ACTUALIZACIÓN EN BASE DE DATOS FINALIZADO!");
            
        } catch (Exception e) {
            System.err.println("Error al actualizar template: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

