package top.brmc.devlib.conversation;

import java.util.function.Consumer;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import top.brmc.devlib.Valid;
import top.brmc.devlib.settings.SimpleLocalization;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

/**
 * 只接受整数或小数的提示
 */
@NoArgsConstructor
@AllArgsConstructor
public class SimpleDecimalPrompt extends SimplePrompt {

	/**
	 * 可以直接在构造器中设置的问题
	 */
	@Setter(value = AccessLevel.PROTECTED)
	private String question = null;

	/**
	 * 输入数字后执行的操作
	 */
	@Setter(value = AccessLevel.PROTECTED)
	private Consumer<Double> successAction;

	/**
	 * 创建一个只带问题的新提示
	 *
	 * @param question
	 */
	public SimpleDecimalPrompt(String question) {
		this(question, null);
	}

	/**
	 * 菜单问题
	 *
	 * @see top.brmc.devlib.conversation.SimplePrompt#getPrompt(org.bukkit.conversations.ConversationContext)
	 */
	@Override
	protected String getPrompt(final ConversationContext ctx) {
		Valid.checkNotNull(this.question, "Please either call setQuestion or override getPrompt");

		return this.question;
	}

	/**
	 * 若输入是有效数字则返回 true
	 *
	 * @see top.brmc.devlib.conversation.SimplePrompt#isInputValid(org.bukkit.conversations.ConversationContext, java.lang.String)
	 */
	@Override
	protected boolean isInputValid(final ConversationContext context, final String input) {
		return Valid.isDecimal(input);
	}

	/**
	 * 输入不是数字时显示的消息
	 *
	 * @see top.brmc.devlib.conversation.SimplePrompt#getFailedValidationText(org.bukkit.conversations.ConversationContext, java.lang.String)
	 */
	@Override
	protected String getFailedValidationText(final ConversationContext context, final String invalidInput) {
		return SimpleLocalization.Commands.INVALID_NUMBER.replace("{input}", invalidInput);
	}

	/**
	 * 解析输入
	 *
	 * @see org.bukkit.conversations.ValidatingPrompt#acceptValidatedInput(org.bukkit.conversations.ConversationContext, java.lang.String)
	 */
	@Override
	protected final Prompt acceptValidatedInput(@NonNull final ConversationContext context, @NonNull final String input) {
		return this.acceptValidatedInput(context, Double.parseDouble(input));
	}

	/**
	 * 输入数字后执行的操作
	 *
	 * @param context
	 * @param input
	 * @return 下一个提示，或返回 {@link Prompt#END_OF_CONVERSATION}（实际上就是 null）以退出
	 */
	protected Prompt acceptValidatedInput(final ConversationContext context, final double input) {
		if (this.successAction != null)
			this.successAction.accept(input);

		else
			this.onValidatedInput(context, input);

		return Prompt.END_OF_CONVERSATION;
	}

	/**
	 * 如果你只需要单个问题的提示且已到达结尾，请覆盖此方法
	 *
	 * @param context
	 * @param input
	 */
	protected void onValidatedInput(ConversationContext context, double input) {
	}

	/**
	 * 向玩家显示问题并绑定操作
	 *
	 * @param player
	 * @param question
	 * @param successAction
	 */
	public static void show(final Player player, final String question, final Consumer<Double> successAction) {
		new SimpleDecimalPrompt(question, successAction).show(player);
	}
}
