import java.sql.*;

public class DeleteDatabase {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306";
        String user = "kriolopos";
        String password = "kriolopos";
        
        try {
            // Cargar driver MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Conectar sin especificar la BD
            Connection conn = DriverManager.getConnection(url, user, password);
            System.out.println("✓ Conectado a MySQL Server");
            
            Statement stmt = conn.createStatement();
            
            // Eliminar la base de datos
            System.out.println("\n--- Eliminando base de datos kriolopos ---");
            stmt.execute("DROP DATABASE IF EXISTS kriolopos");
            System.out.println("✓ Base de datos eliminada");
            
            // Recrear vacía
            System.out.println("\n--- Recreando base de datos kriolopos (vacía) ---");
            stmt.execute("CREATE DATABASE kriolopos");
            System.out.println("✓ Base de datos recreada vacía");
            
            stmt.close();
            conn.close();
            
            System.out.println("\n✓ Operación completada exitosamente");
            System.out.println("La próxima vez que inicie la aplicación, recreará la BD con los nuevos cambios.");
            
        } catch (ClassNotFoundException e) {
            System.out.println("✗ Error: No se encontró el driver MySQL");
            System.out.println("Asegúrate de que mysql-connector-j está en el classpath");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("✗ Error de SQL: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
