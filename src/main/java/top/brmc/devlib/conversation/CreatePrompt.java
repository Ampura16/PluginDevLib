package top.brmc.devlib.conversation;

import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import top.brmc.devlib.ChatUtil;
import top.brmc.devlib.Valid;

/**
 * 表示菜单中用于创建新对象的简单提示。
 *
 * @param <T>
 */
public abstract class CreatePrompt<T> extends SimplePrompt {

	/**
	 * 有效的名称模式。
	 */
	private static final Pattern ENGLISH_ONLY_PATTERN = Pattern.compile("^[a-zA-Z0-9_ ]+$");

	/**
	 * 我们要创建的对象类型，例如 "region"、"Boss" 等。这不是实际的名称。
	 */
	private final String objectName;

	/**
	 * Boss 的名称、区域的名称等。
	 */
	private String name;

	/**
	 * 创建一个新提示
	 *
	 * @param objectName
	 */
	protected CreatePrompt(String objectName) {
		super(false);

		this.objectName = objectName;
	}

	/* ------------------------------------------------------------------------------- */
	/* Core functions */
	/* ------------------------------------------------------------------------------- */

	/**
	 * 根据给定名称创建对象。
	 *
	 * @param name
	 * @return
	 */
	protected abstract T create(String name);

	/**
	 * 按名称返回已存在的对象，用于防止对象重复
	 *
	 * @param name
	 * @return
	 */
	protected abstract String findByName(String name);

	/**
	 * 向玩家显示菜单
	 *
	 * @param player
	 * @param createdItem
	 */
	protected abstract void onCreateFinish(Player player, T createdItem);

	/**
	 * 名称中是否允许空格？
	 *
	 * @return
	 */
	protected boolean allowSpaces() {
		return false;
	}

	/* ------------------------------------------------------------------------------- */
	/* Final methods */
	/* ------------------------------------------------------------------------------- */

	/**
	 * @see top.brmc.devlib.conversation.SimplePrompt#getPrompt(org.bukkit.conversations.ConversationContext)
	 */
	@Override
	protected final String getPrompt(ConversationContext context) {
		return "Please type your " + this.objectName + " name to chat to create it. Use English only alphabet.";
	}

	/**
	 * @see top.brmc.devlib.conversation.SimplePrompt#isInputValid(org.bukkit.conversations.ConversationContext, java.lang.String)
	 */
	@Override
	protected final boolean isInputValid(ConversationContext context, String input) {
		if (input.contains(" ") && !this.allowSpaces())
			return false;

		return ENGLISH_ONLY_PATTERN.matcher(input).matches() && input.length() >= 3 && input.length() <= 24 && this.findByName(input) == null;
	}

	/**
	 * @see top.brmc.devlib.conversation.SimplePrompt#getFailedValidationText(org.bukkit.conversations.ConversationContext, java.lang.String)
	 */
	@Override
	protected final String getFailedValidationText(ConversationContext context, String invalidInput) {

		@Nullable
		final String existing = this.findByName(invalidInput);
		final String name = ChatUtil.capitalize(this.objectName);

		if (existing != null)
			return name + " named '" + existing + "' already exists!";

		if (invalidInput.length() < 3)
			return name + " name must be at least 3 letters long!";

		if (invalidInput.length() > 24)
			return name + " name cannot be longer than 24 letters!";

		return name + " name contains invalid letters! Use English only alphabet without spaces.";
	}

	/**
	 * @see org.bukkit.conversations.ValidatingPrompt#acceptValidatedInput(org.bukkit.conversations.ConversationContext, java.lang.String)
	 */
	@Override
	protected final Prompt acceptValidatedInput(ConversationContext context, String input) {
		this.name = input;

		return END_OF_CONVERSATION;
	}

	/**
	 * @see top.brmc.devlib.conversation.SimplePrompt#onConversationEnd(top.brmc.devlib.conversation.SimpleConversation, org.bukkit.conversations.ConversationAbandonedEvent)
	 */
	@Override
	public final void onConversationEnd(SimpleConversation conversation, ConversationAbandonedEvent event) {
		if (event.gracefulExit()) {
			Valid.checkNotNull(this.name, "Prompt failed to carry " + this.objectName + " name");

			this.onCreateFinish(this.getPlayer(event.getContext()), this.create(this.name));
		}
	}
}
