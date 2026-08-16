package org.mineacademy.fo.remain.nbt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import java.util.function.Function;

import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.fo.Common;

/**
 * 用于简洁、简单地访问 nbt 的通用工具类。
 *
 * @author tr7zw
 */
public class NBT {

	private NBT() {
		// No instances of NBT. Utility class
	}

	/**
	 * 供 shade 版本在 onEnable 期间预加载并检查 API 的工具方法。此方法不会抛出异常，
	 * 而是将其记录到日志。如果出现根本性问题、NBTAPI 处于无法正常工作的状态，
	 * 则返回 false，此时由进行 shade 的插件自行处理。注意：
	 * 在 onLoad 期间调用此方法会导致失败，因此请等到
	 * onEnable。
	 *
	 * @return 若一切正常则为 true
	 */
	public static boolean preloadApi() {
		try {
			// boiled down version of the plugin selfcheck without tests
			if (MinecraftVersion.getVersion() == MinecraftVersion.UNKNOWN) {
				NbtApiException.confirmedBroken = true;
				return false;
			}
			for (final ClassWrapper c : ClassWrapper.values())
				if (c.isEnabled() && c.getClazz() == null) {
					NbtApiException.confirmedBroken = true;
					return false;
				}
			for (final ReflectionMethod method : ReflectionMethod.values())
				if (method.isCompatible() && !method.isLoaded()) {
					NbtApiException.confirmedBroken = true;
					return false;
				}
			// not settings NbtApiException.confirmedBroken = false, as no actual tests were done.
			// This just means the version was found, and all reflections seem to work.
			return true;
		} catch (final Exception ex) {
			NbtApiException.confirmedBroken = true;
			Common.error(ex, "[NBTAPI] Error during the selfcheck!");

			return false;
		}
	}

	/**
	 * 获取物品 NBT 的只读实例。由于需要创建 ItemStack 的副本，此方法比调用 NBT.get
	 * 稍慢，但允许在无上下文的情况下访问数据。
	 *
	 * @param item
	 * @return
	 */
	public static ReadableNBT readNbt(ItemStack item) {
		return new NBTItem(item.clone(), false, true, false);
	}

	/**
	 * 接收一个 ItemStack，以及一个接收 ReadableNBT 并返回泛型类型 T 的函数。
	 * 然后返回将该函数应用于新建 NBTItem
	 * 的结果
	 *
	 * @param item   要从中获取 NBT 的物品堆
	 * @param getter 接收 ReadableNBT 并返回 T 类型值的
	 *               函数。
	 * @return 函数的返回结果。
	 */
	public static <T> T get(ItemStack item, Function<ReadableItemNBT, T> getter) {
		final NBTItem nbt = new NBTItem(item, false, true, false);
		final T ret = getter.apply(nbt);
		if (ret instanceof ReadableNBT || ret instanceof ReadableNBTList<?>)
			throw new NbtApiException("Tried returning part of the NBT to outside of the NBT scope!");
		nbt.setClosed();
		return ret;
	}

	/**
	 * 接收一个 ItemStack，以及一个接收 ReadableNBT 的 Consumer。将该
	 * Consumer 应用于物品的 NBT
	 *
	 * @param item 要从中获取 NBT 的物品堆
	 */
	public static void get(ItemStack item, Consumer<ReadableItemNBT> getter) {
		final NBTItem nbt = new NBTItem(item, false, true, false);
		getter.accept(nbt);
		nbt.setClosed();
	}

	/**
	 * 接收一个实体，以及一个接收 ReadableNBT 并返回泛型类型 T 的函数，
	 * 并返回该函数的结果
	 *
	 * @param entity 要从中获取 NBT 的实体
	 * @param getter 接收 ReadableNBT 并返回一个值的函数。
	 * @return 函数的返回结果。
	 */
	public static <T> T get(Entity entity, Function<ReadableNBT, T> getter) {
		final NBTEntity nbt = new NBTEntity(entity, true);
		final T ret = getter.apply(nbt);
		if (ret instanceof ReadableNBT || ret instanceof ReadableNBTList<?>)
			throw new NbtApiException("Tried returning part of the NBT to outside of the NBT scope!");
		nbt.setClosed();
		return ret;
	}

	/**
	 * 接收一个 Entity，以及一个接收 ReadableNBT 的 Consumer。将该
	 * Consumer 应用于实体的 NBT
	 *
	 * @param entity 要从中获取 NBT 的实体
	 */
	public static void get(Entity entity, Consumer<ReadableNBT> getter) {
		final NBTEntity nbt = new NBTEntity(entity, true);
		getter.accept(nbt);
		nbt.setClosed();
	}

