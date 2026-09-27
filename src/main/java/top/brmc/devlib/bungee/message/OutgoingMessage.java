package top.brmc.devlib.bungee.message;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import top.brmc.devlib.Valid;
import top.brmc.devlib.bungee.BungeeListener;
import top.brmc.devlib.bungee.BungeeMessageType;
import top.brmc.devlib.collection.SerializedMap;
import top.brmc.devlib.exception.FoException;
import top.brmc.devlib.model.ConfigSerializable;
import top.brmc.devlib.plugin.SimplePlugin;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

/**
 * 注意：这里使用标准化的 Foundation 模型，第一个
 * 字符串是服务器名，第二个字符串是
 * 按名称对应的 {@link BungeeMessageType}（*自动写入*）。
 */
public final class OutgoingMessage extends Message {

	/**
	 * 写入消息的待处理队列
	 */
	private final List<Object> queue = new ArrayList<>();

	/**
	 * 创建新的传出消息，见本类头部
	 *
	 * @param action
	 */
	public OutgoingMessage(BungeeMessageType action) {
		this(SimplePlugin.getInstance().getBungeeCord(), action);
	}

	/**
	 * 创建新的传出消息，见本类头部
	 *
	 * @param listener
	 * @param action
	 */
	public OutgoingMessage(BungeeListener listener, BungeeMessageType action) {
		super(listener, action);
	}

	/**
	 * 向消息写入兼容对象
	 *
	 * @param map
	 */
	public void write(ConfigSerializable map) {
		this.write(map.serialize().toJson(), String.class);
	}

	/**
	 * 向消息写入映射
	 *
	 * @param map
	 */
	public void writeMap(SerializedMap map) {
		this.write(map.toJson(), String.class);
	}

	/**
	 * 向消息写入给定字符串
	 *
	 * @param messages
	 */
	public void writeString(String... messages) {
		for (final String message : messages)
			this.write(message, String.class);
	}

	/**
	 * 向消息写入布尔值
	 *
	 * @param bool
	 */
	public void writeBoolean(boolean bool) {
		this.write(bool, Boolean.class);
	}

	/**
	 * 向消息写入字节
	 *
	 * @param number
	 */
	public void writeByte(byte number) {
		this.write(number, Byte.class);
	}

	/**
	 * 向消息写入双精度数
	 *
	 * @param number
	 */
	public void writeDouble(double number) {
		this.write(number, Double.class);
	}

	/**
	 * 向消息写入单精度数
	 *
	 * @param number
	 */
	public void writeFloat(float number) {
		this.write(number, Float.class);
	}

	/**
	 * 向消息写入整数
	 *
	 * @param number
	 */
	public void writeInt(int number) {
		this.write(number, Integer.class);
	}

	/**
	 * 向消息写入单精度数
	 *
	 * @param number
	 */
	public void writeLong(long number) {
		this.write(number, Long.class);
	}

	/**
	 * 向消息写入短整数
	 *
	 * @param number
	 */
	public void writeShort(short number) {
		this.write(number, Short.class);
	}

	/**
	 * 向消息写入 UUID
	 *
	 * @param uuid
	 */
	public void writeUUID(UUID uuid) {
		this.write(uuid, UUID.class);
	}

	/**
	 * 向消息写入给定类型的对象
	 * <p>
	 * 我们移动头指针，确保写入安全，依据
	 * {@link BungeeMessageType#getContent()} 的长度和
	 * 给定位置的数据类型
	 *
	 * @param object
	 * @param typeOf
	 */
	private void write(Object object, Class<?> typeOf) {
		Valid.checkNotNull(object, "Added object must not be null!");

		this.moveHead(typeOf);
		this.queue.add(object);
	}

	/**
	 * 基于队列，
	 * 为字节数组数据输出委托写方法
	 *
	 * @param serverName
	 * @return
	 */
	public byte[] getData(String serverName) {
		final ByteArrayDataOutput out = ByteStreams.newDataOutput();

		// -----------------------------------------------------------------
		// We are automatically writing the first two strings assuming the
		// first is the senders server name and the second is the action
		// -----------------------------------------------------------------

		out.writeUTF(this.getListener().getChannel());
		out.writeUTF(UUID.fromString("00000000-0000-0000-0000-000000000000").toString());
		out.writeUTF(serverName);
		out.writeUTF(this.getAction().name());

		for (final Object object : this.queue)
			if (object instanceof String)
				out.writeUTF((String) object);

			else if (object instanceof Boolean)
				out.writeBoolean((Boolean) object);

			else if (object instanceof Byte)
				out.writeByte((Byte) object);

			else if (object instanceof Double)
				out.writeDouble((Double) object);

			else if (object instanceof Float)
				out.writeFloat((Float) object);

			else if (object instanceof Integer)
				out.writeInt((Integer) object);

			else if (object instanceof Long)
				out.writeLong((Long) object);

			else if (object instanceof Short)
				out.writeShort((Short) object);

			else if (object instanceof byte[])
				out.write((byte[]) object);

			else if (object instanceof UUID)
				out.writeUTF(object.toString());

			else
				throw new FoException("Unsupported write of " + object.getClass().getSimpleName() + " to channel " + this.getChannel() + " with action " + this.getAction().toString());

		return out.toByteArray();
	}

	/**
	 *
	 * @return
	 */
	protected String getChannel() {
		return this.getListener().getChannel();
	}
}