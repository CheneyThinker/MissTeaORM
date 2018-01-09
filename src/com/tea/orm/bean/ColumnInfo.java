package com.tea.orm.bean;

public class ColumnInfo {

	private String columnName;
	private String javaType;
	private String dbType;
	private String precision;
	private Boolean prikey = false;
	private Boolean inc;
	private Boolean notNull;

	public ColumnInfo() {
	}
	public ColumnInfo(String columnName, String javaType, String dbType, String precision, Boolean inc, Boolean notNull) {
		this.columnName = columnName;
		this.javaType = javaType;
		this.dbType = dbType;
		this.precision = precision;
		this.inc = inc;
		this.notNull = notNull;
	}
	public String getColumnName() {
		return columnName;
	}
	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}
	public String getJavaType() {
		return javaType;
	}
	public void setJavaType(String javaType) {
		this.javaType = javaType;
	}
	public String getDbType() {
		return dbType;
	}
	public void setDbType(String dbType) {
		this.dbType = dbType;
	}
	public String getPrecision() {
		return precision;
	}
	public void setPrecision(String precision) {
		this.precision = precision;
	}
	public Boolean getPrikey() {
		return prikey;
	}
	public void setPrikey(Boolean prikey) {
		this.prikey = prikey;
	}
	public Boolean getInc() {
		return inc;
	}
	public void setInc(Boolean inc) {
		this.inc = inc;
	}
	public Boolean getNotNull() {
		return notNull;
	}
	public void setNotNull(Boolean notNull) {
		this.notNull = notNull;
	}

}
