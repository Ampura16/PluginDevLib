package org.mineacademy.fo.conversation;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationCanceller;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationPrefix;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.expiringmap.ExpiringMap;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.model.BoxedMessage;
import org.mineacademy.fo.model.SimpleTask;
import org.mineacademy.fo.model.Variables;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompSound;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.SimpleLocalization;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * 与玩家交流的简单方式
 * ——玩家的聊天会被隔离，其聊天消息会作为
 * 对话输入进行处理。
 */
public abstract class SimpleConversation implements ConversationAbandonedListener {

	/**
	 * 要返回的菜单（如果有）
	 */
	private Menu menuToReturnTo;

	/**
	 * 创建一个简单对话
	 */
	protected SimpleConversation() {
		this(null);
	}

	/**
	 * 创建一个简单对话，结束时
	 * 打开指定菜单
	 *
	 * @param menuToReturnTo
	 */
	protected SimpleConversation(final Menu menuToReturnTo) {
		this.menuToReturnTo = menuToReturnTo;
	}

	/**
	 * 与玩家开始对话；若 {@link Player#isConversing()} 则抛出错误
	 *
	 * @param player
	 * @return
	 */
	public final CustomConversation start(final Player player) {
		Valid.checkBoolean(!player.isConversing(), "Player " + player.getName() + " is already conversing!");

		// Do not allow open inventory since they cannot type anyways
		player.closeInventory();

		// Setup
		final CustomConversation conversation = new CustomConversation(player);
		final CustomCanceller canceller = new CustomCanceller();

		canceller.setConversation(conversation);

		conversation.getCancellers().add(canceller);
		conversation.getCancellers().add(this.getCanceller());

		conversation.addConversationAbandonedListener(this);
		conversation.begin();

		return conversation;
	}

	/**
	 * 获取此对话中面向玩家的第一个提示
	 *
	 * @return
	 */
	protected abstract Prompt getFirstPrompt();

	/**
	 * 监听并处理退出对话
	 */
	@Override
	public final void conversationAbandoned(final ConversationAbandonedEvent event) {
		final ConversationContext context = event.getContext();
		final Conversable conversing = context.getForWhom();

		final Map<Object, Object> sessionData = Remain.getAllSessionData(context);

		final Object source = event.getSource();
		final boolean timeout = (boolean) sessionData.getOrDefault("FLP#TIMEOUT", false);

		// Remove the session data so that they are invisible to other plugnis
		sessionData.remove("FLP#TIMEOUT");

		if (source instanceof CustomConversation) {
			final SimplePrompt lastPrompt = ((CustomConversation) source).getLastSimplePrompt();

			if (lastPrompt != null)
				lastPrompt.onConversationEnd(this, event);
		}

		this.onConversationEnd(event, timeout);

		if (conversing instanceof Player) {
			final Player player = (Player) conversing;

			(event.gracefulExit() ? CompSound.ENTITY_ARROW_HIT_PLAYER : CompSound.BLOCK_NOTE_BLOCK_BASS).play(player, 1F, 1F);

			if (this.menuToReturnTo != null && this.reopenMenu()) {
				final Menu newMenu = this.menuToReturnTo.newInstance();

				newMenu.displayTo(player);

				final String title = this.getMenuAnimatedTitle();

				if (title != null)
					Common.runLater(2, () -> newMenu.animateTitle(title));
			}
		}
	}

	/**
	 * 当用户退出此对话时触发（参见 {@link #getCanceller()}，或
	 * 直接退出游戏）
	 *
	 *
	 * @param event
	 * @param canceledFromInactivity 若用户未在 {@link #getTimeout()} 设置的时间内输入则为 true
	 */
	protected void onConversationEnd(final ConversationAbandonedEvent event, boolean canceledFromInactivity) {
		this.onConversationEnd(event);
	}

	/**
	 * 当用户退出此对话时触发（参见 {@link #getCanceller()}，或
	 * 直接退出游戏）
	 *
	 * @param event
	 */
	protected void onConversationEnd(final ConversationAbandonedEvent event) {
	}

	/**
	 * 获取每条消息前的对话前缀
	 * <p>
	 * 默认使用插件的 tell 前缀
	 * <p>
	 * 提示：可以使用 {@link SimplePrefix}
	 *
	 * @return
	 */
	protected ConversationPrefix getPrefix() {
		return new SimplePrefix(!Common.getTellPrefix().isEmpty() ? this.addLastSpace(Common.getTellPrefix()) : "");
	}

	/*
	 * Add a space to the prefix if it ends with one
	 */
	private final String addLastSpace(final String prefix) {
		return Common.stripColors(prefix).endsWith(" ") ? prefix : prefix + " ";
	}

	/**
	 * 返回监听特定单词以退出对话的取消器，
	 * 默认使用监听 quit|cancel|exit 的 {@link SimpleCanceller}
	 *
	 * @return
	 */
	protected ConversationCanceller getCanceller() {
		return new SimpleCanceller("quit", "cancel", "exit");
	}

	/**
	 * 若应在每条消息前插入前缀则返回 true，参见 {@link #getPrefix()}
	 *
	 * @return
	 */
	protected boolean insertPrefix() {
		return true;
	}

	/**
	 * 如果检测到玩家打开了菜单，是否应重新打开它？
	 *
	 * @return
	 */
	protected boolean reopenMenu() {
		return true;
	}

	/**
	 * 获取自动退出对话前的超时时间（秒）
	 *
	 * @return
	 */
	protected int getTimeout() {
		return 60;
	}

	/**
	 * 模态为 true = Bukkit 会隐藏除你的提示中对话消息以外的
	 * 所有聊天和插件消息
	 *
	 * 默认为 true
	 *
	 * @return
	 */
	protected boolean isModal() {
		return true;
	}

