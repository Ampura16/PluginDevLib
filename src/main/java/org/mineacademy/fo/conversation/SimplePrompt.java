package org.mineacademy.fo.conversation;

import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationPrefix;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.ValidatingPrompt;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.settings.SimpleLocalization;

/**
 * 表示服务器对话中向玩家提出的一个问题
 */
public abstract class SimplePrompt extends ValidatingPrompt {

	/**
	 * 是否重新打开玩家之前的菜单（如果有）？
	 */
	private boolean openMenu = true;

	/**
	 * 参见 {@link SimpleConversation#isModal()}
	 */

	/**
	 * 看到此输入提示的玩家
	 */
	private Player player = null;

	protected SimplePrompt() {
	}

	/**
	 * 创建新提示；如果玩家之前打开了菜单，是否重新打开？
	 *
	 * @param openMenu
	 */
	protected SimplePrompt(final boolean openMenu) {
		this.openMenu = openMenu;
	}

	/**
	 * 返回 tell 消息前的前缀
	 *
	 * @param ctx
	 * @return
	 */
	protected String getCustomPrefix() {
		return null;
	}

	/**
	 * @see {@link SimpleConversation#isModal()}
	 *
	 * @return
	 */
	protected boolean isModal() {
		return true;
	}

	/**
	 * @see SimpleConversation#setMenuAnimatedTitle(String)
	 *
	 * @return
	 */
	protected String getMenuAnimatedTitle() {
		return null;
	}

	/**
	 * 返回问题，以自定义方式使用颜色实现
	 */
	@Override
	public final String getPromptText(final ConversationContext context) {
		String prompt = this.getPrompt(context);
		final String promptColorless = Common.stripColors(prompt);

		if (Messenger.ENABLED
				&& (this.getCustomPrefix() == null || !promptColorless.contains(Common.stripColors(this.getCustomPrefix())))
				&& !promptColorless.contains(Common.stripColors(Messenger.getSuccessPrefix())))
			prompt = Messenger.getQuestionPrefix() + prompt;

		return Variables.replace(prompt, this.getPlayer(context));
	}

	/**
	 * 返回此提示中向用户提出的问题
	 *
	 * @param context
	 * @return
	 */
	protected abstract String getPrompt(ConversationContext context);

	/**
	 * 检查用户的输入是否有效，若有效则可以继续下一个提示
	 *
	 * @param context
	 * @param input
	 * @return
	 */
	@Override
	protected boolean isInputValid(final ConversationContext context, final String input) {
		return true;
	}

	/**
	 * 当 {@link #isInputValid(ConversationContext, String)} 返回 false 时返回的失败错误消息
	 */
	@Override
	protected String getFailedValidationText(final ConversationContext context, final String invalidInput) {
		return null;
	}

	/**
	 * 将 {@link ConversationContext} 转换为 {@link Player}，
	 * 若不是玩家则抛出错误
	 *
	 * @param ctx
	 * @return
	 */
	protected final Player getPlayer(final ConversationContext ctx) {
		Valid.checkBoolean(ctx.getForWhom() instanceof Player, "Conversable is not a player but: " + ctx.getForWhom());

		return (Player) ctx.getForWhom();
	}

	/**
	 * 向玩家（如果有）发送给定消息
	 *
	 * @param ctx
	 * @param message
	 */
	protected final void tell(final String message) {
		Valid.checkNotNull(this.player, "Cannot use tell() when player not yet set!");

		this.tell(this.player, message);
	}

	/**
	 * 向玩家（如果有）发送给定消息
	 *
	 * @param context
	 * @param message
	 */
	protected final void tell(final ConversationContext context, final String message) {
		this.tell(this.getPlayer(context), message);
	}

	/**
	 * 向玩家发送消息
	 *
	 * @param conversable
	 * @param message
	 */
	protected final void tell(final Conversable conversable, final String message) {
		if (this.getCustomPrefix() != null)
			Common.tellConversingNoPrefix(conversable, this.getCustomPrefix() + message);
		else
			Common.tellConversing(conversable, message);
	}

	/**
	 * 向玩家发送消息
	 *
	 * @param conversable
	 * @param message
	 */
	protected final void tellNoPrefix(final Conversable conversable, final String message) {
		Common.tellConversingNoPrefix(conversable, message);
	}

