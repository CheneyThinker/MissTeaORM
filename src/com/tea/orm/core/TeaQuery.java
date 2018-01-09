package com.tea.orm.core;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.tea.orm.annotation.FieldDecorate;
import com.tea.orm.annotation.TypeDecorate;
import com.tea.orm.factory.TeaConnectionFactory;
import com.tea.orm.utils.JDBCUtils;
import com.tea.orm.utils.ReflectUtils;

@SuppressWarnings("unchecked")
public abstract class TeaQuery implements Cloneable {

	public <T> T executeQueryTemplate(String strSQL, Object[] params, Class<?> cls, boolean call, TeaCallBack back) {
		Connection con = TeaConnectionFactory.getConnection();
		PreparedStatement ps = null;
		CallableStatement cstmt = null;
		ResultSet rs = null;
		try {
			if (TeaConnectionFactory.getConf().getModel().equals("debug"))
				System.out.println(strSQL);
			if (call) {
				cstmt = con.prepareCall(strSQL);
				JDBCUtils.handleParams(cstmt, params);
				rs = cstmt.executeQuery();
			} else {
				ps = con.prepareStatement(strSQL);
				JDBCUtils.handleParams(ps, params);
				rs = ps.executeQuery();
			}
			return (T) back.doExecute(con, ps, rs);
		} catch (Exception e) {
			return null;
		}finally{
			TeaConnectionFactory.close(rs, call ? cstmt : ps, con);
		}
	}
	
	public Boolean executeDML(String strSQL) {
		return executeDML(strSQL, null);
	}
	
	public Boolean executeDML(String strSQL, Object[] params) {
		Connection con = TeaConnectionFactory.getConnection();
		PreparedStatement ps = null;
		try {
			if (TeaConnectionFactory.getConf().getModel().equals("debug"))
				System.out.println(strSQL);
			ps = con.prepareStatement(strSQL);
			JDBCUtils.handleParams(ps, params);
			return ps.executeUpdate() != 0 ? Boolean.TRUE : Boolean.FALSE;
		} catch (Exception e) {
			return Boolean.FALSE;
		}finally{
			TeaConnectionFactory.close(ps, con);
		}
	}
	
	protected Integer executeProcedureDML(String procedureName, Object entity) {
		Connection con = TeaConnectionFactory.getConnection();
		CallableStatement cstmt = null;
		try {
			Class<?> c = entity.getClass();
			List<Object> params = new LinkedList<Object>();
			StringBuilder strSQL = new StringBuilder("{CALL ");
			strSQL.append(procedureName);
			strSQL.append("(");
			Field[] fs = c.getDeclaredFields();
			for (Field f : fs) {
				strSQL.append("?,");
				params.add(ReflectUtils.invokeGet(entity, f.getName()));
			}
			strSQL.append("?)}");
			cstmt = con.prepareCall(strSQL.toString());
			if (TeaConnectionFactory.getConf().getModel().equals("debug"))
				System.out.println(strSQL.toString());
			JDBCUtils.handleParams(cstmt, params.toArray());
			cstmt.registerOutParameter(params.size() + 1, java.sql.Types.INTEGER);
			cstmt.execute();
			return cstmt.getInt(params.size() + 1);
		} catch (Exception e) {
			return -1;
		}finally{
			TeaConnectionFactory.close(cstmt, con);
		}
	}
	
	protected Integer executeProcedureDML(String strSQL, Object[] params) {
		Connection con = TeaConnectionFactory.getConnection();
		CallableStatement cstmt = null;
		try {
			if (TeaConnectionFactory.getConf().getModel().equals("debug"))
				System.out.println(strSQL);
			cstmt = con.prepareCall(strSQL);
			JDBCUtils.handleParams(cstmt, params);
			cstmt.registerOutParameter(params.length + 1, java.sql.Types.INTEGER);
			cstmt.execute();
			return cstmt.getInt(params.length + 1);
		} catch (Exception e) {
			return -1;
		}finally{
			TeaConnectionFactory.close(cstmt, con);
		}
	}
	
