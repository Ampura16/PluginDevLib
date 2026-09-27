package top.brmc.devlib.model;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import top.brmc.devlib.Common;
import top.brmc.devlib.MinecraftVersion;
import top.brmc.devlib.MinecraftVersion.V;
import top.brmc.devlib.PlayerUtil;
import top.brmc.devlib.ReflectionUtil;
import top.brmc.devlib.Valid;
import top.brmc.devlib.collection.StrictSet;
import top.brmc.devlib.debug.Debugger;
import top.brmc.devlib.exception.FoException;
import top.brmc.devlib.plugin.SimplePlugin;
import top.brmc.devlib.region.Region;
import top.brmc.devlib.remain.Remain;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.world.MultiverseWorld;
import org.mvplugins.multiverse.external.vavr.control.Option;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import com.Zrips.CMI.Modules.TabList.TabListManager;
import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketListener;
import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.IUser;
import com.earth2me.essentials.User;
import com.earth2me.essentials.UserMap;
import com.gmail.nossr50.datatypes.chat.ChatChannel;
import com.gmail.nossr50.datatypes.party.Party;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.util.player.UserManager;
import com.massivecraft.factions.Rel;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.ps.PS;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import dev.kitteh.factions.FLocation;
import dev.kitteh.factions.permissible.Relation;
import fr.xephi.authme.api.v3.AuthMeApi;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.util.DiscordUtil;
import io.lumine.mythic.api.MythicProvider;
import io.lumine.mythic.api.mobs.MobManager;
import io.lumine.mythic.core.mobs.ActiveMob;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.PlaceholderHook;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.ai.EntityTarget;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * 我们用于挂钩各种插件的主类，让你可以
 * 方便地访问它们的方法。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HookManager {

	// ------------------------------------------------------------------------------------------------------------
	// Store hook classes separately below, avoiding no such method/field errors
	// ------------------------------------------------------------------------------------------------------------

	private static AdvancedVanishHook advancedVanishHook;
	private static AuthMeHook authMeHook;
	private static BanManagerHook banManagerHook;
	private static BentoBoxHook bentoBoxHook;
	private static BossHook bossHook;
	private static CitizensHook citizensHook;
	private static CMIHook CMIHook;
	private static DiscordSRVHook discordSRVHook;
	private static EssentialsHook essentialsHook;
	private static FactionsHook factionsHook;
	private static ItemsAdderHook itemsAdderHook;
	private static LandsHook landsHook;
	private static LiteBansHook liteBansHook;
	private static LocketteProHook locketteProHook;
	private static LWCHook lwcHook;
	private static McMMOHook mcmmoHook;
	private static MultiverseHook multiverseHook;
	private static MVdWPlaceholderHook MVdWPlaceholderHook;
	private static MythicMobsHook mythicMobsHook;
	private static NickyHook nickyHook;
	private static PlaceholderAPIHook placeholderAPIHook;
	private static PlotSquaredHook plotSquaredHook;
	private static PremiumVanishHook premiumVanishHook;
	private static ProtocolLibHook protocolLibHook;
	private static ResidenceHook residenceHook;
	private static TownyHook townyHook;
	private static VaultHook vaultHook;
	private static WorldEditHook worldeditHook;
	private static WorldGuardHook worldguardHook;

	private static boolean nbtAPIDummyHook = false;
	private static boolean nuVotifierDummyHook = false;
	private static boolean townyChatDummyHook = false;

	// ------------------------------------------------------------------------------------------------------------
	// Main loading method
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 检测各种插件，并将它们的方法加载到本库中，以便你之后使用。
	 */
	public static void loadDependencies() {

		if (Common.doesPluginExist("AdvancedVanish"))
			advancedVanishHook = new AdvancedVanishHook();

		if (Common.doesPluginExist("AuthMe"))
			authMeHook = new AuthMeHook();

		if (Common.doesPluginExist("BanManager"))
			banManagerHook = new BanManagerHook();

		if (Common.doesPluginExist("BentoBox"))
			bentoBoxHook = new BentoBoxHook();

		if (Common.doesPluginExist("Boss"))
			bossHook = new BossHook();

		if (Common.doesPluginExist("Citizens"))
			citizensHook = new CitizensHook();

		if (Common.doesPluginExist("CMI"))
			CMIHook = new CMIHook();

		if (Common.doesPluginExist("DiscordSRV"))
			try {
				Class.forName("github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel");

				discordSRVHook = new DiscordSRVHook();

			} catch (final ClassNotFoundException ex) {
				Common.error(ex, "&c" + SimplePlugin.getNamed() + " failed to hook into DiscordSRV because the plugin is outdated (1.18.x is supported)!");
			}

		if (Common.doesPluginExist("Essentials"))
			essentialsHook = new EssentialsHook();

		// Various kinds of Faction plugins.
		final Plugin factions = Bukkit.getPluginManager().getPlugin("Factions");

		if (Common.doesPluginExist("FactionsX") && factions == null)
			Common.log("Note: If you want FactionX integration, install FactionsUUIDAPIProxy.");

		else if (factions != null) {
			final String ver = factions.getDescription().getVersion();
			final String main = factions.getDescription().getMain();

			if (ver.startsWith("1.6") || main.contains("FactionsUUIDAPIProxy"))
				factionsHook = new FactionsUUID();
			else if (ver.startsWith("2.")) {
				Class<?> mplayer = null;

				try {
					mplayer = Class.forName("com.massivecraft.factions.entity.MPlayer"); // only support the free version of the plugin
				} catch (final ClassNotFoundException ex) {
				}

				if (mplayer != null)
					factionsHook = new FactionsMassive();
				else
					Common.warning("Recognized MCore Factions, but it isn't hooked! Check if you have the latest version!");

			}
		}

		if (Common.doesPluginExist("ItemsAdder"))
			itemsAdderHook = new ItemsAdderHook();

		if (Common.doesPluginExist("Lands"))
			landsHook = new LandsHook();

		if (Common.doesPluginExist("LiteBans"))
			liteBansHook = new LiteBansHook();

		if (Common.doesPluginExist("Lockette"))
			locketteProHook = new LocketteProHook();

		if (Common.doesPluginExist("LWC"))
			lwcHook = new LWCHook();

		if (Common.doesPluginExist("mcMMO")) {
			final String ver = Bukkit.getPluginManager().getPlugin("mcMMO").getDescription().getVersion();

			if (ver.startsWith("2."))
				mcmmoHook = new McMMOHook();
			else
				Common.warning("Could not hook into mcMMO. Version 2.x is required, you have " + ver);
		}

		if (Common.doesPluginExist("Multiverse-Core"))
			multiverseHook = new MultiverseHook();

		if (Common.doesPluginExist("MVdWPlaceholderAPI"))
			MVdWPlaceholderHook = new MVdWPlaceholderHook();

		if (Common.doesPluginExist("MythicMobs"))
			mythicMobsHook = new MythicMobsHook();

		if (Common.doesPluginExist("Nicky"))
			nickyHook = new NickyHook();

		if (Common.doesPluginExist("PlaceholderAPI"))
			placeholderAPIHook = new PlaceholderAPIHook();

		if (Common.doesPluginExist("PlotSquared")) {
			final String ver = Bukkit.getPluginManager().getPlugin("PlotSquared").getDescription().getVersion();

			if (ver.startsWith("7.") || ver.startsWith("6.") || ver.startsWith("5.") || ver.startsWith("3."))
				plotSquaredHook = new PlotSquaredHook();
			else
				Common.warning("Could not hook into PlotSquared. Version 3.x, 5.x or 6.x required, you have " + ver);
		}

		if (Common.doesPluginExist("PremiumVanish"))
			premiumVanishHook = new PremiumVanishHook();

		if (Common.doesPluginExist("ProtocolLib")) {
			// Also check if the library is loaded properly.
			try {
				protocolLibHook = new ProtocolLibHook();

				if (MinecraftVersion.newerThan(V.v1_6))
					Class.forName("com.comphenix.protocol.wrappers.WrappedChatComponent");

			} catch (final Throwable t) {
				protocolLibHook = null;

				Common.error(t, "You are running an old and unsupported version of ProtocolLib, please update it. The plugin will continue to function without hooking into it.");
			}
		}

		if (Common.doesPluginExist("Residence"))
			residenceHook = new ResidenceHook();

		if (Common.doesPluginExist("Towny"))
			townyHook = new TownyHook();

		if (Common.doesPluginExist("Vault"))
			vaultHook = new VaultHook();

		if (Common.doesPluginExist("WorldEdit") || Common.doesPluginExist("FastAsyncWorldEdit"))
			worldeditHook = new WorldEditHook();

		if (Common.doesPluginExist("WorldGuard"))
			worldguardHook = new WorldGuardHook(worldeditHook);

		// Dummy hooks.

		if (Common.doesPluginExist("NBTAPI"))
			nbtAPIDummyHook = true;

		if (Common.doesPluginExist("Votifier"))
			nuVotifierDummyHook = true;

		if (Common.doesPluginExist("TownyChat"))
			townyChatDummyHook = true;
	}

	/**
	 * 为某个插件移除 ProtocolLib 中的数据包监听器。
	 *
	 * @param plugin 要使用的插件。
	 *
	 * @deprecated 仅供内部使用，请勿调用。
	 */
	@Deprecated
	public static void unloadDependencies(final Plugin plugin) {
		if (isProtocolLibLoaded())
			protocolLibHook.removePacketListeners(plugin);

		if (isPlaceholderAPILoaded())
			placeholderAPIHook.unregister();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Methods for determining which plugins were loaded after you call the load method
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * AdvancedVanish 是否已加载？
	 *
	 * @return
	 */
	public static boolean isAdvancedVanishLoaded() {
		return advancedVanishHook != null;
	}

	/**
	 * AuthMe Reloaded 是否已加载？我们只支持最新版本
	 *
	 * @return
	 */
	public static boolean isAuthMeLoaded() {
		return authMeHook != null;
	}

	/**
	 * BanManager 是否已加载？
	 *
	 * @return
	 */
	public static boolean isBanManagerLoaded() {
		return banManagerHook != null;
	}

	/**
	 * BentoBox 是否已加载？
	 *
	 * @return
	 */
	public static boolean isBentoBoxLoaded() {
		return bentoBoxHook != null;
	}

	/**
	 * Boss 是否已加载？
	 *
	 * @return
	 */
	public static boolean isBossLoaded() {
		return bossHook != null;
	}

	/**
	 * CMI 是否已加载？
	 *
	 * @return
	 */
	public static boolean isCMILoaded() {
		return CMIHook != null;
	}

	/**
	 * Citizens 是否已加载？
	 *
	 * @return
	 */
	public static boolean isCitizensLoaded() {
		return citizensHook != null;
	}

	/**
	 * DiscordSRV 是否已加载？
	 *
	 * @return
	 */
	public static boolean isDiscordSRVLoaded() {
		return discordSRVHook != null;
	}

	/**
	 * EssentialsX 是否已加载？
	 *
	 * @return
	 */
	public static boolean isEssentialsLoaded() {
		return essentialsHook != null;
	}

	/**
	 * 是否加载了任何派系（Faction）插件？
	 * 我们支持 FactionsUUID 和免费版 Factions。
	 *
	 * @return
	 */
	public static boolean isFactionsLoaded() {
		return factionsHook != null;
	}

	/**
	 * FastAsyncWorldEdit 是否已加载？
	 *
	 * @return
	 */
	public static boolean isFAWELoaded() {

		// Check for FastAsyncWorldEdit directly.
		final Plugin fawe = Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit");

		if (fawe != null && fawe.isEnabled())
			return true;

		// Check for legacy FastAsyncWorldEdit installations.
		final Plugin worldEdit = Bukkit.getPluginManager().getPlugin("WorldEdit");

		if (worldEdit != null && worldEdit.isEnabled() && "Fast Async WorldEdit plugin".equals(worldEdit.getDescription().getDescription()))
			return true;

		return false;
	}

	/**
	 * ItemsAdder 是否已加载？
	 *
	 * @return
	 */
	public static boolean isItemsAdderLoaded() {
		return itemsAdderHook != null;
	}

	/**
	 * Lands 是否已加载？
	 *
	 * @return
	 */
	public static boolean isLandsLoaded() {
		return landsHook != null;
	}

	/**
	 * LiteBans 是否已加载？
	 *
	 * @return
	 */
	public static boolean isLiteBansLoaded() {
		return liteBansHook != null;
	}

	/**
	 * Lockette Pro 是否已加载？
	 *
	 * @return
	 */
	public static boolean isLocketteProLoaded() {
		return locketteProHook != null;
	}

	/**
	 * LWC 是否已加载？
	 *
	 * @return
	 */
	public static boolean isLWCLoaded() {
		return lwcHook != null;
	}

	/**
	 * mcMMO 是否已加载？
	 *
	 * @return
	 */
	public static boolean isMcMMOLoaded() {
		return mcmmoHook != null;
	}

	/**
	 * Multiverse-Core 是否已加载？
	 *
	 * @return
	 */
	public static boolean isMultiverseCoreLoaded() {
		return multiverseHook != null;
	}

	/**
	 * MVdWPlaceholderAPI 是否已加载？
	 *
	 * @return
	 */
	public static boolean isMVdWPlaceholderAPILoaded() {
		return MVdWPlaceholderHook != null;
	}

	/**
	 * MythicMobs 是否已加载？
	 *
	 * @return
	 */
	public static boolean isMythicMobsLoaded() {
		return mythicMobsHook != null;
	}

	/**
	 * NBTAPI 是否已加载？
	 *
	 * @return
	 */
	public static boolean isNbtAPILoaded() {
		return nbtAPIDummyHook;
	}

	/**
	 * Nicky 是否已加载？
	 *
	 * @return
	 */
	public static boolean isNickyLoaded() {
		return nickyHook != null;
	}

	/**
	 * nuVotifier 是否已加载？
	 *
	 * @return
	 */
	public static boolean isNuVotifierLoaded() {
		return nuVotifierDummyHook;
	}

	/**
	 * PlaceholderAPI 是否已加载？
	 *
	 * @return
	 */
	public static boolean isPlaceholderAPILoaded() {
		return placeholderAPIHook != null;
	}

	/**
	 * PlotSquared 是否已加载？
	 *
	 * @return
	 */
	public static boolean isPlotSquaredLoaded() {
		return plotSquaredHook != null;
	}

	/**
	 * PremiumVanish 是否已加载？
	 *
	 * @return
	 */
	public static boolean isPremiumVanishLoaded() {
		return premiumVanishHook != null;
	}

	/**
	 * ProtocolLib 是否已加载？
	 * <p>
	 * 这不仅会检查插件是否在 plugins 文件夹中，还会检查
	 * 它是否已正确加载并正常工作。（插件过时时应能检测到
	 * 其故障。）
	 *
	 * @return
	 */
	public static boolean isProtocolLibLoaded() {
		return protocolLibHook != null;
	}

	/**
	 * Residence 是否已加载？
	 *
	 * @return
	 */
	public static boolean isResidenceLoaded() {
		return residenceHook != null;
	}

	/**
	 * Towny 是否已加载？
	 *
	 * @return
	 */
	public static boolean isTownyLoaded() {
		return townyHook != null;
	}

	/**
	 * TownyChat 是否已加载？
	 *
	 * @return
	 */
	public static boolean isTownyChatLoaded() {
		return townyHook != null && townyChatDummyHook;
	}

	/**
	 * Vault 是否已加载？
	 *
	 * @return
	 */
	public static boolean isVaultLoaded() {
		return vaultHook != null;
	}

	/**
	 * WorldEdit 是否已加载？
	 *
	 * @return
	 */
	public static boolean isWorldEditLoaded() {
		return worldeditHook != null || isFAWELoaded();
	}

	/**
	 * WorldGuard 是否已加载？
	 *
	 * @return
	 */
	public static boolean isWorldGuardLoaded() {
		return worldguardHook != null;
	}

	// ------------------------------------------------------------------------------------------------------------
	//
	//
	// Delegate methods for use from other plugins
	//
	//
	// ------------------------------------------------------------------------------------------------------------

	// ------------------------------------------------------------------------------------------------------------
	// AuthMe
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 若玩家已通过 AuthMe 登录则返回 true；若未安装 AuthMe 也返回 true。
	 *
	 * @param player 要检查的玩家。
	 * @return
	 */
	public static boolean isLogged(final Player player) {
		return !isAuthMeLoaded() || authMeHook.isLogged(player);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Boss and MythicMobs.
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 如果安装了 Boss 且给定实体是 Boss，则返回该实体的 Boss 名称，
	 * 否则返回 null。
	 *
	 * @param entity 要检查的实体。
	 * @return
	 */
	public static String getBossName(@NonNull Entity entity) {
		return isBossLoaded() ? bossHook.getBossName(entity) : null;
	}

	/**
	 * 如果安装了 MythicMobs 且给定实体是 MythicMob，则返回该实体的名称，
	 * 否则返回 null。
	 *
	 * @param entity 要检查的实体。
	 * @return
	 */
	public static String getMythicMobName(@NonNull Entity entity) {
		return isMythicMobsLoaded() ? mythicMobsHook.getBossName(entity) : null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// BentoBox
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 返回指定玩家岛屿的访客；若为 null 则返回空
	 * 集合。
	 *
	 * @param player 要检查其岛屿的玩家。
	 * @return
	 */
	public static Set<UUID> getBentoBoxVisitors(Player player) {
		return isBentoBoxLoaded() ? bentoBoxHook.getIslandVisitors(player) : new HashSet<>();
	}

	/**
	 * 返回指定玩家岛屿的合作者；若为 null 则返回空集合。
	 *
	 * @param player 要检查其岛屿的玩家。
	 * @return
	 */
	public static Set<UUID> getBentoBoxCoops(Player player) {
		return isBentoBoxLoaded() ? bentoBoxHook.getIslandCoops(player) : new HashSet<>();
	}

	/**
	 * 返回指定玩家岛屿的受信任者；若为 null 则返回空
	 * 集合。
	 *
	 * @param player 要检查其岛屿的玩家。
	 * @return
	 */
	public static Set<UUID> getBentoBoxTrustees(Player player) {
		return isBentoBoxLoaded() ? bentoBoxHook.getIslandTrustees(player) : new HashSet<>();
	}

	/**
	 * 返回指定玩家岛屿的成员；若为 null 则返回空
	 * 集合。
	 *
	 * @param player 要检查其岛屿的玩家。
	 * @return
	 */
	public static Set<UUID> getBentoBoxMembers(Player player) {
		return isBentoBoxLoaded() ? bentoBoxHook.getIslandMembers(player) : new HashSet<>();
	}

	/**
	 * 返回指定玩家岛屿的副岛主；若为 null 则返回空
	 * 集合。
	 *
	 * @param player 要检查其岛屿的玩家。
	 * @return
	 */
	public static Set<UUID> getBentoBoxSubOwners(Player player) {
		return isBentoBoxLoaded() ? bentoBoxHook.getIslandSubOwners(player) : new HashSet<>();
	}

	/**
	 * 返回指定玩家岛屿的岛主；若为 null 则返回空集合。
	 *
	 * @param player 要检查其岛屿的玩家。
	 * @return
	 */
	public static Set<UUID> getBentoBoxOwners(Player player) {
		return isBentoBoxLoaded() ? bentoBoxHook.getIslandOwners(player) : new HashSet<>();
	}

	/**
	 * 返回指定玩家岛屿的协管员；若为 null 则返回空
	 * 集合。
	 *
	 * @param player 要检查其岛屿的玩家。
	 * @return
	 */
	public static Set<UUID> getBentoBoxMods(Player player) {
		return isBentoBoxLoaded() ? bentoBoxHook.getIslandMods(player) : new HashSet<>();
	}

	/**
	 * 返回指定玩家岛屿的管理员；若为 null 则返回空集合。
	 *
	 * @param player 要检查其岛屿的玩家。
	 * @return
	 */
	public static Set<UUID> getBentoBoxAdmins(Player player) {
		return isBentoBoxLoaded() ? bentoBoxHook.getIslandAdmins(player) : new HashSet<>();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Lands
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 返回玩家领地中的玩家；若为 null 则返回空
	 * 列表。
	 *
	 * @param player 要检查其领地的玩家。
	 * @return
	 */
	public static Collection<Player> getLandPlayers(Player player) {
		return isLandsLoaded() ? landsHook.getLandPlayers(player) : new ArrayList<>();
	}

	// ------------------------------------------------------------------------------------------------------------
	// AdvancedVanish, CMI and EssentialsX
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 若给定玩家在 EssentialsX 或 CMI 中处于挂机状态则返回 true；若两个插件
	 * 都不存在则返回 false。
	 *
	 * @param player 要检查的玩家。
	 * @return
	 */
	public static boolean isAfk(final Player player) {
		final boolean essAFK = isEssentialsLoaded() && essentialsHook.isAfk(player.getName());
		final boolean cmiAFK = isCMILoaded() && CMIHook.isAfk(player);

		return essAFK || cmiAFK;
	}

	/**
	 * 若给定玩家在 EssentialsX 中处于隐身状态则返回 true。
	 *
	 * @deprecated 这不会对大多数插件进行元数据检查，
	 *             也不会进行 NMS 检查。参见 {@link PlayerUtil#isVanished(Player)}。
	 * @param player 要检查的玩家。
	 * @return
	 */
	@Deprecated
	public static boolean isVanishedEssentials(final Player player) {
		return isEssentialsLoaded() && essentialsHook.isVanished(player.getName());
	}

	/**
	 * 若给定玩家在 CMI 中处于隐身状态则返回 true。
	 *
	 * @deprecated 这不会对大多数插件进行元数据检查，
	 *             也不会进行 NMS 检查。参见 {@link PlayerUtil#isVanished(Player)}。
	 * @param player 要检查的玩家。
	 * @return
	 */
	@Deprecated
	public static boolean isVanishedCMI(final Player player) {
		return isCMILoaded() && CMIHook.isVanished(player);
	}

	/**
	 * 若给定玩家在 AdvancedVanish 中处于隐身状态则返回 true。
	 *
	 * @deprecated 这不会对大多数插件进行元数据检查，
	 *             也不会进行 NMS 检查。参见 {@link PlayerUtil#isVanished(Player)}。
	 * @param player 要检查的玩家。
	 * @return
	 */
	@Deprecated
	public static boolean isVanishedAdvancedVanish(final Player player) {
		return isAdvancedVanishLoaded() && advancedVanishHook.isVanished(player);
	}

	/**
	 * 若给定玩家在 PremiumVanish 中处于隐身状态则返回 true。
	 *
	 * @deprecated 这不会对大多数插件进行元数据检查，
	 *             也不会进行 NMS 检查。参见 {@link PlayerUtil#isVanished(Player)}。
	 * @param player 要检查的玩家。
	 * @return
	 */
	@Deprecated
	public static boolean isVanishedPremiumVanish(final Player player) {
		return isPremiumVanishLoaded() && premiumVanishHook.isVanished(player);
	}

	/**
	 * 返回玩家是否在 AdvancedVanish、CMI、
	 * PremiumVanish 和 EssentialsX 插件中处于隐身状态。
	 *
	 * @param player
	 * @return
	 */
	public static boolean isVanished(final Player player) {

		if (isVanishedPremiumVanish(player))
			return true;

		if (isVanishedAdvancedVanish(player))
			return true;

		if (isVanishedCMI(player))
			return true;

		if (isVanishedEssentials(player))
			return true;

		return false;
	}

	/**
	 * 在 AdvancedVanish、CMI、PremiumVanish 和 EssentialsX 中
	 * 设置玩家的隐身状态。
	 *
	 * @deprecated 这不会移除隐身元数据和 NMS
	 * 隐身效果。如需如此，请使用 {@link PlayerUtil#setVanished(Player, boolean)}
	 * 。
	 * @param player   要设置其隐身状态的玩家。
	 * @param vanished 要为玩家设置的隐身状态。
	 */
	@Deprecated
	public static void setVanished(@NonNull Player player, boolean vanished) {
		if (isEssentialsLoaded())
			essentialsHook.setVanished(player.getName(), vanished);

		if (isCMILoaded())
			CMIHook.setVanished(player, vanished);

		if (isAdvancedVanishLoaded())
			advancedVanishHook.setVanished(player, vanished);

		if (isPremiumVanishLoaded())
			premiumVanishHook.setVanished(player, vanished);
	}

	/**
	 * 若玩家在 BanManager、CMI、EssentialsX 或 LiteBans 中被禁言则返回 true；
	 * 若这些插件都不存在则返回 false。
	 *
	 * @param player 要检查的玩家。
	 * @return
	 */
	public static boolean isMuted(final Player player) {

		if (isEssentialsLoaded() && essentialsHook.isMuted(player.getName()))
			return true;

		if (isCMILoaded() && CMIHook.isMuted(player))
			return true;

		if (isBanManagerLoaded() && banManagerHook.isMuted(player))
			return true;

		if (isLiteBansLoaded() && liteBansHook.isMuted(player))
			return true;

		return false;
	}

	/**
	 * 如果安装了 LiteBans，则禁言给定玩家。这要求你
	 * 拥有 /lmute 命令！
	 *
	 * @param targetPlayerName  要禁言的玩家。
	 * @param durationTokenized 禁言时长。
	 * @param reason            禁言原因。
	 */
	public static void setLiteBansMute(String targetPlayerName, String durationTokenized, String reason) {
		if (isLiteBansLoaded())
			Common.dispatchCommand(Bukkit.getConsoleSender(), "lmute " + targetPlayerName + " " + durationTokenized + (reason == null || reason.isEmpty() ? "" : " " + reason));
	}

	/**
	 * 如果安装了 LiteBans，则解除给定玩家的禁言。这要求你
	 * 拥有 /lunmute 命令！
	 *
	 * @param targetPlayerName 要解除禁言的玩家。
	 */
	public static void setLiteBansUnmute(String targetPlayerName) {
		if (isLiteBansLoaded())
			Common.dispatchCommand(Bukkit.getConsoleSender(), "lunmute " + targetPlayerName);
	}

	/**
	 * 若给定玩家在 EssentialsX 或 CMI 中开启了上帝模式则返回 true；
	 * 若两个插件都不存在则返回 false。
	 *
	 * @param player 要检查的玩家。
	 * @return
	 */
	public static boolean hasGodMode(final Player player) {
		final boolean essGodMode = isEssentialsLoaded() && essentialsHook.hasGodMode(player);
		final boolean cmiGodMode = isCMILoaded() && CMIHook.hasGodMode(player);

		return essGodMode || cmiGodMode;
	}

	/**
	 * 在 CMI 和 EssentialsX 中设置玩家的上帝模式状态。
	 *
	 * @param player  要设置其上帝模式状态的玩家。
	 * @param godMode 要为玩家设置的上帝模式状态。
	 */
	public static void setGodMode(final Player player, final boolean godMode) {
		if (isEssentialsLoaded())
			essentialsHook.setGodMode(player, godMode);

		if (isCMILoaded())
			CMIHook.setGodMode(player, godMode);
	}

	/**
	 * 在 CMI 和 EssentialsX 中设置玩家最后的 /back 位置。
	 *
	 * @param player   要设置其 /back 位置的玩家。
	 * @param location 要设为玩家 /back 位置的位置。
	 */
	public static void setBackLocation(final Player player, final Location location) {
		if (isEssentialsLoaded())
			essentialsHook.setBackLocation(player.getName(), location);

		if (isCMILoaded())
			CMIHook.setLastTeleportLocation(player, location);
	}

	/**
	 * 在 CMI 和 EssentialsX 中设置玩家对给定目标的屏蔽状态
	 *
	 * @param player 要设置其屏蔽状态的玩家。
	 * @param who    希望该玩家屏蔽的目标玩家。
	 * @param ignore 要为玩家设置的屏蔽状态。
	 */
	public static void setIgnore(final UUID player, final UUID who, final boolean ignore) {
		if (isEssentialsLoaded())
			essentialsHook.setIgnore(player, who, ignore);

		if (isCMILoaded())
			CMIHook.setIgnore(player, who, ignore);
	}

	/**
	 * 若玩家在 CMI 或 EssentialsX 中屏蔽了另一名玩家则返回 true；
	 * 若两个插件都不存在则返回 false。
	 *
	 * @param player 要检查的玩家。
	 * @param who    要检查的目标玩家。
	 * @return
	 */
	public static boolean isIgnoring(final UUID player, final UUID who) {
		Valid.checkBoolean(player != null, "Player to check ignore from cannot be null/empty");
		Valid.checkBoolean(who != null, "Player to check ignore to cannot be null/empty");

		return isEssentialsLoaded() ? essentialsHook.isIgnoring(player, who) : isCMILoaded() ? CMIHook.isIgnoring(player, who) : false;
	}

	/**
	 * 从 CMI、EssentialsX 或 Nicky 返回给定接收者带颜色的昵称；
	 * 若是控制台，则返回其名称。
	 *
	 * @param sender 要获取其昵称的玩家。
	 * @return
	 */
	public static String getNickColored(final CommandSender sender) {
		return getNick(sender, false);
	}

	/**
	 * 从 CMI、EssentialsX 或 Nicky 返回给定接收者去除颜色后的昵称；
	 * 若是控制台，则返回其名称。
	 *
	 * @param sender 要获取其昵称的玩家。
	 * @return
	 */
	public static String getNickColorless(final CommandSender sender) {
		return getNick(sender, true);
	}

	/**
	 * 从 CMI、EssentialsX 或 Nicky 返回给定接收者的昵称；
	 * 若是控制台，则返回其名称。
	 *
	 * @param sender      要获取其昵称的玩家。
	 * @param stripColors 是否去除昵称中的颜色？
	 *
	 * @return
	 */
	private static String getNick(final CommandSender sender, boolean stripColors) {
		final Player player = sender instanceof Player ? (Player) sender : null;

		if (player != null && isNPC(player)) {
			Common.log("&eWarn: Called getNick for NPC " + player.getName() + "! Notify the developers to add an ignore check at " + Debugger.traceRoute(true));

			return player.getName();
		}

		if (player == null)
			return sender.getName();

		final String nickyNick = isNickyLoaded() ? nickyHook.getNick(player) : null;
		final String essNick = isEssentialsLoaded() ? essentialsHook.getNick(player.getName()) : null;
		final String cmiNick = isCMILoaded() ? CMIHook.getNick(player) : null;

		final String nick = nickyNick != null ? nickyNick : cmiNick != null ? cmiNick : essNick != null ? essNick : sender.getName();

		return stripColors ? Common.stripColors(Common.revertColorizing(nick).replace(ChatColor.COLOR_CHAR + "x", "")) : nick;
	}

	/**
	 * 尝试根据给定玩家名查找昵称；若为 null 则默认使用
	 * 给定的名称。我们只支持 CMI 和 EssentialsX。
	 *
	 * @param playerName 要使用的玩家名。
	 * @return
	 */
	public static String getNickFromName(final String playerName) {
		final String essNick = isEssentialsLoaded() ? essentialsHook.getNick(playerName) : null;
		final String cmiNick = isCMILoaded() ? CMIHook.getNick(playerName) : null;

		return cmiNick != null ? cmiNick : essNick != null ? essNick : playerName;
	}

	/**
	 * 在 CMI 和 EssentialsX 中设置给定玩家的昵称。
	 *
	 * @param playerId 要设置其昵称的玩家。
	 * @param nick     要设置的昵称。
	 */
	public static void setNick(@NonNull final UUID playerId, @Nullable String nick) {
		if (isEssentialsLoaded())
			essentialsHook.setNick(playerId, nick);

		if (isCMILoaded())
			CMIHook.setNick(playerId, nick);
	}

	/**
	 * 尝试根据昵称反查玩家名。
	 * 仅支持 CMI 和 EssentialsX。
	 *
	 * @param nick 要使用的昵称。
	 * @return
	 */
	public static String getNameFromNick(@NonNull String nick) {
		final String essNick = isEssentialsLoaded() ? essentialsHook.getNameFromNick(nick) : nick;
		final String cmiNick = isCMILoaded() ? CMIHook.getNameFromNick(nick) : nick;

		return !essNick.equals(nick) && !"".equals(essNick) ? essNick : !cmiNick.equals(nick) && !"".equals(cmiNick) ? cmiNick : nick;
	}

	// ------------------------------------------------------------------------------------------------------------
	// EssentialsX
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 返回给定玩家的回复对象；若不存在则返回
	 * null。
	 *
	 * @param player 要检查的玩家。
	 * @return
	 */
	public static Player getReplyTo(final Player player) {
		return isEssentialsLoaded() ? essentialsHook.getReplyTo(player.getName()) : null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// ItemsAdder
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 使用 ItemsAdder 替换消息中的字体图像。
	 *
	 * @param message 消息。
	 * @return
	 */
	public static String replaceFontImages(final String message) {
		return replaceFontImages(null, message);
	}

	/**
	 * 根据玩家权限，使用 ItemsAdder 替换消息中的字体图像
	 *
	 * @param player  要使用的玩家。
	 * @param message 消息。
	 * @return
	 */
	public static String replaceFontImages(@Nullable Player player, final String message) {
		return isItemsAdderLoaded() ? itemsAdderHook.replaceFontImagesLegacy(player, message) : message;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Multiverse-Core
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 返回 Multiverse-Core 中该世界名称的别名。
	 *
	 * @param world 要使用的世界。
	 * @return
	 */
	public static String getWorldAlias(final World world) {
		return isMultiverseCoreLoaded() ? multiverseHook.getWorldAlias(world.getName()) : world.getName();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Towny
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 返回玩家在 Towny 中的国家；若未加载则返回 null。
	 *
	 * @param player 要检查的玩家。
	 * @return
	 */
	public static String getNation(final Player player) {
		return isTownyLoaded() ? townyHook.getNationName(player) : null;
	}

	/**
	 * 返回玩家在 Towny 中的城镇名称；若没有则返回 null。
	 *
	 * @param player 要检查的玩家。
	 * @return
	 */
	public static String getTownName(final Player player) {
		return isTownyLoaded() ? townyHook.getTownName(player) : null;
	}

	/**
	 * 返回玩家所在城镇的在线居民，或空列表。
	 *
	 * @param player 要检查其城镇的玩家。
	 * @return
	 */
	public static Collection<? extends Player> getTownResidentsOnline(final Player player) {
		return isTownyLoaded() ? townyHook.getTownResidentsOnline(player) : new ArrayList<>();
	}

	/**
	 * 返回玩家所在国家的在线国民，或空
	 * 列表。
	 *
	 * @param player 要检查其国家的玩家。
	 * @return
	 */
	public static Collection<? extends Player> getNationPlayersOnline(final Player player) {
		return isTownyLoaded() ? townyHook.getNationPlayersOnline(player) : new ArrayList<>();
	}

	/**
	 * 返回玩家盟国中的在线国民，或空列表。
	 *
	 * @param player 要检查其盟国的玩家。
	 * @return
	 */
	public static Collection<? extends Player> getAllyPlayersOnline(final Player player) {
		return isTownyLoaded() ? townyHook.getAllyPlayersOnline(player) : new ArrayList<>();
	}

	/**
	 * 返回给定位置处城镇的所有者名称；若没有则返回 null。
	 *
	 * @param location 要检查的位置。
	 * @return
	 */
	public static String getTownOwner(final Location location) {
		return isTownyLoaded() ? townyHook.getTownOwner(location) : null;
	}

	/**
	 * 返回给定位置处的城镇名称；若没有则返回 null。
	 *
	 * @param location 要检查的位置。
	 * @return
	 */
	public static String getTown(final Location location) {
		return isTownyLoaded() ? townyHook.getTownName(location) : null;
	}

	/**
	 * 返回所有已加载城镇的列表；若没有则返回空列表。
	 *
	 * @return
	 */
	public static List<String> getTowns() {
		return isTownyLoaded() ? townyHook.getTowns() : new ArrayList<>();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Vault
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 返回玩家的前缀；若没有则返回空字符串。
	 *
	 * @param player 要获取其前缀的玩家。
	 * @return
	 */
	public static String getPlayerPrefix(final Player player) {
		return isVaultLoaded() ? vaultHook.getPlayerPrefix(player) : "";
	}

	/**
	 * 返回玩家的后缀；若没有则返回空字符串。
	 *
	 * @param player 要获取其后缀的玩家。
	 * @return
	 */
	public static String getPlayerSuffix(final Player player) {
		return isVaultLoaded() ? vaultHook.getPlayerSuffix(player) : "";
	}

	/**
	 * 返回玩家的权限组；若没有则返回空
	 * 字符串。
	 *
	 * @param player 要获取其权限组的玩家。
	 * @return
	 */
	public static String getPlayerPermissionGroup(final Player player) {
		return isVaultLoaded() ? vaultHook.getPlayerGroup(player) : "";
	}

	/**
	 * 从 Vault 返回玩家的余额（挂钩到你的经济插件）。
	 *
	 * @param player 要获取其余额的玩家。
	 * @return
	 */
	public static double getBalance(final Player player) {
		return isVaultLoaded() ? vaultHook.getBalance(player) : 0;
	}

	/**
	 * 返回货币的单数名称；若 Vault 未加载则为空。
	 *
	 * @return
	 */
	public static String getCurrencySingular() {
		return isVaultLoaded() ? vaultHook.getCurrencyNameSG() : "";
	}

	/**
	 * 返回货币的复数名称；若 Vault 未加载则为空。
	 *
	 * @return
	 */
	public static String getCurrencyPlural() {
		return isVaultLoaded() ? vaultHook.getCurrencyNamePL() : "";
	}

	/**
	 * 如果安装了 Vault，则从玩家处扣除给定数量的金钱。
	 *
	 * @param player 要扣钱的玩家。
	 * @param amount 要扣除的金额。
	 */
	public static void withdraw(final Player player, final double amount) {
		if (isVaultLoaded())
			vaultHook.withdraw(player, amount);
	}

	/**
	 * 如果安装了 Vault，则给予玩家给定数量的金钱。
	 *
	 * @param player 要给钱的玩家。
	 * @param amount 要给予的金额。
	 */
	public static void deposit(final Player player, final double amount) {
		if (isVaultLoaded())
			vaultHook.deposit(player, amount);
	}

	/**
	 * 检查给定玩家是否拥有给定权限。对于玩家可能是
	 * 来自 ProtocolLib 的临时玩家的情况，这也可以安全使用，
	 * 此时我们会使用 Vault 检查该
	 * 玩家的权限。
	 *
	 * @param player 要检查的玩家。
	 * @param perm   要检查的权限。
	 * @return
	 */
	public static boolean hasProtocolLibPermission(Player player, String perm) {
		if (isProtocolLibLoaded() && protocolLibHook.isTemporaryPlayer(player))
			return hasVaultPermission(player, perm);

		return PlayerUtil.hasPerm(player, perm);
	}

	/**
	 * 使用 Vault 检查给定玩家名是否拥有某个权限；
	 * 若 Vault 不存在则抛出错误。
	 *
	 * @param offlinePlayer 要检查的玩家。
	 * @param perm          要检查的权限。
	 *
	 * @return
	 */
	public static boolean hasVaultPermission(final OfflinePlayer offlinePlayer, final String perm) {
		Valid.checkBoolean(isVaultLoaded(), "hasVaultPermission called - Please install Vault to enable this functionality!");

		return vaultHook.hasPerm(offlinePlayer, perm);
	}

	/**
	 * 检查给定命令发送者是否拥有给定权限。
	 *
	 * 建议你事先完成以下检查，
	 * 为了获得最高性能，此方法不包含这些检查：
	 *
	 * **发送者和权限不能为 NULL**
	 * **必须安装 VAULT**
	 *
	 * 若 Vault 无法连接到兼容的权限插件，返回 NULL。
	 * 根据 Vault 检查的结果返回 TRUE 或 FALSE。
	 * 发生异常时返回 FALSE，并通过将错误打印到控制台静默失败。
	 *
	 * @param player
	 * @param permission
	 * @return
	 */
	public static Boolean hasVaultPermissionFast(final Player player, final String permission) {
		return vaultHook.hasPerm(player, permission);
	}

	/**
	 * 使用 Vault 返回玩家的主权限组；若没有则返回空
	 * 字符串。
	 *
	 * @param player 要检查的玩家。
	 * @return
	 */
	public static String getPlayerPrimaryGroup(final Player player) {
		return isVaultLoaded() ? vaultHook.getPrimaryGroup(player) : "";
	}

	/**
	 * 若 Vault 找到了可挂钩的合适聊天插件
	 * 则返回 true。
	 *
	 * @return
	 */
	public static boolean isChatIntegrated() {
		return isVaultLoaded() ? vaultHook.isChatIntegrated() : false;
	}

	/**
	 * 若 Vault 找到了可挂钩的合适经济插件
	 * 则返回 true。
	 *
	 * @return
	 */
	public static boolean isEconomyIntegrated() {
		return isVaultLoaded() ? vaultHook.isEconomyIntegrated() : false;
	}

	/**
	 * 更新 Vault 服务提供者。
	 *
	 * @deprecated 仅供内部使用。
	 */
	@Deprecated
	public static void updateVaultIntegration() {
		if (isVaultLoaded())
			vaultHook.setIntegration();
	}

	// ------------------------------------------------------------------------------------------------------------
	// PlaceholderAPI and MVdWPlaceholderAPI
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 使用 PlaceholderAPI 和 MVdWPlaceholderAPI 替换消息中的
	 * 占位符。
	 *
	 * @param player  用于解析占位符的玩家。
	 * @param message 要在其中解析占位符的消息。
	 * @return
	 */
	public static String replacePlaceholders(final @Nullable OfflinePlayer player, String message) {
		if (message == null || "".equals(message.trim()))
			return message;

		message = isPlaceholderAPILoaded() ? placeholderAPIHook.replacePlaceholders(player, message) : message;
		message = isMVdWPlaceholderAPILoaded() ? MVdWPlaceholderHook.replacePlaceholders(player, message) : message;

		return message;
	}

	/**
	 * 使用 PlaceholderAPI 替换消息中的关系型占位符。
	 *
	 * @param one     要比较的第一个玩家。
	 * @param two     要比较的第二个玩家。
	 * @param message 要在其中解析占位符的消息。
	 * @return
	 */
	public static String replaceRelationPlaceholders(final Player one, final Player two, final String message) {
		if (message == null || "".equals(message.trim()))
			return message;

		return isPlaceholderAPILoaded() ? placeholderAPIHook.replaceRelationPlaceholders(one, two, message) : message;
	}

	/**
	 * 如果已加载 PlaceholderAPI，此方法会在其中以给定的变量和值
	 * 注册一个新占位符。
	 * <p>
	 * 		变量前会自动加上你的插件名称（小写）+ _，
	 *      例如 chatcontrol_ 或 boss_ + 你的变量。
	 * <p>
	 * 		示例：如果变量是 ChatControl 中的玩家生命值："chatcontrol_health"。
	 * <p>
	 * 		该值会针对给定玩家进行计算。
	 * <p>
	 *
	 * 	 * 注意：我们现在有了新的系统，你可以改为通过
	 *                {@link Variables#addExpansion(SimpleExpansion)} 注册变量。
	 * 			      它为你提供了更好的灵活性，并且像
	 *                PlaceholderAPI 一样，你可以动态替换
	 *                不同的变量。
	 *
	 * @param variable 要添加的变量。
	 * @param value    变量的值。
	 */
	public static void addPlaceholder(final String variable, final Function<Player, String> value) {
		Variables.addExpansion(new SimpleExpansion() {

			@Override
			protected String onReplace(@NonNull CommandSender sender, String identifier) {
				return variable.equalsIgnoreCase(identifier) && sender instanceof Player ? value.apply((Player) sender) : null;
			}
		});
	}

	// ------------------------------------------------------------------------------------------------------------
	// Factions
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 获取所有已加载的派系；若没有则返回 null。
	 *
	 * @return
	 */
	public static Collection<String> getFactions() {
		return isFactionsLoaded() ? factionsHook.getFactions() : null;
	}

	/**
	 * 返回玩家所属的派系；若没有则返回 null。
	 *
	 * @param player 要检查的玩家。
	 * @return
	 */
	public static String getFaction(final Player player) {
		return isFactionsLoaded() ? factionsHook.getFaction(player) : null;
	}

	/**
	 * 返回玩家所属派系中的玩家；若没有则为空。
	 *
	 * @param player 要检查其派系的玩家。
	 * @return
	 */
	public static Collection<? extends Player> getOnlineFactionPlayers(final Player player) {
		return isFactionsLoaded() ? factionsHook.getSameFactionPlayers(player) : new ArrayList<>();
	}

	/**
	 * 返回给定位置处的派系名称；若没有则返回 null。
	 *
	 * @param location 要检查的位置。
	 * @return
	 */
	public static String getFaction(final Location location) {
		return isFactionsLoaded() ? factionsHook.getFaction(location) : null;
	}

	/**
	 * 返回给定位置处派系所有者的名称；若没有
	 * 则返回 null。
	 *
	 * @param location 要检查的位置。
	 * @return
	 */
	public static String getFactionOwner(final Location location) {
		return isFactionsLoaded() ? factionsHook.getFactionOwner(location) : null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// ProtocolLib
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 向 ProtocolLib 添加 {@link PacketAdapter} 数据包监听器。
	 * <p>
	 * 如果缺少该插件，会抛出错误。
	 *
	 * @param adapter 要添加的适配器。
	 */
	public static void addPacketListener(/* Uses an Object to prevent errors if the plugin is not installed. */final Object adapter) {
		Valid.checkBoolean(isProtocolLibLoaded(), "Cannot add packet listeners if ProtocolLib isn't installed");

		protocolLibHook.addPacketListener(adapter);
	}

	/**
	 * 从 ProtocolLib 移除 {@link PacketAdapter} 数据包监听器。
	 * <p>
	 * 如果缺少该插件，或监听器尚未注册，会抛出错误
	 *
	 * @param adapter 要移除的适配器。
	 */
	public static void removePacketListener(final Object adapter) {
		Valid.checkBoolean(isProtocolLibLoaded(), "Cannot remove packet listeners if ProtocolLib isn't installed");

		protocolLibHook.removePacketListener(adapter);
	}

	/**
	 * 向给定玩家发送 {@link PacketContainer}。
	 *
	 * @param player          要向其发送数据包容器的玩家。
	 * @param packetContainer 要发送的数据包容器。
	 */
	public static void sendPacket(final Player player, final Object packetContainer) {
		Valid.checkBoolean(isProtocolLibLoaded(), "Sending packets requires ProtocolLib to be installed and loaded");

		protocolLibHook.sendPacket(player, packetContainer);
	}

	// ------------------------------------------------------------------------------------------------------------
	// LWC
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 返回 LWC 中给定方块的所有者；若没有则返回 null。
	 *
	 * @param block 要检查的方块。
	 * @return
	 */
	public static String getLWCOwner(final Block block) {
		return isLWCLoaded() ? lwcHook.getOwner(block) : null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Lockette Pro
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 返回给定玩家在 Lockette Pro 中是否拥有给定方块。
	 *
	 * @param block  要检查的方块。
	 * @param player 要检查的玩家。
	 * @return
	 */
	public static boolean isLocketteOwner(final Block block, final Player player) {
		return isLocketteProLoaded() ? locketteProHook.isOwner(block, player) : false;
	}

	// ------------------------------------------------------------------------------------------------------------
	// Residence
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 返回 Residence 领地列表；若没有则返回空列表。
	 *
	 * @return
	 */
	public static Collection<String> getResidences() {
		return isResidenceLoaded() ? residenceHook.getResidences() : new ArrayList<>();
	}

	/**
	 * 获取给定位置处的 Residence 领地名称；若没有则返回 null。
	 *
	 * @param location 要检查的位置。
	 * @return
	 */
	public static String getResidence(final Location location) {
		return isResidenceLoaded() ? residenceHook.getResidence(location) : null;
	}

	/**
	 * 获取给定位置处的 Residence 领地所有者；若没有则返回 null。
	 *
	 * @param location 要检查的位置。
	 * @return
	 */
	public static String getResidenceOwner(final Location location) {
		return isResidenceLoaded() ? residenceHook.getResidenceOwner(location) : null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// WorldGuard
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 返回给定位置处的区域列表；若没有
	 * 则返回空列表。
	 *
	 * @param loc 要检查的位置。
	 * @return
	 */
	public static List<String> getRegions(final Location loc) {
		return isWorldGuardLoaded() ? worldguardHook.getRegionsAt(loc) : new ArrayList<>();
	}

	/**
	 * 返回已加载区域的列表；若没有则返回空列表。
	 *
	 * @return
	 */
	public static List<String> getRegions() {
		return isWorldGuardLoaded() ? worldguardHook.getAllRegions() : new ArrayList<>();
	}

	/**
	 * 按名称获取我们对 WorldGuard 区域的表示；若没有
	 * 则返回 null。
	 *
	 * @param name 要使用的名称。
	 * @return
	 */
	public static Region getRegion(final String name) {
		return isWorldGuardLoaded() ? worldguardHook.getRegion(name) : null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// PlotSquared
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 获取地皮内的玩家列表；若插件未加载则为空。
	 *
	 * @param players 用于检查其所在位置玩家的玩家。
	 * @return
	 */
	public static Collection<? extends Player> getPlotPlayers(final Player players) {
		return isPlotSquaredLoaded() ? plotSquaredHook.getPlotPlayers(players) : new ArrayList<>();
	}

	// ------------------------------------------------------------------------------------------------------------
	// mcMMO
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 返回当前激活的 mcMMO 队伍聊天。
	 *
	 * @param player 要检查的玩家。
	 * @return
	 */
	public static String getActivePartyChat(final Player player) {
		return isMcMMOLoaded() ? mcmmoHook.getActivePartyChat(player) : null;
	}

	/**
	 * 返回玩家所在队伍的在线成员；若没有
	 * 则返回空列表。
	 *
	 * @param player 要检查其队伍的玩家。
	 * @return
	 */
	public static List<Player> getMcMMOPartyRecipients(final Player player) {
		return isMcMMOLoaded() ? mcmmoHook.getPartyRecipients(player) : new ArrayList<>();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Citizens
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 若该实体是 Citizens NPC 则返回 true。
	 *
	 * @param entity 要检查的实体。
	 * @return
	 */
	public static boolean isNPC(final Entity entity) {
		return isCitizensLoaded() ? citizensHook.isNPC(entity) : false;
	}

	/**
	 * 返回该实体的目标。
	 *
	 * @param entity 要检查的实体。
	 * @return
	 */
	public static Entity getNPCTarget(final Entity entity) {
		return isCitizensLoaded() ? citizensHook.getNPCTarget(entity) : null;
	}

	// ------------------------------------------------------------------------------------------------------------
	// DiscordSRV
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * 返回所有已关联的 Discord 频道。你可以在 DiscordSRV 的 config.yml
	 * 中关联它们。
	 *
	 * @return 已关联的频道；若 DiscordSRV 未加载则为空集合。
	 */
	public static Set<String> getDiscordChannels() {
		return isDiscordSRVLoaded() ? discordSRVHook.getChannels() : new HashSet<>();
	}

	/**
	 * 使用 DiscordSRV，以给定发送者的身份向 Discord 上的某个频道
	 * 发送消息。
	 * <p>
	 * 如果发送者是玩家，可使用增强功能。
	 *
	 * @param sender  发送消息的发送者。
	 * @param channel 发送消息的频道。
	 * @param message 要发送的消息。
	 */
	public static void sendDiscordMessage(final CommandSender sender, final String channel, @NonNull final String message) {
		if (isDiscordSRVLoaded() && !Common.stripColors(message).isEmpty())
			discordSRVHook.sendMessage(sender, channel, message);
	}

	/**
	 * 如果安装了 DiscordSRV，向 Discord 频道发送消息。
	 *
	 * @param channel 发送消息的频道。
	 * @param message 要发送的消息。
	 */
	public static void sendDiscordMessage(final String channel, @NonNull final String message) {
		if (isDiscordSRVLoaded() && !Common.stripColors(message).isEmpty())
			discordSRVHook.sendMessage(channel, message);
	}
}

// ------------------------------------------------------------------------------------------------------------
//
// Below are the individual classes responsible for hooking into third party plugins
// and getting data from them. Due to often changes we do not keep these documented.
//
// ------------------------------------------------------------------------------------------------------------

class AdvancedVanishHook {

	boolean isVanished(Player player) {
		final Class<?> clazz = ReflectionUtil.lookupClass("me.quantiom.advancedvanish.util.AdvancedVanishAPI");
		final Object instance = ReflectionUtil.getStaticFieldContent(clazz, "INSTANCE");

		final Method isPlayerVanished = ReflectionUtil.getMethod(clazz, "isPlayerVanished", Player.class);

		return ReflectionUtil.invoke(isPlayerVanished, instance, player);
	}

	void setVanished(Player player, boolean vanished) {
		final Class<?> clazz = ReflectionUtil.lookupClass("me.quantiom.advancedvanish.util.AdvancedVanishAPI");
		final Object instance = ReflectionUtil.getStaticFieldContent(clazz, "INSTANCE");

		if (vanished) {
			if (!this.isVanished(player)) {
				final Method vanishPlayer = ReflectionUtil.getMethod(clazz, "vanishPlayer", Player.class, boolean.class);

				ReflectionUtil.invoke(vanishPlayer, instance, player, false);
			}

		} else if (this.isVanished(player)) {
			final Method unVanishPlayer = ReflectionUtil.getMethod(clazz, "unVanishPlayer", Player.class, boolean.class);

			ReflectionUtil.invoke(unVanishPlayer, instance, player, false);
		}
	}
}

class AuthMeHook {

	boolean isLogged(final Player player) {
		try {
			final AuthMeApi instance = AuthMeApi.getInstance();

			return instance.isAuthenticated(player);
		} catch (final Throwable t) {
			return false;
		}
	}
}

class EssentialsHook {

	private final Essentials ess;

	EssentialsHook() {
		this.ess = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
	}

	boolean hasGodMode(final Player player) {
		final User user = this.getUser(player.getName());

		return user != null ? user.isGodModeEnabled() : false;
	}

	void setGodMode(final Player player, final boolean godMode) {
		final User user = this.getUser(player.getName());

		if (user != null)
			user.setGodModeEnabled(godMode);
	}

	void setIgnore(final UUID player, final UUID toIgnore, final boolean ignore) {
		try {
			final com.earth2me.essentials.User user = this.ess.getUser(player);
			final com.earth2me.essentials.User toIgnoreUser = this.ess.getUser(toIgnore);

			if (toIgnoreUser != null)
				user.setIgnoredPlayer(toIgnoreUser, ignore);

		} catch (final Throwable t) {
		}
	}

	boolean isIgnoring(final UUID player, final UUID ignoringPlayer) {
		try {
			final com.earth2me.essentials.User user = this.ess.getUser(player);
			final com.earth2me.essentials.User ignored = this.ess.getUser(ignoringPlayer);

			return user != null && ignored != null && user.isIgnoredPlayer(ignored);

		} catch (final Throwable t) {
			return false;
		}
	}

	boolean isAfk(final String pl) {
		final IUser user = this.getUser(pl);

		return user != null ? user.isAfk() : false;
	}

	boolean isVanished(final String pl) {
		final IUser user = this.getUser(pl);

		return user != null ? user.isVanished() : false;
	}

	void setVanished(final String playerName, boolean vanished) {
		final IUser user = this.getUser(playerName);

		if (user != null && user.isVanished() != vanished)
			user.setVanished(false);
	}

	boolean isMuted(final String pl) {
		final com.earth2me.essentials.User user = this.getUser(pl);

		return user != null ? user.isMuted() : false;
	}

	Player getReplyTo(final String recipient) {
		final User user = this.getUser(recipient);

		if (user == null)
			return null;

		String replyPlayer = null;

		try {
			replyPlayer = user.getReplyRecipient().getName();

		} catch (final Throwable ex) {
			try {
				final Method getReplyTo = ReflectionUtil.getMethod(user.getClass(), "getReplyTo");

				if (getReplyTo != null) {
					final CommandSource commandSource = ReflectionUtil.invoke(getReplyTo, user);

					replyPlayer = commandSource == null ? null : commandSource.getPlayer().getName();
				}

			} catch (final Throwable t) {
				replyPlayer = null;
			}
		}

		final Player bukkitPlayer = replyPlayer == null ? null : Bukkit.getPlayer(replyPlayer);

		if (bukkitPlayer != null && bukkitPlayer.isOnline())
			return bukkitPlayer;

		return null;
	}

	String getNick(final String player) {
		final User user = this.getUser(player);

		if (user == null)
			return player;

		final String essNick = Common.getOrEmpty(user.getNickname());

		return "".equals(essNick) ? null : essNick;
	}

	void setNick(final UUID uniqueId, String nick) {
		final User user = this.getUser(uniqueId);

		if (user != null) {
			final boolean isEmpty = nick == null || Common.stripColors(nick).replace(" ", "").isEmpty();

			user.setNickname(isEmpty ? null : Common.colorize(nick));
		}
	}

	String getNameFromNick(final String maybeNick) {
		final UserMap users = this.ess.getUserMap();

		if (users != null)
			for (final UUID userId : users.getAllUniqueUsers()) {
				final User user = users.getUser(userId);

				if (user != null && user.getNickname() != null && Valid.colorlessEquals(user.getNickname(), maybeNick))
					return Common.getOrDefault(user.getName(), maybeNick);
			}

		return maybeNick;
	}

	void setBackLocation(final String player, final Location loc) {
		final User user = this.getUser(player);

		if (user != null)
			try {
				user.setLastLocation(loc);

			} catch (final Throwable t) {
			}
	}

	private User getUser(final String name) {
		User user = null;

		try {
			user = this.ess.getUser(name);
		} catch (final Throwable t) {
		}

		if (user != null)
			return user;

		if (this.ess.getUserMap() == null)
			return null;

		try {
			user = this.ess.getUserMap().getUser(name);
		} catch (final Throwable t) {
		}

		if (user != null)
			return user;

		try {
			final Method getUserFromBukkit = ReflectionUtil.getMethod(this.ess.getUserMap().getClass(), "getUserFromBukkit", String.class);

			if (getUserFromBukkit == null)
				user = this.ess.getUser(name);
			else
				user = ReflectionUtil.invoke(getUserFromBukkit, this.ess.getUserMap(), name);

		} catch (final Throwable ex) {
		}

		return user;
	}

	private User getUser(final UUID uniqueId) {
		if (this.ess.getUserMap() == null)
			return null;

		User user = null;

		try {
			user = this.ess.getUserMap().getUser(uniqueId);
		} catch (final Throwable t) {
		}

		if (user == null)
			try {
				user = this.ess.getUser(uniqueId);
			} catch (final Throwable ex) {
			}

		return user;
	}

}

class MultiverseHook {

	private Object legacyWorldManager;
	private Method legacyGetMVWorld;
	private Method legacyGetColoredWorldString;

	MultiverseHook() {
		final Plugin plugin = Bukkit.getPluginManager().getPlugin("Multiverse-Core");

		// Also support generations 2, 3, 4 give the api there didnt change much
		if (plugin != null && !plugin.getDescription().getVersion().startsWith("5")) {
			try {
				this.legacyWorldManager = plugin.getClass().getMethod("getMVWorldManager").invoke(plugin);
				this.legacyGetMVWorld = this.legacyWorldManager.getClass().getMethod("getMVWorld", String.class);

				final Class<?> mvWorld = Class.forName("com.onarandombox.MultiverseCore.api.MultiverseWorld");
				this.legacyGetColoredWorldString = mvWorld.getMethod("getColoredWorldString");

			} catch (final ReflectiveOperationException ex) {
				Common.error(ex, "Unable to hook into legacy Multiverse-Core 4. The plugin will continue normally, but world aliases will default to world names.");
			}
		}
	}

	String getWorldAlias(final String worldName) {
		if (this.legacyWorldManager != null) {
			try {
				final Object mvWorld = this.legacyGetMVWorld.invoke(this.legacyWorldManager, worldName);

				if (mvWorld != null)
					return (String) this.legacyGetColoredWorldString.invoke(mvWorld);

			} catch (final ReflectiveOperationException ex) {
				Common.error(ex, "Unable to get world alias for '" + worldName + "' from legacy Multiverse-Core 4, returning world name.");
			}

			return worldName;
		}

		final MultiverseCoreApi api = MultiverseCoreApi.get();
		final Option<MultiverseWorld> worldOption = api.getWorldManager().getWorld(worldName);

		if (!worldOption.isEmpty()) {
			final MultiverseWorld world = worldOption.get();

			return world.getAliasOrName();
		}

		return worldName;
	}
}

class TownyHook {

	Collection<? extends Player> getTownResidentsOnline(final Player pl) {
		final List<Player> recipients = new ArrayList<>();
		final String playersTown = this.getTownName(pl);

		if (!playersTown.isEmpty())
			for (final Player online : Remain.getOnlinePlayers())
				if (playersTown.equals(this.getTownName(online)))
					recipients.add(online);

		return recipients;
	}

	Collection<? extends Player> getNationPlayersOnline(final Player pl) {
		final List<Player> recipients = new ArrayList<>();
		final String playerNation = this.getNationName(pl);

		if (!playerNation.isEmpty())
			for (final Player online : Remain.getOnlinePlayers())
				if (playerNation.equals(this.getNationName(online)))
					recipients.add(online);

		return recipients;
	}

	Collection<? extends Player> getAllyPlayersOnline(final Player pl) {
		final List<Player> recipients = new ArrayList<>();
		final Resident resident = this.getResident(pl);

		if (resident != null)
			for (final Player online : Remain.getOnlinePlayers()) {
				final Resident otherResident = this.getResident(online);

				if (otherResident != null && otherResident.isAlliedWith(resident))
					recipients.add(online);
			}

		return recipients;
	}

	String getTownName(final Player pl) {
		final Town t = this.getTown(pl);

		return t != null ? t.getName() : "";
	}

	String getNationName(final Player pl) {
		final Nation n = this.getNation(pl);

		return n != null ? n.getName() : "";
	}

	List<String> getTowns() {
		try {
			//import com.palmergames.bukkit.towny.object.TownyUniverse;

			return Common.convert(TownyUniverse.getInstance().getTowns(), Town::getName);

		} catch (final Throwable e) {
			return new ArrayList<>();
		}
	}

	String getTownName(final Location loc) {
		final Town town = this.getTown(loc);

		return town != null ? town.getName() : null;
	}

	private Town getTown(final Location loc) {
		try {
			final WorldCoord worldCoord = WorldCoord.parseWorldCoord(loc);
			final TownBlock townBlock = TownyUniverse.getInstance().getTownBlock(worldCoord);

			return townBlock != null ? townBlock.getTown() : null;

		} catch (final Throwable e) {
			return null;
		}
	}

	String getTownOwner(final Location loc) {
		try {
			final Town town = this.getTown(loc);

			return town != null ? town.getMayor().getName() : null;

		} catch (final Throwable e) {
			return null;
		}
	}

	private Nation getNation(final Player pl) {
		final Town town = this.getTown(pl);

		try {
			return town.getNation();

		} catch (final Throwable ex) {
			return null;
		}
	}

	private Town getTown(final Player pl) {
		final Resident res = this.getResident(pl);

		try {
			return res.getTown();

		} catch (final Throwable ex) {
			return null;
		}
	}

	private Resident getResident(final Player player) {
		try {
			return TownyUniverse.getInstance().getResident(player.getName());

		} catch (final Throwable e) {
			return null;
		}
	}
}

class ProtocolLibHook {

	private final ProtocolManager manager;
	private final StrictSet<Object> registeredListeners = new StrictSet<>();

	ProtocolLibHook() {
		this.manager = ProtocolLibrary.getProtocolManager();

		if (this.manager == null)
			Common.warning("Unable to get protocol manager. Ensure ProtocolLib threw no errors in your startup log and is compatible with your server version. "
					+ "If you're a developer, place ProtocolLib to softDepend in plugin.yml. Packet features won't function.");
	}

	final void addPacketListener(final Object listener) {
		Valid.checkBoolean(listener instanceof PacketListener, "Listener must extend or implements PacketListener or PacketAdapter");

		if (this.manager != null) {
			try {
				this.manager.addPacketListener((PacketListener) listener);

			} catch (final Throwable t) {
				Common.error(t, "Failed to register ProtocolLib packet listener! Ensure you have the latest ProtocolLib. If you reloaded, try a fresh startup (some ProtocolLib esp. for 1.8.8 fails on reload).");

				return;
			}

			this.registeredListeners.add(listener);
		}
	}

	final void removePacketListener(final Object listener) {
		Valid.checkBoolean(listener instanceof PacketListener, "Listener must extend or implements PacketListener or PacketAdapter");

		if (this.manager != null) {
			Valid.checkBoolean(this.registeredListeners.contains(listener), "Listener must already be registered with ProtocolLib.");

			try {
				this.manager.removePacketListener((PacketListener) listener);

			} catch (final Throwable t) {
				Common.error(t, "Failed to unregister ProtocolLib packet listener!");

				return;
			}

			this.registeredListeners.remove(listener);
		}
	}

	final void removePacketListeners(final Plugin plugin) {
		if (this.manager != null) {
			try {
				this.manager.removePacketListeners(plugin);
			} catch (final Throwable t) {
			}

			this.registeredListeners.clear();
		}
	}

	final void sendPacket(final PacketContainer packet) {
		for (final Player player : Remain.getOnlinePlayers())
			this.sendPacket(player, packet);
	}

	final void sendPacket(final Player player, final Object packet) {
		Valid.checkNotNull(player);
		Valid.checkBoolean(packet instanceof PacketContainer, "Packet must be instance of PacketContainer from ProtocolLib");

		if (this.manager != null)
			try {
				this.manager.sendServerPacket(player, (PacketContainer) packet);

			} catch (final Exception e) {
				Common.error(e, "Failed to send " + ((PacketContainer) packet).getType() + " packet to " + player.getName());
			}
	}

	final boolean isTemporaryPlayer(Player player) {
		try {
			return player != null && player.getClass().getSimpleName().contains("TemporaryPlayer"); // Solves compatibiltiy issues

		} catch (final NoClassDefFoundError err) {
			return false;
		}
	}
}

class VaultHook {

	private Chat chat;
	private Economy economy;
	private Permission permissions;

	VaultHook() {
		this.setIntegration();
	}

	void setIntegration() {
		final RegisteredServiceProvider<Economy> economyProvider = Bukkit.getServicesManager().getRegistration(Economy.class);
		final RegisteredServiceProvider<Chat> chatProvider = Bukkit.getServicesManager().getRegistration(Chat.class);
		final RegisteredServiceProvider<Permission> permProvider = Bukkit.getServicesManager().getRegistration(Permission.class);

		if (economyProvider != null)
			this.economy = economyProvider.getProvider();

		if (chatProvider != null)
			this.chat = chatProvider.getProvider();

		if (permProvider != null)
			this.permissions = permProvider.getProvider();
	}

	boolean isChatIntegrated() {
		return this.chat != null;
	}

	boolean isEconomyIntegrated() {
		return this.economy != null;
	}

	// ------------------------------------------------------------------------------
	// Economy
	// ------------------------------------------------------------------------------

	String getCurrencyNameSG() {
		return this.economy != null ? Common.getOrEmpty(this.economy.currencyNameSingular()) : "Money";
	}

	String getCurrencyNamePL() {
		return this.economy != null ? Common.getOrEmpty(this.economy.currencyNamePlural()) : "Money";
	}

	double getBalance(final Player player) {
		return this.economy != null ? this.economy.getBalance(player) : -1;
	}

	void withdraw(final Player player, final double amount) {
		if (this.economy != null)
			this.economy.withdrawPlayer(player.getName(), amount);
	}

	void deposit(final Player player, final double amount) {
		if (this.economy != null)
			this.economy.depositPlayer(player.getName(), amount);
	}

	// ------------------------------------------------------------------------------
	// Permissions
	// ------------------------------------------------------------------------------

	@Nullable
	Boolean hasPerm(final Player player, final String permission) {
		if (this.permissions == null)
			return null;

		try {
			return this.permissions.playerHas((World) null, player.getName(), permission);

		} catch (final Throwable t) {
			Common.logTimed(900,
					"SEVERE: Unable to ask Vault plugin if " + player.getName() + " has '" + permission + "' permission, returning false. "
							+ "This error only shows every 15 minutes. "
							+ "Run /vault-info and check if your permissions plugin is running correctly.");

			return false;
		}
	}

	Boolean hasPerm(@NonNull final OfflinePlayer player, final String perm) {
		try {
			return this.permissions != null ? perm != null ? this.permissions.playerHas((String) null, player, perm) : true : null;

		} catch (final Throwable t) {
			Common.logTimed(900,
					"SEVERE: Unable to ask Vault plugin if " + player.getName() + " has " + perm + " permission, returning false. "
							+ "This error only shows every 15 minutes. "
							+ "Run /vault-info and check if your permissions plugin is running correctly.");

			return false;
		}
	}

	Boolean hasPerm(@NonNull final String player, final String perm) {
		try {
			return this.permissions != null ? perm != null ? this.permissions.has((String) null, player, perm) : true : null;
		} catch (final UnsupportedOperationException t) {
			return false; // No supported plugin installed.
		}
	}

	Boolean hasPerm(@NonNull final String world, @NonNull final String player, final String perm) {
		try {
			return this.permissions != null ? perm != null ? this.permissions.has(world, player, perm) : true : null;
		} catch (final UnsupportedOperationException t) {
			return false; // No supported plugin installed.
		}
	}

	String getPrimaryGroup(final Player player) {
		try {
			return this.permissions != null ? this.permissions.getPrimaryGroup(player) : "";

		} catch (final UnsupportedOperationException t) {
			return ""; // No supported plugin installed.
		}
	}

	// ------------------------------------------------------------------------------
	// Prefix / Suffix
	// ------------------------------------------------------------------------------

	String getPlayerPrefix(final Player player) {
		try {
			return this.lookupVault(player, VaultPart.PREFIX);
		} catch (final UnsupportedOperationException t) {
			return ""; // No supported plugin installed.
		}
	}

	String getPlayerSuffix(final Player player) {
		try {
			return this.lookupVault(player, VaultPart.SUFFIX);
		} catch (final UnsupportedOperationException t) {
			return ""; // No supported plugin installed.
		}
	}

	String getPlayerGroup(final Player player) {
		try {
			return this.lookupVault(player, VaultPart.GROUP);
		} catch (final UnsupportedOperationException t) {
			return ""; // No supported plugin installed.
		}
	}

	private String lookupVault(final Player player, final VaultPart vaultPart) {
		if (this.chat == null)
			return "";

		final String[] groups = this.chat.getPlayerGroups(player);
		String fallback = vaultPart == VaultPart.PREFIX ? this.chat.getPlayerPrefix(player) : vaultPart == VaultPart.SUFFIX ? this.chat.getPlayerSuffix(player) : groups != null && groups.length > 0 ? groups[0] : "";

		if (fallback == null)
			fallback = "";

		if (vaultPart == VaultPart.PREFIX /*&& !SimplePlugin.getInstance().vaultMultiPrefix()*/ || vaultPart == VaultPart.SUFFIX /*&& !SimplePlugin.getInstance().vaultMultiSuffix()*/)
			return fallback;

		final List<String> list = new ArrayList<>();

		if (!fallback.isEmpty())
			list.add(fallback);

		if (groups != null)
			for (final String group : groups) {
				final String part = vaultPart == VaultPart.PREFIX ? this.chat.getGroupPrefix(player.getWorld(), group) : vaultPart == VaultPart.SUFFIX ? this.chat.getGroupSuffix(player.getWorld(), group) : group;

				if (part != null && !part.isEmpty() && !list.contains(part))
					list.add(part);
			}

		return Common.join(list, vaultPart == VaultPart.GROUP ? ", " : "");
	}

	enum VaultPart {
		PREFIX,
		SUFFIX,
		GROUP,
	}
}

class PlaceholderAPIHook {

	private final VariablesInjector injector;

	PlaceholderAPIHook() {
		injector = new VariablesInjector();

		try {
			injector.register();

		} catch (final Throwable throwable) {
			Common.error(throwable, "Failed to inject our variables into PlaceholderAPI!");
		}
	}

	final void unregister() {
		if (injector != null)
			try {
				injector.unregister();

			} catch (final Throwable t) {
				// Silence, the plugin probably got removed in the meantime.
			}
	}

	final String replacePlaceholders(final OfflinePlayer player, final String msg) {
		try {
			return this.setPlaceholders(player, msg);

		} catch (final Throwable t) {
			Common.error(t,
					"PlaceholderAPI failed to replace variables!",
					"Player: " + (player == null ? "none" : player.getName()),
					"Message: " + msg,
					"Error: %error");

			return msg;
		}
	}

	private String setPlaceholders(final OfflinePlayer player, String text) {
		final String oldText = text;
		final Map<String, PlaceholderExpansion> hooks = new HashMap<>();

		// MineAcademy edit: Case insensitive
		for (final PlaceholderExpansion expansion : PlaceholderAPIPlugin.getInstance().getLocalExpansionManager().getExpansions())
			hooks.put(expansion.getIdentifier().toLowerCase(), expansion);

		if (hooks.isEmpty())
			return text;

		text = this.setPlaceholders(player, oldText, text, hooks, Variables.VARIABLE_PATTERN.matcher(text));
		text = this.setPlaceholders(player, oldText, text, hooks, Variables.BRACKET_VARIABLE_PATTERN.matcher(text));

		return text;
	}

	private String setPlaceholders(@Nullable OfflinePlayer player, String oldText, String text, Map<String, PlaceholderExpansion> hooks, Matcher matcher) {
		while (matcher.find()) {
			String format = matcher.group(1);
			boolean frontSpace = false;
			boolean backSpace = false;

			if (format.startsWith("+")) {
				frontSpace = true;

				format = format.substring(1);
			}

			if (format.endsWith("+")) {
				backSpace = true;

				format = format.substring(0, format.length() - 1);
			}

			final int index = format.indexOf("_");

			if (index <= 0 || index >= format.length())
				continue;

			final String identifier = format.substring(0, index).toLowerCase();
			final String params = format.substring(index + 1);
			final String finalFormat = format;

			if (hooks.containsKey(identifier)) {

				// Wait 0.5 seconds then kill the thread to prevent server
				// crashing on PlaceholderAPI variables hanging up on the main thread
				final Thread currentThread = Thread.currentThread();
				final boolean main = Bukkit.isPrimaryThread();
				final BukkitTask watchDog = Common.runLater(main ? 30 : 80, () -> {
					Common.logFramed(
							"IMPORTANT: PREVENTED SERVER CRASH FROM PLACEHOLDERAPI",
							"",
							"Replacing PlaceholderAPI variable took over " + (main ? "1.5" : "4") + " sec",
							"and was interrupted to prevent hanging the server.",
							"",
							"This is typically caused when a variable sends a",
							"blocking HTTP request, such as checking stuff on",
							"the Internet or resolving offline player names.",
							"This is NOT an error in " + SimplePlugin.getNamed() + ", you need",
							"to contact the placeholder expansion's author instead.",
							"",
							"Variable: " + finalFormat,
							"Text: " + oldText,
							"Player: " + (player == null ? "none" : player.getName()));

					currentThread.stop();
				});

				String value = hooks.get(identifier).onRequest(player, params);

				// Indicate we no longer have to kill the thread.
				watchDog.cancel();

				if (value != null) {
					value = Matcher.quoteReplacement(Common.colorize(value));

					text = text.replaceAll(Pattern.quote(matcher.group()), value.isEmpty() ? "" : (frontSpace ? " " : "") + value + (backSpace ? " " : ""));
				}
			}
		}

		return text;
	}

	final String replaceRelationPlaceholders(final Player one, final Player two, final String message) {
		try {
			return this.setRelationalPlaceholders(one, two, message);

		} catch (final Throwable t) {
			Common.error(t,
					"PlaceholderAPI failed to replace relation variables!",
					"Player one: " + one,
					"Player two: " + two,
					"Message: " + message,
					"Error: %error");

			return message;
		}
	}

	private String setRelationalPlaceholders(final Player one, final Player two, String text) {
		final Map<String, PlaceholderHook> hooks = PlaceholderAPI.getPlaceholders();

		if (hooks.isEmpty())
			return text;

		text = this.setRelationalPlaceholders(one, two, text, hooks, Variables.REL_VARIABLE_PATTERN.matcher(text));
		text = this.setRelationalPlaceholders(one, two, text, hooks, Variables.BRACKET_REL_VARIABLE_PATTERN.matcher(text));

		return text;
	}

	private String setRelationalPlaceholders(final Player one, final Player two, String text, Map<String, PlaceholderHook> hooks, Matcher matcher) {
		while (matcher.find()) {
			final String format = matcher.group(2);
			final int index = format.indexOf("_");

			if (index <= 0 || index >= format.length())
				continue;

			final String identifier = format.substring(0, index);
			final String params = format.substring(index + 1);

			if (hooks.containsKey(identifier)) {
				if (!(hooks.get(identifier) instanceof Relational))
					continue;

				final Relational rel = (Relational) hooks.get(identifier);
				final String value = one != null && two != null ? rel.onPlaceholderRequest(one, two, params) : "";

				if (value != null)
					text = text.replaceAll(Pattern.quote(matcher.group()), Matcher.quoteReplacement(Common.colorize(value)));
			}
		}

		return text;
	}

	private class VariablesInjector extends PlaceholderExpansion {

		/**
		 * 由于这是一个内部类，
		 * 你必须重写此方法，告诉 PlaceholderAPI 在其重载时
		 * 不要注销你的扩展类。
		 *
		 * @return true 表示在重载后保留。
		 */
		@Override
		public boolean persist() {
			return true;
		}

		/**
		 * 由于这是一个内部类，不需要此检查，
		 * 我们直接返回 true 即可。
		 *
		 * @return 始终为 true，因为这是内部类。
		 */
		@Override
		public boolean canRegister() {
			return true;
		}

		/**
		 * 这里应填写创建此扩展的作者名称。
		 * <br>为方便起见，我们返回 plugin.yml 中的作者。
		 *
		 * @return 作者名称，String 类型。
		 */
		@Override
		public String getAuthor() {
			return SimplePlugin.getInstance().getDescription().getAuthors().toString();
		}

		/**
		 * 这里应填写占位符标识符。
		 * <br>当占位符以我们的标识符开头时，PlaceholderAPI 会据此调用我们的 onRequest
		 * 方法来获取值。
		 * <br>它必须唯一，且不能包含 % 或 _。
		 *
		 * @return {@code %<identifier>_<value>%} 中的标识符，String 类型。
		 */
		@Override
		public String getIdentifier() {
			return SimplePlugin.getNamed().toLowerCase().replace("%", "").replace(" ", "").replace("_", "");
		}

		/**
		 * 这是扩展的版本。
		 * <br>由于它被设置为 String，你不必使用数字。
		 * <p>
		 * 为方便起见，我们返回 plugin.yml 中的版本。
		 *
		 * @return 版本，String 类型。
		 */
		@Override
		public String getVersion() {
			return SimplePlugin.getInstance().getDescription().getVersion();
		}

		/**
		 * 替换 Foundation 变量，但会加上我们的插件名称作为
		 * 前缀。
		 *
		 * 如果提供了无效的占位符（例如 %ourplugin_nonexistingplaceholder%），
		 * 我们返回 null。
		 */
		@Override
		public String onRequest(OfflinePlayer offlinePlayer, @NonNull String identifier) {
			final Player player = offlinePlayer != null ? offlinePlayer.getPlayer() : null;

			if (player == null || !player.isOnline())
				return null;

			final boolean frontSpace = identifier.startsWith("+");
			final boolean backSpace = identifier.endsWith("+");

			identifier = frontSpace ? identifier.substring(1) : identifier;
			identifier = backSpace ? identifier.substring(0, identifier.length() - 1) : identifier;

			final Function<CommandSender, String> variable = Variables.getVariable(identifier);

			try {
				if (variable != null) {
					final String value = variable.apply(player);

					if (value != null)
						return value;
				}

				for (final SimpleExpansion expansion : Variables.getExpansions()) {
					final String value = expansion.replacePlaceholders(player, identifier);

					if (value != null) {
						final boolean emptyColorless = Common.stripColors(value).isEmpty();

						return (!value.isEmpty() && frontSpace && !emptyColorless ? " " : "") + value + (!value.isEmpty() && backSpace && !emptyColorless ? " " : "");
					}
				}

			} catch (final Exception ex) {
				Common.error(ex,
						"Error replacing PlaceholderAPI variables",
						"Identifier: " + identifier,
						"Player: " + player.getName());
			}

			return null;
		}
	}
}

class NickyHook {

	NickyHook() {
	}

	String getNick(final Player player) {
		final Constructor<?> nickConstructor = ReflectionUtil.getConstructor("io.loyloy.nicky.Nick", Player.class);
		final Object nick = ReflectionUtil.instantiate(nickConstructor, player);
		String nickname = ReflectionUtil.invoke("get", nick);

		if (nickname != null) {
			final Method formatMethod = ReflectionUtil.getMethod(nick.getClass(), "format", String.class);

			if (formatMethod != null)
				nickname = ReflectionUtil.invoke(formatMethod, nick, nickname);
		}

		return nickname != null && !nickname.isEmpty() ? nickname : null;
	}
}

class MVdWPlaceholderHook {

	MVdWPlaceholderHook() {
	}

	String replacePlaceholders(@Nullable OfflinePlayer player, final String message) {

		if (player == null)
			return message;

		try {
			final Class<?> placeholderAPI = ReflectionUtil.lookupClass("be.maximvdw.placeholderapi.PlaceholderAPI");
			Valid.checkNotNull(placeholderAPI, "Failed to look up class be.maximvdw.placeholderapi.PlaceholderAPI");

			final Method replacePlaceholders = ReflectionUtil.getMethod(placeholderAPI, "replacePlaceholders", OfflinePlayer.class, String.class);
			Valid.checkNotNull(replacePlaceholders, "Failed to look up method PlaceholderAPI#replacePlaceholders(Player, String)");

			final String replaced = ReflectionUtil.invoke(replacePlaceholders, null, player, message);

			return replaced == null ? "" : replaced;

		} catch (final IllegalArgumentException ex) {
			if (!Common.getOrEmpty(ex.getMessage()).contains("Illegal group reference"))
				ex.printStackTrace();

		} catch (final Throwable t) {
			Common.error(t,
					"MvdWPlaceholderAPI placeholders failed!",
					"Player: " + player.getName(),
					"Message: '" + message + "'",
					"Consider writing to the developer of that library",
					"first as this may be a bug we cannot handle!",
					"",
					"Your chat message will appear without replacements.");
		}

		return message;
	}
}

class PremiumVanishHook {

	private final Method isInvisible;
	private final Method hidePlayer;
	private final Method showPlayer;

	public PremiumVanishHook() {
		final Class<?> clazz = ReflectionUtil.lookupClass("de.myzelyam.api.vanish.VanishAPI");

		this.isInvisible = ReflectionUtil.getMethod(clazz, "isInvisible", Player.class);
		this.hidePlayer = ReflectionUtil.getMethod(clazz, "hidePlayer", Player.class, boolean.class, boolean.class);
		this.showPlayer = ReflectionUtil.getMethod(clazz, "showPlayer", Player.class, boolean.class);
	}

	boolean isVanished(Player player) {
		return ReflectionUtil.invokeStatic(this.isInvisible, player);
	}

	void setVanished(Player player, boolean vanished) {
		if (vanished) {
			if (!this.isVanished(player))
				ReflectionUtil.invokeStatic(this.hidePlayer, player, true, false);

		} else {
			if (this.isVanished(player))
				ReflectionUtil.invokeStatic(this.showPlayer, player, true);
		}
	}
}

class LWCHook {

	private final Class<?> mainClass;
	private final boolean enabled;

	private final Object instance;
	private final Method findProtection;

	LWCHook() {
		this.mainClass = ReflectionUtil.lookupClass("com.griefcraft.lwc.LWC");
		this.enabled = (boolean) ReflectionUtil.getStaticFieldContent(this.mainClass, "ENABLED");

		this.instance = ReflectionUtil.invokeStatic(this.mainClass, "getInstance");
		this.findProtection = ReflectionUtil.getMethod(this.mainClass, "findProtection", Block.class);
	}

	String getOwner(final Block block) {
		if (!this.enabled)
			return null;

		final Object protection = ReflectionUtil.invoke(this.findProtection, this.instance, block);

		if (protection != null) {
			final Object ownerUid = ReflectionUtil.invoke("getOwner", protection);

			if (ownerUid != null) {
				final OfflinePlayer offlinePlayer = Remain.getOfflinePlayerByUUID(UUID.fromString(ownerUid.toString()));

				if (offlinePlayer != null)
					return offlinePlayer.getName();
			}
		}

		return null;
	}
}

class LocketteProHook {

	boolean isOwner(final Block block, final Player player) {
		final Class<?> locketteProAPI = ReflectionUtil.lookupClass("me.crafter.mc.lockettepro.LocketteProAPI");
		final Method isProtected = ReflectionUtil.getMethod(locketteProAPI, "isProtected", Block.class);
		final Method isOwner = ReflectionUtil.getMethod(locketteProAPI, "isOwner", Block.class, Player.class);

		return (boolean) ReflectionUtil.invoke(isProtected, null, block) ? ReflectionUtil.invoke(isOwner, null, block, player) : false;
	}
}

class ResidenceHook {

	public Collection<String> getResidences() {
		return Residence.getInstance().getResidenceManager().getResidences().keySet();
	}

	public String getResidence(final Location loc) {
		final ClaimedResidence res = Residence.getInstance().getResidenceManager().getByLoc(loc);

		if (res != null)
			return res.getName();

		return null;
	}

	public String getResidenceOwner(final Location loc) {
		final ClaimedResidence res = Residence.getInstance().getResidenceManager().getByLoc(loc);

		if (res != null)
			return res.getOwner();

		return null;
	}
}

class WorldEditHook {

	public final boolean legacy;

	public WorldEditHook() {
		boolean ok = false;
		try {
			Class.forName("com.sk89q.worldedit.world.World");
			ok = true;
		} catch (final ClassNotFoundException e) {
		}

		this.legacy = !ok;
	}
}

class WorldGuardHook {

	private final boolean legacy;

	public WorldGuardHook(final WorldEditHook we) {
		final Plugin wg = Bukkit.getPluginManager().getPlugin("WorldGuard");

		this.legacy = !wg.getDescription().getVersion().startsWith("7") || we != null && we.legacy;
	}

	public List<String> getRegionsAt(final Location location) {
		final List<String> list = new ArrayList<>();

		this.getApplicableRegions(location).forEach(region -> {
			final String name = Common.stripColors(region.getId());

			if (!name.startsWith("__"))
				list.add(name);
		});

		return list;
	}

	public Region getRegion(final String name) {
		for (final World w : Bukkit.getWorlds()) {
			final Object rm = this.getRegionManager(w);
			if (this.legacy)
				try {

					final Map<?, ?> regionMap = (Map<?, ?>) rm.getClass().getMethod("getRegions").invoke(rm);
					for (final Object regObj : regionMap.values()) {
						if (regObj == null)
							continue;

						if (Common.stripColors(((ProtectedRegion) regObj).getId()).equals(name)) {

							final Class<?> clazz = regObj.getClass();
							final Method getMax = clazz.getMethod("getMaximumPoint");
							final Method getMin = clazz.getMethod("getMinimumPoint");

							final Object regMax = getMax.invoke(regObj);
							final Object regMin = getMin.invoke(regObj);

							final Class<?> vectorClass = Class.forName("com.sk89q.worldedit.BlockVector");
							final Method getX = vectorClass.getMethod("getX");
							final Method getY = vectorClass.getMethod("getY");
							final Method getZ = vectorClass.getMethod("getZ");

							final Location locMax;
							final Location locMin;
							locMax = new Location(w, (Double) getX.invoke(regMax), (Double) getY.invoke(regMax), (Double) getZ.invoke(regMax));
							locMin = new Location(w, (Double) getX.invoke(regMin), (Double) getY.invoke(regMin), (Double) getZ.invoke(regMin));

							return new Region(name, locMin, locMax);
						}
					}

				} catch (final Throwable t) {
					t.printStackTrace();

					throw new FoException("Failed WorldEdit 6 legacy hook, see above and report");
				}
			else
				for (final ProtectedRegion reg : ((com.sk89q.worldguard.protection.managers.RegionManager) rm).getRegions().values())
					if (reg != null && reg.getId() != null && Common.stripColors(reg.getId()).equals(name)) {
						//if(reg instanceof com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion) {
						// just going to pretend that everything is a cuboid..
						final Location locMax;
						final Location locMin;
						final com.sk89q.worldedit.math.BlockVector3 regMax = reg.getMaximumPoint();
						final com.sk89q.worldedit.math.BlockVector3 regMin = reg.getMinimumPoint();

						locMax = new Location(w, regMax.getX(), regMax.getY(), regMax.getZ());
						locMin = new Location(w, regMin.getX(), regMin.getY(), regMin.getZ());

						return new Region(name, locMin, locMax);
					}
		}
		return null;
	}

	public List<String> getAllRegions() {
		final List<String> list = new ArrayList<>();

		for (final World w : Bukkit.getWorlds()) {
			final Object rm = this.getRegionManager(w);
			if (this.legacy)
				try {
					final Map<?, ?> regionMap = (Map<?, ?>) rm.getClass().getMethod("getRegions").invoke(rm);
					Method getId = null;
					for (final Object regObj : regionMap.values()) {
						if (regObj == null)
							continue;
						if (getId == null)
							getId = regObj.getClass().getMethod("getId");

						final String name = Common.stripColors(getId.invoke(regObj).toString());

						if (!name.startsWith("__"))
							list.add(name);
					}
				} catch (final Throwable t) {
					t.printStackTrace();

					throw new FoException("Failed WorldEdit 6 legacy hook, see above and report");
				}
			else
				((com.sk89q.worldguard.protection.managers.RegionManager) rm)
						.getRegions().values().forEach(reg -> {
							if (reg == null || reg.getId() == null)
								return;

							final String name = Common.stripColors(reg.getId());

							if (!name.startsWith("__"))
								list.add(name);
						});
		}

		return list;
	}

	private Iterable<ProtectedRegion> getApplicableRegions(final Location loc) {
		final Object rm = this.getRegionManager(loc.getWorld());

		if (this.legacy)
			try {
				return (Iterable<ProtectedRegion>) rm.getClass().getMethod("getApplicableRegions", Location.class).invoke(rm, loc);

			} catch (final Throwable t) {
				t.printStackTrace();

				throw new FoException("Failed WorldEdit 6 legacy hook, see above and report");
			}

		return ((com.sk89q.worldguard.protection.managers.RegionManager) rm)
				.getApplicableRegions(com.sk89q.worldedit.math.BlockVector3.at(loc.getX(), loc.getY(), loc.getZ()));
	}

	private Object getRegionManager(final World w) {
		if (this.legacy)
			try {
				return Class.forName("com.sk89q.worldguard.bukkit.WGBukkit").getMethod("getRegionManager", World.class).invoke(null, w);

			} catch (final Throwable t) {
				t.printStackTrace();

				throw new FoException("Failed WorldGuard 6 legacy hook, see above and report");
			}

		// Causes class errors.
		//return com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer().get(new com.sk89q.worldedit.bukkit.BukkitWorld(w));
		// Dynamically load modern WorldEdit.
		try {

			final Class<?> bwClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitWorld");
			final Constructor<?> bwClassNew = bwClass.getConstructor(World.class);

			Object t = Class.forName("com.sk89q.worldguard.WorldGuard").getMethod("getInstance").invoke(null);
			t = t.getClass().getMethod("getPlatform").invoke(t);
			t = t.getClass().getMethod("getRegionContainer").invoke(t);
			return t.getClass().getMethod("get", Class.forName("com.sk89q.worldedit.world.World")).invoke(t, bwClassNew.newInstance(w));

		} catch (final Throwable t) {
			t.printStackTrace();

			throw new FoException("Failed WorldGuard hook, see above and report");
		}
	}
}

abstract class FactionsHook {

	/**
	 * 获取所有已加载的派系。
	 */
	abstract Collection<String> getFactions();

	/**
	 * 获取玩家所属的派系。
	 */
	abstract String getFaction(Player player);

	/**
	 * 获取玩家所属派系的 ID。
	 */
	abstract String getFactionId(Player player);

	/**
	 * 获取给定位置处的派系
	 */
	abstract String getFaction(Location location);

	/**
	 * 获取给定位置处的派系所有者。
	 */
	abstract String getFactionOwner(Location location);

	/**
	 * 获取与该玩家派系结盟的所有派系
	 */
	abstract List<String> getFactionRelationIDs(Player player, String relation);

	/**
	 * 获取同一派系中的所有玩家，用于队伍聊天。
	 */
	final Collection<? extends Player> getSameFactionPlayers(final Player player) {
		final List<Player> recipients = new ArrayList<>();
		final String playerFaction = this.getFaction(player);

		if (playerFaction != null && !playerFaction.isEmpty())
			for (final Player online : Remain.getOnlinePlayers()) {
				final String onlineFaction = this.getFaction(online);

				if (playerFaction.equals(onlineFaction))
					recipients.add(online);
			}

		return recipients;
	}

	/**
	 * 获取所有结盟派系中的玩家，用于队伍聊天。
	 */
	final Collection<? extends Player> getRelatedFactionPlayers(final Player player, final String relation) {
		final List<Player> recipients = new ArrayList<>();
		final String factionId = this.getFactionId(player);
		final List<String> relatedFactions = this.getFactionRelationIDs(player, relation);

		if (relatedFactions != null && !relatedFactions.isEmpty())
			for (final Player online : Remain.getOnlinePlayers()) {
				if (online.equals(player))
					continue;
				final String onlineFactionId = this.getFactionId(online);
				final List<String> onlineRelatedFactions = this.getFactionRelationIDs(online, relation);
				if (relatedFactions.contains(onlineFactionId) && onlineRelatedFactions.contains(factionId))
					recipients.add(online);
			}

		return recipients;
	}
}

final class FactionsMassive extends FactionsHook {

	FactionsMassive() {
	}

	@Override
	public Collection<String> getFactions() {
		return Common.convert(com.massivecraft.factions.entity.FactionColl.get().getAll(), object -> Common.stripColors(object.getName()));
	}

	@Override
	public String getFaction(final Player player) {
		try {
			return MPlayer.get(player.getUniqueId()).getFactionName();
		} catch (final Exception ex) {
			return null;
		}
	}

	@Override
	String getFactionId(Player player) {
		try {
			return MPlayer.get(player.getUniqueId()).getFaction().getId();
		} catch (final Exception ex) {
			return null;
		}
	}

	@Override
	public String getFaction(final Location location) {
		final Faction faction = BoardColl.get().getFactionAt(PS.valueOf(location));

		if (faction != null)
			return faction.getName();

		return null;
	}

	@Override
	public String getFactionOwner(final Location location) {
		final Faction faction = BoardColl.get().getFactionAt(PS.valueOf(location));

		if (faction != null)
			return faction.getLeader() != null ? faction.getLeader().getName() : null;

		return null;
	}

	@Override
	List<String> getFactionRelationIDs(Player player, String relation) {
		final List<String> relations = new ArrayList<>();

		final MPlayer mPlayer = MPlayer.get(player.getUniqueId());
		if (mPlayer == null)
			return relations;

		final Faction faction = mPlayer.getFaction();
		if (faction == null)
			return relations;

		final Map<String, Rel> relationWishes = faction.getRelationWishes();
		if (relationWishes == null)
			return relations;

		for (final Map.Entry<String, Rel> entry : relationWishes.entrySet()) {
			final String factionName = entry.getKey();
			final Rel factionRelation = entry.getValue();
			if (factionName != null && factionRelation != null && factionRelation.name().equalsIgnoreCase(relation))
				relations.add(factionName);
		}
		return relations;
	}
}

final class FactionsUUID extends FactionsHook {

	private Collection<dev.kitteh.factions.Faction> getFactionObjects() {
		try {
			final Object instance = this.factionsInstance();
			final List<dev.kitteh.factions.Faction> facs = (List<dev.kitteh.factions.Faction>) instance.getClass().getMethod("all").invoke(instance);

			return facs;
		} catch (final Throwable t) {
			t.printStackTrace();

			return null;
		}
	}

	@Override
	public Collection<String> getFactions() {
		final Collection<dev.kitteh.factions.Faction> factionObjects = this.getFactionObjects();

		if (factionObjects == null) {
			return new ArrayList<>();
		}

		return factionObjects.stream().map(dev.kitteh.factions.Faction::tag).collect(Collectors.toList());
	}

	private dev.kitteh.factions.Faction getFactionById(String tag) {
		final Collection<dev.kitteh.factions.Faction> factionObjects = this.getFactionObjects();
		if (factionObjects == null)
			return null;

		for (final dev.kitteh.factions.Faction f : factionObjects) {
			if (f.tag().equals(tag))
				return f;
		}
		return null;
	}

	@Override
	public String getFaction(final Player player) {
		try {
			final Object factionPlayers = this.fPlayers();
			final Object factionPlayer = factionPlayers.getClass().getMethod("get", UUID.class).invoke(factionPlayers, player.getUniqueId());
			final Object faction = factionPlayer != null ? factionPlayer.getClass().getMethod("faction").invoke(factionPlayer) : null;
			final Object factionName = faction != null ? faction.getClass().getMethod("tag").invoke(faction) : null;

			return factionName != null ? factionName.toString() : null;

		} catch (final ReflectiveOperationException ex) {
			ex.printStackTrace();

			return null;
		}
	}

	@Override
	String getFactionId(Player player) {
		try {
			final Object factionPlayers = this.fPlayers();
			final Object factionPlayer = factionPlayers.getClass().getMethod("get", UUID.class).invoke(factionPlayers, player.getUniqueId());
			final Object faction = factionPlayer != null ? factionPlayer.getClass().getMethod("faction").invoke(factionPlayer) : null;
			final Object factionId = faction != null ? faction.getClass().getMethod("id").invoke(faction) : null;

			return factionId != null ? factionId.toString() : null;

		} catch (final ReflectiveOperationException ex) {
			ex.printStackTrace();

			return null;
		}
	}

	@Override
	public String getFaction(final Location location) {
		final Object faction = this.findFaction(location);

		try {
			return faction != null ? faction.getClass().getMethod("tag").invoke(faction).toString() : null;
		} catch (final ReflectiveOperationException ex) {
			ex.printStackTrace();

			return null;
		}
	}

	@Override
	public String getFactionOwner(final Location location) {
		final Object faction = this.findFaction(location);

		try {
			return faction != null ? ((dev.kitteh.factions.FPlayer) faction.getClass().getMethod("admin").invoke(faction)).name() : null;
		} catch (final ReflectiveOperationException ex) {
			ex.printStackTrace();

			return null;
		}
	}

	@Override
	List<String> getFactionRelationIDs(Player player, String relation) {
		final List<String> relationList = new ArrayList<>();

		final dev.kitteh.factions.Faction playerFaction = this.getFactionById(this.getFaction(player));

		if (playerFaction == null)
			return relationList;

		final Collection<dev.kitteh.factions.Faction> factionObjects = this.getFactionObjects();
		if (factionObjects == null)
			return relationList;

		for (final dev.kitteh.factions.Faction faction : factionObjects) {
			if (faction.tag().equals(playerFaction.tag()))
				continue;

			final Relation rel = playerFaction.relationWish(faction);

			if (rel.name().equalsIgnoreCase(relation))
				relationList.add(String.valueOf(faction.id()));
		}

		return relationList;
	}

	private Object findFaction(final Location location) {
		final Class<dev.kitteh.factions.Board> factionBoard = dev.kitteh.factions.Board.class;

		try {
			return factionBoard.getMethod("factionAt", FLocation.class).invoke(factionBoard.getMethod("board").invoke(null), new dev.kitteh.factions.FLocation(location));
		} catch (final ReflectiveOperationException ex) {
			ex.printStackTrace();

			return null;
		}
	}

	private Object factionsInstance() {
		try {
			return Class.forName("dev.kitteh.factions.Factions").getDeclaredMethod("factions").invoke(null);
		} catch (final ReflectiveOperationException ex) {
			ex.printStackTrace();

			throw new FoException(ex);
		}
	}

	private Object fPlayers() {
		try {
			return Class.forName("dev.kitteh.factions.FPlayers").getDeclaredMethod("fPlayers").invoke(null);
		} catch (final ReflectiveOperationException ex) {
			ex.printStackTrace();

			throw new FoException(ex);
		}
	}
}

class McMMOHook {

	// Only display error once
	private boolean errorLogged = false;

	String getActivePartyChat(final Player player) {
		try {
			final McMMOPlayer mcplayer = UserManager.getPlayer(player);

			if (mcplayer != null) {
				final Party party = mcplayer.getParty();
				final ChatChannel channelType = mcplayer.getChatChannel();

				return channelType == ChatChannel.PARTY || channelType == ChatChannel.PARTY_OFFICER && party != null ? party.getName() : null;
			}

		} catch (final Throwable throwable) {
			if (!this.errorLogged) {
				Common.warning("Failed getting mcMMO party chat for " + player.getName() + " due to an error. Returning null."
						+ " Ensure you have the latest mcMMO version. If so, contact the plugin authors to update the integration. Error was: " + throwable);

				this.errorLogged = true;
			}
		}

		return null;
	}

	List<Player> getPartyRecipients(final Player bukkitPlayer) {
		try {
			final McMMOPlayer mcplayer = UserManager.getPlayer(bukkitPlayer);

			if (mcplayer != null) {
				final Party party = mcplayer.getParty();

				if (party != null)
					return party.getOnlineMembers();
			}

		} catch (final Throwable throwable) {
			if (!this.errorLogged) {
				Common.warning("Failed getting mcMMO party recipients for " + bukkitPlayer.getName() + " due to an error. Returning null."
						+ " Ensure you have the latest mcMMO version. If so, contact the plugin authors to update the integration. Error was: " + throwable);

				this.errorLogged = true;
			}
		}

		return new ArrayList<>();
	}
}

class PlotSquaredHook {

	private final boolean legacy;

	/**
	 *
	 */
	PlotSquaredHook() {
		final Plugin plugin = Bukkit.getPluginManager().getPlugin("PlotSquared");
		Valid.checkNotNull(plugin, "PlotSquared not hooked yet!");

		this.legacy = plugin.getDescription().getVersion().startsWith("3");
	}

	List<Player> getPlotPlayers(final Player player) {
		final List<Player> players = new ArrayList<>();

		final Class<?> plotPlayerClass = ReflectionUtil.lookupClass((this.legacy ? "com.intellectualcrafters.plot.object" : "com.plotsquared.core.player") + ".PlotPlayer");
		Method wrap;

		try {
			wrap = plotPlayerClass.getMethod("from", Object.class);

		} catch (final ReflectiveOperationException ex3) {
			try {
				wrap = plotPlayerClass.getMethod("wrap", Object.class);

			} catch (final ReflectiveOperationException ex2) {
				try {
					wrap = plotPlayerClass.getMethod("wrap", Player.class);

				} catch (final ReflectiveOperationException ex) {
					throw new FoException(ex3, "PlotSquared could not convert " + player.getName() + " into PlotPlayer! Is the integration outdated?");
				}
			}
		}

		final Object plotPlayer = ReflectionUtil.invokeStatic(wrap, player);
		Valid.checkNotNull(plotPlayer, "Failed to convert player " + player.getName() + " to PlotPlayer!");

		final Object currentPlot = ReflectionUtil.invoke("getCurrentPlot", plotPlayer);

		if (currentPlot != null)
			for (final Object playerInPlot : (Iterable<?>) ReflectionUtil.invoke("getPlayersInPlot", currentPlot)) {
				final UUID id = ReflectionUtil.invoke("getUUID", playerInPlot);
				final Player online = Bukkit.getPlayer(id);

				if (online != null && online.isOnline())
					players.add(online);
			}

		return players;
	}
}

class CMIHook {

	boolean isVanished(final Player player) {
		final CMIUser user = this.getUser(player);

		return user != null && user.isVanished();
	}

	void setVanished(Player player, boolean vanished) {
		final CMIUser user = this.getUser(player);

		if (user != null && user.isVanished() != vanished)
			user.setVanished(false);
	}

	boolean isAfk(final Player player) {
		final CMIUser user = this.getUser(player);

		return user != null && user.isAfk();
	}

	boolean isMuted(final Player player) {
		final CMIUser user = this.getUser(player);

		try {
			return user != null && user.getMutedUntil() != 0 && user.getMutedUntil() != null && user.getMutedUntil() > System.currentTimeMillis();

		} catch (final Exception ex) {
			return false;
		}
	}

	boolean hasGodMode(final Player player) {
		final CMIUser user = this.getUser(player);

		return user != null ? user.isGod() : false;
	}

	void setGodMode(final Player player, final boolean godMode) {
		final CMIUser user = this.getUser(player);

		if (user != null)
			try {
				CMI.getInstance().getNMS().changeGodMode(player, godMode);

			} catch (final Throwable tt) {
				try {
					final Method setGod = CMIUser.class.getMethod("setGod", Boolean.class);

					setGod.invoke(user, godMode);

				} catch (final Throwable t) {
					// unavailable
				}
			}
	}

	void setLastTeleportLocation(final Player player, final Location location) {
		final CMIUser user = this.getUser(player);

		try {
			user.getClass().getMethod("setLastTeleportLocation", Location.class).invoke(user, location);
		} catch (final Throwable t) {
			// Silently fail.
		}
	}

	void setIgnore(final UUID player, final UUID who, final boolean ignore) {
		final CMIUser user = CMI.getInstance().getPlayerManager().getUser(player);

		if (ignore)
			user.addIgnore(who, true /* Save now. */);
		else
			user.removeIgnore(who);
	}

	boolean isIgnoring(final UUID player, final UUID who) {
		try {
			final CMIUser user = CMI.getInstance().getPlayerManager().getUser(player);

			return user.isIgnoring(who);

		} catch (final NullPointerException ex) {
			return false;
		}
	}

	String getNick(final Player player) {
		final CMIUser user = this.getUser(player);
		final String nick = user == null ? null : user.getNickName();

		return nick == null || "".equals(nick) ? null : nick;
	}

	String getNick(final String playerName) {
		final CMIUser user = this.getUser(playerName);
		final String nick = user == null ? null : user.getNickName();

		return nick == null || "".equals(nick) ? null : nick;
	}

	void setNick(final UUID uniqueId, String nick) {
		final CMIUser user = this.getUser(uniqueId);
		final TabListManager tabManager = CMI.getInstance().getTabListManager();

		if (user != null) {
			final boolean isEmpty = nick == null || Common.stripColors(nick).replace(" ", "").isEmpty();

			user.setNickName(isEmpty ? null : Common.colorize(nick), true);
			user.updateDisplayName();

			if (tabManager.isUpdatesOnNickChange())
				tabManager.updateTabList(3);
		}
	}

	String getNameFromNick(String nick) {
		for (final CMIUser user : CMI.getInstance().getPlayerManager().getAllUsers().values())
			if (user != null && user.getNickName() != null && Valid.colorlessEquals(user.getNickName(), nick))
				return Common.getOrDefault(user.getName(), nick);

		return nick;
	}

	private CMIUser getUser(final Player player) {
		return CMI.getInstance().getPlayerManager().getUser(player);
	}

	private CMIUser getUser(final UUID uniqueId) {
		return CMI.getInstance().getPlayerManager().getUser(uniqueId);
	}

	private CMIUser getUser(final String name) {
		return CMI.getInstance().getPlayerManager().getUser(name);
	}
}

class CitizensHook {

	boolean isNPC(final Entity entity) {
		try {
			final NPCRegistry reg = CitizensAPI.getNPCRegistry();

			return reg != null ? reg.isNPC(entity) : false;
		} catch (final NoClassDefFoundError err) {
			Common.logTimed(60 * 30, "Unable to check if " + entity + " is Citizens NPC, got " + err + ". This error only shows once per 30min.");

			return false;
		}
	}

	Entity getNPCTarget(Entity entity) {
		final NPC npc = CitizensAPI.getNPCRegistry().getNPC(entity);

		if (npc != null) {
			final EntityTarget target = npc.getNavigator().getEntityTarget();

			if (target != null)
				return target.getTarget();
		}

		return null;
	}
}

class DiscordSRVHook {

	Set<String> getChannels() {
		return DiscordSRV.getPlugin().getChannels().keySet();
	}

	boolean sendMessage(final String channel, final String message) {
		return this.sendMessage(null, channel, message);
	}

	boolean sendMessage(@Nullable CommandSender sender, final String channel, final String message) {
		final TextChannel textChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channel);

		// The channel is not configured in the config.yml of Discord,
		// so we can ignore it.
		if (textChannel == null) {
			Debugger.debug("discord", "[MC->Discord] Could not find Discord channel '" + channel + "'. Available: " + String.join(", ", this.getChannels()) + ". Not sending: " + message);

			return false;
		}

		if (sender instanceof Player) {
			Debugger.debug("discord", "[MC->Discord] " + sender.getName() + " send message to '" + channel + "' channel. Message: '" + message + "'");

			final DiscordSRV instance = JavaPlugin.getPlugin(DiscordSRV.class);

			// Dirty: We have to temporarily set a configuration value in
			// DiscordSRV to enable the processChatMessage method to function.
			final String key = "DiscordChatChannelMinecraftToDiscord";
			final Map<String, Object> runtimeValues = ReflectionUtil.getFieldContent(DiscordSRV.config(), "runtimeValues");
			final Object oldValue = runtimeValues.get(key);

			runtimeValues.put(key, true);

			try {
				instance.processChatMessage((Player) sender, message, channel, false);

			} finally {
				if (oldValue == null)
					runtimeValues.remove(key);
				else
					runtimeValues.put(key, oldValue);
			}

		} else {
			Debugger.debug("discord", "[MC->Discord] " + (sender == null ? "No sender " : sender.getName() + " (generic)") + "sent message to '" + channel + "' channel. Message: '" + message + "'");

			DiscordUtil.sendMessage(textChannel, message);
		}

		return true;
	}
}

class BanManagerHook {

	/*
	 * Return true if the given player is muted.
	 */
	boolean isMuted(final Player player) {
		try {
			final Class<?> api = ReflectionUtil.lookupClass("me.confuser.banmanager.common.api.BmAPI");
			final Method isMuted = ReflectionUtil.getMethod(api, "isMuted", UUID.class);

			return ReflectionUtil.invoke(isMuted, null, player.getUniqueId());

		} catch (final Throwable t) {
			if (!t.toString().contains("Could not find class"))
				Common.log("Unable to check if " + player.getName() + " is muted at BanManager. Is the API hook outdated? Got: " + t);

			return false;
		}
	}
}

class BentoBoxHook {

	Set<UUID> getIslandVisitors(Player player) {
		return this.getIslandUsers(player, RanksManager.VISITOR_RANK);
	}

	Set<UUID> getIslandCoops(Player player) {
		return this.getIslandUsers(player, RanksManager.COOP_RANK);
	}

	Set<UUID> getIslandTrustees(Player player) {
		return this.getIslandUsers(player, RanksManager.TRUSTED_RANK);
	}

	Set<UUID> getIslandMembers(Player player) {
		return this.getIslandUsers(player, RanksManager.MEMBER_RANK);
	}

	Set<UUID> getIslandSubOwners(Player player) {
		return this.getIslandUsers(player, RanksManager.SUB_OWNER_RANK);
	}

	Set<UUID> getIslandOwners(Player player) {
		return this.getIslandUsers(player, RanksManager.OWNER_RANK);
	}

	Set<UUID> getIslandMods(Player player) {
		return this.getIslandUsers(player, RanksManager.MOD_RANK);
	}

	Set<UUID> getIslandAdmins(Player player) {
		return this.getIslandUsers(player, RanksManager.ADMIN_RANK);
	}

	private Set<UUID> getIslandUsers(Player player, int rank) {
		final IslandsManager manager = BentoBox.getInstance().getIslands();
		final Optional<Island> maybeIsland = manager.getIslandAt(player.getLocation());

		if (maybeIsland.isPresent()) {
			final Island island = maybeIsland.get();

			return island.getMemberSet(rank);

		} else {
			final UUID uniqueId = player.getUniqueId();

			for (final World world : Bukkit.getWorlds()) {
				try {
					final Island island = manager.getIsland(world, uniqueId);

					if (island != null)
						return island.getMemberSet(rank);

				} catch (final Throwable t) {
				}
			}
		}

		return new HashSet<>();
	}
}

class BossHook {

	/*
	 * Return the Boss name if the given player is a Boss or null
	 */
	String getBossName(final Entity entity) {
		try {
			final Class<?> api = ReflectionUtil.lookupClass("org.mineacademy.boss.api.BossAPI");
			final Method getBoss = ReflectionUtil.getMethod(api, "getBoss", Entity.class);

			final Object boss = ReflectionUtil.invoke(getBoss, null, entity);

			if (boss != null) {
				final Method getName = ReflectionUtil.getMethod(boss.getClass(), "getName");

				return ReflectionUtil.invoke(getName, boss);
			}

		} catch (final Throwable t) {
			Common.log("Unable to check if " + entity + " is a Boss. Is the API hook outdated? Got: " + t);
		}

		return null;
	}
}

class MythicMobsHook {

	private Boolean legacyVersion = null;

	MythicMobsHook() {
		final Plugin mythicMobs = Bukkit.getPluginManager().getPlugin("MythicMobs");
		final String version = mythicMobs.getDescription().getVersion();

		if (version.startsWith("4."))
			this.legacyVersion = true;

		else if (version.startsWith("5."))
			this.legacyVersion = false;

		else
			Common.warning("Skipping hooking into unsupported MythicMob version " + version + "! Only 4.X.X and 5.X.X are supported.");

	}

	/*
	 * Attempt to return a MythicMob name from the given entity,
	 * or null if the entity is not a MythicMob.
	 */
	String getBossName(Entity entity) {
		if (this.legacyVersion == null)
			return null;

		if (this.legacyVersion)
			return this.getBossNameV4(entity);

		return this.getBossNameV5Direct(entity);
	}

	private String getBossNameV4(Entity entity) {
		try {
			final Class<?> mythicMobs = ReflectionUtil.lookupClass("io.lumine.xikage.mythicmobs.MythicMobs");
			final Object instance = ReflectionUtil.invokeStatic(mythicMobs, "inst");
			final Object mobManager = ReflectionUtil.invoke("getMobManager", instance);
			final Optional<Object> activeMob = ReflectionUtil.invoke(ReflectionUtil.getMethod(mobManager.getClass(), "getActiveMob", UUID.class), mobManager, entity.getUniqueId());
			final Object mob = activeMob != null && activeMob.isPresent() ? activeMob.get() : null;

			if (mob != null) {
				final Object mythicEntity = ReflectionUtil.invoke("getEntity", mob);

				if (mythicEntity != null)
					return (String) ReflectionUtil.invoke("getName", mythicEntity);
			}

		} catch (final NoSuchElementException ex) {
		}

		return Remain.getName(entity);
	}

	private String getBossNameV5Direct(Entity entity) {
		final UUID ourUniqueId = entity.getUniqueId();
		final MobManager mobManager = MythicProvider.get().getMobManager();

		for (final ActiveMob mob : mobManager.getActiveMobs()) {
			if (ourUniqueId.equals(mob.getUniqueId()))
				return mob.getName();
		}

		/*try {
			final Object mythicPlugin = ReflectionUtil.invokeStatic(ReflectionUtil.lookupClass("io.lumine.mythic.api.MythicProvider"), "get");
			final Object mobManager = ReflectionUtil.invoke("getMobManager", mythicPlugin);
		
			final Method getActiveMobsMethod = ReflectionUtil.getMethod(mobManager.getClass(), "getActiveMobs");
			final Collection<?> activeMobs = ReflectionUtil.invoke(getActiveMobsMethod, mobManager);
		
			for (final Object mob : activeMobs) {
				final UUID uniqueId = ReflectionUtil.invoke("getUniqueId", mob);
		
				if (uniqueId.equals(entity.getUniqueId()))
					return ReflectionUtil.invoke("getName", mob);
			}
		
		} catch (Throwable t) {
			Common.error(t, "MythicMobs integration failed getting mob name, contact plugin developer to update the integration!");
		}*/

		return Remain.getName(entity);
	}
}

class LandsHook {

	private final Object landsClass;
	private final Method getArea;
	private final Method getLand;
	private final Method getName;

	LandsHook() {
		final Class<?> lands = ReflectionUtil.lookupClass("me.angeschossen.lands.api.LandsIntegration");
		final Class<?> area = ReflectionUtil.lookupClass("me.angeschossen.lands.api.land.Area");
		final Class<?> land = ReflectionUtil.lookupClass("me.angeschossen.lands.api.land.Land");

		final Method of = ReflectionUtil.getMethod(lands, "of", Plugin.class);

		this.landsClass = ReflectionUtil.invokeStatic(of, SimplePlugin.getInstance());
		this.getArea = ReflectionUtil.getMethod(lands, "getArea", Location.class);
		this.getLand = ReflectionUtil.getMethod(area, "getLand");
		this.getName = ReflectionUtil.getMethod(land, "getName");
	}

	Collection<Player> getLandPlayers(Player sender) {
		final List<Player> playersAtLocation = new ArrayList<>();

		final Object senderArea = ReflectionUtil.invoke(this.getArea, this.landsClass, sender.getLocation());
		final Object senderLand = senderArea != null ? ReflectionUtil.invoke(this.getLand, senderArea) : null;

		final boolean senderInWilderness = senderLand == null;
		final String senderLandName = senderInWilderness ? "" : ReflectionUtil.invoke(this.getName, senderLand);

		for (final Player recipient : Remain.getOnlinePlayers()) {

			final Object recipientArea = ReflectionUtil.invoke(this.getArea, this.landsClass, recipient.getLocation());
			final Object recipientLand = recipientArea != null ? ReflectionUtil.invoke(this.getLand, recipientArea) : null;
			final boolean recipientInWilderness = recipientLand == null;

			// Both in wilderness
			if (recipientInWilderness && senderInWilderness)
				playersAtLocation.add(recipient);

			// Other player in land
			if (senderLand == null)
				continue;

			if (recipientLand != null && ReflectionUtil.invoke(this.getName, recipientLand).equals(senderLandName))
				playersAtLocation.add(recipient);
		}

		return playersAtLocation;
	}
}

class LiteBansHook {

	/*
	 * Return true if the given player is muted.
	 */
	boolean isMuted(final Player player) {
		return false; // Problematic, we're investigating this.
		/*try {
			final Class<?> api = ReflectionUtil.lookupClass("litebans.api.Database");
			final Object instance = ReflectionUtil.invokeStatic(api, "get");
		
			return ReflectionUtil.invoke("isPlayerMuted", instance, player.getUniqueId());
		
		} catch (final Throwable t) {
			if (!t.toString().contains("Could not find class")) {
				Common.log("Unable to check if " + player.getName() + " is muted at LiteBans. Is the API hook outdated? See console error:");
		
				t.printStackTrace();
			}
		
			return false;
		}*/
	}
}

class ItemsAdderHook {

	private Class<?> itemsAdder;
	private Method replaceFontImagesString;
	private Method replaceFontImagesStringNoPlayer;

	private boolean failed = false;

	ItemsAdderHook() {
	}

	String replaceFontImagesLegacy(@Nullable final Player player, final String messageOrComponent) {
		if (this.replaceFontImagesString == null && !this.failed) {
			try {
				this.itemsAdder = ReflectionUtil.lookupClass("dev.lone.itemsadder.api.FontImages.FontImageWrapper");

				this.replaceFontImagesString = ReflectionUtil.getMethod(this.itemsAdder, "replaceFontImages", Permissible.class, String.class);
				this.replaceFontImagesStringNoPlayer = ReflectionUtil.getMethod(this.itemsAdder, "replaceFontImages", String.class);

			} catch (final Throwable original) {
				try {
					this.replaceFontImagesString = ReflectionUtil.getMethod(this.itemsAdder, "replaceFontImages", Player.class, String.class);
					this.replaceFontImagesStringNoPlayer = ReflectionUtil.getMethod(this.itemsAdder, "replaceFontImages", String.class);

				} catch (final Throwable tt) {
					Common.warning("Unable to resolve ItemsAdder API. The plugin will continue to function, but no font images will be replaced. Is the integration outdated?");

					original.printStackTrace();
					this.failed = true;
				}
			}
		}

		if (this.failed)
			return messageOrComponent;

		if (player == null) {
			if (this.replaceFontImagesStringNoPlayer != null) {
				final String message = messageOrComponent;
				final String result = (String) ReflectionUtil.invokeStatic(this.replaceFontImagesStringNoPlayer, message);

				return result;
			}

		} else {
			if (this.replaceFontImagesString != null) {
				final String message = messageOrComponent;
				final String result = (String) ReflectionUtil.invokeStatic(this.replaceFontImagesString, player, message);

				return result;
			}
		}

		// Fallback to original message or component if replacement fails
		return messageOrComponent;
	}
}
