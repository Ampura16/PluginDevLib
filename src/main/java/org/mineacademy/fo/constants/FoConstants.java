package org.mineacademy.fo.constants;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.plugin.SimplePlugin;

/**
 * 存放本插件的常量
 */
public final class FoConstants {

	/**
	 * 表示全部由 0 组成的 UUID
	 */
	public static final UUID NULL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

	public static final class File {

		/**
		 * 我们的设置文件名
		 */
		public static final String SETTINGS = "settings.yml";

		/**
		 * 自动创建、用于记录错误的错误文件
		 */
		public static final String ERRORS = "error.log";

		/**
		 * 用于记录调试信息的调试文件
		 */
		public static final String DEBUG = "debug.log";

		/**
		 * 用于保存各种数据的数据文件（使用 YAML）
		 */
		public static final String DATA = "data.db";

		/**
		 * 与 ChatControl 插件相关的文件
		 */
		public static final class ChatControl {

			/**
			 * logs/ 文件夹中的 command-spy.log 文件
			 */
			public static final String COMMAND_SPY = "logs/command-spy.log";

			/**
			 * logs/ 文件夹中的聊天日志文件
			 */
			public static final String CHAT_LOG = "logs/chat.log";

			/**
			 * log/s 文件夹中的管理员日志
			 */
			public static final String ADMIN_CHAT = "logs/admin-chat.log";

			/**
			 * logs/ 文件夹中的 bungee 聊天日志文件
			 */
			public static final String BUNGEE_CHAT = "logs/bungee-chat.log";

			/**
			 * logs/ 文件夹中的规则日志文件
			 */
			public static final String RULES_LOG = "logs/rules.log";

			/**
			 * logs/ 文件夹中的控制台日志文件
			 */
			public static final String CONSOLE_LOG = "logs/console.log";

			/**
			 * logs/ 文件夹中记录频道加入与离开的文件
			 */
			public static final String CHANNEL_JOINS = "logs/channel-joins.log";
		}
	}

	public static final class Header {

		/**
		 * 数据文件的文件头
		 *
		 * 可使用 YamlConfig/setHeader() 覆盖它。
		 */
		public static final String[] DATA_FILE = {
				"",
				"This file stores various data you create via the plugin.",
				"",
				" ** THE FILE IS MACHINE GENERATED. PLEASE DO NOT EDIT **",
				""
		};

		/**
		 * 没有默认原型的变量文件所使用的文件头。
		 */
		public static final String[] VARIABLE_FILE = {
				"-------------------------------------------------------------------------------------------------",
				SimplePlugin.getNamed() + " supports dynamic, high performance JavaScript variables! They will",
				"automatically be used when calling Variables#replace for your messages.",
				"",
				"Because variables return a JavaScript value, you can sneak in code to play sounds or spawn",
				"monsters directly in your variable instead of it just displaying text!",
				"",
				"For example of how variables can be used, see our plugin ChatControl's wikipedia article:",
				"https://github.com/kangarko/ChatControl-Red/wiki/JavaScript-Variables",
				" -------------------------------------------------------------------------------------------------",
		};
	}

	public static final class NBT {

		/**
		 * 玩家打开菜单时获得的内部元数据标签。
		 *
		 * <p>
		 * 用于 {@link Menu#getMenu(Player)}
		 */
		public static final String TAG_MENU_CURRENT = SimplePlugin.getNamed() + "_Menu";

		/**
		 * 玩家打开另一个菜单时获得的内部元数据标签。
		 *
		 * <p>
		 * 用于 {@link Menu#getPreviousMenu(Player)}
		 */
		public static final String TAG_MENU_PREVIOUS = SimplePlugin.getNamed() + "_Previous_Menu";

		/**
		 * 玩家关闭我们的菜单时获得的内部元数据标签，
		 * 以便你手动重新打开上次关闭的菜单。
		 *
		 * <p>
		 * 用于 {@link Menu#getLastClosedMenu(Player)}
		 */
		public static final String TAG_MENU_LAST_CLOSED = SimplePlugin.getNamed() + "_Last_Closed_Menu";

		/**
		 * 在旧版 Minecraft 中玩家打开告示牌时获得的内部元数据标签。
		 *
		 * 我们在告示牌更新数据包监听器中使用它来处理告示牌更新。
		 */
		public static final String METADATA_OPENED_SIGN = SimplePlugin.getNamed() + "_OpenedSign";
	}
}