	public Boolean save(Object entity) {
		Class<?> c = entity.getClass();
		List<Object> params = new LinkedList<Object>();
		StringBuilder strSQL = new StringBuilder("INSERT INTO ");
		strSQL.append(c.getAnnotation(TypeDecorate.class).table()).append("(");
		int countNotNullField = 0;
		Field[] fs = c.getDeclaredFields();
		for (Field f : fs) {
			FieldDecorate fieldDecorate = f.getAnnotation(FieldDecorate.class);
			if(fieldDecorate.inc())
				continue;
			Object fieldValue = ReflectUtils.invokeGet(entity, f.getName());
			if (fieldDecorate.notNull() || null != fieldValue) {
				countNotNullField++;
				strSQL.append(f.getName()).append(",");
				params.add(fieldValue);
			}
		}
		if (countNotNullField==0)
			return Boolean.FALSE;
		strSQL.setCharAt(strSQL.length() - 1, ')');
		strSQL.append(" VALUES(");
		for (int i = 0; i < countNotNullField; i++)
			strSQL.append("?,");
		strSQL.setCharAt(strSQL.length() - 1, ')');
		return executeDML(strSQL.toString(), params.toArray());
	}
	
	public Boolean delete(Class<?> cls, Serializable priKey) {
		Field[] fields = cls.getDeclaredFields();
		StringBuilder builder = new StringBuilder();
		for (Field field : fields) {
			if(field.getAnnotation(FieldDecorate.class).prikey()) {
				builder.append("DELETE FROM ").append(cls.getAnnotation(TypeDecorate.class).table()).append(" WHERE ").append(field.getName()).append("=?");
				break;
			}
		}
		return executeDML(builder.toString(), new Object[]{priKey});
	}
	
	public Boolean delete(Object entity) {
		Class<?> c = entity.getClass();
		Field[] fields = c.getDeclaredFields();
		StringBuilder builder = new StringBuilder();
		Object priKeyValue = null;
		for (Field field : fields) {
			if(field.getAnnotation(FieldDecorate.class).prikey()) {
				builder.append("DELETE FROM ").append(c.getAnnotation(TypeDecorate.class).table()).append(" WHERE ").append(field.getName()).append("=?");
				priKeyValue = ReflectUtils.invokeGet(entity, field.getName());
				break;
			}
		}
		return executeDML(builder.toString(), new Object[]{priKeyValue});
	}

	public Boolean update(Object entity, String[] fieldNames) {
		Class<?> c = entity.getClass();
		List<Object> params = new LinkedList<Object>();
		String key = null;
		Object priKeyValue = null;
		Field[] fields = c.getDeclaredFields();
		for (Field field : fields) {
			if(field.getAnnotation(FieldDecorate.class).prikey()) {
				key = field.getName();
				priKeyValue = ReflectUtils.invokeGet(entity, key);
				break;
			}
		}
		StringBuilder strSQL = new StringBuilder("UPDATE ").append(c.getAnnotation(TypeDecorate.class).table()).append(" SET ");
		for(String fname : fieldNames) {
			Object fvalue = ReflectUtils.invokeGet(entity, fname);
			params.add(fvalue);
			strSQL.append(fname).append("=?,");
		}
		strSQL.setCharAt(strSQL.length() - 1, ' ');
		strSQL.append("WHERE ");
		strSQL.append(key).append("=?");
		params.add(priKeyValue);
		key = null;
		return executeDML(strSQL.toString(), params.toArray());
	}
	
