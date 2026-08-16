package org.mineacademy.fo.settings;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.model.BoxedMessage;
import org.mineacademy.fo.model.IsInList;
import org.mineacademy.fo.model.SimpleSound;
import org.mineacademy.fo.model.SimpleTime;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.FileConfig.AccusativeHelper;
import org.mineacademy.fo.settings.FileConfig.TitleHelper;

/**
 * 一种特殊的 {@link YamlConfig}，允许以静态方式访问配置。
 * <p>
 * 只能在初始化期间加载或设置值。在你的类（以及内部类）中编写 "private static void init()"
 * 方法，我们会自动调用它！
 * <p>
 * 类加载完成后就不能再设置值了！
 */
public abstract class YamlStaticConfig {

	/**
	 * 表示 "null"，在加载没有内部来源路径的配置时
	 * 可作为便捷的简写使用。
	 */
	public static final String NO_DEFAULT = null;

	/**
	 * 我们在此存储的临时 {@link YamlConfig} 实例，用于从中获取值
	 */
	private static YamlConfig TEMPORARY_INSTANCE;

	/**
	 * 仅供内部使用：创建新的 {@link YamlConfig} 实例，并将其关联起来，
	 * 以便通过反射加载字段。
	 */
	protected YamlStaticConfig() {
		TEMPORARY_INSTANCE = new YamlConfig() {

			{
				YamlStaticConfig.this.beforeLoad();
			}

			@Override
			protected boolean saveComments() {
				return YamlStaticConfig.this.saveComments();
			}

			@Override
			protected boolean alwaysSaveOnLoad() {
				return YamlStaticConfig.this.alwaysSaveOnLoad();
			}

			@Override
			protected List<String> getUncommentedSections() {
				return YamlStaticConfig.this.getUncommentedSections();
			}

			@Override
			protected void onLoad() {
				YamlStaticConfig.this.loadViaReflection();
			}
		};
	}

	// -----------------------------------------------------------------------------------------------------
	// Main
	// -----------------------------------------------------------------------------------------------------

	/**
	 * 加载给定的静态配置类
	 *
	 * @param clazz
	 */
	public static final void load(Class<? extends YamlStaticConfig> clazz) {
		try {
			final YamlStaticConfig config = clazz.newInstance();

			config.onLoad();

			TEMPORARY_INSTANCE = null;

		} catch (final Throwable t) {
			Common.throwError(t, "Failed to load static settings " + clazz);
		}
	}

	/**
	 * 如果你需要在设置文件真正加载之前对其做任何修改，
	 * 请调用此方法。
	 */
	protected void beforeLoad() {
	}

	/**
	 * 在通过反射扫描并调用此类之前执行代码
	 * <p>
	 * 此方法在我们加载文件之后调用
	 */
	protected void preLoad() {
	}

	/**
	 * 在 {@link #load(List)} 中自动调用，你应在此调用
	 * {@link YamlConfig} 的标准加载方法
	 *
	 * @throws Exception
	 */
	protected abstract void onLoad() throws Exception;

	/**
	 * 如果你有默认文件并希望保存其中的注释，则返回 true
	 *
	 * 用户编写的注释和用户填写的值都会丢失。
	 *
	 * 请参见 {@link #getUncommentedSections()}，将包含用户可创建映射的节写入其中，
	 * 以避免丢失。
	 *
	 * @return
	 */
	protected boolean saveComments() {
		return true;
	}

	/**
	 * 若加载后总是应保存文件则返回 true。
	 *
	 * @return
	 */
	protected boolean alwaysSaveOnLoad() {
		return false;
	}

	/**
	 * 参见 {@link #saveComments()}
	 *
	 * @return
	 */
	protected List<String> getUncommentedSections() {
		return new ArrayList<>();
	}

