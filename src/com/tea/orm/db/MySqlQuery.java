package com.tea.orm.db;

import java.lang.reflect.Field;
import java.util.List;

import com.tea.orm.annotation.FieldDecorate;
import com.tea.orm.annotation.TypeDecorate;
import com.tea.orm.core.TeaQuery;

public abstract class MySqlQuery extends TeaQuery {

	public <T> List<T> queryGistColumn(Class<?> cls, Integer pageNo, Integer pageSize, String orderColumnName, Sorted sorted) {
		StringBuilder builder = new StringBuilder("SELECT * FROM " + cls.getAnnotation(TypeDecorate.class).table() + " WHERE " + orderColumnName + (sorted.ordinal() == 0 ? ">" : "<") + "=");
		builder.append("(SELECT " + orderColumnName + " FROM " + cls.getAnnotation(TypeDecorate.class).table() + " ORDER BY " + orderColumnName + " " + sorted.name() + " ");
		builder.append("LIMIT " + (pageNo - 1 ) * pageSize + ",1) ORDER BY " + orderColumnName + " " + sorted.name() + " LIMIT " + pageSize);
		return queryRows(builder.toString(), cls);
	}

	public <T> List<T> queryGistColumnNotSorted(Class<?> cls, Integer pageNo, Integer pageSize) {
		return queryRows("SELECT * FROM " + cls.getAnnotation(TypeDecorate.class).table() + " LIMIT " + (pageNo - 1) * pageSize + "," + pageSize, cls);
	}
	
	public Boolean createSaveUpdateProcedure(Class<?> cls) {
		StringBuilder builder = new StringBuilder("CREATE PROCEDURE SaveUpdate");
		builder.append(cls.getSimpleName());
		builder.append("(");
		Field[] fields = cls.getDeclaredFields();
		int length = fields.length;
		for (int i = 0; i < length; i++) {
			FieldDecorate decorate = fields[i].getAnnotation(FieldDecorate.class);
			builder.append("IN tea_");
			builder.append(fields[i].getName());
			builder.append(" ");
			builder.append(decorate.dbType().toUpperCase());
			builder.append("(");
			builder.append(decorate.precision());
			builder.append("),");
		}
		String primary = null;
		boolean find = false;
		builder.append("OUT RESULT INTEGER)\nBEGIN\n");
		for (int i = 0; i < length; i++) {
			FieldDecorate decorate = fields[i].getAnnotation(FieldDecorate.class);
			if (decorate.prikey() || decorate.inc()) {
				primary = fields[i].getName();
				builder.append("DECLARE temp_");
				builder.append(primary);
				builder.append(" ");
				builder.append(decorate.dbType().toUpperCase());
				builder.append("(");
				builder.append(decorate.precision());
				builder.append(") DEFAULT ");
				builder.append(decorate.dbType().toUpperCase().equals("INTEGER") ? "0" : "''");
				builder.append(";\nSELECT ");
				builder.append(primary);
				builder.append(" INTO temp_");
				builder.append(primary);
				builder.append(" FROM ");
				builder.append(cls.getAnnotation(TypeDecorate.class).table());
				builder.append(" WHERE ");
				builder.append(primary);
				builder.append("=tea_");
				builder.append(primary);
				builder.append(";\nIF temp_");
				builder.append(primary);
				builder.append("=");
				builder.append(decorate.dbType().toUpperCase().equals("INTEGER") ? "0" : "''");
				builder.append("\nTHEN\n");
				find = true;
				break;
			}
		}
		builder.append("INSERT INTO ");
		builder.append(cls.getAnnotation(TypeDecorate.class).table());
		builder.append("(");
		int index = -1;
		for (int i = 0; i < length; i++) {
			FieldDecorate decorate = fields[i].getAnnotation(FieldDecorate.class);
			if (decorate.inc()) {
				index = i;
				continue;
			}
			builder.append(fields[i].getName());
			builder.append(",");
		}
		builder.setCharAt(builder.length() - 1, ')');
		builder.append(" VALUES(");
		for (int i = 0; i < length; i++) {
			if (i != index) {
				builder.append("tea_");
				builder.append(fields[i].getName());
				builder.append(",");
			}
		}
		builder.setCharAt(builder.length() - 1, ')');
		builder.append(";\nSET RESULT = 1;\n");
		if (!find) {
			builder.append("END");
			return executeDML(builder.toString());
		}
		builder.append("ELSE\nUPDATE ");
		builder.append(cls.getAnnotation(TypeDecorate.class).table());
		builder.append(" SET ");
		for (int i = 0; i < length; i++) {
			FieldDecorate decorate = fields[i].getAnnotation(FieldDecorate.class);
			if (decorate.prikey()) {
				continue;
			}
			builder.append(fields[i].getName());
			builder.append("=tea_");
			builder.append(fields[i].getName());
			builder.append(",");
		}
		builder.setCharAt(builder.length() - 1, ' ');
		builder.append("WHERE ");
		builder.append(primary);
		builder.append("=tea_");
		builder.append(primary);
		builder.append(";\nSET RESULT = 2;\nEND IF;\nEND");
		return executeDML(builder.toString());
	}
	
