package top.brmc.devlib.event;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@link SimpleEvent} 的扩展，同时让你的自定义事件
 * 可以被取消。
 */
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class SimpleCancellableEvent extends SimpleEvent {

	/**
	 * 事件是否已被取消？
	 */
	private boolean cancelled;

	/**
	 * 创建一个新事件，并指明它是否在
	 * Minecraft 服务器主线程上运行
	 *
	 * @param async
	 */
	protected SimpleCancellableEvent(boolean async) {
		super(async);
	}
}
