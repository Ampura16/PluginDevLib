package org.mineacademy.fo.remain.nbt;

/**
 * Minecraft 中所有 NBT 类型的枚举
 *
 * @author tr7zw
 */

public enum NBTType {
	NBTTagEnd(0, ""),
	NBTTagByte(1, "BYTE"),
	NBTTagShort(2, "SHORT"),
	NBTTagInt(3, "INT"),
	NBTTagLong(4, "LONG"),
	NBTTagFloat(5, "FLOAT"),
	NBTTagDouble(6, "DOUBLE"),
	NBTTagByteArray(7, "BYTE[]"),
	NBTTagString(8, "STRING"),
	NBTTagList(9, "LIST"),
	NBTTagCompound(10, "COMPOUND"),
	NBTTagIntArray(11, "INT[]"),
	NBTTagLongArray(12, "LONG[]");

	NBTType(int i, String name) {
		this.id = i;
		this.name = name;
	}

	private final int id;
	private final String name;

	/**
	 * @return Minecraft 内部使用的 Id
	 */
	public int getId() {
		return this.id;
	}

	/**
	 * @return NBTType 的名称
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @param id Minecraft 内部 id
	 * @return 表示该 id 的枚举，无效 id 返回 NBTTagEnd
	 */
	public static NBTType valueOf(int id) {
		for (final NBTType t : values())
			if (t.getId() == id)
				return t;
		return NBTType.NBTTagEnd;
	}

	public static NBTType fromName(String name) {
		for (final NBTType t : values())
			if (t.getName().equals(name))
				return t;
		return NBTType.NBTTagEnd;
	}

}
