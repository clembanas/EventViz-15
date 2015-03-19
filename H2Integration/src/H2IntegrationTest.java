import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class H2IntegrationTest {

	public static void main(final String[] args) {
		Connection conn = null;
		final String tab = "myH2Table";

		try {
			Class.forName("org.h2.Driver");
			final URL location = H2IntegrationTest.class.getProtectionDomain().getCodeSource().getLocation();
			conn = DriverManager.getConnection("jdbc:h2:" + location + "../database/myH2DB");

			// SQL statements
			final Statement stmt = conn.createStatement();

			final String dropQ = "DROP TABLE IF EXISTS " + tab;
			stmt.executeUpdate(dropQ);
			final String createQ = "CREATE TABLE IF NOT EXISTS " + tab + "(ID INT PRIMARY KEY AUTO_INCREMENT(1,1) NOT NULL, NAME VARCHAR(255))";
			stmt.executeUpdate(createQ);
			final String insertQ = "INSERT INTO " + tab + " VALUES(TRANSACTION_ID(),'Hello World')";
			stmt.executeUpdate(insertQ);

			final ResultSet selectRS = stmt.executeQuery("SELECT * FROM " + tab);
			while (selectRS.next()) {
				System.out.printf("%s, %s\n", selectRS.getString(1), selectRS.getString(2));
			}
			conn.close();
		} catch (final SQLException e) {
			e.printStackTrace();
		} catch (final ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (conn != null)
				try {
					conn.close();
				} catch (final SQLException e) {
					e.printStackTrace();
				}
		}
	}

}