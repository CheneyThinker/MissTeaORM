package com.tea.orm.utils;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class JDBCUtils {

	public static void handleParams(PreparedStatement pstmt, Object[] params) {
		if(params != null) {
			for (int i = 0; i < params.length; i++) {
				try {
					pstmt.setObject(i + 1, params[i]);
				} catch (SQLException e) {
					return;
				}
			}
		}
	}
	
	public static void handleParams(PreparedStatement pstmt, Object params) {
		if(params != null) {
			try {
				pstmt.setObject(1, params);
			} catch (SQLException e) {
				return;
			}
		}
	}
	
}
