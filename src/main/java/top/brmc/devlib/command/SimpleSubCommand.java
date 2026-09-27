package top.brmc.devlib.command;

import java.util.Arrays;

import top.brmc.devlib.Valid;
import top.brmc.devlib.plugin.SimplePlugin;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * 归属于 {@link SimpleCommandGroup} 的简单子命令
 */
public abstract class SimpleSubCommand extends SimpleCommand {

	/**
	 * 该子命令可拥有的全部已注册子标签
	 */
	@Getter
	private final String[] sublabels;

	/**
	 * 子命令运行时使用的最新子标签，
	 * 每次执行都会更新
	 */
	@Setter(value = AccessLevel.PROTECTED)
	@Getter(value = AccessLevel.PROTECTED)
	private String sublabel;

	/**
	 * 创建新的子命令，要求主插件实例定义了主命令组
	 *
	 * @param sublabel
	 */
	protected SimpleSubCommand(String sublabel) {
		this(getMainCommandGroup0(), sublabel);
	}

	/*
	 * Attempts to get the main command group, failing with an error if not defined
	 */
	private static SimpleCommandGroup getMainCommandGroup0() {
		final SimpleCommandGroup main = SimplePlugin.getInstance().getMainCommand();

		Valid.checkNotNull(main, SimplePlugin.getNamed() + " does not define a main command group!"
				+ " You need to put @AutoRegister over your class extending a SimpleCommandGroup that has a no args constructor to register it automatically");

		return main;
	}

	/**
	 * 创建归属于命令组的新子命令
	 *
	 * @param parent
	 * @param sublabel
	 */
	protected SimpleSubCommand(SimpleCommandGroup parent, String sublabel) {
		super(parent.getLabel());

		this.sublabels = sublabel.split("(\\||\\/)");
		Valid.checkBoolean(this.sublabels.length > 0, "Please set at least 1 sublabel");

		this.sublabel = this.sublabels[0];

		// If the default perm was not changed, improve it
		if (this.getRawPermission().equals(getDefaultPermission()))
			if (SimplePlugin.getInstance().getMainCommand() != null && SimplePlugin.getInstance().getMainCommand().getLabel().equals(this.getLabel()))
				this.setPermission(this.getRawPermission().replace("{label}", "{sublabel}")); // simply replace label with sublabel

			else
				this.setPermission(this.getRawPermission() + ".{sublabel}"); // append the sublabel at the end since this is not our main command
	}

	/**
	 * 命令组会自动在 /{label} help|? 菜单中显示全部子命令。
	 * 该子命令是否显示在此菜单中？
	 *
	 * @return
	 */
	protected boolean showInHelp() {
		return true;
	}

	/**
	 * 为该子命令替换额外的 {sublabel} 占位符。
	 * 见 {@link SimpleCommand#replacePlaceholders(String)}
	 */
	@Override
	protected String replacePlaceholders(String message) {
		return super.replacePlaceholders(message).replace("{sublabel}", this.getSublabel());
	}

	@Override
	public String toString() {
		return "SubCommand{parent=/" + this.getLabel() + ", label=" + this.getSublabel() + "}";
	}

	@Override
	public final boolean equals(Object obj) {
		return obj instanceof SimpleSubCommand ? Arrays.equals(((SimpleSubCommand) obj).sublabels, this.sublabels) : false;
	}
}
