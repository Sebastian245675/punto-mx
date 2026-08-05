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
            
            String db_user = config.getProperty("db.user");
            String db_url = config.getProperty("db.URL") + config.getProperty("db.schema") + config.getProperty("db.options");
            String db_password = config.getProperty("db.password");
            
            if (db_password != null && db_password.startsWith("crypt:")) {
                AltEncrypter cypher = new AltEncrypter("cypherkey" + db_user);
                db_password = cypher.decrypt(db_password.substring(6));
            }
            
            System.out.println("Conectando a la base de datos: " + db_url);
            
            // Crear sesión
            Session session = new Session(db_url, db_user, db_password);
            session.begin();
            
            // Inicializar DataLogicSystem
            DataLogicSystem dlSystem = new DataLogicSystem();
            dlSystem.init(session);
            
            // Leer el archivo XML actualizado
            File templateFile = new File("kriolos-opos-app/src/main/resources/com/openbravo/pos/templates/Printer.Ticket.xml");
            if (!templateFile.exists()) {
                System.err.println("Error: No se encontró el archivo template en: " + templateFile.getAbsolutePath());
                System.exit(1);
            }
            
            byte[] templateContent = Files.readAllBytes(templateFile.toPath());
            
            // Actualizar el recurso en la base de datos
            dlSystem.setResource("Printer.Ticket", 0, templateContent);
            
            session.commit();
            session.close();
            
            System.out.println("✓ Template Printer.Ticket actualizado exitosamente en la base de datos!");
            System.out.println("  - Logo eliminado");
            System.out.println("  - Textos traducidos al español");
            System.out.println("\nReinicia la aplicación para ver los cambios.");
            
        } catch (Exception e) {
            System.err.println("Error al actualizar template: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
