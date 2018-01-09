package com.tea.orm.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.tea.orm.bean.ColumnInfo;
import com.tea.orm.bean.JavaFieldGetSet;
import com.tea.orm.bean.TableInfo;
import com.tea.orm.factory.TeaConnectionFactory;

public final class JavaFileUtils {

	private static JavaFieldGetSet createFieldGetSetSRC(ColumnInfo column) {
		JavaFieldGetSet jfgs = new JavaFieldGetSet();
		StringBuilder builder = new StringBuilder();
		builder.append("\t@FieldDecorate(dbType = \"").append(column.getDbType())
		.append("\", precision = \"").append(column.getPrecision()).append("\" ");
		if (column.getPrikey())
			builder.append(", prikey = true ");
		if (column.getInc())
			builder.append(", inc = true ");
		if (column.getNotNull())
			builder.append(", notNull = true ");
		builder.append(")");
		builder.append("\n\tprivate ").append(column.getJavaType()).append(" ").append(column.getColumnName()).append(";\n");
		jfgs.setFieldInfo(builder.toString());
		StringBuilder getSrc = new StringBuilder();
		getSrc.append("\tpublic ").append(column.getJavaType()).append(" get").append(StringUtils.firstChar2UpperCase(column.getColumnName())).append("() {\n");
		getSrc.append("\t\treturn ").append(column.getColumnName()).append(";\n\t}\n");
		jfgs.setGetInfo(getSrc.toString());	
		StringBuilder setSrc = new StringBuilder();
		setSrc.append("\tpublic void set").append(StringUtils.firstChar2UpperCase(column.getColumnName())).append("(");
		setSrc.append(column.getJavaType()).append(" ").append(column.getColumnName()).append(") {\n");
		setSrc.append("\t\tthis.").append(column.getColumnName()).append(" = ").append(column.getColumnName()).append(";\n\t}\n");
		jfgs.setSetInfo(setSrc.toString());	
		return jfgs;
	}
	
	private static String createJavaSrc(TableInfo tableInfo) {
		Map<String, ColumnInfo> columns = tableInfo.getColumnInfos();
		List<JavaFieldGetSet> javaFields = new LinkedList<JavaFieldGetSet>();
		for (ColumnInfo c:columns.values())
			javaFields.add(createFieldGetSetSRC(c));
		StringBuilder src = new StringBuilder();
		src.append("package ").append(TeaConnectionFactory.getConf().getPackageName()).append(";\n\n");
		src.append("import com.tea.orm.annotation.FieldDecorate;\n");
		src.append("import com.tea.orm.annotation.TypeDecorate;\n\n");
		src.append("@SuppressWarnings(\"serial\")\n");
		src.append("@TypeDecorate(table = \"").append(tableInfo.getTableName()).append("\" )\n");
		src.append("public class ").append(StringUtils.firstChar2UpperCase(tableInfo.getTableName())).append(" implements java.io.Serializable {\n\n");
		for (JavaFieldGetSet f:javaFields)
			src.append(f.getFieldInfo());
		src.append("\n");
		src.append("\tpublic ").append(StringUtils.firstChar2UpperCase(tableInfo.getTableName())).append("() {\n");
		src.append("\t}\n");
		src.append("\tpublic ").append(StringUtils.firstChar2UpperCase(tableInfo.getTableName())).append("(");
		Iterator<ColumnInfo> iterator = columns.values().iterator();
		StringBuilder construction = new StringBuilder();
		while (iterator.hasNext()) {
			ColumnInfo columnInfo = (ColumnInfo) iterator.next();
			if(columnInfo.getInc())
				continue;
			src.append(columnInfo.getJavaType()).append(" ").append(columnInfo.getColumnName()).append(", ");
			construction.append("\t\tthis.").append(columnInfo.getColumnName()).append(" = ").append(columnInfo.getColumnName()).append(";\n");
		}
		src.deleteCharAt(src.length() - 2);
		src.setCharAt(src.length() - 1, ')');
		src.append(" {\n");
		src.append(construction.toString());
		construction = null;
		src.append("\t}\n");
		for (JavaFieldGetSet f:javaFields)
			src.append(f.getGetInfo());
		for (JavaFieldGetSet f:javaFields)
			src.append(f.getSetInfo());
		src.append("\n}\n");
		return src.toString();
	}

	public static void createJavaPOFile(TableInfo tableInfo) {
		StringBuilder builder = new StringBuilder();
		builder.append(System.getProperty("user.dir")).append(File.separator);
		builder.append("src").append(File.separator).append(TeaConnectionFactory.getConf().getPackageName().toString().replace('.', File.separatorChar).toString());
		File file = new File(builder.toString());
		if (!file.exists())
			file.mkdirs();
		BufferedWriter bw = null;
		try {
			builder.delete(0, builder.length());
			builder.append(file.getAbsoluteFile()).append(File.separator).append(StringUtils.firstChar2UpperCase(tableInfo.getTableName())).append(".java");
			bw = new BufferedWriter(new FileWriter(builder.toString()));
			bw.write(createJavaSrc(tableInfo));
			builder.delete(0, builder.length());
			if (TeaConnectionFactory.getConf().getModel().equals("debug")) {
				System.err.println(file.getPath());
				System.out.println(builder.append("CREATE TABLE['").append(tableInfo.getTableName()).append("']The Corresponding Java Classes:").append(StringUtils.firstChar2UpperCase(tableInfo.getTableName())).append(".java").toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (null != bw) {
					bw.close();
					bw = null;
				} 
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
}
