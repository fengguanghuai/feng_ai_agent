import java.sql.DriverManager;

/** Read-only connectivity check. Pass the PostgreSQL JDBC jar on --class-path. */
class DatabaseCheck {
    public static void main(String[] args) throws Exception {
        try (var connection = DriverManager.getConnection(
                System.getenv("SUPABASE_JDBC_URL"), System.getenv("SUPABASE_DB_USER"),
                System.getenv("SUPABASE_DB_PASSWORD"));
             var statement = connection.createStatement()) {
            statement.setQueryTimeout(15);
            try (var rows = statement.executeQuery("select current_database(), version()")) {
                rows.next();
                System.out.println("Connected to " + rows.getString(1) + ": " + rows.getString(2));
            }
            try (var rows = statement.executeQuery("select count(*) from public.chat_message")) {
                rows.next();
                System.out.println("Chat message table accessible; rows=" + rows.getLong(1));
            }
        }
    }
}
