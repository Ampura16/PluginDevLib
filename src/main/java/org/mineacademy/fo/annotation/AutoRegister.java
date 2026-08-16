package org.mineacademy.fo.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 将此注解放在以下任一类上，让 Foundation
 * 在插件启动时自动注册它，并正确重载。
 *
 * 支持的类：
 * - SimpleListener
 * - PacketListener
 * - BungeeListener
 * - DiscordListener
 * - SimpleCommand
 * - SimpleCommandGroup
 * - SimpleExpansion
 * - YamlConfig（我们将在插件启动时加载你的配置并正确重载）
 * - 任何 "implements Listener" 的类
 *
 * 此外，以下类无论你是否放置此注解
 * 都会自动自我注册：
 * - Tool（及其派生类，如 Rocket）
 * - SimpleEnchantment
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface AutoRegister {

	/**
	 * 为 false 时，我们不会打印控制台警告，例如注册失败
	 * 是因为服务器 MC 版本过旧（例如 SimpleEnchantment），或缺少
	 * 要对接的必要插件（例如 DiscordListener、PacketListener）
	 *
	 * @return
	 */
	boolean hideIncompatibilityWarnings() default false;
}