	public Boolean update(Object entity) {
		Class<?> c = entity.getClass();
		List<Object> params = new LinkedList<Object>();
		String key = null;
		Object priKeyValue = null;
		Field[] fields = c.getDeclaredFields();
		StringBuilder strSQL = new StringBuilder("UPDATE ").append(c.getAnnotation(TypeDecorate.class).table()).append(" SET ");
		for (Field field : fields) {
			if(field.getAnnotation(FieldDecorate.class).prikey()) {
				key = field.getName();
				priKeyValue = ReflectUtils.invokeGet(entity, key);
				continue;
			}
			Object fvalue = ReflectUtils.invokeGet(entity, field.getName());
			if (fvalue != null || field.getAnnotation(FieldDecorate.class).notNull()) {
				params.add(fvalue);
				strSQL.append(field.getName()).append("=?,");
			}
		}
		strSQL.setCharAt(strSQL.length() - 1, ' ');
		strSQL.append("WHERE ");
		strSQL.append(key).append("=?");
		params.add(priKeyValue);
		return executeDML(strSQL.toString(), params.toArray());
	}
	
	public <T> List<T> query(String strSQL, Class<?> cls, Object params, boolean call) {
		return query(strSQL, cls, new Object[]{params}, call);
	}
	
	public <T> List<T> query(String strSQL, final Class<?> cls, Object[] params, boolean call) {
		return executeQueryTemplate(strSQL, params, cls, call, new TeaCallBack() {
			
			public List<Object> doExecute(Connection con, PreparedStatement ps, ResultSet rs) {
				List<Object> list = null;
				try {
					ResultSetMetaData metaData = rs.getMetaData();
					while(rs.next()){
						if(list == null)
							list = new LinkedList<Object>();
						Object rowObj = cls.newInstance();
						for(int i = 0;i < metaData.getColumnCount(); i++)
							ReflectUtils.invokeSet(rowObj, metaData.getColumnLabel(i + 1), rs.getObject(i + 1));
						list.add(rowObj);
					}
				} catch (Exception e) {
					return list = null;
				} 
				return list;
			}
		});
	}

	public <T> T queryUnique(String strSQL, final Class<?> cls, Object[] params, boolean call) {
		return executeQueryTemplate(strSQL, params, cls, call, new TeaCallBack() {
			
			public Object doExecute(Connection con, PreparedStatement ps, ResultSet rs) {
				Object object = null;
				try {
					ResultSetMetaData metaData = rs.getMetaData();
					while(rs.next()){
						object = cls.newInstance();
						for(int i = 0;i < metaData.getColumnCount(); i++)
							ReflectUtils.invokeSet(object, metaData.getColumnLabel(i + 1), rs.getObject(i + 1));
					}
				} catch (Exception e) {
					return object = null;
				} 
				return object;
			}
		});
	}
	
	public String handler(Class<?> cls) {
		Field[] fields = cls.getDeclaredFields();
		int index = 0;
		for (int i = 0; i < fields.length; i++) {
			if (fields[i].getAnnotation(FieldDecorate.class).prikey()) {
				index = i;
				break;
			}
			if (fields[i].getAnnotation(FieldDecorate.class).notNull())
				index = i;
		}
		return fields[index].getName();
	}

	public <T> T queryUniqueByPriKey(Class<?> cls, Serializable priKey) {
		Field[] fields = cls.getDeclaredFields();
		String key = null;
		for (Field field : fields) {
			if(field.getAnnotation(FieldDecorate.class).prikey()) {
				key = field.getName();
				break;
			}
		}
		StringBuilder strSQL = new StringBuilder("SELECT * FROM ");
		strSQL.append(cls.getAnnotation(TypeDecorate.class).table())
		.append(" WHERE ").append(key).append("=?");
		return queryUnique(strSQL.toString(), cls, new Object[]{priKey}, false);
	}
	