	/**
	 * 设置此对话结束后要返回的菜单
	 *
	 * @param menu
	 */
	public final void setMenuToReturnTo(final Menu menu) {
		this.menuToReturnTo = menu;
	}

	/**
	 * 重新为玩家打开菜单时，在菜单标题中闪烁的消息
	 *
	 * @return 菜单动画标题
	 */
	public String getMenuAnimatedTitle() {
		return null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Static access
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 稍后向可对话玩家发送带边框的消息
	 *
	 * @param delayTicks
	 * @param conversable
	 * @param messages
	 */
	protected static final void tellBoxed(final int delayTicks, final Conversable conversable, final String... messages) {
		Common.runLater(delayTicks, () -> tellBoxed(conversable, messages));
	}

	/**
	 * 向可对话玩家发送带边框的消息
	 *
	 * @param conversable
	 * @param messages
	 */
	protected static final void tellBoxed(final Conversable conversable, final String... messages) {
		BoxedMessage.tell((Player) conversable, messages);
	}

	/**
	 * 直接向玩家发送消息的快捷方法
	 *
	 * @param conversable
	 * @param message
	 */
	protected static final void tell(final Conversable conversable, final String message) {
		Common.tellConversing(conversable, Variables.replace(message, (Player) conversable));
	}

	/**
	 * 稍后向可对话玩家发送消息
	 *
	 * @param delayTicks
	 * @param conversable
	 * @param message
	 */
	protected static final void tellLater(final int delayTicks, final Conversable conversable, final String message) {
		Common.tellLaterConversing(delayTicks, conversable, Variables.replace(message, (Player) conversable));
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	private final class CustomCanceller implements ConversationCanceller {

		protected Conversation conversation;

		private final int timeoutSeconds;
		private SimpleTask task = null;

		/**
		 */
		public CustomCanceller() {
			this.timeoutSeconds = SimpleConversation.this.getTimeout();
		}

		@Override
		public void setConversation(Conversation conversation) {
			this.conversation = conversation;

			this.startTimer();
		}

		@Override
		public boolean cancelBasedOnInput(ConversationContext context, String input) {
			this.stopTimer();
			this.startTimer();

			return false;
		}

		@Override
		public ConversationCanceller clone() {
			return new CustomCanceller();
		}

		/*
		 * Starts an inactivity timer.
		 */
		private void startTimer() {
			this.task = Common.runLater(this.timeoutSeconds * 20, (Runnable) () -> {
				if (this.conversation.getState() == Conversation.ConversationState.UNSTARTED)
					this.startTimer();

				else if (this.conversation.getState() == Conversation.ConversationState.STARTED) {
					this.conversation.getContext().setSessionData("FLP#TIMEOUT", true);

					this.conversation.abandon(new ConversationAbandonedEvent(this.conversation, this));
				}
			});
		}

		/*
		 * Stops the active inactivity timer.
		 */
		private void stopTimer() {
			if (this.task != null) {
				this.task.cancel();

				this.task = null;
			}
		}
	}

	/**
	 * 自定义对话类，每 20 秒内只显示一次问题
	 */
	private final class CustomConversation extends Conversation {

		/**
		 * 保存上一个提示的信息，用于调用 onConversationEnd
		 */
		@Getter(value = AccessLevel.PRIVATE)
		private SimplePrompt lastSimplePrompt;

		private CustomConversation(final Conversable forWhom) {
			super(SimplePlugin.getInstance(), forWhom, SimpleConversation.this.getFirstPrompt());

			this.localEchoEnabled = false;
			this.modal = SimpleConversation.this.isModal();

			if (SimpleConversation.this.insertPrefix() && SimpleConversation.this.getPrefix() != null)
				this.prefix = SimpleConversation.this.getPrefix();
		}

		@Override
		public void outputNextPrompt() {
			if (this.currentPrompt == null)
				try {
					this.abandon(new ConversationAbandonedEvent(this));

				} catch (final Throwable t) {
					tell(this.context.getForWhom(), (Messenger.ENABLED ? Messenger.getErrorPrefix() : "") + SimpleLocalization.Conversation.CONVERSATION_ERROR);

					t.printStackTrace();
				}

			else {
				// Save the time when we showed the question to the player
				// so that we only show it once per the given threshold
				final String promptClass = this.currentPrompt.getClass().getSimpleName();
				String question = this.currentPrompt.getPromptText(this.context);

				try {
					final ExpiringMap<String, Void /*dont have expiring set class*/> askedQuestions = (ExpiringMap<String, Void>) Remain.getAllSessionData(this.context).getOrDefault("Asked_" + promptClass, ExpiringMap.builder().expiration(SimpleConversation.this.getTimeout(), TimeUnit.SECONDS).build());

					if (!askedQuestions.containsKey(question)) {
						askedQuestions.put(question, null);

						if (!question.contains(Common.colorize(Messenger.getQuestionPrefix())))
							question = this.prefix.getPrefix(this.context) + question;

						this.context.setSessionData("Asked_" + promptClass, askedQuestions);
						this.context.getForWhom().sendRawMessage(question);
					}
				} catch (final NoSuchMethodError ex) {
					// Unfortunately, old MC version was detected
				}

				// Save last prompt if it is our class
				if (this.currentPrompt instanceof SimplePrompt)
					this.lastSimplePrompt = (SimplePrompt) this.currentPrompt;

				if (!this.currentPrompt.blocksForInput(this.context)) {
					this.currentPrompt = this.currentPrompt.acceptInput(this.context, null);
					this.outputNextPrompt();
				}
			}
		}
	}
}