	/**
	 * 接收一个方块状态，以及一个接收可读 NBT 并返回 T 类型值的函数。
	 * 然后返回将该函数应用于根据该方块状态新建的
	 * NBTTileEntity 所得到的值
	 *
	 * @param blockState 要从中获取 NBT 的方块的方块状态。
	 * @param getter     接收 ReadableNBT 并返回 T 类型值的
	 *                   函数。
	 * @return 返回类型与 getter 函数的返回类型相同。
	 */
	public static <T> T get(BlockState blockState, Function<ReadableNBT, T> getter) {
		final NBTTileEntity nbt = new NBTTileEntity(blockState, true);
		final T ret = getter.apply(nbt);
		if (ret instanceof ReadableNBT || ret instanceof ReadableNBTList<?>)
			throw new NbtApiException("Tried returning part of the NBT to outside of the NBT scope!");
		nbt.setClosed();
		return ret;
	}

	/**
	 * 接收一个 BlockEntity，以及一个接收 ReadableNBT 的 Consumer。将该
	 * Consumer 应用于方块实体的 NBT
	 *
	 * @param blockState 要从中获取 NBT 的方块的方块状态。
	 */
	public static void get(BlockState blockState, Consumer<ReadableNBT> getter) {
		final NBTTileEntity nbt = new NBTTileEntity(blockState, true);
		getter.accept(nbt);
		nbt.setClosed();
	}

	/**
	 * 接收一个实体，以及一个接收 ReadableNBT 并返回泛型类型 T 的函数，
	 * 并返回将该函数应用于实体持久化数据容器
	 * 的结果
	 *
	 * @param entity 要从中获取数据的实体
	 * @param getter 接收 ReadableNBT 并返回 T 类型值的
	 *               函数。
	 * @return 返回类型为 T，即泛型类型。
	 */
	public static <T> T getPersistentData(Entity entity, Function<ReadableNBT, T> getter) {
		final T ret = getter.apply(new NBTEntity(entity).getPersistentDataContainer());
		if (ret instanceof ReadableNBT || ret instanceof ReadableNBTList<?>)
			throw new NbtApiException("Tried returning part of the NBT to outside of the NBT scope!");
		return ret;
	}

	/**
	 * 接收一个方块实体，以及一个接收 ReadableNBT 并返回泛型类型 T 的函数，
	 * 并返回将该函数应用于方块实体持久化数据容器
	 * 的结果
	 *
	 * @param blockState 要从中获取数据的方块的方块状态。
	 * @param getter     接收 ReadableNBT 并返回 T 类型值的
	 *                   函数。
	 * @return NBT 标签的值。
	 */
	public static <T> T getPersistentData(BlockState blockState, Function<ReadableNBT, T> getter) {
		final T ret = getter.apply(new NBTTileEntity(blockState).getPersistentDataContainer());
		if (ret instanceof ReadableNBT || ret instanceof ReadableNBTList<?>)
			throw new NbtApiException("Tried returning part of the NBT to outside of the NBT scope!");
		return ret;
	}

	/**
	 * 接收一个 ItemStack，对其 NBT 应用函数，并返回该函数
	 * 的结果
	 *
	 * @param item     要修改的物品
	 * @param function 将应用于该物品的函数。
	 * @return 函数的返回值。
	 */
	public static <T> T modify(ItemStack item, Function<ReadWriteItemNBT, T> function) {
		final NBTItem nbti = new NBTItem(item, false, false, true);
		final T val = function.apply(nbti);
		nbti.finalizeChanges();
		if (val instanceof ReadableNBT || val instanceof ReadableNBTList<?>)
			throw new NbtApiException("Tried returning part of the NBT to outside of the NBT scope!");
		nbti.setClosed();
		return val;
	}

	/**
	 * 接收一个 ItemStack 和一个 Consumer&lt;ReadWriteNBT&gt;，然后将该
	 * Consumer 应用于 ItemStack 的 NBT
	 *
	 * @param item     要修改的物品
	 * @param consumer 用于修改 NBT 的 consumer。
	 */
	public static void modify(ItemStack item, Consumer<ReadWriteItemNBT> consumer) {
		final NBTItem nbti = new NBTItem(item, false, false, true);
		consumer.accept(nbti);
		nbti.finalizeChanges();
		nbti.setClosed();
	}

