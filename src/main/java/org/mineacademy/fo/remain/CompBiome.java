package org.mineacademy.fo.remain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.ValidCore;

import lombok.Getter;
import lombok.NonNull;

/**
 * 跨版本兼容的生物群系类。
 */
public enum CompBiome {

	WINDSWEPT_HILLS("MOUNTAINS", "EXTREME_HILLS") {
		@Override
		public int getId() {
			return 3;
		}
	},
	SNOWY_PLAINS("SNOWY_TUNDRA", "ICE_FLATS", "ICE_PLAINS") {
		@Override
		public int getId() {
			return 12;
		}
	},
	SPARSE_JUNGLE("JUNGLE_EDGE", "JUNGLE_EDGE") {
		@Override
		public int getId() {
			return 23;
		}
	},
	STONY_SHORE("STONE_SHORE", "STONE_BEACH") {
		@Override
		public int getId() {
			return 25;
		}
	},
	CHERRY_GROVE,
	PALE_GARDEN,
	OLD_GROWTH_PINE_TAIGA("GIANT_TREE_TAIGA", "REDWOOD_TAIGA", "MEGA_TAIGA"),
	WINDSWEPT_FOREST("WOODED_MOUNTAINS", "EXTREME_HILLS_WITH_TREES", "EXTREME_HILLS_PLUS"),
	WOODED_BADLANDS("WOODED_BADLANDS_PLATEAU", "MESA_ROCK", "MESA_PLATEAU_FOREST"),
	WINDSWEPT_GRAVELLY_HILLS("GRAVELLY_MOUNTAINS", "MUTATED_EXTREME_HILLS", "EXTREME_HILLS_MOUNTAINS") {
		@Override
		public int getId() {
			return -125;
		}
	},
	OLD_GROWTH_BIRCH_FOREST("TALL_BIRCH_FOREST", "MUTATED_BIRCH_FOREST", "BIRCH_FOREST_MOUNTAINS"),
	OLD_GROWTH_SPRUCE_TAIGA("GIANT_SPRUCE_TAIGA", "MUTATED_REDWOOD_TAIGA", "MEGA_SPRUCE_TAIGA") {
		@Override
		public int getId() {
			return -96;
		}
	},
	WINDSWEPT_SAVANNA("SHATTERED_SAVANNA", "MUTATED_SAVANNA", "SAVANNA_MOUNTAINS") {
		@Override
		public int getId() {
			return -93;
		}
	},
	MEADOW,
	MANGROVE_SWAMP,
	DEEP_DARK,
	GROVE,
	SNOWY_SLOPES,
	FROZEN_PEAKS,
	JAGGED_PEAKS,
	STONY_PEAKS,
	CUSTOM,
	BADLANDS("MESA") {
		@Override
		public int getId() {
			return 37;
		}
	},
	BADLANDS_PLATEAU(WOODED_BADLANDS, "MESA_CLEAR_ROCK", "MESA_PLATEAU") {
		@Override
		public int getId() {
			return 39;
		}
	},
	BEACH("BEACHES") {
		@Override
		public int getId() {
			return 16;
		}
	},
	BIRCH_FOREST(OLD_GROWTH_BIRCH_FOREST, "BIRCH_FOREST") {
		@Override
		public int getId() {
			return 27;
		}
	},
	BIRCH_FOREST_HILLS(OLD_GROWTH_BIRCH_FOREST, "BIRCH_FOREST_HILLS", "BIRCH_FOREST_HILLS_MOUNTAINS") {
		@Override
		public int getId() {
			return 28;
		}
	},
	COLD_OCEAN("COLD_OCEAN"),
	DARK_FOREST("ROOFED_FOREST") {
		@Override
		public int getId() {
			return 29;
		}
	},
	DARK_FOREST_HILLS("MUTATED_ROOFED_FOREST", "ROOFED_FOREST_MOUNTAINS") {
		@Override
		public int getId() {
			return -99;
		}
	},
	DEEP_COLD_OCEAN("COLD_DEEP_OCEAN"),
	DEEP_FROZEN_OCEAN("FROZEN_DEEP_OCEAN"),
	DEEP_LUKEWARM_OCEAN("LUKEWARM_DEEP_OCEAN"),
	DEEP_OCEAN("DEEP_OCEAN") {
		@Override
		public int getId() {
			return 24;
		}
	},
	DEEP_WARM_OCEAN("WARM_DEEP_OCEAN"),
	DESERT("DESERT") {
		@Override
		public int getId() {
			return 2;
		}
	},
	DESERT_HILLS("DESERT_HILLS") {
		@Override
		public int getId() {
			return 17;
		}
	},
	DESERT_LAKES("MUTATED_DESERT", "DESERT_MOUNTAINS") {
		@Override
		public int getId() {
			return -126;
		}
	},
	END_BARRENS(World.Environment.THE_END, "SKY_ISLAND_BARREN"),
	END_HIGHLANDS(World.Environment.THE_END, "SKY_ISLAND_HIGH"),
	END_MIDLANDS(World.Environment.THE_END, "SKY_ISLAND_MEDIUM"),
	ERODED_BADLANDS("MUTATED_MESA", "MESA_BRYCE") {
		@Override
		public int getId() {
			return -91;
		}
	},
	FLOWER_FOREST("MUTATED_FOREST") {
		@Override
		public int getId() {
			return -124;
		}
	},
	FOREST("FOREST") {
		@Override
		public int getId() {
			return 4;
		}
	},
	FROZEN_OCEAN("FROZEN_OCEAN") {
		@Override
		public int getId() {
			return 10;
		}
	},
	FROZEN_RIVER("FROZEN_RIVER") {
		@Override
		public int getId() {
			return 11;
		}
	},
	GIANT_SPRUCE_TAIGA(OLD_GROWTH_SPRUCE_TAIGA, "MUTATED_REDWOOD_TAIGA", "MEGA_SPRUCE_TAIGA"),
	GIANT_SPRUCE_TAIGA_HILLS(OLD_GROWTH_SPRUCE_TAIGA, "MUTATED_REDWOOD_TAIGA_HILLS", "MEGA_SPRUCE_TAIGA_HILLS") {
		@Override
		public int getId() {
			return -95;
		}
	},
	GIANT_TREE_TAIGA(OLD_GROWTH_PINE_TAIGA, "REDWOOD_TAIGA", "MEGA_TAIGA") {
		@Override
		public int getId() {
			return 32;
		}
	},
	GIANT_TREE_TAIGA_HILLS(OLD_GROWTH_PINE_TAIGA, "REDWOOD_TAIGA_HILLS", "MEGA_TAIGA_HILLS") {
		@Override
		public int getId() {
			return 33;
		}
	},
	ICE_SPIKES("MUTATED_ICE_FLATS", "ICE_PLAINS_SPIKES") {
		@Override
		public int getId() {
			return -116;
		}
	},
	JUNGLE("JUNGLE") {
		@Override
		public int getId() {
			return 21;
		}
	},
	JUNGLE_HILLS("JUNGLE_HILLS") {
		@Override
		public int getId() {
			return 22;
		}
	},
	LUKEWARM_OCEAN("LUKEWARM_OCEAN"),
	MODIFIED_BADLANDS_PLATEAU(WOODED_BADLANDS, "MUTATED_MESA_CLEAR_ROCK", "MESA_PLATEAU") {
		@Override
		public int getId() {
			return -89;
		}
	},
	MODIFIED_GRAVELLY_MOUNTAINS(WINDSWEPT_GRAVELLY_HILLS, "MUTATED_EXTREME_HILLS_WITH_TREES", "EXTREME_HILLS_MOUNTAINS", "SMALL_MOUNTAINS") {
		@Override
		public int getId() {
			return -94;
		}
	},
	MODIFIED_JUNGLE("MUTATED_JUNGLE", "JUNGLE_MOUNTAINS") {
		@Override
		public int getId() {
			return -107;
		}
	},
	MODIFIED_JUNGLE_EDGE(SPARSE_JUNGLE, "MUTATED_JUNGLE_EDGE", "JUNGLE_EDGE_MOUNTAINS") {
		@Override
		public int getId() {
			return -105;
		}
	},
	MODIFIED_WOODED_BADLANDS_PLATEAU(WOODED_BADLANDS, "MUTATED_MESA_ROCK", "MESA_PLATEAU_FOREST_MOUNTAINS") {
		@Override
		public int getId() {
			return -90;
		}
	},
	MOUNTAIN_EDGE(SPARSE_JUNGLE, "SMALLER_EXTREME_HILLS") {
		@Override
		public int getId() {
			return 20;
		}
	},
	MUSHROOM_FIELDS("MUSHROOM_ISLAND") {
		@Override
		public int getId() {
			return 14;
		}
	},
	MUSHROOM_FIELD_SHORE(STONY_SHORE, "MUSHROOM_ISLAND_SHORE", "MUSHROOM_SHORE") {
		@Override
		public int getId() {
			return 15;
		}
	},
	SOUL_SAND_VALLEY(World.Environment.NETHER),
	CRIMSON_FOREST(World.Environment.NETHER),
	WARPED_FOREST(World.Environment.NETHER),
	BASALT_DELTAS(World.Environment.NETHER),
	NETHER_WASTES(World.Environment.NETHER, "NETHER", "HELL") {
		@Override
		public int getId() {
			return 8;
		}
	},
	OCEAN("OCEAN") {
		@Override
		public int getId() {
			return 0;
		}
	},
	PLAINS("PLAINS") {
		@Override
		public int getId() {
			return 1;
		}
	},
	RIVER("RIVER") {
		@Override
		public int getId() {
			return 7;
		}
	},
	SAVANNA("SAVANNA") {
		@Override
		public int getId() {
			return 35;
		}
	},
	SAVANNA_PLATEAU(WINDSWEPT_SAVANNA, "SAVANNA_ROCK", "SAVANNA_PLATEAU") {
		@Override
		public int getId() {
			return 35;
		}
	},
	SHATTERED_SAVANNA_PLATEAU(WINDSWEPT_SAVANNA, "MUTATED_SAVANNA_ROCK", "SAVANNA_PLATEAU_MOUNTAINS") {
		@Override
		public int getId() {
			return -92;
		}
	},
	SMALL_END_ISLANDS(World.Environment.THE_END, "SKY_ISLAND_LOW"),
	SNOWY_BEACH("COLD_BEACH") {
		@Override
		public int getId() {
			return 26;
		}
	},
	SNOWY_MOUNTAINS(WINDSWEPT_HILLS, "ICE_MOUNTAINS") {
		@Override
		public int getId() {
			return 13;
		}
	},
	SNOWY_TAIGA("TAIGA_COLD", "COLD_TAIGA") {
		@Override
		public int getId() {
			return 10;
		}
	},
	TAIGA_COLD("TAIGA_COLD") {
		@Override
		public int getId() {
			return 30;
		}
	},
	SNOWY_TAIGA_HILLS("TAIGA_COLD_HILLS", "COLD_TAIGA_HILLS") {
		@Override
		public int getId() {
			return 31;
		}
	},
	SNOWY_TAIGA_MOUNTAINS(WINDSWEPT_FOREST, "MUTATED_TAIGA_COLD", "COLD_TAIGA_MOUNTAINS") {
		@Override
		public int getId() {
			return -98;
		}
	},
	SUNFLOWER_PLAINS("MUTATED_PLAINS") {
		@Override
		public int getId() {
			return -127;
		}
	},
	SWAMP("SWAMPLAND") {
		@Override
		public int getId() {
			return 6;
		}
	},
	SWAMP_HILLS("MUTATED_SWAMPLAND", "SWAMPLAND_MOUNTAINS") {
		@Override
		public int getId() {
			return -122;
		}
	},
	TAIGA("TAIGA") {
		@Override
		public int getId() {
			return 5;
		}
	},
	TAIGA_HILLS("TAIGA_HILLS") {
		@Override
		public int getId() {
			return 19;
		}
	},
	TAIGA_MOUNTAINS(WINDSWEPT_FOREST, "MUTATED_TAIGA") {
		@Override
		public int getId() {
			return -123;
		}
	},
	/**
	 * @deprecated 已在 1.18 中移除
	 */
	@Deprecated
	TALL_BIRCH_FOREST(OLD_GROWTH_BIRCH_FOREST, "MUTATED_BIRCH_FOREST", "BIRCH_FOREST_MOUNTAINS") {
		@Override
		public int getId() {
			return -101;
		}
	},
	/**
	 * @deprecated 已在 1.18 中移除
	 */
	@Deprecated
	TALL_BIRCH_HILLS(OLD_GROWTH_BIRCH_FOREST, "MUTATED_BIRCH_FOREST_HILLS", "MESA_PLATEAU_FOREST_MOUNTAINS") {
		@Override
		public int getId() {
			return -100;
		}
	},
	THE_END(World.Environment.THE_END, "SKY") {
		@Override
		public int getId() {
			return 9;
		}
	},
	THE_VOID("VOID") {
		@Override
		public int getId() {
			return 127;
		}
	},
	WARM_OCEAN("WARM_OCEAN"),
	WOODED_BADLANDS_PLATEAU("MESA_ROCK", "MESA_PLATEAU_FOREST") {
		@Override
		public int getId() {
			return 38;
		}
	},
	WOODED_HILLS("FOREST_HILLS") {
		@Override
		public int getId() {
			return 18;
		}
	},
	WOODED_MOUNTAINS("EXTREME_HILLS_WITH_TREES", "EXTREME_HILLS_PLUS") {
		@Override
		public int getId() {
			return 34;
		}
	},
	BAMBOO_JUNGLE,
	BAMBOO_JUNGLE_HILLS,
	DRIPSTONE_CAVES,
	LUSH_CAVES;