	/*
	 * Loads the class via reflection, scanning for "private static void init()" methods to run
	 */
	private void loadViaReflection() {
		Valid.checkNotNull(TEMPORARY_INSTANCE, "Instance cannot be null " + getFileName());
		Valid.checkNotNull(TEMPORARY_INSTANCE.defaults, "Default config cannot be null for " + getFileName());

		try {
			this.preLoad();

			// Parent class if applicable.
			if (YamlStaticConfig.class.isAssignableFrom(this.getClass().getSuperclass())) {
				final Class<?> superClass = this.getClass().getSuperclass();

				this.invokeAll(superClass);
			}

			// The class itself.
			this.invokeAll(this.getClass());

		} catch (Throwable t) {
			if (t instanceof InvocationTargetException && t.getCause() != null)
				t = t.getCause();

			Remain.sneaky(t);
		}
	}

	/*
	 * Invoke all "private static void init()" methods in the class and its subclasses
	 */
	private void invokeAll(final Class<?> clazz) throws Exception {
		this.invokeMethodsIn(clazz);

		// All sub-classes in superclass.
		for (final Class<?> subClazz : clazz.getDeclaredClasses())
			this.invokeAll(subClazz);
	}

	/*
	 * Invoke all "private static void init()" methods in the class
	 */
	private void invokeMethodsIn(final Class<?> clazz) throws Exception {
		final SimplePlugin instance = SimplePlugin.getInstance();

		for (final Method method : clazz.getDeclaredMethods()) {

			// After each invocation check if the invoication broke the plugin and ignore
			if (!instance.isEnabled())
				return;

			final int mod = method.getModifiers();

			if (method.getName().equals("init")) {
				Valid.checkBoolean(Modifier.isPrivate(mod) &&
						Modifier.isStatic(mod) &&
						method.getReturnType() == Void.TYPE &&
						method.getParameterTypes().length == 0,
						"Method '" + method.getName() + "' in " + clazz + " must be 'private static void init()'");

				method.setAccessible(true);
				method.invoke(null);
			}
		}

		this.checkFields(clazz);
	}

	/*
	 * Safety check whether all fields have been set
	 */
	private void checkFields(final Class<?> clazz) throws Exception {

		if (clazz == YamlStaticConfig.class)
			return;

		for (final Field field : clazz.getDeclaredFields()) {
			field.setAccessible(true);

			if (Modifier.isPublic(field.getModifiers()))
				Valid.checkBoolean(!field.getType().isPrimitive(), "Field '" + field.getName() + "' in " + clazz + " must not be primitive!");

			Object result = null;
			try {
				result = field.get(null);
			} catch (final NullPointerException ex) {
			}
			Valid.checkNotNull(result, "Null " + field.getType().getSimpleName() + " field '" + field.getName() + "' in " + clazz);
		}
	}

	// -----------------------------------------------------------------------------------------------------
	// Delegate methods
	// -----------------------------------------------------------------------------------------------------

	protected final void loadConfiguration(String internalPath) {
		TEMPORARY_INSTANCE.loadConfiguration(internalPath, internalPath);
	}

	protected final void loadConfiguration(String from, String to) {
		TEMPORARY_INSTANCE.loadConfiguration(from, to);
	}

	protected static final void set(final String path, final Object value) {
		TEMPORARY_INSTANCE.set(path, value);
	}

	protected static final boolean isSet(final String path) {
		return TEMPORARY_INSTANCE.isSet(path);
	}

	protected static final boolean isSetDefault(final String path) {
		return TEMPORARY_INSTANCE.isSetDefault(path);
	}

	protected static final void move(final String fromRelative, final String toAbsolute) {
		TEMPORARY_INSTANCE.move(fromRelative, toAbsolute);
	}

	/**
	 * @deprecated 已重命名，请改用 {@link #setPathPrefix(String)}
	 * @param pathPrefix
	 */
	@Deprecated
	protected static final void pathPrefix(final String pathPrefix) {
		setPathPrefix(pathPrefix);
	}

