package com.tea.orm.db;

import java.lang.reflect.Field;
import java.util.List;

import com.tea.orm.annotation.FieldDecorate;
import com.tea.orm.annotation.TypeDecorate;
import com.tea.orm.core.TeaQuery;

public abstract class SQLServerQuery extends TeaQuery {

	public <T> List<T> queryGistColumn(Class<?> cls, Integer pageNo, Integer pageSize, String orderColumnName, Sorted sorted) {
		StringBuilder builder = new StringBuilder("SELECT * FROM (SELECT TOP " + pageSize + " * FROM (SELECT TOP " + pageSize * pageNo + " * FROM ");
		builder.append(cls.getAnnotation(TypeDecorate.class).table() + " ORDER BY " + orderColumnName + " ASC) ");
		builder.append("AS T ORDER BY " + orderColumnName + " DESC) AS TT ORDER BY " + orderColumnName + " " + sorted.name());
		return queryRows(builder.toString(), cls);
	}
	
	public <T> List<T> queryGistColumnNotSorted(Class<?> cls, Integer pageNo, Integer pageSize) {
		StringBuilder builder = new StringBuilder("SELECT * FROM (SELECT TOP " + pageSize + " * FROM (SELECT TOP " + pageSize * pageNo + " * FROM ");
		builder.append(cls.getAnnotation(TypeDecorate.class).table() + ") AS T ) AS TT");
		return queryRows(builder.toString(), cls);
	}
	
	public Boolean createSaveUpdateProcedure(Class<?> cls) {
		StringBuilder builder = new StringBuilder("CREATE PROCEDURE SaveUpdate");
		builder.append(cls.getSimpleName());
		builder.append("(");
		Field[] fields = cls.getDeclaredFields();
		int length = fields.length;
		for (int i = 0; i < length; i++) {
			FieldDecorate decorate = fields[i].getAnnotation(FieldDecorate.class);
			builder.append("@");
			builder.append(fields[i].getName());
			builder.append(" ");
			builder.append(decorate.dbType().toUpperCase());
			if (!decorate.dbType().toUpperCase().equals("INT"))
				builder.append("(" + decorate.precision() + ")");
			builder.append(",");
		}
		builder.setCharAt(builder.length() - 1, ')');
		builder.append("\nAS DECLARE ");
		String primary = null;
		for (int i = 0; i < length; i++) {
			FieldDecorate decorate = fields[i].getAnnotation(FieldDecorate.class);
			if (decorate.prikey()) {
				primary = fields[i].getName();
				continue;
			}
			builder.append("@tea");
			builder.append(fields[i].getName());
			builder.append(" ");
			builder.append(decorate.dbType().toUpperCase());
			if (!decorate.dbType().toUpperCase().equals("INT")) {
				builder.append("(");
				builder.append(decorate.precision());
				builder.append(")");
			}
			builder.append(",");
		}
		builder.setCharAt(builder.length() - 1, ' ');
		builder.append("IF EXISTS(SELECT * FROM ");
		builder.append(cls.getAnnotation(TypeDecorate.class).table());
		builder.append(" WHERE ");
		builder.append(primary);
		builder.append("=@");
		builder.append(primary);
		builder.append(")\nBEGIN\nSELECT ");
		for (int i = 0; i < length; i++) {
			FieldDecorate decorate = fields[i].getAnnotation(FieldDecorate.class);
			if (decorate.prikey() || decorate.inc())
				continue;
			builder.append("@tea");
			builder.append(fields[i].getName());
			builder.append("=");
			builder.append(fields[i].getName());
			builder.append(",");
		}
		builder.setCharAt(builder.length() - 1, ' ');
		builder.append("FROM ");
		builder.append(cls.getAnnotation(TypeDecorate.class).table());
		builder.append(" WHERE ");
		builder.append(primary);
		builder.append("=@");
		builder.append(primary);
		builder.append("\nIF (");
		for (int i = 0; i < length; i++) {
			FieldDecorate decorate = fields[i].getAnnotation(FieldDecorate.class);
			if (decorate.prikey())
				continue;
			builder.append("(@tea");
			builder.append(fields[i].getName());
			builder.append("=@");
			builder.append(fields[i].getName());
			if (i < length - 1) {
				builder.append(") AND ");
			} else {
				builder.append("))\n");
			}
		}
		builder.append("BEGIN\nRETURN 0\nEND\nELSE\nBEGIN\nUPDATE ");
		builder.append(cls.getAnnotation(TypeDecorate.class).table());
		builder.append(" SET ");
		for (int i = 0; i < length; i++) {
			FieldDecorate decorate = fields[i].getAnnotation(FieldDecorate.class);
			if (decorate.prikey() || decorate.inc())
				continue;
			builder.append(fields[i].getName());
			builder.append("=@");
			builder.append(fields[i].getName());
			builder.append(",");
		}
		builder.setCharAt(builder.length() - 1, ' ');
		builder.append("WHERE ");
		builder.append(primary);
		builder.append("=@");
		builder.append(primary);
		builder.append("\nRETURN 2\nEND\nEND\nELSE\nBEGIN\nINSERT INTO ");
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
				builder.append("@");
				builder.append(fields[i].getName());
				builder.append(",");
			}
		}
		builder.setCharAt(builder.length() - 1, ')');
		builder.append("\nRETURN 1\nEND");
		return executeDML(builder.toString());
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
	
	public <T> List<T> callProcedureByName(String procedureName, Class<?> cls) {
		return callProcedure("CALL " + procedureName, cls);
	}
	
	public <T> T callProcedureUniqueRowByName(String procedureName, Class<?> cls) {
		return callProcedureUniqueRow("CALL " + procedureName, cls);
	}
	
}
