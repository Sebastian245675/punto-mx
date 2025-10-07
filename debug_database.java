import java.sql.*;

public class debug_database {
    public static void main(String[] args) {
        try {
            // Conectar a la base de datos HSQLDB
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
            String dbURL = "jdbc:hsqldb:file:" + System.getProperty("user.home") + "/kriolopos/kriolopos;shutdown=true";
            Connection conn = DriverManager.getConnection(dbURL, "SA", "");
            
            System.out.println("✅ Conectado a la base de datos: " + dbURL);
            
            // 1. Verificar estructura de la tabla closedcash
            System.out.println("\n📋 ESTRUCTURA DE LA TABLA CLOSEDCASH:");
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet columns = meta.getColumns(null, null, "CLOSEDCASH", null);
            
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String dataType = columns.getString("TYPE_NAME");
                int size = columns.getInt("COLUMN_SIZE");
                boolean nullable = columns.getBoolean("NULLABLE");
                
                System.out.println("  - " + columnName + " (" + dataType + 
                    (size > 0 ? "(" + size + ")" : "") + 
                    (nullable ? " NULL" : " NOT NULL") + ")");
            }
            columns.close();
            
            // 2. Verificar si existe la columna initial_amount específicamente
            System.out.println("\n🔍 VERIFICANDO COLUMNA 'INITIAL_AMOUNT':");
            try {
                PreparedStatement stmt = conn.prepareStatement(
                    "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_NAME = 'CLOSEDCASH' AND COLUMN_NAME = 'INITIAL_AMOUNT'"
                );
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    System.out.println("✅ La columna 'INITIAL_AMOUNT' SÍ existe en la tabla");
                } else {
                    System.out.println("❌ La columna 'INITIAL_AMOUNT' NO existe en la tabla");
                }
                rs.close();
                stmt.close();
            } catch (Exception e) {
                System.out.println("⚠️ Error verificando columna: " + e.getMessage());
            }
            
            // 3. Consultar datos actuales de la tabla closedcash
            System.out.println("\n📊 DATOS ACTUALES DE CLOSEDCASH:");
            PreparedStatement queryStmt = conn.prepareStatement(
                "SELECT MONEY, HOST, DATESTART, DATEEND, INITIAL_AMOUNT FROM CLOSEDCASH ORDER BY DATESTART DESC"
            );
            
            ResultSet rs = queryStmt.executeQuery();
            int count = 0;
            
            while (rs.next() && count < 5) {
                String money = rs.getString("MONEY");
                String host = rs.getString("HOST");
                Timestamp dateStart = rs.getTimestamp("DATESTART");
                Timestamp dateEnd = rs.getTimestamp("DATEEND");
                
                System.out.print("  [" + (count + 1) + "] MONEY: " + money + 
                    ", HOST: " + host + 
                    ", START: " + dateStart +
                    ", END: " + dateEnd);
                
                try {
                    double initialAmount = rs.getDouble("INITIAL_AMOUNT");
                    boolean wasNull = rs.wasNull();
                    
                    if (wasNull) {
                        System.out.println(", INITIAL_AMOUNT: NULL");
                    } else {
                        System.out.println(", INITIAL_AMOUNT: $" + initialAmount);
                    }
                } catch (SQLException e) {
                    System.out.println(", INITIAL_AMOUNT: [COLUMNA NO EXISTE] - " + e.getMessage());
                }
                
                count++;
            }
            
            rs.close();
            queryStmt.close();
            
            // 4. Intentar una consulta SELECT específica para initial_amount
            System.out.println("\n🎯 PRUEBA CONSULTA ESPECÍFICA:");
            try {
                PreparedStatement testStmt = conn.prepareStatement(
                    "SELECT MONEY, INITIAL_AMOUNT FROM CLOSEDCASH WHERE HOST = 'DESKTOP-DFKGUKU' ORDER BY DATESTART DESC"
                );
                
                ResultSet testRs = testStmt.executeQuery();
                if (testRs.next()) {
                    String money = testRs.getString("MONEY");
                    double initialAmount = testRs.getDouble("INITIAL_AMOUNT");
                    boolean wasNull = testRs.wasNull();
                    
                    System.out.println("  MONEY: " + money);
                    System.out.println("  INITIAL_AMOUNT: " + (wasNull ? "NULL" : "$" + initialAmount));
                    System.out.println("  wasNull(): " + wasNull);
                } else {
                    System.out.println("  ❌ No se encontraron registros");
                }
                
                testRs.close();
                testStmt.close();
            } catch (SQLException e) {
                System.out.println("  ❌ Error en consulta: " + e.getMessage());
            }
            
            conn.close();
            System.out.println("\n🔒 Conexión cerrada");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}