package org.mineacademy.fo.remain.nbt;

/**
 * 用于 {@link NBTCompoundList} 内部的 {@link NBTCompound} 精简版。
 * 此复合标签实现不支持进一步的子复合标签和列表。
 * 此类将来可能会发生变化
 *
 * @author tr7zw
 */
public class NBTListCompound extends NBTCompound {

	private NBTList<?> owner;
	private Object compound;

	protected NBTListCompound(NBTList<?> parent, Object obj) {
		super(null, null);
		this.owner = parent;
		this.compound = obj;
	}

	public NBTList<?> getListParent() {
		return this.owner;
	}

	@Override
	protected boolean isClosed() {
		return this.owner.getParent().isClosed();
	}

	@Override
	protected boolean isReadOnly() {
		return this.owner.getParent().isReadOnly();
	}

	@Override
	public Object getCompound() {
		if (this.isClosed())
			throw new NbtApiException("Tried using closed NBT data!");
		return this.compound;
	}

	@Override
	protected void setCompound(Object compound) {
		if (this.isClosed())
			throw new NbtApiException("Tried using closed NBT data!");
		if (this.isReadOnly())
			throw new NbtApiException("Tried setting data in read only mode!");
		this.compound = compound;
	}

	@Override
	protected void saveCompound() {
		this.owner.save();
	}

}