	private static final boolean HAS_HORIZONTAL_SUPPORT = MinecraftVersion.atLeast(V.v1_16);
	private static final boolean HAS_NEGATIVE = MinecraftVersion.atLeast(V.v1_17);

	@Getter
	private final Biome biome;

	@Getter
	private final World.Environment environment;

	CompBiome(@NonNull final World.Environment environment, @NonNull final String... legacyNames) {
		this(environment, null, legacyNames);
	}

	CompBiome(@NonNull final String... legacyNames) {
		this(World.Environment.NORMAL, legacyNames);
	}

	CompBiome(final CompBiome newBiome, @NonNull final String... legacyNames) {
		this(World.Environment.NORMAL, newBiome, legacyNames);
	}

	CompBiome(@NonNull final World.Environment environment, final CompBiome newVersion, @NonNull final String... legacyNames) {
		this.environment = environment;

		BiomeData.BY_NAME.put(this.name(), this);

		for (final String legacy : legacyNames)
			BiomeData.BY_NAME.put(legacy, this);

		Biome biome = ReflectionUtil.lookupEnumSilent(Biome.class, this.name());

		if (biome == null) {

			// Try the biome from upstream
			if (newVersion != null)
				biome = newVersion.biome;

			if (biome == null)
				for (final String legacy : legacyNames) {
					biome = ReflectionUtil.lookupEnumSilent(Biome.class, legacy);

					if (biome != null)
						break;
				}
		}

		if (biome != null && this.getId() != -1)
			BiomeData.BY_ID.put(this.getId(), this);

		this.biome = biome;
	}

