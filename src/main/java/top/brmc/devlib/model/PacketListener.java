package top.brmc.devlib.model;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.entity.Player;
import top.brmc.devlib.Common;
import top.brmc.devlib.MinecraftVersion;
import top.brmc.devlib.MinecraftVersion.V;
import top.brmc.devlib.ReflectionUtil;
import top.brmc.devlib.collection.SerializedMap;
import top.brmc.devlib.exception.EventHandledException;
import top.brmc.devlib.exception.FoException;
import top.brmc.devlib.exception.RegexTimeoutException;
import top.brmc.devlib.plugin.SimplePlugin;
import top.brmc.devlib.remain.Remain;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.AdventureComponentConverter;
import com.comphenix.protocol.wrappers.EnumWrappers.ChatType;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedServerPing;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.md_5.bungee.api.chat.BaseComponent;

/**
 * 表示使用 ProtocolLib 处理数据包
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class PacketListener {

	/**
	 * 存放 1.19 系统聊天数据包构造器和 Adventure 相关内容，以获得最佳性能
	 */
	private static Class<?> textComponentClass;

	/**
	 * 使用 \@AutoRegister 时自动调用，请在此注入
	 * 你的数据包监听器。
	 */
	public abstract void onRegister();

	/**
	 * 添加数据包监听器的便捷快捷方式
	 *
	 * @param adapter
	 */
	protected void addPacketListener(final SimpleAdapter adapter) {
		HookManager.addPacketListener(adapter);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Receiving
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 监听给定类型的 客户端>服务器 数据包的便捷方法。
	 *
	 * @param type
	 * @param consumer
	 */
	protected void addReceivingListener(final PacketType type, final Consumer<PacketEvent> consumer) {
		this.addReceivingListener(ListenerPriority.NORMAL, type, consumer);
	}

	/**
	 * 按给定类型和优先级监听 客户端>服务器 数据包的便捷方法。
	 *
	 * @param priority
	 * @param type
	 * @param consumer
	 */
	protected void addReceivingListener(final ListenerPriority priority, final PacketType type, final Consumer<PacketEvent> consumer) {
		this.addPacketListener(new SimpleAdapter(priority, type) {

			/**
			 * @see com.comphenix.protocol.events.PacketAdapter#onPacketReceiving(com.comphenix.protocol.events.PacketEvent)
			 */
			@Override
			public void onPacketReceiving(final PacketEvent event) {

				if (event.getPlayer() != null)
					consumer.accept(event);
			}
		});
	}

	// ------------------------------------------------------------------------------------------------------------
	// Sending
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 监听给定类型的 服务器>客户端 数据包的便捷方法。
	 *
	 * @param type
	 * @param consumer
	 */
	protected void addSendingListener(final PacketType type, final Consumer<PacketEvent> consumer) {
		this.addSendingListener(ListenerPriority.NORMAL, type, consumer);
	}

	/**
	 * 按给定类型和优先级监听 服务器>客户端 数据包的便捷方法。
	 *
	 * @param priority
	 * @param type
	 * @param consumer
	 */
	protected void addSendingListener(final ListenerPriority priority, final PacketType type, final Consumer<PacketEvent> consumer) {
		this.addPacketListener(new SimpleAdapter(priority, type) {

			/**
			 * @see com.comphenix.protocol.events.PacketAdapter#onPacketReceiving(com.comphenix.protocol.events.PacketEvent)
			 */
			@Override
			public void onPacketSending(final PacketEvent event) {

				if (event.getPlayer() != null)
					consumer.accept(event);
			}

			@Override
			public void onPacketReceiving(PacketEvent event) {
				if (type == PacketType.Play.Server.CHAT || type == PacketType.Play.Client.CHAT) {
					// Packet can be both sided
				} else
					super.onPacketReceiving(event);
			}
		});
	}

	/**
	 * 设置服务器列表菜单中的悬停文本
	 * 使用方法：为 PacketType.Status.Server.SERVER_INFO 创建一个新的 addSendingListener，
	 * 通过 event.getPacket().getServerPings().read(0) 获取 {@link WrappedServerPing}，
	 * 最后调用 WrappedServerPing#setPlayers 方法
	 *
	 * @param hoverTexts
	 */
	protected List<WrappedGameProfile> compileHoverText(final String... hoverTexts) {
		final List<WrappedGameProfile> profiles = new ArrayList<>();

		int count = 0;

		for (final String hoverText : hoverTexts) {
			WrappedGameProfile profile;

			try {
				profile = new WrappedGameProfile(UUID.randomUUID(), Common.colorize(hoverText));

			} catch (final Throwable t) {
				profile = new WrappedGameProfile(String.valueOf(count++), Common.colorize(hoverText));
			}

			profiles.add(profile);
		}

		return profiles;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Classes
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 处理聊天数据包的便捷适配器，替你完成大部分繁重工作。
	 */
	protected abstract class SimpleChatAdapter extends SimpleAdapter {

		/**
		 * 此刻正在方法内部处理的玩家。用于防止死循环。
		 */
		private final Set<String> processedPlayers = new HashSet<>();

		/**
		 * 事件字段，便于在可覆盖的方法中调用
		 */
		@Getter
		private PacketEvent event;

		/**
		 * 可在下方使用的玩家字段
		 */
		@Getter
		private Player player;

		/**
		 * 当前正在过滤的 json 消息
		 */
		private String jsonMessage;

		/**
		 * 支持 md_5 BaseComponent API
		 */
		private boolean isBaseComponent = false;

		/**
		 * 支持 PaperSpigot 的 Adventure 库
		 */
		private boolean adventure = false;

		/**
		 * 支持 1.19+ 系统聊天
		 */
		private final boolean systemChat = MinecraftVersion.atLeast(V.v1_19);

		/**
		 * 创建新的聊天监听器
		 */
		public SimpleChatAdapter() {
			super(ListenerPriority.HIGHEST, MinecraftVersion.atLeast(V.v1_19) ? PacketType.Play.Server.SYSTEM_CHAT : PacketType.Play.Server.CHAT);
		}

		@Override
		public void onPacketSending(final PacketEvent event) {
			if (event.getPlayer() == null)
				return;

			this.event = event;
			this.player = event.getPlayer();

			final String playerName = event.getPlayer().getName();
			final PacketContainer packet = event.getPacket();

			// Ignore temporary players
			try {
				this.player.getUniqueId();

			} catch (final UnsupportedOperationException ex) {
				return;
			}

			// Ignore dummy instances and rare reload case
			if (!this.player.isOnline() || SimplePlugin.isReloading())
				return;

			// Prevent deadlock
			if (this.processedPlayers.contains(playerName))
				return;

			// Ignore actionbar messages
			if (MinecraftVersion.atLeast(V.v1_19) && packet.getHandle().getClass().getSimpleName().equals("ClientboundSystemChatPacket") &&
					!packet.getBooleans().getFields().isEmpty() && packet.getBooleans().read(0) == true)
				return;

			// Lock processing to one instance only to prevent another packet filtering
			// in a filtering
			try {
				this.processedPlayers.add(playerName);

				final String legacyText = this.compileChatMessage(event);
				String parsedText = legacyText;

				try {
					parsedText = this.onMessage(parsedText);

				} catch (final RegexTimeoutException ex) {
					// Such errors mean the parsed message took too long to process.
					// Only show such errors every 30 minutes to prevent console spam
					Common.logTimed(1800, "&cWarning: &fPacket message '" + Common.limit(this.jsonMessage, 500)
							+ "' (possibly longer) took too long time to edit received message and was ignored."
							+ " This message only shows once per 30 minutes when that happens. For most cases, this can be ignored.");

					return;

				} catch (final EventHandledException ex) {
					event.setCancelled(true);

					return;
				}

				if (this.jsonMessage != null && !this.jsonMessage.isEmpty())
					this.jsonMessage = this.onJsonMessage(this.jsonMessage);

				if (!legacyText.equals(parsedText))
					this.writeEditedMessage(parsedText, event);

			} finally {
				this.processedPlayers.remove(this.player.getName());
			}
		}

		/*
		 * Read the chat message in unpacked format from the event
		 */
		private String compileChatMessage(PacketEvent event) {

			// Reset
			this.jsonMessage = null;

			// Components
			if (MinecraftVersion.atLeast(V.v1_7)) {

				// System chat
				if (this.systemChat) {

					try {
						// Minecraft 1.20.4+ uses Component field instead of text
						this.jsonMessage = event.getPacket().getChatComponents().read(0).getJson();

					} catch (final Exception ex) {
						this.jsonMessage = event.getPacket().getStrings().read(0);
					}

					if (this.jsonMessage != null)
						return Remain.toLegacyText(this.jsonMessage, false);

					try {
						final StructureModifier<Object> adventureModifier = event.getPacket().getModifier().withType(AdventureComponentConverter.getComponentClass());

						if (!adventureModifier.getFields().isEmpty()) {
							final Object comp = adventureModifier.read(0);

							final Class<?> serializerClass = ReflectionUtil.lookupClass("net.kyori.adventure.text.serializer.gson.GsonComponentSerializer");
							final Object gsonInstance = ReflectionUtil.invokeStatic(serializerClass, "gson");

							final Class<?> componentClass = ReflectionUtil.lookupClass("net.kyori.adventure.text.Component");
							final Method gsonMethod = ReflectionUtil.getMethod(gsonInstance.getClass(), "serialize", componentClass);

							final String json = ReflectionUtil.invoke(gsonMethod, gsonInstance, comp);
							this.jsonMessage = WrappedChatComponent.fromJson(json).getJson();
						}

					} catch (final Throwable ignored) {
						ignored.printStackTrace();
						// Ignore if Adventure is unavailable
					}

					final Object adventureContent = ReflectionUtil.getFieldContent(event.getPacket().getHandle(), "adventure$content");

					if (adventureContent != null) {
						final List<String> contents = new ArrayList<>();

						this.mergeChildren(adventureContent, contents);
						final String mergedContents = String.join("", contents);

						return mergedContents;
					}

				} else {
					final StructureModifier<Object> packet = event.getPacket().getModifier();
					final StructureModifier<WrappedChatComponent> chat = event.getPacket().getChatComponents();
					final WrappedChatComponent component = chat.read(0);

					try {
						final ChatType chatType = event.getPacket().getChatTypes().readSafely(0);

						if (chatType == ChatType.GAME_INFO)
							return "";

					} catch (final NoSuchMethodError t) {
						// Silence on legacy MC
					}

					if (component != null)
						this.jsonMessage = component.getJson();

					// Md_5 way of dealing with packets
					else if (packet.size() > 1) {
						Object secondField = packet.readSafely(1);

						// Support "Adventure" library in PaperSpigot
						if (secondField == null) {
							secondField = packet.readSafely(2);

							if (secondField != null)
								this.adventure = true;
						}

						if (secondField instanceof BaseComponent[]) {
							this.jsonMessage = Remain.toJson((BaseComponent[]) secondField);

							this.isBaseComponent = true;
						}
					}
				}
			}

			// No components for this MC version
			else
				this.jsonMessage = event.getPacket().getStrings().read(0);

			if (this.jsonMessage != null && !this.jsonMessage.isEmpty())
				// Only check valid messages, skipping those over 50k since it would cause rules
				// to take too long and overflow. 99% packets are below this size, it may even be
				// that such oversized packets are maliciously sent so we protect the server from freeze
				if (this.jsonMessage.length() < 50_000) {
					final String legacyText;

					// Catch errors from other plugins and silence them
					try {
						legacyText = Remain.toLegacyText(this.jsonMessage, false);

					} catch (final Throwable t) {
						return "";
					}

					return legacyText;
				}

			return "";
		}

		/*
		 * Helper method to get content of all children of the given component
		 */
		private void mergeChildren(Object component, List<String> contents) {
			final Method contentMethod = ReflectionUtil.getMethod(component.getClass(), "content");
			final Method childrenMethod = ReflectionUtil.getMethod(component.getClass(), "children");

			if (textComponentClass == null)
				textComponentClass = ReflectionUtil.lookupClass("net.kyori.adventure.text.TextComponent");

			if (textComponentClass.isAssignableFrom(component.getClass())) {
				contents.add(ReflectionUtil.invoke(contentMethod, component));

				for (final Object child : (List<?>) ReflectionUtil.invoke(childrenMethod, component))
					mergeChildren(child, contents);
			}
		}

		/*
		 * Writes the edited message as JSON format from the event
		 */
		private void writeEditedMessage(String message, PacketEvent event) {
			final PacketContainer packet = event.getPacket();

			if (!this.editJson())
				this.jsonMessage = Remain.toJson(message);

			if (this.systemChat) {

				// We first need to get rid of Adventure library adding an extra field, so that the string JSON will be used below
				// Thanks to lukalt for help! https://github.com/dmulloy2/ProtocolLib/issues/2330#issuecomment-1517542145
				try {
					final StructureModifier<Object> adventureModifier = packet.getModifier().withType(AdventureComponentConverter.getComponentClass());

					if (!adventureModifier.getFields().isEmpty())
						adventureModifier.write(0, null);

				} catch (final Throwable ignored) {
					// Ignore if Adventure is unavailable
				}

				try {
					packet.getChatComponents().write(0, WrappedChatComponent.fromJson(this.jsonMessage));

				} catch (final FieldAccessException t) {
					packet.getStrings().write(0, this.jsonMessage);
				}

			} else if (this.isBaseComponent)
				packet.getModifier().writeSafely(this.adventure ? 2 : 1, Remain.toComponent(this.jsonMessage));

			else if (MinecraftVersion.atLeast(V.v1_7))
				packet.getChatComponents().writeSafely(0, WrappedChatComponent.fromJson(this.jsonMessage));

			else
				packet.getStrings().writeSafely(0, SerializedMap.of("text", this.jsonMessage.substring(1, this.jsonMessage.length() - 1)).toJson());
		}

		/**
		 * 在我们接收并解析聊天消息数据包时自动调用。
		 * <p>
		 * 这里可以使用 {@link #getEvent()} 和 {@link #getPlayer()}。
		 * 取消数据包的推荐方式是抛出 {@link EventHandledException}
		 * <p>
		 * 如果你编辑了消息，我们会自动将其设置回去。
		 *
		 * @param message
		 * @return
		 */
		protected abstract String onMessage(String message);

		/**
		 * 在我们接收聊天消息并将其解析为纯 json 时自动调用。
		 * 这里可以使用 {@link #getEvent()} 和 {@link #getPlayer()}。
		 * <p>
		 * 如果你编辑了 jsonMessage，除非调用 {@link #editJson()} 并将其设为 true，否则我们不会把它设置回去。
		 *
		 * @param jsonMessage
		 *
		 * @return
		 */
		protected String onJsonMessage(final String jsonMessage) {
			return jsonMessage;
		}

		/**
		 * false（默认）= 根据 {@link #onMessage(String)} 编辑消息
		 * true = 根据 {@link #onJsonMessage(String)} 编辑消息
		 *
		 * @return
		 */
		protected boolean editJson() {
			return false;
		}
	}

	/**
	 * 便捷类，使你无需指定哪个插件是数据包适配器的所有者
	 */
	protected class SimpleAdapter extends PacketAdapter {

		/**
		 * 我们正在监听的数据包
		 */
		@Getter
		private final PacketType type;

		/**
		 * 为给定数据包类型创建新的数据包适配器
		 *
		 * @param type
		 */
		public SimpleAdapter(final PacketType type) {
			this(ListenerPriority.NORMAL, type);
		}

		/**
		 * 以给定优先级为给定数据包类型创建新的数据包适配器
		 *
		 * @param priority
		 * @param type
		 */
		public SimpleAdapter(final ListenerPriority priority, final PacketType type) {
			super(SimplePlugin.getInstance(), priority, type);

			this.type = type;
		}

		/**
		 * 当客户端向服务器发送 {@link #type} 时自动触发此方法。
		 *
		 * @param event
		 */
		@Override
		public void onPacketReceiving(final PacketEvent event) {
			throw new FoException("Override onPacketReceiving to handle receiving client>server packet type " + this.type);
		}

		/**
		 * 当服务器要向客户端发送 {@link #type} 时自动触发此方法。
		 *
		 * @param event
		 */
		@Override
		public void onPacketSending(final PacketEvent event) {
			throw new FoException("Override onPacketReceiving to handle sending server>client packet type " + this.type);
		}
	}
}
