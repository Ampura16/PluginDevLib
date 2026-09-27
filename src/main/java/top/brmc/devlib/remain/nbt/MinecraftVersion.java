package top.brmc.devlib.remain.nbt;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;

/**
 * 此类是 NBTApi 的“大脑”。它包含供其他类使用的主日志记录器，
 * 注册 bStats，并检查 Maven shading 是否
 * 正确完成。
 *
 * @author tr7zw
 */

public enum MinecraftVersion {
	UNKNOWN(Integer.MAX_VALUE), // Use the newest known mappings
	MC1_7_R4(174),
	MC1_8_R3(183),
	MC1_9_R1(191),
	MC1_9_R2(192),
	MC1_10_R1(1101),
	MC1_11_R1(1111),
	MC1_12_R1(1121),
	MC1_13_R1(1131),
	MC1_13_R2(1132),
	MC1_14_R1(1141),
	MC1_15_R1(1151),
	MC1_16_R1(1161),
	MC1_16_R2(1162),
	MC1_16_R3(1163),
	MC1_17_R1(1171),
	MC1_18_R1(1181, true),
	MC1_18_R2(1182, true),
	MC1_19_R1(1191, true),
	MC1_19_R2(1192, true),
	MC1_19_R3(1193, true),
	MC1_20_R1(1201, true),
	MC1_20_R2(1202, true),
	MC1_20_R3(1203, true),
	MC1_20_R4(1204, true),
	MC1_21_R1(1211, true),
	MC1_21_R2(1212, true),
	MC1_21_R3(1213, true),
	MC1_21_R4(1214, true),
	MC1_21_R5(1215, true),
	MC1_21_R6(1216, true),
	MC1_21_R7(1217, true),
	MC26_1(260100, true),
	MC26_2(260200, true);

	private static MinecraftVersion version;

	private static Boolean isForgePresent;
	private static Boolean isNeoForgePresent;
	private static Boolean isFabricPresent;
	private static Boolean isFoliaPresent;

	private final int versionId;
	private final boolean mojangMapping;

	private static final Map<String, MinecraftVersion> VERSION_TO_REVISION = new HashMap<String, MinecraftVersion>() {
		{
			this.put("1.20", MC1_20_R1);
			this.put("1.20.1", MC1_20_R1);
			this.put("1.20.2", MC1_20_R2);
			this.put("1.20.3", MC1_20_R3);
			this.put("1.20.4", MC1_20_R3);
			this.put("1.20.5", MC1_20_R4);
			this.put("1.20.6", MC1_20_R4);
			this.put("1.21", MC1_21_R1);
			this.put("1.21.1", MC1_21_R1);
			this.put("1.21.2", MC1_21_R2);
			this.put("1.21.3", MC1_21_R2);
			this.put("1.21.4", MC1_21_R3);
			this.put("1.21.5", MC1_21_R4);
			this.put("1.21.6", MC1_21_R5);
			this.put("1.21.7", MC1_21_R5);
			this.put("1.21.8", MC1_21_R5);
			this.put("1.21.9", MC1_21_R6);
			this.put("1.21.10", MC1_21_R6);
			this.put("1.21.11", MC1_21_R7);
			this.put("26.1", MC26_1);
			this.put("26.2", MC26_2);
		}
	};

	MinecraftVersion(int versionId) {
		this(versionId, false);
	}

	MinecraftVersion(int versionId, boolean mojangMapping) {
		this.versionId = versionId;
		this.mojangMapping = mojangMapping;
	}

	/**
	 * @return 表示版本的简单可比较 Integer。
	 */
	public int getVersionId() {
		return this.versionId;
	}

	/**
	 * @return 若方法名为 Mojang 格式、需要在内部重新映射
	 *         则为 True
	 */
	public boolean isMojangMapping() {
		return this.mojangMapping;
	}

