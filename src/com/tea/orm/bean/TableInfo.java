package com.tea.orm.bean;

import java.util.Map;

public class TableInfo {

	private String tableName;
	private Map<String, ColumnInfo> columnInfos;
	
	public TableInfo() {
	}
	public TableInfo(String tableName, Map<String, ColumnInfo>columnInfos) {
		this.tableName = tableName;
		this.columnInfos = columnInfos;
	}
	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	public Map<String, ColumnInfo> getColumnInfos() {
		return columnInfos;
	}
	public void setColumnInfos(Map<String, ColumnInfo>columnInfos) {
		this.columnInfos = columnInfos;
	}

}
