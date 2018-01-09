package com.tea.orm.factory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import com.tea.orm.bean.Configuration;

public final class TeaConnectionFactory {

	private static Configuration conf = new Configuration();
	private static List<Connection> pool = new LinkedList<Connection>();
	
	static {
		ResourceBundle bundle = ResourceBundle.getBundle("cheney", Locale.CHINA);
		conf.setPackageName(bundle.containsKey("packageName") ? bundle.getString("packageName") : "com.tea.entity");
		conf.setQueryClass(bundle.getString("queryClass"));
		conf.setModel(bundle.containsKey("model") ? bundle.getString("model") : "run");
		try {
			Class.forName(bundle.containsKey("driver") ? bundle.getString("driver") : "");
			int poolSize = bundle.containsKey("poolSize") ? Integer.parseInt(bundle.getString("poolSize")) : 1;
			String url = bundle.containsKey("url") ? bundle.getString("url") : "";
			String username = bundle.containsKey("username") ? bundle.getString("username") : "";
			String password = bundle.containsKey("password") ? bundle.getString("password") : "";
			while(pool.size() < poolSize) {
				Connection con = DriverManager.getConnection(url, username, password);
				if (null != con)
					pool.add(con);
				if (conf.getModel().equals("debug"))
					System.out.println("The Current Number of Connections in the pool:" + pool.size());
			}
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static synchronized Connection getConnection() {
		return pool.remove(0);
	}
	
	public static void close(ResultSet rs, Statement stmt, Connection con) {
		try {
			if (rs != null) {
				rs.close();
				rs = null;
			}
			if (stmt != null) {
				stmt.close();
				stmt = null;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		close(con);
	}

	public static void close(Statement stmt, Connection con) {
		close(null, stmt, con);
	}
	
	public static void close(Connection con) {
		if (con != null)
			pool.add(con);
	}
	
	public static void release() {
		if (pool.size() > 0 ) {
			Connection con = getConnection();
			try {
				if (con != null)
					con.close();
			} catch (SQLException e) {
			} finally {
				con = null;
			}
		}
	}
	
	public static Configuration getConf() {
		return conf;
	}
	
}
