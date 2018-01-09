package com.tea.orm.factory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.tea.orm.bean.ColumnInfo;
import com.tea.orm.bean.TableInfo;
import com.tea.orm.utils.JavaFileUtils;

public final class TeaJavaFileFactory {

	private static Map<String, TableInfo> tables = new HashMap<String, TableInfo>();
	
	public static void createJavaFiles() {
		build();
		generateJavaFiles();
	}
	
	public static void createJavaFiles(String tableName) {
		build(tableName);
		generateJavaFiles();
	}
	
	private static void generateJavaFiles() {
		Iterator<TableInfo> iterator = tables.values().iterator();
		while (iterator.hasNext()) {
			TableInfo ti = iterator.next();
			JavaFileUtils.createJavaPOFile(ti);
		}
		tables = null;
	}
	
	private static void build() {
		Connection con = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		try {
			con = TeaConnectionFactory.getConnection();
			DatabaseMetaData dbmd = con.getMetaData();
			rs = dbmd.getTables(null, "%", "%", new String[]{"TABLE"});
			while (rs.next()) {
				String tableName = rs.getString("TABLE_NAME");
				internal(ps, con, tableName, dbmd);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			TeaConnectionFactory.close(rs, ps, con);
		}
	}
	
	private static void build(String tableName) {
		Connection con = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		try {
			con = TeaConnectionFactory.getConnection();
			DatabaseMetaData dbmd = con.getMetaData();
			internal(ps, con, tableName, dbmd);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			TeaConnectionFactory.close(rs, ps, con);
		}
	}
	
	private static void internal(PreparedStatement ps, Connection con, String tableName, DatabaseMetaData dbmd) throws SQLException {
		ps = con.prepareStatement("SELECT * FROM " + tableName);
		ResultSet rs = ps.executeQuery();
		ResultSetMetaData rsmd = rs.getMetaData();
		rs.next();
		int columnCount = rsmd.getColumnCount();
		Map<String, ColumnInfo> columnInfos = new HashMap<String, ColumnInfo>();
		TableInfo ti = new TableInfo(tableName, columnInfos);
		tables.put(tableName, ti);
		for (int i = 1; i <= columnCount; i++) {
			String columnLabel = rsmd.getColumnLabel(i);
			String string = rsmd.getColumnClassName(i);
			if (string.split("\\.").length == 3 && string.startsWith("java.lang"))
				string = string.substring(string.lastIndexOf('.') + 1);
			int precision = rsmd.getPrecision(i);
			if (string.equals("java.sql.Timestamp")) {
				string = "java.util.Date";
				precision = 6;
			}
			ColumnInfo ci = new ColumnInfo(columnLabel, string, rsmd.getColumnTypeName(i), precision + (rsmd.getScale(i) == 0 ? "" : "," + rsmd.getScale(i)), rsmd.isAutoIncrement(i), rsmd.isNullable(i) == 0);
			columnInfos.put(columnLabel, ci);
			string = null;
		}
		ResultSet set = dbmd.getPrimaryKeys(null, null, tableName);
		while (set.next()) {
			ColumnInfo ci = columnInfos.get(set.getString("COLUMN_NAME"));
			ci.setPrikey(true);
		}
		TeaConnectionFactory.close(set, null, null);
	}
	
}
