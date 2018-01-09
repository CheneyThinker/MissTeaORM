package com.tea.orm.utils;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class ReflectUtils {

	public static Object invokeGet(Object obj, String fieldName) {
		Object object = null;
		try {
			Class<?> beanClass = obj.getClass();
			PropertyDescriptor pd = new PropertyDescriptor(fieldName, beanClass);
			Method method = pd.getReadMethod();
			if (pd!=null) {
				object = method.invoke(obj);
			}
		} catch (IntrospectionException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
		return object;
	}
	
	public static void invokeSet(Object obj, String fieldName, Object fieldValue) {
		try {
			if(fieldValue != null) {
				Class<?> beanClass = obj.getClass();
				PropertyDescriptor pd = new PropertyDescriptor(fieldName, beanClass);
				Method m = pd.getWriteMethod();
				m.invoke(obj, fieldValue);
			}
		} catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | IntrospectionException e) {
			e.printStackTrace();
		}
	}
	
	public static Object invokeGet(String fieldName, Object obj) {	
		try {
			Class<?> c = obj.getClass();
			Method m = c.getDeclaredMethod("get" + StringUtils.firstChar2UpperCase(fieldName));
			return m.invoke(obj);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void invokeSet(String fieldName, Object fieldValue, Object obj){
		try {
			if(fieldValue!=null){
				Method m = obj.getClass().getDeclaredMethod("set" + StringUtils.firstChar2UpperCase(fieldName), fieldValue.getClass());
				m.invoke(obj, fieldValue);
			}
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
	}
	
}
