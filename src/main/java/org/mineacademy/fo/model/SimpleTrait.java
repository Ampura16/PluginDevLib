package org.mineacademy.fo.model;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.ClickType;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.exception.EventHandledException;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.citizensnpcs.api.event.NPCClickEvent;
import net.citizensnpcs.api.event.NPCLeftClickEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;

/**
 * 这是一个通过 /trait killboss 命令应用到 NPC 上的特性。每个 NPC 都拥有此类的独立实例。
 * Trait 类通过受保护字段 'npc' 或 getNPC() 引用所附加的 NPC 类。
 * Trait 类还实现了 Listener，因此你可以直接在特性中添加 EventHandler。
 *
 * @author https://wiki.citizensnpcs.co/API, improved by kangarko
 */
public abstract class SimpleTrait extends Trait {

	private int tickAmount = 0;

	@Getter(value = AccessLevel.PROTECTED)
	@Setter(value = AccessLevel.PROTECTED)
	private int tickThreshold;

	protected SimpleTrait(String traitName) {
		super(traitName);
	}

	/**
	 * @see net.citizensnpcs.api.trait.Trait#load(net.citizensnpcs.api.util.DataKey)
	 */
	@Override
	public final void load(DataKey key) {
		final SerializedMap map = SerializedMap.fromJson(key.getString("Data"));

		this.load(map);
	}

	/**
	 * 你应在此加载之前保存过的任何值（可选）。
	 * 首次应用特性时不会调用此方法，只有在服务器启动时加载到已有 NPC 上时才会调用。
	 *
	 * 此方法在 onSpawn 之前调用，此时 npc.getEntity() 会返回 null。
	 *
	 * @param map
	 */
	protected abstract void load(SerializedMap map);

	/**
	 * @see net.citizensnpcs.api.trait.Trait#save(net.citizensnpcs.api.util.DataKey)
	 */
	@Override
	public final void save(DataKey key) {
		final SerializedMap map = new SerializedMap();

		this.save(map);

		key.setString("Data", map.toJson());
	}

	/**
	 * 保存此 NPC 的设置（可选）。这些值会被持久化到 Citizens 的存档文件中
	 *
	 * @param map
	 */
	protected abstract void save(SerializedMap map);

	/**
	 * 处理点击。
	 *
	 * @param event
	 */
	@EventHandler
	public final void onRightClick(NPCRightClickEvent event) {
		this.handleClickEvent(event, ClickType.RIGHT);
	}

	/**
	 * 处理点击。
	 *
	 * @param event
	 */
	@EventHandler
	public final void onLeftClick(NPCLeftClickEvent event) {
		this.handleClickEvent(event, ClickType.LEFT);
	}

	/*
	 * A helper method for click events
	 */
	private void handleClickEvent(NPCClickEvent event, ClickType clickType) {

		// Only apply this event for this particular NPC
		if (!event.getNPC().equals(this.getNPC()))
			return;

		final Player player = event.getClicker();

		try {
			this.onClick(player, clickType);

		} catch (final EventHandledException ex) {
			if (ex.getMessages() != null)
				if (Messenger.ENABLED)
					Messenger.error(player, ex.getMessages());
				else
					Common.tell(player, ex.getMessages());

			if (ex.isCancelled())
				event.setCancelled(true);
		}
	}

	/**
	 * 当此 NPC 被玩家点击时自动调用。
	 * 仅支持 RIGHT 或 LEFT 点击类型。
	 *
	 * @param player
	 * @param clickType
	 *
	 * @throws EventHandledException
	 */
	public void onClick(Player player, ClickType clickType) throws EventHandledException {
	}

	/**
	 * 辅助方法，可在 onX 方法（例如 {@link #onClick(Player)} 方法）中使用，
	 * 用于取消事件并可选地向玩家发送错误消息。
	 *
	 * @param playerMessage
	 */
	protected final void cancel(String... playerMessage) {
		throw new EventHandledException(true, playerMessage);
	}

	/**
	 * @see net.citizensnpcs.api.trait.Trait#run()
	 */
	@Override
	public final void run() {

		if (this.tickAmount++ % this.tickThreshold == 0) {
			this.tickAmount = 1;

			this.onTick();
		}
	}

	/**
	 * 每隔 {@link #tickThreshold} 调用一次
	 */
	protected abstract void onTick();

	/**
	 * NPC 生成时运行的代码。注意在调用此方法之前 npc.getEntity() 为 null。
	 * 服务器启动时，此方法在 Load 之后调用。
	 *
	 * @see net.citizensnpcs.api.trait.Trait#onSpawn()
	 */
	@Override
	public void onSpawn() {
	}

	/**
	 * NPC 消失时运行的代码。此方法在实体真正消失之前调用，因此 npc.getEntity() 仍然有效。
	 *
	 * @see net.citizensnpcs.api.trait.Trait#onDespawn()
	 */
	@Override
	public void onDespawn() {
	}

	/**
	 * NPC 被移除时运行的代码。
	 */
	@Override
	public void onRemove() {
	}
}
