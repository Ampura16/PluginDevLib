package top.brmc.devlib.command.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import top.brmc.devlib.command.PermsCommand;

/**
 * 用于 {@link PermsCommand} 命令的注解，用法示例见
 * https://github.com/kangarko/PluginTemplate/blob/main/src/main/java/org/mineacademy/template/model/Permissions.java
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface PermissionGroup {

	public String value() default "";
}