	/**
	 * 接收一个实体，以及一个接收 ReadWriteNBT 并返回泛型类型 T 的函数。
	 * 然后返回该函数的结果
	 *
	 * @param entity   要修改的实体
	 * @param function 将被调用的函数。
	 * @return 返回类型与该函数的返回类型相同。
	 */
	public static <T> T modify(Entity entity, Function<ReadWriteNBT, T> function) {
		final NBTEntity nbtEnt = new NBTEntity(entity);
		final NBTContainer cont = new NBTContainer(nbtEnt.getCompound());
		final T ret = function.apply(cont);
		nbtEnt.setCompound(cont.getCompound());
		if (ret instanceof ReadableNBT || ret instanceof ReadableNBTList<?>)
			throw new NbtApiException("Tried returning part of the NBT to outside of the NBT scope!");
		nbtEnt.setClosed();
		return ret;
	}

	/**
	 * 接收一个 ItemStack 和一个 Consumer&lt;ReadWriteNBT&gt;，然后将该
	 * Consumer 以 NBT 形式应用于 ItemStack 的组件。仅适用于 1.20.5+。
	 * 此方法开销较大，请勿滥用。
	 *
	 * @param item     要修改其组件的物品
	 * @param consumer 用于修改组件的 consumer。
	 */
	public static void modifyComponents(ItemStack item, Consumer<ReadWriteNBT> consumer) {
		if (!MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_20_R4))
			throw new NbtApiException("This method only works for 1.20.5+!");
		final ReadWriteNBT nbti = NBT.itemStackToNBT(item);
		consumer.accept(nbti.getOrCreateCompound("components"));
		final ItemStack tmp = NBT.itemStackFromNBT(nbti);
		item.setItemMeta(tmp.getItemMeta());
	}

	/**
	 * 接收一个 ItemStack 和一个 Consumer&lt;ReadWriteNBT&gt;，然后将该
	 * Consumer 以 NBT 形式应用于 ItemStack 的组件。仅适用于 1.20.5+。
	 * 此方法开销较大，请勿滥用。
	 *
	 * @param item     要修改其组件的物品
	 * @param function 用于修改组件的 consumer。
	 * @return 返回类型与该函数的返回类型相同。
	 */
	public static <T> T modifyComponents(ItemStack item, Function<ReadWriteNBT, T> function) {
		if (!MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_20_R4))
			throw new NbtApiException("This method only works for 1.20.5+!");
		final ReadWriteNBT nbti = NBT.itemStackToNBT(item);
		final T ret = function.apply(nbti.getOrCreateCompound("components"));
		final ItemStack tmp = NBT.itemStackFromNBT(nbti);
		item.setItemMeta(tmp.getItemMeta());
		return ret;
	}

	/**
	 * 接收一个 ItemStack 和一个 Consumer&lt;ReadWriteNBT&gt;，然后将该
	 * Consumer 以 NBT 形式应用于 ItemStack 的组件。仅适用于 1.20.5+。
	 * 此方法开销较大，请尽量缓存结果/合理使用。
	 *
	 * @param item     要读取其组件的物品
	 * @param consumer 用于读取组件的 consumer。
	 */
	public static void getComponents(ItemStack item, Consumer<ReadableNBT> consumer) {
		if (!MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_20_R4))
			throw new NbtApiException("This method only works for 1.20.5+!");
		final ReadWriteNBT nbti = NBT.itemStackToNBT(item);
		consumer.accept(nbti.getOrCreateCompound("components"));
	}

	/**
	 * 接收一个 ItemStack 和一个 Consumer&lt;ReadWriteNBT&gt;，然后将该
	 * Consumer 以 NBT 形式应用于 ItemStack 的组件。仅适用于 1.20.5+。
	 * 此方法开销较大，请尽量缓存结果/合理使用。
	 *
	 * @param item     要读取其组件的物品
	 * @param function 用于读取组件的 consumer。
	 * @return 返回类型与该函数的返回类型相同。
	 */
	public static <T> T getComponents(ItemStack item, Function<ReadableNBT, T> function) {
		if (!MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_20_R4))
			throw new NbtApiException("This method only works for 1.20.5+!");
		final ReadWriteNBT nbti = NBT.itemStackToNBT(item);
		return function.apply(nbti.getOrCreateCompound("components"));
	}

	/**
	 * 接收一个实体，以及一个接收 ReadWriteNBT 的函数，并将该
	 * 函数应用于该实体
	 *
	 * @param entity   要修改的实体
	 * @param consumer 将以 NBTEntity 为参数调用的 consumer。
	 */
	public static void modify(Entity entity, Consumer<ReadWriteNBT> consumer) {
		final NBTEntity nbtEnt = new NBTEntity(entity);
		final NBTContainer cont = new NBTContainer(nbtEnt.getCompound());
		consumer.accept(cont);
		nbtEnt.setCompound(cont.getCompound());
		nbtEnt.setClosed();
	}

	/**
	 * 接收一个实体，以及一个接收实体持久化数据的 ReadWriteNBT 并返回泛型类型 T
	 * 的函数。然后返回该函数
	 * 的结果
	 *
	 * @param entity   要修改其数据的实体。
	 * @param function 将被调用的函数。
	 * @return 返回类型与该函数的返回类型相同。
	 */
	public static <T> T modifyPersistentData(Entity entity, Function<ReadWriteNBT, T> function) {
		final T ret = function.apply(new NBTEntity(entity).getPersistentDataContainer());
		if (ret instanceof ReadableNBT || ret instanceof ReadableNBTList<?>)
			throw new NbtApiException("Tried returning part of the NBT to outside of the NBT scope!");
		return ret;
	}

	/**
	 * 允许你修改实体的持久化数据，且没有任何返回
	 * 值
	 *
	 * @param entity   要修改的实体
	 * @param consumer 用于修改持久化数据的 consumer。
	 */
	public static void modifyPersistentData(Entity entity, Consumer<ReadWriteNBT> consumer) {
		consumer.accept(new NBTEntity(entity).getPersistentDataContainer());
	}

	/**
	 * 接收一个方块状态，以及一个接收 ReadWriteNBT 并返回泛型类型 T 的函数。
	 * 然后返回该函数的结果
	 *
	 * @param blockState 要修改的方块状态
	 * @param function   将被调用的函数。
	 * @return 返回类型与该函数的返回类型相同。
	 */
	public static <T> T modify(BlockState blockState, Function<ReadWriteNBT, T> function) {
		final NBTTileEntity blockEnt = new NBTTileEntity(blockState);
		final NBTContainer cont = new NBTContainer(blockEnt.getCompound());
		final T ret = function.apply(cont);
		blockEnt.setCompound(cont.getCompound());
		if (ret instanceof ReadableNBT || ret instanceof ReadableNBTList<?>)
			throw new NbtApiException("Tried returning part of the NBT to outside of the NBT scope!");
		blockEnt.setClosed();
		return ret;
	}

	/**
	 * 接收一个方块状态和一个 consumer，然后用该方块状态创建新的
	 * NBTTileEntity 对象，再将该
	 * NBTTileEntity 对象传给 consumer
	 *
	 * @param blockState 要修改的方块状态
	 * @param consumer   一个 Consumer&lt;ReadWriteNBT&gt;。这是一个接收
	 *                   ReadWriteNBT 并对其进行处理的函数。
	 */
	public static void modify(BlockState blockState, Consumer<ReadWriteNBT> consumer) {
		final NBTTileEntity blockEnt = new NBTTileEntity(blockState);
		final NBTContainer cont = new NBTContainer(blockEnt.getCompound());
		consumer.accept(cont);
		blockEnt.setCompound(cont.getCompound());
		blockEnt.setClosed();
	}

	/**
	 * 接收一个方块状态，以及一个接收方块实体持久化数据的 ReadWriteNBT
	 * 并返回泛型类型 T 的函数。然后返回该
	 * 函数的结果
	 *
	 * @param blockState 要修改的方块的方块状态。
	 * @param function   将被调用以修改 NBT 数据的函数。
	 * @return 返回类型与该函数的返回类型相同。
	 */
	public static <T> T modifyPersistentData(BlockState blockState, Function<ReadWriteNBT, T> function) {
		final T ret = function.apply(new NBTTileEntity(blockState).getPersistentDataContainer());
		if (ret instanceof ReadableNBT || ret instanceof ReadableNBTList<?>)
			throw new NbtApiException("Tried returning part of the NBT to outside of the NBT scope!");
		return ret;
	}

	/**
	 * 接收一个方块状态和一个 consumer，然后以方块实体的
	 * 持久化数据容器调用该 consumer
	 *
	 * @param blockState 要修改的方块的方块状态。
	 * @param consumer   一个 Consumer&lt;ReadWriteNBT&gt;。这是一个接收
	 *                   ReadWriteNBT 并对其进行处理的函数。
	 */
	public static void modifyPersistentData(BlockState blockState, Consumer<ReadWriteNBT> consumer) {
		consumer.accept(new NBTTileEntity(blockState).getPersistentDataContainer());
	}

	/**
	 * 将 ItemStack 转换为 ReadWriteNBT 对象
	 *
	 * @param itemStack 要转换为 NBT 的物品堆。
	 * @return ReadWriteNBT 对象。
	 */
	public static ReadWriteNBT itemStackToNBT(ItemStack itemStack) {
		return NBTItem.convertItemtoNBT(itemStack);
	}

	/**
	 * 将 ReadableNBT 对象转换为 ItemStack
	 *
	 * @param compound 要转换为 ItemStack 的 NBT 标签
	 * @return ItemStack
	 */
	public static ItemStack itemStackFromNBT(ReadableNBT compound) {
		return NBTItem.convertNBTtoItem((NBTCompound) compound);
	}

	/**
	 * 将 ItemStack 数组转换为 ReadWriteNBT 对象
	 *
	 * @param itemStacks 要转换为 NBT 的 ItemStack[]
	 * @return NBTItem 对象。
	 */
	public static ReadWriteNBT itemStackArrayToNBT(ItemStack[] itemStacks) {
		return NBTItem.convertItemArraytoNBT(itemStacks);
	}

	/**
	 * 将 ReadableNBT 对象转换为 ItemStack 数组
	 *
	 * @param compound 要转换为 ItemStack 数组的 NBT 标签。
	 * @return ItemStack 数组。
	 */
	public static ItemStack[] itemStackArrayFromNBT(ReadableNBT compound) {
		return NBTItem.convertNBTtoItemArray((NBTCompound) compound);
	}

	/**
	 * 创建并返回新的 NBTContainer 对象。
	 *
	 * @return NBTContainer 类的新实例。
	 */
	public static ReadWriteNBT createNBTObject() {
		return new NBTContainer();
	}

	/**
	 * 接收一个 nbt json 字符串，并返回 ReadWriteNBT 对象
	 *
	 * @param nbtString 要解析的 NBT 字符串。
	 * @return 新的 ReadWriteNBT 对象。
	 */
	public static ReadWriteNBT parseNBT(String nbtString) {
		return new NBTContainer(nbtString);
	}

	/**
	 * 读取 NBT 流并返回 ReadWriteNBT 对象
	 *
	 * @param stream 要读取的 NBT 流。
	 * @return 新的 ReadWriteNBT 对象。
	 */
	public static ReadWriteNBT readNBT(InputStream stream) {
		return new NBTContainer(stream);
	}

	/**
	 * 供使用 NMS 的其他开发者使用的辅助方法。可将任意
	 * net.minecraft.nbt.CompoundTag 包装为 NBTAPI 的 ReadWriteNBT 对象。
	 *
	 * @param nmsNbtTag 必须是有效的 net.minecraft.nbt.CompoundTag
	 * @return 新的 ReadWriteNBT 对象。
	 */
	public static ReadWriteNBT wrapNMSTag(Object nmsNbtTag) {
		return new NBTContainer(nmsNbtTag);
	}

	/**
	 * 创建一个使用 @param file 存储数据的 NBTFileHandle。如果该文件
	 * 存在，则加载其数据，否则创建新文件。
	 *
	 * @param file
	 * @throws IOException
	 */
	public static NBTFileHandle getFileHandle(File file) throws IOException {
		return new NBTFile(file);
	}

	/**
	 * 从所提供的文件读取 NBT 数据。
	 * <p>
	 * 若文件不存在则返回空标签。
	 *
	 * @param file 要读取的文件
	 * @return 文件数据的 ReadWriteNBT
	 * @throws IOException 异常
	 */
	public static ReadWriteNBT readFile(File file) throws IOException {
		return NBTFile.readFrom(file);
	}

	/**
	 * 将 NBT 数据保存到所提供的文件。
	 * <p>
	 * 若文件已存在，会将其完全覆盖。
	 *
	 * @param file 文件
	 * @param nbt  NBT 数据
	 * @throws IOException 异常
	 */
	public static void writeFile(File file, ReadWriteNBT nbt) throws IOException {
		NBTFile.saveTo(file, (NBTCompound) nbt);
	}

	/**
	 * 根据带注解的接口，为 NBT 创建只读代理类。
	 *
	 * @param <T>
	 * @param item
	 * @param wrapper
	 * @return
	 */
	public static <T extends NBTProxy> T readNbt(ItemStack item, Class<T> wrapper) {
		return new ProxyBuilder<>(new NBTItem(item, false, true, false), wrapper).readOnly().build();
	}

	/**
	 * 根据带注解的接口，为 NBT 创建只读代理类。
	 *
	 * @param <T>
	 * @param entity
	 * @param wrapper
	 * @return
	 */
	public static <T extends NBTProxy> T readNbt(Entity entity, Class<T> wrapper) {
		return new ProxyBuilder<>(new NBTEntity(entity, true), wrapper).readOnly().build();
	}

	/**
	 * 根据带注解的接口，为 NBT 创建只读代理类。
	 *
	 * @param <T>
	 * @param blockState
	 * @param wrapper
	 * @return
	 */
	public static <T extends NBTProxy> T readNbt(BlockState blockState, Class<T> wrapper) {
		return new ProxyBuilder<>(new NBTTileEntity(blockState, true), wrapper).readOnly().build();
	}

	/**
	 * 接收一个 ItemStack，对其包装在代理中的 NBT 应用函数，并
	 * 返回该函数的结果
	 *
	 * @param item     目标物品
	 * @param wrapper  目标代理类
	 * @param function 将应用于该物品的函数。
	 * @return 函数的返回值。
	 */
	public static <T, X extends NBTProxy> T modify(ItemStack item, Class<X> wrapper, Function<X, T> function) {
		final NBTItem nbti = new NBTItem(item, false, false, true);
		final T val = function.apply(new ProxyBuilder<>(nbti, wrapper).build());
		nbti.finalizeChanges();
		if (val instanceof ReadableNBT || val instanceof ReadableNBTList<?>)
			throw new NbtApiException("Tried returning part of the NBT to outside of the NBT scope!");
		nbti.setClosed();
		return val;
	}

	/**
	 * 接收一个 ItemStack，对其包装在代理中的 NBT 应用函数。
	 *
	 * @param item     要修改的物品
	 * @param wrapper  目标代理类
	 * @param consumer 用于修改 NBT 的 consumer。
	 */
	public static <X extends NBTProxy> void modify(ItemStack item, Class<X> wrapper, Consumer<X> consumer) {
		final NBTItem nbti = new NBTItem(item, false, false, true);
		consumer.accept(new ProxyBuilder<>(nbti, wrapper).build());
		nbti.finalizeChanges();
		nbti.setClosed();
	}

	/**
	 * 接收一个实体和一个函数，通过代理修改该实体
	 *
	 * @param entity   要修改的实体
	 * @param wrapper  目标代理类
	 * @param consumer 将以代理为参数调用的 consumer。
	 */
	public static <X extends NBTProxy> void modify(Entity entity, Class<X> wrapper, Consumer<X> consumer) {
		final NBTEntity nbtEnt = new NBTEntity(entity);
		final NBTContainer cont = new NBTContainer(nbtEnt.getCompound());
		consumer.accept(new ProxyBuilder<>(cont, wrapper).build());
		nbtEnt.setCompound(cont.getCompound());
		cont.setClosed();
	}

	/**
	 * 接收一个实体和一个函数，通过代理修改该实体
	 *
	 * @param entity   要修改的实体
	 * @param wrapper  目标代理类
	 * @param function 将以代理为参数调用的函数。
	 * @return 函数的返回值。
	 */
	public static <T, X extends NBTProxy> T modify(Entity entity, Class<X> wrapper, Function<X, T> function) {
		final NBTEntity nbtEnt = new NBTEntity(entity);
		final NBTContainer cont = new NBTContainer(nbtEnt.getCompound());
		final T val = function.apply(new ProxyBuilder<>(cont, wrapper).build());
		nbtEnt.setCompound(cont.getCompound());
		cont.setClosed();
		return val;
	}

	/**
	 * 接收一个方块实体和一个函数，通过代理修改该实体
	 *
	 * @param blockState 要修改的方块状态
	 * @param wrapper    目标代理类
	 * @param consumer   将被调用的 Consumer。
	 */
	public static <X extends NBTProxy> void modify(BlockState blockState, Class<X> wrapper, Consumer<X> consumer) {
		final NBTTileEntity blockEnt = new NBTTileEntity(blockState);
		final NBTContainer cont = new NBTContainer(blockEnt.getCompound());
		consumer.accept(new ProxyBuilder<>(cont, wrapper).build());
		blockEnt.setCompound(cont);
		cont.setClosed();
	}

	/**
	 * 接收一个方块实体和一个函数，通过代理修改该实体
	 *
	 * @param blockState 要修改的方块状态
	 * @param wrapper    目标代理类
	 * @param function   将被调用的函数。
	 * @return 函数的返回值。
	 */
	public static <T, X extends NBTProxy> T modify(BlockState blockState, Class<X> wrapper, Function<X, T> function) {
		final NBTTileEntity blockEnt = new NBTTileEntity(blockState);
		final NBTContainer cont = new NBTContainer(blockEnt.getCompound());
		final T val = function.apply(new ProxyBuilder<>(cont, wrapper).build());
		blockEnt.setCompound(cont);
		cont.setClosed();
		return val;
	}

	/**
	 * 读取之前存储在区块持久化数据容器中的方块专属数据。
	 * <p>
	 * 数据存储在区块持久化数据容器的 {@code blocks} compound 中，
	 * 使用 {@code X_Y_Z} 形式的键，其中 X、Y、Z
	 * 为方块的坐标。
	 * <p>
	 * <b>Minecraft 版本：</b>此方法需要 <b>1.16.4 或更高版本</b>。
	 *
	 * @param block    要读取其存储数据的方块
	 * @param consumer 接收数据的 {@link ReadableNBT} 视图的
	 *                 consumer
	 */
	public static void readChunkPDC(Block block, Consumer<ReadableNBT> consumer) {
		processBlockChunkPDC(block, false, nbt -> {
			consumer.accept(nbt);

			return null;
		});
	}

	/**
	 * 从区块的持久化数据容器读取方块专属数据，并
	 * 应用函数以产生结果。
	 * <p>
	 * 数据存储在区块持久化数据容器的 {@code blocks} compound 中，
	 * 使用 {@code X_Y_Z} 形式的键，其中 X、Y、Z
	 * 为方块的坐标。
	 * <p>
	 * <b>Minecraft 版本：</b>此方法需要 <b>1.16.4 或更高版本</b>。
	 *
	 * @param block    要读取其存储数据的方块
	 * @param function 处理 {@link ReadableNBT} 并
	 *                 返回一个值的函数
	 * @param <T>      返回值的类型
	 * @return 将函数应用于数据后的结果
	 */
	public static <T> T readAndGetChunkPDC(Block block, Function<ReadableNBT, T> function) {
		return processBlockChunkPDC(block, false, function::apply);
	}

	/**
	 * 修改存储在区块持久化数据容器中的方块专属数据。
	 * <p>
	 * 数据存储在区块持久化数据容器的 {@code blocks} compound 中，
	 * 使用 {@code X_Y_Z} 形式的键，其中 X、Y、Z
	 * 为方块的坐标。
	 * <p>
	 * <b>注意：</b>即使方块被破坏、替换、移动
	 * 或炸毁，数据也会保留。不再需要时，你应手动移除这些数据。
	 * <p>
	 * <b>Minecraft 版本：</b>此方法需要 <b>1.16.4 或更高版本</b>。
	 *
	 * @param block    要修改其数据的方块
	 * @param consumer 接收 {@link ReadWriteNBT} 并
	 *                 进行修改的 consumer
	 */
	public static void modifyChunkPDC(Block block, Consumer<ReadWriteNBT> consumer) {
		processBlockChunkPDC(block, true, nbt -> {
			consumer.accept(nbt);

			return null;
		});
	}

	/**
	 * 修改区块持久化数据容器中的方块专属数据，并
	 * 返回根据修改后的数据计算出的结果。
	 * <p>
	 * 数据存储在区块持久化数据容器的 {@code blocks} compound 中，
	 * 使用 {@code X_Y_Z} 形式的键，其中 X、Y、Z
	 * 为方块的坐标。
	 * <p>
	 * <b>注意：</b>即使方块被破坏、替换、移动
	 * 或炸毁，数据也会保留。不再需要时，你应手动移除这些数据。
	 * <p>
	 * <b>Minecraft 版本：</b>此方法需要 <b>1.16.4 或更高版本</b>。
	 *
	 * @param block    要修改其数据的方块
	 * @param function 接收 {@link ReadWriteNBT}、对其进行修改
	 *                 并返回一个值的函数
	 * @param <T>      返回值的类型
	 * @return 修改后函数的结果
	 */
	public static <T> T modifyAndGetChunkPDC(Block block, Function<ReadWriteNBT, T> function) {
		return processBlockChunkPDC(block, true, function::apply);
	}

	/*
	 * Resolve the block's compound inside the chunk's persistent data container and
	 * hand it to the given action, pruning empty leftovers when writing.
	 */
	private static <T> T processBlockChunkPDC(Block block, boolean createIfAbsent, Function<NBTCompound, T> action) {
		checkChunkPersistentDataSupported();

		final String blockKey = block.getX() + "_" + block.getY() + "_" + block.getZ();
		final NBTCompound chunkData = new NBTPersistentDataContainer(block.getChunk().getPersistentDataContainer(), !createIfAbsent);
		final NBTCompound blocksData = createIfAbsent ? chunkData.getOrCreateCompound("blocks") : chunkData.getCompound("blocks");

		NBTCompound blockData;

		if (createIfAbsent)
			blockData = blocksData.getOrCreateCompound(blockKey);

		else {
			blockData = blocksData == null ? null : blocksData.getCompound(blockKey);

			if (blockData == null)
				blockData = new NBTContainer().setReadOnly(true);
		}

		final T result = action.apply(blockData);

		if (createIfAbsent) {
			if (blockData.isEmpty())
				blocksData.removeKey(blockKey);

			if (blocksData.isEmpty())
				chunkData.removeKey("blocks");
		}

		if (result instanceof ReadableNBT || result instanceof ReadableNBTList<?>)
			throw new NbtApiException("Tried returning part of the NBT to outside of the NBT scope!");

		return result;
	}

	/**
	 * 读取区块的持久化数据容器。
	 * <p>
	 * <b>Minecraft 版本：</b>此方法需要 <b>1.16.4 或更高版本</b>。
	 *
	 * @param chunk    要读取其持久化数据的区块
	 * @param consumer 接收数据的 {@link ReadableNBT} 视图的
	 *                 consumer
	 */
	public static void readChunkPDC(Chunk chunk, Consumer<ReadableNBT> consumer) {
		processChunkPDC(chunk, false, nbt -> {
			consumer.accept(nbt);

			return null;
		});
	}

	/**
	 * 读取区块的持久化数据容器，并应用函数以产生
	 * 结果。
	 * <p>
	 * <b>Minecraft 版本：</b>此方法需要 <b>1.16.4 或更高版本</b>。
	 *
	 * @param chunk    要读取其持久化数据的区块
	 * @param function 处理 {@link ReadableNBT} 并
	 *                 返回一个值的函数
	 * @param <T>      返回值的类型
	 * @return 将函数应用于数据后的结果
	 */
	public static <T> T readAndGetChunkPDC(Chunk chunk, Function<ReadableNBT, T> function) {
		return processChunkPDC(chunk, false, function::apply);
	}

	/**
	 * 修改区块的持久化数据容器。
	 * <p>
	 * <b>Minecraft 版本：</b>此方法需要 <b>1.16.4 或更高版本</b>。
	 *
	 * @param chunk    要修改其持久化数据的区块
	 * @param consumer 接收 {@link ReadWriteNBT} 并
	 *                 进行修改的 consumer
	 */
	public static void modifyChunkPDC(Chunk chunk, Consumer<ReadWriteNBT> consumer) {
		processChunkPDC(chunk, true, nbt -> {
			consumer.accept(nbt);

			return null;
		});
	}

	/**
	 * 修改区块的持久化数据容器，并返回根据修改后的数据
	 * 计算出的结果。
	 * <p>
	 * <b>Minecraft 版本：</b>此方法需要 <b>1.16.4 或更高版本</b>。
	 *
	 * @param chunk    要修改其持久化数据的区块
	 * @param function 接收 {@link ReadWriteNBT}、对其进行修改
	 *                 并返回一个值的函数
	 * @param <T>      返回值的类型
	 * @return 修改后函数的结果
	 */
	public static <T> T modifyAndGetChunkPDC(Chunk chunk, Function<ReadWriteNBT, T> function) {
		return processChunkPDC(chunk, true, function::apply);
	}

	/*
	 * Hand the chunk's persistent data container to the given action.
	 */
	private static <T> T processChunkPDC(Chunk chunk, boolean createIfAbsent, Function<NBTCompound, T> action) {
		checkChunkPersistentDataSupported();

		final NBTCompound chunkData = new NBTPersistentDataContainer(chunk.getPersistentDataContainer(), !createIfAbsent);
		final T result = action.apply(chunkData);

		if (result instanceof ReadableNBT || result instanceof ReadableNBTList<?>)
			throw new NbtApiException("Tried returning part of the NBT to outside of the NBT scope!");

		return result;
	}

	/*
	 * Chunks only carry a persistent data container since Minecraft 1.16.4.
	 */
	private static void checkChunkPersistentDataSupported() {
		if (!MinecraftVersion.isAtLeastVersion(MinecraftVersion.MC1_16_R3))
			throw new NbtApiException("This method is only available for the version " + MinecraftVersion.MC1_16_R3.name() + " and above!");
	}

}
