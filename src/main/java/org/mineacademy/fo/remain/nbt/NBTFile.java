package org.mineacademy.fo.remain.nbt;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * 由 {@link File} 支持的 {@link NBTCompound} 实现
 *
 * @author tr7zw
 */
public class NBTFile extends NBTCompound implements NBTFileHandle {

	private final File file;
	private Object nbt;

	/**
	 * 创建一个使用 file 参数存储数据的 NBTFile。如果该文件
	 * 已存在，则会加载其中的数据。
	 *
	 * @param file
	 * @throws IOException
	 * @deprecated 请使用 NBT.getFileHandle(file)
	 */
	@Deprecated
	public NBTFile(File file) throws IOException {
		super(null, null);
		if (file == null)
			throw new NullPointerException("File can't be null!");
		this.file = file;
		if (file.exists())
			this.nbt = NBTReflectionUtil.readNBT(Files.newInputStream(file.toPath()));
		else {
			this.nbt = ObjectCreator.NMS_NBTTAGCOMPOUND.getInstance();
			this.save();
		}
	}

	/**
	 * 将数据保存到文件
	 *
	 * @throws IOException
	 */
	@Override
	public void save() throws IOException {
		try {
			this.getWriteLock().lock();
			saveTo(this.file, this);
		} finally {
			this.getWriteLock().unlock();
		}
	}

	/**
	 * @return 用于存储数据的 File
	 */
	@Override
	public File getFile() {
		return this.file;
	}

	@Override
	public Object getCompound() {
		return this.nbt;
	}

	@Override
	protected void setCompound(Object compound) {
		this.nbt = compound;
	}

	/**
	 * 从给定文件读取 NBT 数据。
	 * <p>
	 * 文件不存在时返回空的 NBTContainer。
	 *
	 * @param file 要读取的文件
	 * @return 保存文件 nbt 数据的 NBTCompound
	 * @throws IOException 异常
	 * @deprecated 请使用 NBT.readFile(file)
	 */
	@Deprecated
	public static NBTCompound readFrom(File file) throws IOException {
		if (!file.exists())
			return new NBTContainer();
		return new NBTContainer(NBTReflectionUtil.readNBT(Files.newInputStream(file.toPath())));
	}

	/**
	 * 将 NBT 数据保存到给定文件。
	 * <p>
	 * 如果文件已存在，将被完全覆盖。
	 *
	 * @param file 文件
	 * @param nbt  NBT 数据
	 * @throws IOException 异常
	 * @deprecated 请使用 NBT.writeFile(file, nbt)
	 */
	@Deprecated
	public static void saveTo(File file, NBTCompound nbt) throws IOException {
		if (!file.exists()) {
			file.getParentFile().mkdirs();
			if (!file.createNewFile())
				throw new IOException("Unable to create file at " + file.getAbsolutePath());
		}
		nbt.writeCompound(Files.newOutputStream(file.toPath()));
	}

}
