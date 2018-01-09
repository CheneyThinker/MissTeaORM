package com.tea.orm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldDecorate {

	boolean prikey() default false;
	boolean inc() default false;
	boolean notNull() default false;
	String dbType() default "VARCHAR";
	String precision() default "20";
	
}
