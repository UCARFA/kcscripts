package edu.ucar.fanda.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

public class DbUtils {
	
	public static Connection getConnection(String serverName) {
		Connection conn = null;
		String server = serverName;
		int dbPort = 3306;
		if (server.startsWith("kcnonprod")) {
			String serverNumber = server.substring(server.length() - 1);
			dbPort = Integer.parseInt(serverNumber + Integer.toString(dbPort));
		}
		
		if (server != "localhost") {
			server = server + ".fanda.ucar.edu";
		}
		
		try {
			MysqlDataSource dataSource = new MysqlDataSource();
			dataSource.setUser("coeus");
			dataSource.setPassword("kuali");
			dataSource.setServerName(server);
			dataSource.setPort(dbPort);
			dataSource.setDatabaseName("coeus");			
			conn = dataSource.getConnection();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return conn;
	}
	
	public static String getUserId(String userName, String serverName) throws SQLException {
//		System.out.println("     ********************* getUserId() ********************\n");
		String userId = null;
		Connection conn = null;
		try {
			conn = getConnection(serverName);
			if (conn != null) {
				String sql = "SELECT prncpl_id FROM krim_prncpl_t WHERE prncpl_nm = ?";
				PreparedStatement stmt = conn.prepareStatement(sql);
				stmt.setString(1, userName);
				ResultSet rs = stmt.executeQuery();
				int size = 0;
				while (rs.next()) {
					userId = rs.getString("prncpl_id");
					size++;
				}
				if (size != 1) {
					userId = null;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (conn != null) {
				conn.close();
			}
		}
//		System.out.println("     ********************* END getUserId() ********************\n");
		return userId;
	}
	
	public static boolean checkUnit(String unit, String serverName) throws SQLException {
		boolean unitFound = false;
		Connection conn = null;
		try {
			conn = getConnection(serverName);
			if (conn != null) {
				String sql = "SELECT UNIT_NUMBER FROM unit WHERE unit_number = ?";
				PreparedStatement stmt = conn.prepareStatement(sql);
				stmt.setString(1, unit);
				ResultSet rs = stmt.executeQuery();
				if (rs.next()) {
					unitFound = true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (conn != null) {
				conn.close();
			}
		}
		return unitFound;
	}
}
