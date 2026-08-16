/* Copyright 2016-2017 Clifton Labs
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */
package org.mineacademy.fo.jsonsimple;

/**
 * JsonException 说明了反序列化期间源 JSON 文本中问题发生的方式和位置。
 * @author https://cliftonlabs.github.io/json-simple/
 * @since 3.0.0
 */
public class JSONParseException extends Exception {
	/** 可能触发 JsonException 的异常种类。 */
	public enum Problems {

		DISALLOWED_TOKEN,
		/** @since 2.3.0 用于统一反序列化过程中发生的异常。 */
		IOEXCEPTION,

		UNEXPECTED_CHARACTER,

		UNEXPECTED_EXCEPTION,

		UNEXPECTED_TOKEN;
	}

	private static final long serialVersionUID = 1L;
	private final long position;
	private final Problems problemType;
	private final Object unexpectedObject;

	/**
	 * 在不做任何假设的情况下实例化一个 JsonException。
	 * @param position 异常发生的位置。
	 * @param problemType 异常发生的方式。
	 * @param unexpectedObject 导致异常的内容。
	 */
	public JSONParseException(final long position, final Problems problemType, final Object unexpectedObject) {
		this.position = position;
		this.problemType = problemType;
		this.unexpectedObject = unexpectedObject;
		if (Problems.IOEXCEPTION.equals(problemType) || Problems.UNEXPECTED_EXCEPTION.equals(problemType))
			if (unexpectedObject instanceof Throwable)
				this.initCause((Throwable) unexpectedObject);
	}

	@Override
	public String getMessage() {
		final StringBuilder sb = new StringBuilder();
		switch (this.problemType) {
			case DISALLOWED_TOKEN:
				sb.append("The disallowed token (").append(this.unexpectedObject).append(") was found at position ").append(this.position).append(". If this is in error, try again with a deserialization method in Jsoner that allows the token instead. Otherwise, fix the parsable string and try again.");
				break;
			case IOEXCEPTION:
				sb.append("An IOException was encountered, ensure the reader is properly instantiated, isn't closed, or that it is ready before trying again.\n").append(this.unexpectedObject);
				break;
			case UNEXPECTED_CHARACTER:
				sb.append("The unexpected character (").append(this.unexpectedObject).append(") was found at position ").append(this.position).append(". Fix the parsable string and try again.");
				break;
			case UNEXPECTED_TOKEN:
				sb.append("The unexpected token ").append(this.unexpectedObject).append(" was found at position ").append(this.position).append(". Fix the parsable string and try again.");
				break;
			case UNEXPECTED_EXCEPTION:
				sb.append("Please report this to the library's maintainer. The unexpected exception that should be addressed before trying again occurred at position ").append(this.position).append(":\n").append(this.unexpectedObject);
				break;
			default:
				sb.append("Please report this to the library's maintainer. An error at position ").append(this.position).append(" occurred. There are no recovery recommendations available.");
				break;
		}
		return sb.toString();
	}

	/**
	 * 帮助调试问题所在的位置。
	 * @return 发生错误的字符串字符索引。
	 */
	public long getPosition() {
		return this.position;
	}

	/**
	 * 帮助为问题找到合适的解决方案。
	 * @return 表示异常发生方式的枚举。
	 */
	public Problems getProblemType() {
		return this.problemType;
	}

	/**
	 * 帮助识别问题。
	 * @return 导致异常的内容的表示形式。
	 */
	public Object getUnexpectedObject() {
		return this.unexpectedObject;
	}
}
