package top.brmc.devlib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import top.brmc.devlib.SerializeUtil.Mode;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Tab 补全工具类。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TabUtil {

	/**
	 * 为给定数组返回 Tab 补全列表，会尝试推断数组元素的类型，
	 * 支持聊天颜色、命令发送者、枚举等。
	 *
	 * @param <T>
	 * @param partialName
	 * @param elements
	 * @return
	 */
	@SafeVarargs
	public static <T> List<String> complete(String partialName, T... elements) {
		final List<String> clone = new ArrayList<>();

		if (elements != null)
			for (final T element : elements)
				if (element != null)
					if (element instanceof Iterable)
						for (final Object iterable : (Iterable<?>) element)
							clone.add(iterable instanceof Enum ? iterable.toString().toLowerCase() : SerializeUtil.serialize(Mode.YAML, iterable).toString());

					// Trick: Automatically parse enum constants
					else if (element instanceof Enum[])
						for (final Object iterable : ((Enum[]) element)[0].getClass().getEnumConstants())
							clone.add(iterable.toString().toLowerCase());

					else {
						final boolean lowercase = element instanceof Enum;
						final String parsed = SerializeUtil.serialize(Mode.YAML, element).toString();

						if (!"".equals(parsed))
							clone.add(lowercase ? parsed.toLowerCase() : parsed);
					}

		return complete(partialName, clone);
	}

	/**
	 * 为给定集合返回有效的 Tab 补全
	 *
	 * @param partialName
	 * @param all
	 * @return
	 */
	public static List<String> complete(String partialName, Iterable<String> all) {
		final ArrayList<String> tab = new ArrayList<>();

		for (final String s : all)
			tab.add(s);

		partialName = partialName.toLowerCase();

		for (final Iterator<String> iterator = tab.iterator(); iterator.hasNext();) {
			final String val = iterator.next();

			if (!val.toLowerCase().startsWith(partialName))
				iterator.remove();
		}

		Collections.sort(tab);

		return tab;
	}
}
