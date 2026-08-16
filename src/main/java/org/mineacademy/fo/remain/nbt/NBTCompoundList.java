package org.mineacademy.fo.remain.nbt;

/**
 * 用于 NBTList 的 {@link NBTListCompound} 实现
 *
 * @author tr7zw
 */
public class NBTCompoundList extends NBTList<ReadWriteNBT> implements ReadWriteNBTCompoundList {

	protected NBTCompoundList(NBTCompound owner, String name, NBTType type, Object list) {
		super(owner, name, type, list);
	}

	/**
	 * 在列表末尾添加一个新的 Compound 并返回它。
	 *
	 * @return 添加的 {@link NBTListCompound}
	 */
	@Override
	public NBTListCompound addCompound() {
		return (NBTListCompound) this.addCompound(null);
	}

	/**
	 * 在列表末尾添加该 Compound 的副本并返回它。若传入 null，
	 * 则会创建一个新的 Compound
	 *
	 * @param comp
	 * @return
	 */
	public NBTCompound addCompound(NBTCompound comp) {
		if (this.getParent().isReadOnly())
			throw new NbtApiException("Tried setting data in read only mode!");
		try {
			final Object compound = ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz().newInstance();
			if (MinecraftVersion.getVersion().getVersionId() >= MinecraftVersion.MC1_14_R1.getVersionId())
				ReflectionMethod.LIST_ADD.run(this.listObject, this.size(), compound);
			else
				ReflectionMethod.LEGACY_LIST_ADD.run(this.listObject, compound);
			this.getParent().saveCompound();
			final NBTListCompound listcomp = new NBTListCompound(this, compound);
			if (comp != null)
				listcomp.mergeCompound(comp);
			return listcomp;
		} catch (final Exception ex) {
			throw new NbtApiException(ex);
		}
	}

	@Override
	public ReadWriteNBT addCompound(ReadableNBT comp) {
		if (comp instanceof NBTCompound)
			return this.addCompound((NBTCompound) comp);
		return null;
	}

	/**
	 * 在列表末尾添加一个新的 Compound。
	 *
	 *
	 * @deprecated 请使用 addCompound！
	 * @param empty
	 * @return 若成功添加 compound 则为 True
	 */
	@Override
	@Deprecated
	public boolean add(ReadWriteNBT empty) {
		return this.addCompound(empty) != null;
	}

	@Override
	public void add(int index, ReadWriteNBT element) {
		if (element != null)
			throw new NbtApiException("You need to pass null! ListCompounds from other lists won't work.");
		if (this.getParent().isReadOnly())
			throw new NbtApiException("Tried setting data in read only mode!");
		try {
			final Object compound = ClassWrapper.NMS_NBTTAGCOMPOUND.getClazz().newInstance();
			if (MinecraftVersion.getVersion().getVersionId() >= MinecraftVersion.MC1_14_R1.getVersionId())
				ReflectionMethod.LIST_ADD.run(this.listObject, index, compound);
			else
				ReflectionMethod.LEGACY_LIST_ADD.run(this.listObject, compound);
			super.getParent().saveCompound();
		} catch (final Exception ex) {
			throw new NbtApiException(ex);
		}
	}

	@Override
	public NBTListCompound get(int index) {
		try {
			final Object compound = ReflectionMethod.LIST_GET_COMPOUND.run(this.listObject, index);
			return new NBTListCompound(this, compound);
		} catch (final Exception ex) {
			throw new NbtApiException(ex);
		}
	}

	@Override
	public NBTListCompound set(int index, ReadWriteNBT element) {
		throw new NbtApiException("This method doesn't work in the ListCompound context.");
	}

	@Override
	protected Object asTag(ReadWriteNBT object) {
		return null;
	}

}