	/**
	 * 稍后向玩家发送消息
	 *
	 * @param delayTicks
	 * @param conversable
	 * @param message
	 */
	protected final void tellLater(final int delayTicks, final Conversable conversable, final String message) {
		if (this.getCustomPrefix() != null)
			Common.tellLaterConversingNoPrefix(delayTicks, conversable, this.getCustomPrefix() + message);
		else
			Common.tellLaterConversing(delayTicks, conversable, message);
	}

	/**
	 * 稍后向玩家发送消息
	 *
	 * @param delayTicks
	 * @param conversable
	 * @param message
	 */
	protected final void tellLaterNoPrefix(final int delayTicks, final Conversable conversable, final String message) {
		Common.tellLaterConversingNoPrefix(delayTicks, conversable, message);
	}

	/**
	 * 整个对话结束时调用。此方法在 onConversationEnd 之前调用
	 *
	 * @param conversation
	 * @param event
	 */
	public void onConversationEnd(final SimpleConversation conversation, final ConversationAbandonedEvent event) {
	}

	// Do not allow superclasses to modify this since we have isInputValid here
	@Override
	public final Prompt acceptInput(final ConversationContext context, final String input) {
		try {
			// Since developers use try-catch blocks to validate input, do not save this as error
			FoException.setErrorSavedAutomatically(false);

			if (this.isInputValid(context, input))
				return this.acceptValidatedInput(context, input);

			else {
				final String failPrompt = this.getFailedValidationText(context, input);

				if (failPrompt != null) {
					final String failPromptColorless = Common.stripColors(failPrompt);
					final String prefixColorless = Common.stripColors(Messenger.getErrorPrefix());

					this.tellLaterNoPrefix(0, context.getForWhom(), Variables.replace((Messenger.ENABLED && !failPromptColorless.contains(prefixColorless) ? Messenger.getErrorPrefix() : "") + "&c" + failPrompt, this.getPlayer(context)));
				}

				// Redisplay this prompt to the user to re-collect input
				return this;
			}

		} finally {
			FoException.setErrorSavedAutomatically(true);
		}
	}

	/**
	 * 以对话形式向玩家显示此提示
	 * <p>
	 * 注意：不要在已有对话进行中调用此方法来显示此提示，
	 * 否则会失败！请改用 acceptValidatedInput
	 * 来显示下一个提示
	 *
	 * @param player
	 * @return
	 */
	public final Conversation show(final Player player) {
		Valid.checkBoolean(!player.isConversing(), "Player " + player.getName() + " is already conversing! Show them their next prompt in acceptValidatedInput() in " + this.getClass().getSimpleName() + " instead!");

		this.player = player;

		final SimpleConversation conversation = new SimpleConversation() {

			@Override
			protected Prompt getFirstPrompt() {
				return SimplePrompt.this;
			}

			@Override
			protected boolean isModal() {
				return SimplePrompt.this.isModal();
			}

			@Override
			protected ConversationPrefix getPrefix() {
				final String prefix = SimplePrompt.this.getCustomPrefix();

				return prefix != null ? new SimplePrefix(prefix) : super.getPrefix();
			}

			@Override
			public String getMenuAnimatedTitle() {
				return SimplePrompt.this.getMenuAnimatedTitle();
			}

			@Override
			protected void onConversationEnd(ConversationAbandonedEvent event, boolean canceledFromInactivity) {
				final String message = canceledFromInactivity ? SimpleLocalization.Conversation.CONVERSATION_CANCELLED_INACTIVE : SimpleLocalization.Conversation.CONVERSATION_CANCELLED;
				final Player player = SimplePrompt.this.getPlayer(event.getContext());

				if (!event.gracefulExit())
					if (Messenger.ENABLED)
						Messenger.warn(player, message);
					else
						Common.tell(player, message);
			}
		};

		if (this.openMenu) {
			final Menu menu = Menu.getMenu(player);

			if (menu != null)
				conversation.setMenuToReturnTo(menu);
		}

		return conversation.start(player);
	}

	/**
	 * 向玩家显示给定提示
	 *
	 * @param player
	 * @param prompt
	 */
	public static final void show(final Player player, final SimplePrompt prompt) {
		prompt.show(player);
	}
}
