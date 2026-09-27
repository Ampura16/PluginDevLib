package top.brmc.devlib.remain;

import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import top.brmc.devlib.ReflectionUtil;
import top.brmc.devlib.ReflectionUtil.MissingEnumException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 村民职业的兼容枚举，会转换为“村庄与掠夺”更新之前
 * 最接近的职业（基于我这个非英语母语者的理解 :)）。
 */
@RequiredArgsConstructor
public enum CompVillagerProfession {

	NONE("FARMER"),

	/**
	 * 盔甲匠职业。穿黑色围裙。盔甲匠主要交易
	 * 铁盔甲、锁链盔甲，有时也交易钻石盔甲。
	 */
	ARMORER("BLACKSMITH"),

	/**
	 * 屠夫职业。穿白色围裙。屠夫主要交易
	 * 生的和熟的食物。
	 */
	BUTCHER("BUTCHER"),

	/**
	 * 制图师职业。穿白色长袍。制图师主要
	 * 交易探险家地图和一些纸。
	 */
	CARTOGRAPHER("FARMER"),

	/**
	 * 牧师职业。穿紫色长袍。牧师主要交易
	 * 腐肉、金锭、红石、青金石、末影珍珠、荧石
	 * 和附魔之瓶。
	 */
	CLERIC("FARMER"),

	/**
	 * 农民职业。穿棕色长袍。农民主要交易
	 * 与食物相关的物品。
	 */
	FARMER("FARMER"),

	/**
	 * 渔夫职业。穿棕色长袍。渔夫主要交易
	 * 鱼，也可能出售线和/或煤炭。
	 */
	FISHERMAN("FARMER"),

	/**
	 * 制箭师职业。穿棕色长袍。制箭师主要交易
	 * 线、弓和箭。
	 */
	FLETCHER("PRIEST"),

	/**
	 * 皮匠职业。穿白色围裙。皮匠
	 * 主要交易皮革、皮革盔甲以及鞍。
	 */
	LEATHERWORKER("BUTCHER"),

	/**
	 * 图书管理员职业。穿白色长袍。图书管理员主要交易
	 * 纸、书和附魔书。
	 */
	LIBRARIAN("LIBRARIAN"),

	/**
	 * 石匠职业。
	 */
	MASON("FARMER"),

	/**
	 * 傻子职业。穿绿色围裙，不能交易。傻子
	 * 村民什么也不做，默认没有任何交易。
	 */
	NITWIT("FARMER"),

	/**
	 * 牧羊人职业。穿棕色长袍。牧羊人主要交易
	 * 羊毛类物品和剪刀。
	 */
	SHEPHERD("FARMER"),

	/**
	 * 工具匠职业。穿黑色围裙。工具匠主要
	 * 交易铁制和钻石工具。
	 */
	TOOLSMITH("BLACKSMITH"),

	/**
	 * 武器匠职业。穿黑色围裙。武器匠主要
	 * 交易铁制和钻石武器，有时带附魔。
	 */
	WEAPONSMITH("BLACKSMITH");

	@Getter
	private final String legacyName;

	/**
	 * 以 Bukkit 枚举形式返回村民职业
	 *
	 * @return
	 */
	public Villager.Profession toBukkit() {
		try {
			return ReflectionUtil.lookupEnum(Villager.Profession.class, this.name());

		} catch (final MissingEnumException t) {
			return ReflectionUtil.lookupEnum(Villager.Profession.class, this.legacyName);
		}
	}

	/**
	 * 将此职业应用到给定村民
	 *
	 * @param villager
	 */
	public void apply(final Villager villager) {
		villager.setProfession(this.toBukkit());
	}

	/**
	 * 将名称转换为职业
	 *
	 * @param name
	 * @return
	 */
	public static Profession convertNameToBukkit(final String name) {
		for (final CompVillagerProfession profession : values())
			if (profession.name().equalsIgnoreCase(name) || profession.getLegacyName().equalsIgnoreCase(name))
				return profession.toBukkit();

		return ReflectionUtil.lookupEnum(Villager.Profession.class, name);
	}
}