package com.tea.orm.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public interface TeaCallBack {

	Object doExecute(Connection con, PreparedStatement ps, ResultSet rs);
	
}