	/**
	 * 若该生物群系在此 MC 版本中可用则返回 true。
	 *
	 * @return
	 */
	public final boolean isAvailable() {
		return this.biome != null;
	}

	/**
	 * 返回此生物群系的 Bukkit 名称
	 *
	 * @return
	 */
	public final String getBukkitName() {
		ValidCore.checkNotNull(this.biome, "Biome " + this.name() + " is not available in this server version");

		return ReflectionUtil.getEnumName(this.biome);
	}

	/**
	 * 返回生物群系 ID，不可用则为 -1
	 *
	 * @return
	 */
	public int getId() {
		return -1;
	}

	/**
	 * 设置整个区块的生物群系。区块未加载时会创建它。
	 * 注意不会发送客户端更新数据包。
	 *
	 * @param chunk
	 */
	public void setBiome(@NonNull final Chunk chunk) {
		ValidCore.checkNotNull(this.biome, "Biome " + this.name() + " is not available in this server version");

		if (!chunk.isLoaded())
			if (!chunk.load(true))
				throw new IllegalStateException("Error loading chunk at " + chunk.getX() + ", " + chunk.getZ());

		final int heightMax = HAS_HORIZONTAL_SUPPORT ? chunk.getWorld().getMaxHeight() : 1;
		final int heightMin = HAS_NEGATIVE ? chunk.getWorld().getMinHeight() : 0;

		for (int x = 0; x < 16; x++)
			for (int y = heightMin; y < heightMax; y += 4)
				for (int z = 0; z < 16; z++) {
					final Block block = chunk.getBlock(x, y, z);

					// Save performance by only changing different biomes
					if (Remain.getBiome(block) != this.biome)
						block.setBiome(this.biome);
				}

	}

