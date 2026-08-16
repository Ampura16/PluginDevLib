package org.mineacademy.fo.menu.button;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.conversation.SimpleConversation;
import org.mineacademy.fo.conversation.SimplePrompt;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.Getter;

/**
 * 运行服务器对话的按钮
 */
public final class ButtonConversation extends Button {

	/**
	 * 要启动的服务器对话；若设置了 {@link SimplePrompt} 则为 null
	 */
	private final SimpleConversation conversation;

	/**
	 * 要显示的提示；若设置了 {@link #conversation} 则为 null
	 */
	private final SimplePrompt prompt;

	/**
	 * 代表此按钮的物品
	 */
	@Getter
	private final ItemStack item;

	/**
	 * {@link ItemCreator#of(CompMaterial, String, String...)} 的便捷快捷方式
	 *
	 * @param convo
	 * @param material
	 * @param title
	 * @param lore
	 */
	public ButtonConversation(SimpleConversation convo, CompMaterial material, String title, String... lore) {
		this(convo, ItemCreator.of(material, title, lore));
	}

	/**
	 * 创建一个点击时启动服务器对话的新按钮
	 *
	 * @param convo
	 * @param item
	 */
	public ButtonConversation(SimpleConversation convo, ItemCreator item) {
		this(convo, null, item.make());
	}

	/**
	 * {@link ItemCreator#of(CompMaterial, String, String...)} 的便捷快捷方式
	 *
	 * @param prompt
	 * @param material
	 * @param title
	 * @param lore
	 */
	public ButtonConversation(SimplePrompt prompt, CompMaterial material, String title, String... lore) {
		this(prompt, ItemCreator.of(material, title, lore));
	}

	/**
	 * 根据单个提示创建一个新对话
	 *
	 * @param prompt
	 * @param item
	 */
	public ButtonConversation(SimplePrompt prompt, ItemCreator item) {
		this(null, prompt, item.hideTags(true).make());
	}

	private ButtonConversation(SimpleConversation conversation, SimplePrompt prompt, ItemStack item) {
		this.conversation = conversation;
		this.prompt = prompt;
		this.item = item;
	}

	@Override
	public void onClickedInMenu(Player player, Menu menu, ClickType click) {
		Valid.checkBoolean(this.conversation != null || this.prompt != null, "Conversation and prompt cannot be null!");

		if (this.conversation != null) {
			this.conversation.setMenuToReturnTo(menu);

			this.conversation.start(player);

		} else
			this.prompt.show(player);

	}
}