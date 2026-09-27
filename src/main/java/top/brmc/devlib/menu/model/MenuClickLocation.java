package top.brmc.devlib.menu.model;

/**
 * 表示菜单打开时的点击位置
 */
public enum MenuClickLocation {

	/**
	 * 点击了上方的菜单
	 */
	MENU,

	/**
	 * 点击了下方的玩家背包
	 */
	PLAYER_INVENTORY,

	/**
	 * 点击了 GUI 之外的区域
	 */
	OUTSIDE
}