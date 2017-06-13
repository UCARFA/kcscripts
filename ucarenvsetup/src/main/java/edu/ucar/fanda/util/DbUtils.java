package edu.ucar.fanda.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

public class DbUtils {
	
	public static Connection getConnection() {
		Connection conn = null;
		try {
			MysqlDataSource dataSource = new MysqlDataSource();
			dataSource.setUser("coeus");
			dataSource.setPassword("kuali");
	//		dataSource.setServerName("localhost");
	//		dataSource.setServerName("kuali-eval.fanda.ucar.edu");
			dataSource.setServerName("kcnonprod2.fanda.ucar.edu");
			dataSource.setPort(23306);
			dataSource.setDatabaseName("coeus");			
			conn = dataSource.getConnection();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return conn;
	}
	
	public static String getUserId(String userName) throws SQLException {
		System.out.println("     ********************* getUserId() ********************\n");
		String userId = null;
		Connection conn = null;
		try {
			conn = getConnection();
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
		System.out.println("     ********************* END getUserId() ********************\n");
		return userId;
	}
}
