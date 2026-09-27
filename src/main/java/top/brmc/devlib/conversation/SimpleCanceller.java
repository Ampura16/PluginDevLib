package top.brmc.devlib.conversation;

import java.util.Arrays;
import java.util.List;

import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationCanceller;
import org.bukkit.conversations.ConversationContext;
import top.brmc.devlib.Valid;

/**
 * 简单的对话取消器
 * 如果玩家的消息与列表中的任意单词匹配，其对话将被取消
 */
public final class SimpleCanceller implements ConversationCanceller {

	/**
	 * 触发对话取消的单词
	 */
	private final List<String> cancelPhrases;

	/**
	 * 根据给定字符串创建一个新的对话取消器
	 * 如果玩家的消息与列表中的任意单词匹配，其对话将被取消
	 *
	 * @param cancelPhrases
	 */
	public SimpleCanceller(String... cancelPhrases) {
		this(Arrays.asList(cancelPhrases));
	}

	/**
	 * 根据给定列表创建一个新的对话取消器
	 * 如果玩家的消息与列表中的任意单词匹配，其对话将被取消
	 *
	 * @param cancelPhrases
	 */
	public SimpleCanceller(List<String> cancelPhrases) {
		Valid.checkBoolean(!cancelPhrases.isEmpty(), "Cancel phrases are empty for conversation cancel listener!");

		this.cancelPhrases = cancelPhrases;
	}

	@Override
	public void setConversation(Conversation conversation) {
	}

	/**
	 * 监听取消短语，若匹配则退出
	 */
	@Override
	public boolean cancelBasedOnInput(ConversationContext context, String input) {
		for (final String phrase : this.cancelPhrases)
			if (input.equalsIgnoreCase(phrase))
				return true;

		return false;
	}

	@Override
	public ConversationCanceller clone() {
		return new SimpleCanceller(this.cancelPhrases);
	}
}