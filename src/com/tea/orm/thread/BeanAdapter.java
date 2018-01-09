package com.tea.orm.thread;

import java.util.List;

public class BeanAdapter {
	
	public <U> U[][] getComplex() { return null; }
	
	public void fill(Object[][] objects) {}
	
	public <U> U[] getSimple() { return null; }
	
	public void fill(Object[] objects) {}
	
	public <T> List<T> getLists() { return null; }
	
	public <T> void fill(List<T> lists) {}
	
	public void failure() {}

}
