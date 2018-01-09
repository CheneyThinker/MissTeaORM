package com.tea.orm.thread;

import java.util.List;

public class BeanAdapter<T> {
	
	public T[][] getComplex() { return null; }
	
	public void fill(T[][] objects) {}
	
	public T[] getSimple() { return null; }
	
	public void fill(T[] objects) {}
	
	public List<T> getLists() { return null; }
	
	public void fill(List<T> lists) {}
	
	public void failure() {}

}
