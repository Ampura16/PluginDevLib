package top.brmc.devlib.conversation;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationPrefix;
import top.brmc.devlib.Common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 使用静态字符串的简单对话前缀
 */
@RequiredArgsConstructor
public final class SimplePrefix implements ConversationPrefix {

	/**
	 * 对话前缀
	 */
	@Getter
	private final String prefix;

	@Override
	public String getPrefix(ConversationContext context) {
		return Common.colorize(this.prefix);
	}
}