	/**
	 * 修改所选区域内的生物群系。
	 * 未加载的区块会被忽略。
	 * 注意这不会向附近的客户端发送任何更新数据包。
	 *
	 * @param start 起始位置。
	 * @param end   结束位置。
	 * @since 1.0.0
	 */
	public void setBiome(@NonNull final Location start, @NonNull final Location end) {
		ValidCore.checkNotNull(this.biome, "Biome " + this.name() + " is not available in this server version");
		ValidCore.checkBoolean(start.getWorld().equals(end.getWorld()), "Locations must be in the same world, got " + start.getWorld().getName() + " and " + end.getWorld().getName());

		final World world = start.getWorld();
		final int heightMax = HAS_HORIZONTAL_SUPPORT ? world.getMaxHeight() : 1;
		final int heightMin = HAS_NEGATIVE ? world.getMinHeight() : 0;

		for (int x = start.getBlockX(); x < end.getBlockX(); x++)
			for (int y = heightMin; y < heightMax; y += 4)
				for (int z = start.getBlockZ(); z < end.getBlockZ(); z++) {
					final Block block = new Location(world, x, y, z).getBlock();

					// Save performance by only changing different biomes
					if (Remain.getBiome(block) != this.biome)
						block.setBiome(this.biome);
				}
	}

