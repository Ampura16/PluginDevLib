package top.brmc.devlib.command;

import top.brmc.devlib.Common;
import top.brmc.devlib.settings.SimpleLocalization;

/**
 * 开箱即用的子命令，允许用户发送会话
 * 输入，以防其他插件拦截输入而我们正在
 * 与发送者会话。
 *
 * 会话指等待玩家在聊天中输入内容以便处理。
 * 例如 Boss 插件要求输入玩家想创建的 Boss
 * 名称。
 */
public final class ConversationCommand extends SimpleSubCommand {

	public ConversationCommand() {
		super("conversation|conv");

		this.setDescription("Reply to a server's conversation manually.");
		this.setUsage("<message ...>");
		this.setMinArguments(1);
	}

	@Override
	protected void onCommand() {
		this.checkConsole();
		this.checkBoolean(this.getPlayer().isConversing(), SimpleLocalization.Conversation.CONVERSATION_NOT_CONVERSING);

		this.getPlayer().acceptConversationInput(Common.joinRange(0, this.args));
	}
}