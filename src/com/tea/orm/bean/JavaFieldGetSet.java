package com.tea.orm.bean;

public class JavaFieldGetSet {

	private String fieldInfo;
	private String getInfo;
	private String setInfo;
	
	public JavaFieldGetSet() {
	}
	public JavaFieldGetSet(String fieldInfo, String getInfo, String setInfo) {
		this.fieldInfo = fieldInfo;
		this.getInfo = getInfo;
		this.setInfo = setInfo;
	}
	public String getFieldInfo() {
		return fieldInfo;
	}
	public void setFieldInfo(String fieldInfo) {
		this.fieldInfo = fieldInfo;
	}
	public String getGetInfo() {
		return getInfo;
	}
	public void setGetInfo(String getInfo) {
		this.getInfo = getInfo;
	}
	public String getSetInfo() {
		return setInfo;
	}
	public void setSetInfo(String setInfo) {
		this.setInfo = setInfo;
	}
	
}
