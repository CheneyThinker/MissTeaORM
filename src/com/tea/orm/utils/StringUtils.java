package com.tea.orm.utils;

public final class StringUtils {

	public static String firstChar2UpperCase(String str) {
		return Character.toUpperCase(str.charAt(0)) + str.substring(1);
	}
	
}
