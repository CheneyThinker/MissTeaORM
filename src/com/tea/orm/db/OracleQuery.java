package com.tea.orm.db;

import java.util.List;

import com.tea.orm.annotation.TypeDecorate;
import com.tea.orm.core.TeaQuery;

public abstract class OracleQuery extends TeaQuery {
	
	public <T> List<T> querGistColumnNotSorted(Class<?> cls, Integer pageNo, Integer pageSize, String columnName) {
		StringBuilder builder = new StringBuilder();
		builder.append("SELECT *");
		builder.append(" FROM (SELECT " + columnName + " AS ROWNO,T.*");
		builder.append(" FROM " + cls.getAnnotation(TypeDecorate.class).table() + " T");
		builder.append(" WHERE " + columnName + "<=" + (pageNo - 1) * pageSize + ") TABLE_ALIAS");
		builder.append(" WHERE TABLE_ALIAS.ROWNO>=" + pageSize);
		return queryRows(builder.toString(), cls);
	}
	
	public <T> List<T> queryGistColumn(Class<?> cls, Integer pageNo, Integer pageSize, String orderColumnName, Sorted sorted) {
		StringBuilder builder = new StringBuilder();
		builder.append("SELECT *");
		builder.append(" FROM (SELECT TT.*," + orderColumnName + " AS ROWNO");
		builder.append(" FROM (SELECT T.* FROM " + cls.getAnnotation(TypeDecorate.class).table() + " T");
		builder.append(" ORDER BY " + orderColumnName + " " + sorted.name() + ") TT");
		builder.append(" WHERE " + orderColumnName + "<=" + (pageNo - 1) * pageSize + ") TABLE_ALIAS");
		builder.append(" WHERE TABLE_ALIAS.ROWNO>=" + pageSize);
		return queryRows(builder.toString(), cls);
	}

}