	/**
	 * 返回所有可用的生物群系
	 *
	 * @return
	 */
	public static List<CompBiome> getAvailable() {
		final List<CompBiome> availableBiomes = new ArrayList<>();

		for (final CompBiome biome : values())
			if (biome.isAvailable())
				availableBiomes.add(biome);

		Collections.sort(availableBiomes, (first, second) -> first.name().compareTo(second.name()));

		return Collections.unmodifiableList(availableBiomes);
	}

	/**
	 * 根据给定名称返回生物群系
	 *
	 * @param biome
	 * @return
	 */
	public static CompBiome fromName(@NonNull final String biome) {
		return BiomeData.BY_NAME.get(biome.toUpperCase());
	}

	/**
	 * 根据给定 id 返回生物群系
	 *
	 * @param id
	 * @return
	 */
	public static CompBiome fromId(final int id) {
		return BiomeData.BY_ID.get(id);
	}

	/**
	 * 根据给定的 Bukkit 生物群系返回生物群系
	 *
	 * @param biome
	 * @return
	 */
	public static CompBiome fromBukkit(@NonNull final Biome biome) {
		return BiomeData.BY_NAME.get(biome.name());
	}

	/**
	 * 根据给定方块返回生物群系
	 *
	 * @param block
	 * @return
	 */
	public static CompBiome fromBlock(@NonNull final Block block) {
		return fromName(ReflectionUtil.getEnumName(Remain.getBiome(block)));
	}
}

final class BiomeData {
	static final Map<String, CompBiome> BY_NAME = new HashMap<>();
	static final Map<Integer, CompBiome> BY_ID = new HashMap<>();
}