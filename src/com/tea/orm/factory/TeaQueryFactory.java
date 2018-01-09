package com.tea.orm.factory;

import com.tea.orm.core.TeaQuery;

@SuppressWarnings("unchecked")
public final class TeaQueryFactory {

	private static TeaQuery prototypeObj;
	
	static {
		try {
			Class<?> c = Class.forName(TeaConnectionFactory.getConf().getQueryClass());
			prototypeObj = (TeaQuery) c.newInstance();
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	private TeaQueryFactory(){}
	
	public static <T> T createQuery() {
		try {
			return (T) prototypeObj.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
}