	protected static final void setPathPrefix(final String pathPrefix) {
		TEMPORARY_INSTANCE.setPathPrefix(pathPrefix);
	}

	protected static final String getPathPrefix() {
		return TEMPORARY_INSTANCE.getPathPrefix();
	}

	protected static final String getFileName() {
		return TEMPORARY_INSTANCE.getFileName();
	}

	/**
	 * @deprecated 为我们一些旧插件准备的难看的变通方案，请勿使用
	 *
	 * @return
	 */
	@Deprecated
	protected static YamlConfig getInstance() {
		return TEMPORARY_INSTANCE;
	}

	// -----------------------------------------------------------------------------------------------------
	// Config manipulators
	// -----------------------------------------------------------------------------------------------------

	protected static final List<CompMaterial> getMaterialList(final String path) {
		return TEMPORARY_INSTANCE.getMaterialList(path);
	}

	protected static final StrictList<String> getCommandList(final String path) {
		return TEMPORARY_INSTANCE.getCommandList(path);
	}

	protected static final List<String> getStringList(final String path) {
		return TEMPORARY_INSTANCE.getStringList(path);
	}

	protected static final <E> Set<E> getSet(final String path, Class<E> typeOf) {
		return TEMPORARY_INSTANCE.getSet(path, typeOf);
	}

	protected static final <E> List<E> getList(final String path, final Class<E> listType) {
		return TEMPORARY_INSTANCE.getList(path, listType);
	}

	protected static final List<SerializedMap> getMapList(final String path) {
		return TEMPORARY_INSTANCE.getMapList(path);
	}

	protected static final <K, V> Map<K, List<V>> getMapList(final String path, final Class<K> keyType, Class<V> setType, Object setDeserializerParams) {
		return TEMPORARY_INSTANCE.getMapList(path, keyType, setType, setDeserializerParams);
	}

	protected static final <E> IsInList<E> getIsInList(final String path, final Class<E> listType) {
		return TEMPORARY_INSTANCE.getIsInList(path, listType);
	}

	protected static final boolean getBoolean(final String path) {
		return TEMPORARY_INSTANCE.getBoolean(path);
	}

	protected static final String getString(final String path) {
		return TEMPORARY_INSTANCE.getString(path);
	}

	protected static final int getInteger(final String path) {
		return TEMPORARY_INSTANCE.getInteger(path);
	}

	protected static final double getDouble(final String path) {
		return TEMPORARY_INSTANCE.getDouble(path);
	}

	protected static final SimpleSound getSound(final String path) {
		return TEMPORARY_INSTANCE.getSound(path);
	}

	protected static final AccusativeHelper getCasus(final String path) {
		return TEMPORARY_INSTANCE.getAccusativePeriod(path);
	}

	protected static final TitleHelper getTitle(final String path) {
		return TEMPORARY_INSTANCE.getTitle(path);
	}

	protected static final SimpleTime getTime(final String path) {
		return TEMPORARY_INSTANCE.getTime(path);
	}

	protected static final double getPercentage(String path) {
		return TEMPORARY_INSTANCE.getPercentage(path);
	}

	protected static final CompMaterial getMaterial(final String path) {
		return TEMPORARY_INSTANCE.getMaterial(path);
	}

	protected static final BoxedMessage getBoxedMessage(final String path) {
		return TEMPORARY_INSTANCE.getBoxedMessage(path);
	}

	protected static final <E> E get(final String path, final Class<E> typeOf) {
		return TEMPORARY_INSTANCE.get(path, typeOf);
	}

	protected static final Object getObject(final String path) {
		return TEMPORARY_INSTANCE.getObject(path);
	}

	protected static final SerializedMap getMap(final String path) {
		return TEMPORARY_INSTANCE.getMap(path);
	}

	protected static final <Key, Value> LinkedHashMap<Key, Value> getMap(final String path, final Class<Key> keyType, final Class<Value> valueType) {
		return TEMPORARY_INSTANCE.getMap(path, keyType, valueType);
	}
}