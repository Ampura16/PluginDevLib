package org.mineacademy.fo.command.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.mineacademy.fo.command.PermsCommand;

/**
 * 用于 {@link PermsCommand} 命令的注解，用法示例见
 * https://github.com/kangarko/PluginTemplate/blob/main/src/main/java/org/mineacademy/template/model/Permissions.java
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface Permission {

	public String value() default "";

	public boolean def() default false;
}
