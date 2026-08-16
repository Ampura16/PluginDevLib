package org.mineacademy.fo.bungee.message;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.bungee.BungeeListener;
import org.mineacademy.fo.bungee.BungeeMessageType;
import org.mineacademy.fo.collection.SerializedMap;

import com.google.common.io.ByteArrayDataInput;

import lombok.Getter;

/**
 * 代表传入的插件消息。
 * <p>
 * 注意：这里使用标准化的 Foundation 模型，第一个
 * 字符串是服务器名，第二个字符串是
 * 按名称对应的 {@link BungeeMessageType}（*自动读取*）。
 */
public final class IncomingMessage extends Message {

	/**
	 * 要读取的原始字节数组
	 */
	@Getter
	private final byte[] data;

	/**
	 * 发送者 UUID
	 */
	@Getter
	private final UUID senderUid;

	/**
	 * 服务器名
	 */
	@Getter
	private final String serverName;

	/**
	 * 我们读取数据数组用的输入
	 */
	private ByteArrayDataInput input;

	/**
	 * 内部流
	 */
	private final ByteArrayInputStream stream;

	/**
	 * 从给定数组创建新的传入消息
	 *
	 * 注意：这里使用标准化的 Foundation 头：
	 *
	 * 1. 频道名（字符串）（因为我们在 BungeeCord 频道上广播）
	 * 2. 发送者 UUID（字符串）
	 * 3. 服务器名（字符串）
	 * 4. 动作（String 转为 {@link BungeeMessageType} 枚举）
	 *
	 * @param listener
	 * @param senderUid
	 * @param serverName
	 * @param type
	 * @param data
	 * @param input
	 * @param stream
	 */
	public IncomingMessage(BungeeListener listener, UUID senderUid, String serverName, BungeeMessageType type, byte[] data, ByteArrayDataInput input, ByteArrayInputStream stream) {
		super(listener, type);

		this.data = data;
		this.senderUid = senderUid;
		this.serverName = serverName;
		this.input = input;
		this.stream = stream;
	}

	/**
	 * 从数据中读取字符串
	 *
	 * @return
	 */
	public String readString() {
		this.moveHead(String.class);

		return this.input.readUTF();
	}

	/**
	 * 从字符串数据中读取 UUID
	 *
	 * @return
	 */
	public UUID readUUID() {
		this.moveHead(UUID.class);

		return UUID.fromString(this.input.readUTF());
	}

	/**
	 * 若为 json 则从字符串数据中读取映射
	 *
	 * @return
	 */
	public SerializedMap readMap() {
		this.moveHead(String.class);

		return SerializedMap.fromJson(this.input.readUTF());
	}

	/**
	 * 从给定字符串数据中读取枚举值
	 *
	 * @param <T>
	 * @param typeOf
	 * @return
	 */
	public <T extends Enum<T>> T readEnum(Class<T> typeOf) {
		this.moveHead(typeOf);

		return ReflectionUtil.lookupEnum(typeOf, this.input.readUTF());
	}

	/**
	 * 从数据中读取布尔值
	 *
	 * @return
	 */
	public boolean readBoolean() {
		this.moveHead(Boolean.class);

		return this.input.readBoolean();
	}

	/**
	 * 从数据中读取字节
	 *
	 * @return
	 */
	public byte readByte() {
		this.moveHead(Byte.class);

		return this.input.readByte();
	}

	/**
	 * 读取剩余字节
	 *
	 * @return
	 */
	public byte[] readBytes() {
		this.moveHead(byte[].class);

		final byte[] array = new byte[this.stream.available()];

		try {
			this.stream.read(array);

		} catch (final IOException e) {
			e.printStackTrace();
		}

		return array;
	}

	/**
	 * 从数据中读取双精度数
	 *
	 * @return
	 */
	public double readDouble() {
		this.moveHead(Double.class);

		return this.input.readDouble();
	}

	/**
	 * 从数据中读取单精度数
	 *
	 * @return
	 */
	public float readFloat() {
		this.moveHead(Float.class);

		return this.input.readFloat();
	}

	/**
	 * 从数据中读取整数
	 *
	 * @return
	 */
	public int readInt() {
		this.moveHead(Integer.class);

		return this.input.readInt();
	}

	/**
	 * 从数据中读取长整数
	 *
	 * @return
	 */
	public long readLong() {
		this.moveHead(Long.class);

		return this.input.readLong();
	}

	/**
	 * 从数据中读取短整数
	 *
	 * @return
	 */
	public short readShort() {
		this.moveHead(Short.class);

		return this.input.readShort();
	}

	/**
	 *
	 * @return
	 */
	public String getChannel() {
		return this.getListener().getChannel();
	}
}