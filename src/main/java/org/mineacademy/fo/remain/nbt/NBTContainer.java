package org.mineacademy.fo.remain.nbt;

import java.io.InputStream;

/**
 * 独立的 {@link NBTCompound} 实现。所有数据都只保存在
 * 此对象内部。
 *
 * @author tr7zw
 */
public class NBTContainer extends NBTCompound {

	private Object nbt;
	private boolean closed;
	private boolean readOnly;

	/**
	 * 创建一个空的独立 NBTCompound
	 *
	 * @deprecated 请使用 {@link NBT#createNBTObject()}
	 */
	@Deprecated
	public NBTContainer() {
		super(null, null);
		this.nbt = ObjectCreator.NMS_NBTTAGCOMPOUND.getInstance();
	}

	/**
	 * 接收任意 NMS 复合标签并将其包装
	 *
	 * @param nbt
	 * @deprecated 请使用 NBT.wrapNMSTag
	 */
	@Deprecated
	public NBTContainer(Object nbt) {
		super(null, null);
		if (nbt == null)
			nbt = ObjectCreator.NMS_NBTTAGCOMPOUND.getInstance();
		if (!ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz().isAssignableFrom(nbt.getClass()))
			throw new NbtApiException("The object '" + nbt.getClass() + "' is not a valid NBT-Object!");
		this.nbt = nbt;
	}

	/**
	 * 读取 NBT 输入流
	 *
	 * @param inputsteam
	 * @deprecated 请使用 NBT.readNBT
	 */
	@Deprecated
	public NBTContainer(InputStream inputsteam) {
		super(null, null);
		this.nbt = NBTReflectionUtil.readNBT(inputsteam);
	}

	/**
	 * 将 NBT 字符串解析为独立的 {@link NBTCompound}。出错时可能抛出
	 * {@link NbtApiException}。
	 *
	 * @param nbtString
	 * @deprecated 请使用 NBT.parseNBT
	 */
	@Deprecated
	public NBTContainer(String nbtString) {
		super(null, null);
		if (nbtString == null)
			throw new NullPointerException("The String can't be null!");
		try {
			this.nbt = ReflectionMethod.PARSE_NBT.run(null, nbtString);
		} catch (final Exception ex) {
			throw new NbtApiException("Unable to parse Malformed Json!", ex);
		}
	}

	@Override
	public Object getCompound() {
		return this.nbt;
	}

	@Override
	public void setCompound(Object tag) {
		this.nbt = tag;
	}

	@Override
	protected void setClosed() {
		this.closed = true;
	}

	@Override
	protected boolean isClosed() {
		return this.closed;
	}

	@Override
	protected boolean isReadOnly() {
		return this.readOnly;
	}

	protected NBTContainer setReadOnly(boolean readOnly) {
		this.readOnly = true;
		return this;
	}

}