	public <T> T queryUniqueColumnValue(String strSQL, Object[] params, boolean call) {
		return executeQueryTemplate(strSQL, params, null, call, new TeaCallBack() {
			
			public Object doExecute(Connection con, PreparedStatement ps, ResultSet rs) {
				Object value = null;
				try {
					while(rs.next())
						value = rs.getObject(1);
				} catch (SQLException e) {
					return value = null;
				}
				return value;
			}
		});
	}
	
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public Boolean executeDMLS(List<String> strSQLS) {
		Connection con = TeaConnectionFactory.getConnection();
		PreparedStatement ps = null;
		try {
			con.setAutoCommit(false);
			int size = strSQLS.size();
			for (int i = 0; i < size; i++) {
				if (TeaConnectionFactory.getConf().getModel().equals("debug"))
					System.out.println(strSQLS.get(i));
				ps = con.prepareStatement(strSQLS.get(i));
				ps.addBatch();
				ps.executeBatch();
			}
			con.commit();
			ps.clearBatch();
			con.setAutoCommit(true);
			return Boolean.TRUE;
		} catch (SQLException e) {
			try {
				con.rollback();
				con.setAutoCommit(true);
			} catch (SQLException ex) {
				return Boolean.FALSE;
			}
			return Boolean.FALSE;
		} finally {
			TeaConnectionFactory.close(ps, con);
		}
	}
	
	public Boolean executeDMLS(List<String> strSQLS, List<Object[]> params) {
		Connection con = TeaConnectionFactory.getConnection();
		PreparedStatement ps = null;
		try {
			con.setAutoCommit(false);
			int size = strSQLS.size();
			for (int i = 0; i < size; i++) {
				if (TeaConnectionFactory.getConf().getModel().equals("debug"))
					System.out.println(strSQLS.get(i));
				ps = con.prepareStatement(strSQLS.get(i));
				JDBCUtils.handleParams(ps, params.get(i));
				ps.addBatch();
				ps.executeBatch();
			}
			con.commit();
			ps.clearBatch();
			con.setAutoCommit(true);
			return Boolean.TRUE;
		} catch (SQLException e) {
			try {
				con.rollback();
				con.setAutoCommit(true);
			} catch (SQLException ex) {
				return Boolean.FALSE;
			}
			return Boolean.FALSE;
		} finally {
			TeaConnectionFactory.close(ps, con);
		}
	}
	
	public <T> T[][] executeQuery(String strSQL) {
		Connection con = TeaConnectionFactory.getConnection();
		Statement stmt = null;
		ResultSet rs = null;
		Object[][] object = null;
		try {
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			rs = stmt.executeQuery(strSQL);
			rs.last();
			object = new Object[rs.getRow()][];
			rs.first();
			rs.beforeFirst();
			int k=0;
			int columnCount = rs.getMetaData().getColumnCount();
			while(rs.next()) {
				Object[] row = new Object[columnCount];
				for(int i=0; i < columnCount; i++)
					row[i] = rs.getObject(i + 1);
				object[k] = row;
				k++;
			}
		} catch (SQLException e) {
			return null;
		} finally {
			TeaConnectionFactory.close(rs, stmt, con);
		}
		return (T[][]) object;
	}
	
	public <T> List<T> executeQueryLists(String strSQL) {
		Connection con = TeaConnectionFactory.getConnection();
		Statement stmt = null;
		ResultSet rs = null;
		List<Object> lists = new LinkedList<Object>();
		try {
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			rs = stmt.executeQuery(strSQL);
			int columnCount = rs.getMetaData().getColumnCount();
			while(rs.next())
				for(int i=0; i < columnCount; i++)
					lists.add(rs.getObject(i + 1));
		} catch (SQLException e) {
			return null;
		} finally {
			TeaConnectionFactory.close(rs, stmt, con);
		}
		return (List<T>) lists;
	}
	
	/**
	 * @param strSQL "CREATE TABLE tea( id INT PRIMARY KEY,name VARCHAR(20),password VARCHAR(20) );"<br/>
	 * @param tableName "tea"<br/>
	 * no contain DROP TABLE IF EXISTS "tea"
	 * @return Successful Boolean.TRUE Failed Boolean.FALSE
	 */
	public Boolean createTable(String strSQL) {
		return executeDML(strSQL);
	}
	
