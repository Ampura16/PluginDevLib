package top.brmc.devlib.enchant;

public enum SimpleEnchantmentRarity {

	COMMON(10),
	UNCOMMON(5),
	RARE(2),
	VERY_RARE(1);

	private final int weight;

	SimpleEnchantmentRarity(int weight) {
		this.weight = weight;
	}

	/**
	 * 获取该稀有度的权重。
	 *
	 * @return 权重
	 */
	public int getWeight() {
		return weight;
	}
}