	public Boolean createSaveProcedure(Class<?> cls, Pattern pattern) {
		StringBuilder builder = new StringBuilder("CREATE PROCEDURE tea_save" + cls.getSimpleName() + "(");
		Field[] fields = cls.getDeclaredFields();
		StringBuilder params = new StringBuilder();
		StringBuilder column = new StringBuilder();
		for (int i = 0; i < fields.length; i++) {
			FieldDecorate decorate = fields[i].getAnnotation(FieldDecorate.class);
			if (decorate.inc())
				continue;
			builder.append(pattern.name() + " ");
			builder.append("tea_" + fields[i].getName() + " " + decorate.dbType() + "(" + decorate.precision() + "),");
			params.append("tea_" + fields[i].getName() + ",");
			column.append(fields[i].getName() + ",");
		}
		column.setCharAt(column.length() - 1, ')');
		params.setCharAt(params.length() - 1, ')');
		builder.setCharAt(builder.length() - 1, ')');
		builder.append(" ");
		builder.append("BEGIN INSERT INTO ." + cls.getAnnotation(TypeDecorate.class).table() + "(" + column.toString() + " VALUES(" + params.toString());
		builder.append("; END");
		return executeDML(builder.toString());
	}
	
	public <T> List<T> callProcedureByName(String procedureName, Class<?> cls) {
		return callProcedure("CALL " + procedureName, cls);
	}
	
	public <T> T callProcedureUniqueRowByName(String procedureName, Class<?> cls) {
		return callProcedureUniqueRow("CALL " + procedureName, cls);
	}
	
	public <T> List<T> callProcedureByName(String procedureName, Class<?> cls, Object param) {
		return callProcedureByName(procedureName, cls, new Object[]{param});
	}
	
	public <T> List<T> callSaveProcedure(Class<?> cls, Object param) {
		return callProcedureByName("tea_save" + cls.getSimpleName(), cls, new Object[]{param});
	}
	
	public <T> List<T> callSaveProcedure(Class<?> cls, List<Object> params) {
		return callProcedureByName("tea_save" + cls.getSimpleName(), cls, params.toArray());
	}
	
	public <T> T callProcedureUniqueRowByName(String procedureName, Class<?> cls, Object param) {
		return callProcedureUniqueRowByName(procedureName, cls, new Object[]{param});
	}

	public <T> List<T> callProcedureByName(String procedureName, Class<?> cls, Object[] params) {
		StringBuilder builder = new StringBuilder("CALL " + procedureName + "(");
		for (int i = 0; i < params.length; i++)
			builder.append("?,");
		builder.setCharAt(builder.length() - 1, ')');
		return callProcedure(builder.toString(), cls, params);
	}
	
	public <T> T callProcedureUniqueRowByName(String procedureName, Class<?> cls, Object[] params) {
		StringBuilder builder = new StringBuilder("CALL " + procedureName + "(");
		for (int i = 0; i < params.length; i++)
			builder.append("?,");
		builder.setCharAt(builder.length() - 1, ')');
		return callProcedureUniqueRow(builder.toString(), cls, params);
	}

	/**
	 * {@link #saveUpdate(String, Object[])}
	 */
	public Integer saveUpdate(String procedureName, List<Object> params) {
		return saveUpdate(procedureName, params.toArray());
	}
	
	/**
	 * @param procedureName saveUpdate plus className Example saveUpdateTea
	 * @param params table field value list
	 * @return 0.NO DO<br/>1.INSERT<br/>2.UPDATE
	 */
	public Integer saveUpdate(String procedureName, Object[] params) {
		StringBuilder builder = new StringBuilder("{CALL ");
		builder.append(procedureName);
		builder.append("(");
		for (int i = 0; i < params.length; i++) {
			builder.append("?,");
		}
		builder.append("?)}");
		return executeProcedureDML(builder.toString(), params);
	}
	
	public Integer saveUpdate(String procedureName, Object entity) {
		return executeProcedureDML(procedureName, entity);
	}
	
	public <T> List<T> callProcedureByName(String procedureName, Class<?> cls, List<Object> params) {
		return callProcedureByName(procedureName, cls, params.toArray());
	}
	
	public <T> T callProcedureUniqueRowByName(String procedureName, Class<?> cls, Object param, List<Object> params) {
		return callProcedureUniqueRowByName(procedureName, cls, params.toArray());
	}

	public static enum Pattern {
		IN,
		OUT,
		INOUT
	}
	
}