	/**
	 * 由于 md_5 没有使用 mojmap，在为较新的 mc 版本生成映射时，
	 * 需要用此方法对插件进行“热启动”。
	 *
	 * @return
	 */
	public String getPackageName() {
		if (this == UNKNOWN)
			try {
				return Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
			} catch (final Exception ex) {
				// ignore, paper without remap, will fail
			}
		return this.name().replace("MC", "v");
	}

	/**
	 * 若当前版本至少为给定版本则返回 true
	 *
	 * @param version 最低版本
	 * @return
	 */
	public static boolean isAtLeastVersion(MinecraftVersion version) {
		return getVersion().getVersionId() >= version.getVersionId();
	}

	/**
	 * 若当前版本比给定版本更新（不相等）则返回 true
	 *
	 * @param version 最低版本
	 * @return
	 */
	public static boolean isNewerThan(MinecraftVersion version) {
		return getVersion().getVersionId() > version.getVersionId();
	}

	/**
	 * 获取此服务器的 MinecraftVersion。同时初始化 bStats 并检查
	 * shading。
	 *
	 * @return 此服务器运行的 MinecraftVersion 枚举
	 */
	public static MinecraftVersion getVersion() {
		if (version != null)
			return version;
		try {
			final String ver = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

			version = MinecraftVersion.valueOf(ver.replace("v", "MC"));

		} catch (final Exception ex) {
			version = VERSION_TO_REVISION.get(Bukkit.getServer().getBukkitVersion().split("-")[0]);

			if (version == null) {
				// check for modern versions with the new versioning scheme, also the new paper version format
				final String versionString = Bukkit.getServer().getBukkitVersion().split("-")[0].split(".build")[0];
				for (final Entry<String, MinecraftVersion> entry : VERSION_TO_REVISION.entrySet())
					if (versionString.startsWith(entry.getKey()) && version == null)
						version = entry.getValue();
					// pick the highest revision that matches the version string, in case 26.1.3 is somehow different to 26.1
					else if (versionString.startsWith(entry.getKey()) && entry.getValue().getVersionId() > version.getVersionId())
						version = entry.getValue();
			}
			if (version == null)
				version = UNKNOWN;
		}

		return version;
	}

	/**
	 * @return 若存在 Fabric 则为 True
	 */
	public static boolean isFabricPresent() {
		if (isFabricPresent != null)
			return isFabricPresent;

		try {
			Class.forName("net.fabricmc.api.ModInitializer");

			isFabricPresent = true;

		} catch (final Exception ex) {
			isFabricPresent = false;
		}

		return isFabricPresent;
	}

	/**
	 * @return 若存在 Forge 则为 True
	 */
	public static boolean isForgePresent() {
		if (isForgePresent != null)
			return isForgePresent;

		try {
			if (getVersion() == MinecraftVersion.MC1_7_R4)
				Class.forName("cpw.mods.fml.common.Loader");
			else
				Class.forName("net.minecraftforge.fml.common.Loader");

			isForgePresent = true;

		} catch (final Exception ex) {
			isForgePresent = false;
		}

		return isForgePresent;
	}

	/**
	 * @return 若存在 NeoForge 则为 True
	 */
	public static boolean isNeoForgePresent() {
		if (isNeoForgePresent != null)
			return isNeoForgePresent;

		try {
			Class.forName("net.neoforged.neoforge.common.NeoForge");

			isNeoForgePresent = true;
		} catch (final Exception ex) {
			isNeoForgePresent = false;
		}

		return isNeoForgePresent;
	}

	/**
	 * @return 若存在 Folia 则为 True
	 */
	public static boolean isFoliaPresent() {
		if (isFoliaPresent != null)
			return isFoliaPresent;

		try {
			Class.forName("io.papermc.paper.threadedregions.RegionizedServer");

			isFoliaPresent = true;
		} catch (final Exception ex) {
			isFoliaPresent = false;
		}

		return isFoliaPresent;
	}
}
