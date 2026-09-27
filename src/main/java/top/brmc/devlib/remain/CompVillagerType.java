package top.brmc.devlib.remain;

import org.bukkit.entity.Villager;
import top.brmc.devlib.ReflectionUtil;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 村民类型的兼容枚举。
 */
@RequiredArgsConstructor
public enum CompVillagerType {

	DESERT("DESERT"),
	JUNGLE("JUNGLE"),
	PLAINS("PLAINS"),
	SAVANNA("SAVANNA"),
	SNOW("SNOW"),
	SWAMP("SWAMP"),
	TAIGA("TAIGA");

	@Getter
	private final String enumName;

	/**
	 * 以 Bukkit 枚举形式返回村民职业
	 *
	 * @return
	 */
	public Villager.Type toBukkit() {
		return ReflectionUtil.lookupEnum(Villager.Type.class, this.enumName);
	}

	/**
	 * 将此职业应用到给定村民
	 *
	 * @param villager
	 */
	public void apply(final Villager villager) {
		villager.setVillagerType(this.toBukkit());
	}

	/**
	 * 将名称转换为职业
	 *
	 * @param name
	 * @return
	 */
	public static Villager.Type convertNameToBukkit(final String name) {
		for (final CompVillagerType type : values())
			if (type.getEnumName().equalsIgnoreCase(name))
				return type.toBukkit();

		return ReflectionUtil.lookupEnum(Villager.Type.class, name);
	}
}