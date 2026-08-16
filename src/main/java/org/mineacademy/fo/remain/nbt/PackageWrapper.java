package org.mineacademy.fo.remain.nbt;

/**
 * 包枚举
 *
 * @author tr7zw
 */

enum PackageWrapper {
	NMS(new String(new byte[] { 'n', 'e', 't', '.', 'm', 'i', 'n', 'e', 'c', 'r', 'a', 'f', 't', '.', 's', 'e', 'r',
			'v', 'e', 'r' })),
	CRAFTBUKKIT(new String(new byte[] { 'o', 'r', 'g', '.', 'b', 'u', 'k', 'k', 'i', 't', '.', 'c', 'r', 'a', 'f', 't',
			'b', 'u', 'k', 'k', 'i', 't' })),
	NONE("");

	private final String uri;

	PackageWrapper(String uri) {
		this.uri = uri;
	}

	/**
	 * @return 该包的 Uri
	 */
	public String getUri() {
		return this.uri;
	}

}
