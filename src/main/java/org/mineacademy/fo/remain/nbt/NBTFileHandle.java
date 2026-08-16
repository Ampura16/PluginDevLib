package org.mineacademy.fo.remain.nbt;

import java.io.File;
import java.io.IOException;

public interface NBTFileHandle extends ReadWriteNBT {

	/**
	 * 将数据保存到文件
	 *
	 * @throws IOException
	 */
	void save() throws IOException;

	/**
	 * @return 用于存储数据的 File
	 */
	File getFile();

}