	/**
	 * @param strSQL "CREATE TABLE tea( id INT PRIMARY KEY,name VARCHAR(20),password VARCHAR(20) );"<br/>
	 * @param tableName "tea"<br/>
	 * contain DROP TABLE IF EXISTS "tea"
	 * @return Successful Boolean.TRUE Failed Boolean.FALSE
	 */
	public Boolean createTable(String strSQL, String tableName) {
		List<String> strSQLs = new ArrayList<String>();
		strSQLs.add("DROP TABLE IF EXISTS " + tableName + ";");
		strSQLs.add(strSQL);
		return executeDMLS(strSQLs);
	}
	
	/**
	 * 
	 * @param tableName "tea"
	 * @param fields
	 * fields.add("id INT PRIMARY KEY")<br/>
	 * fields.add("name VARCHAR(20)")<br/>
	 * fields.add("password VARCHAR(20)")<br/>
	 * contain DROP TABLE IF EXISTS "tea"<br/>
	 * {@link #createTable(String tableName, String[] fields)}
	 * @return Successful Boolean.TRUE Failed Boolean.FALSE
	 */
	public Boolean createTable(String tableName, List<String> fields) {
		return createTable(tableName, (String[]) fields.toArray());
	}
	
	/**
	 * @param tableName "tea"
	 * @param fields
	 * fields[0] = "id INT PRIMARY KEY"<br/>
	 * fields[1] = "name VARCHAR(20)"<br/>
	 * fields[2] = "password VARCHAR(20)"<br/>
	 * contain DROP TABLE IF EXISTS "tea"<br/>
	 * {@link #executeDMLS(List strSQLS)}
	 * @return Successful Boolean.TRUE Failed Boolean.FALSE
	 */
	public Boolean createTable(String tableName, String[] fields) {
		List<String> strSQLs = new ArrayList<String>();
		strSQLs.add("DROP TABLE IF EXISTS " + tableName + ";");
		StringBuilder strSQL = new StringBuilder("CREATE TABLE ").append(tableName).append("( ");
		for (int i = 0; i < fields.length; i++)
			strSQL.append(fields[i]).append(",");
		strSQL.setCharAt(strSQL.length() - 1, ' ');
		strSQL.append(");");
		strSQLs.add(strSQL.toString());
		return executeDMLS(strSQLs);
	}
	
	/**
	 * @param tableName "tea"
	 * {@link #executeDML(String strSQL)}
	 * @return Successful Boolean.TRUE Failed Boolean.FALSE
	 */
	public Boolean dropTable(String tableName) {
		return executeDML("DROP TABLE IF EXISTS " + tableName + ";");
	}
	
	/**
	 * @param procedureSQL (MYSQL)->"CREATE PROCEDURE teaBrowse(IN ID INT) "<br>
	 * 						"BEGIN DECLARE tea_ID INT;"</br>
	 *						"SET tea_ID=ID;"</br>
	 *						"SELECT id,name,password FROM tea WHERE id=tea_ID;"</br>
	 *						"END;"<br/>
	 * @param procedureName "teaBrowse"<br/>
	 * no contain DROP PROCEDURE IF EXISTS "teaBrowse"<br/>
	 * {@link #executeDML(String strSQL)}
	 * @return Successful Boolean.TRUE Failed Boolean.FALSE
	 */
	public Boolean createProcedure(String procedureSQL) {
		return executeDML(procedureSQL);
	}
	
	/**
	 * @param procedureSQL (MYSQL)->"CREATE PROCEDURE teaBrowse(IN ID INT) "<br>
	 * 						"BEGIN DECLARE tea_ID INT;"</br>
	 *						"SET tea_ID=ID;"</br>
	 *						"SELECT id,name,password FROM tea WHERE id=tea_ID;"</br>
	 *						"END;"<br/>
	 * @param procedureName "teaBrowse"<br/>
	 * contain DROP PROCEDURE IF EXISTS "teaBrowse"<br/>
	 * {@link #executeDMLS(List strSQLS)}
	 * @return Successful Boolean.TRUE Failed Boolean.FALSE
	 */
	public Boolean createProcedure(String procedureSQL, String procedureName) {
		List<String> strSQL = new ArrayList<String>();
		strSQL.add("DROP PROCEDURE IF EXISTS " + procedureName + ";");
		strSQL.add(procedureSQL);
		return executeDMLS(strSQL);
	}
	
	public Boolean alterProcedure(String procedureSQL, String procedureName) {
		return createProcedure(procedureSQL, procedureName);
	}

	public Boolean dropProcedure(String procedureName) {
		return executeDML("DROP PROCEDURE IF EXISTS " + procedureName + ";");
	}

	public <T> List<T> callProcedure(String strSQL, Class<?> cls) {
		return query(strSQL, cls, null, true);
	}
	
	public <T> T callProcedureUniqueRow(String strSQL, Class<?> cls) {
		return queryUnique(strSQL, cls, null, true);
	}
	
	public <T> List<T> callProcedure(String strSQL, Class<?> cls, Object param) {
		return callProcedure(strSQL, cls, new Object[]{param});
	}
	
	public <T> T callProcedureUniqueRow(String strSQL, Class<?> cls, Object param) {
		return callProcedureUniqueRow(strSQL, cls, new Object[]{param});
	}
	
	public <T> List<T> callProcedure(String strSQL, Class<?> cls, Object[] params) {
		return query(strSQL, cls, params, true);
	}
	
	public <T> T callProcedureUniqueRow(String strSQL, Class<?> cls, Object[] params) {
		return queryUnique(strSQL, cls, params, true);
	}
	
	public <T> List<T> callProcedure(String strSQL, Class<?> cls, List<Object> params) {
		return query(strSQL, cls, params.toArray(), true);
	}
	
	public <T> T callProcedureUniqueRow(String strSQL, Class<?> cls, List<Object> params) {
		return callProcedureUniqueRow(strSQL, cls, params.toArray());
	}
	
	public <T> List<T> queryRows(String strSQL, Class<?> cls, Object[] params) {
		return query(strSQL, cls, params, false);
	}
	
	public <T> List<T> queryRows(String strSQL, Class<?> cls) {
		return query(strSQL, cls, null, false);
	}
	
	public <T> List<T> queryRows(Class<?> cls) {
		return query("SELECT * FROM " + cls.getAnnotation(TypeDecorate.class).table(), cls, null, false);
	}
	
	public <T> T queryUniqueRow(String strSQL, Class<?> cls, Object[] params) {
		return queryUnique(strSQL, cls, params, false);
	}
	
	public Number queryNumber(String strSQL, Object[] params, boolean call) {
		return (Number)queryUniqueColumnValue(strSQL, params, call);
	}
	
	public Integer count(String strSQL, Object[] params) {
		return queryNumber(strSQL, params, false).intValue();
	}
	
	public <T> T load(Class<?> cls, Serializable priKey) {
		return queryUniqueByPriKey(cls, priKey);
	}
	
	public <T> List<T> queryByPriKeyOrUniqueNotNull(Class<?> cls, Integer pageNo, Integer pageSize, Sorted sorted) {
		return queryGistColumn(cls, pageNo, pageSize, handler(cls), sorted);
	}
	
	public <T> List<T> queryByPriKeyOrUniqueNotNull(Class<?> cls, Integer pageNo, Integer pageSize) {
		return queryGistColumn(cls, pageNo, pageSize, handler(cls), Sorted.DESC);
	}
	
	public <T> List<T> queryGistColumn(Class<?> cls, Integer pageNo, Integer pageSize, String orderColumnName) {
		return queryGistColumn(cls, pageNo, pageSize, orderColumnName, Sorted.DESC);
	}

	public abstract <T> List<T> queryGistColumn(Class<?> cls, Integer pageNo, Integer pageSize, String orderColumnName, Sorted sorted);

	public enum Sorted {
		ASC,
		DESC
	}
	
}
