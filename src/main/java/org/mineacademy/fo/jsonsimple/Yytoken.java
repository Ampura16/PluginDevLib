/* Copyright 2016 Clifton Labs
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
 * 表示 JSON 中的结构实体。
 * @since 2.0.0
 */
class Yytoken {
	/** 表示不同种类的记号。 */
	enum Types {
		/** 此类型的记号值始终为 ":" */
		COLON,
		/** 此类型的记号值始终为 "," */
		COMMA,
		/** 此类型的记号值始终为 boolean、null、数字或字符串。 */
		DATUM,
		/** 此类型的记号值始终为 "" */
		END,
		/** 此类型的记号值始终为 "{" */
		LEFT_BRACE,
		/** 此类型的记号值始终为 "[" */
		LEFT_SQUARE,
		/** 此类型的记号值始终为 "}" */
		RIGHT_BRACE,
		/** 此类型的记号值始终为 "]" */
		RIGHT_SQUARE;
	}

	private final Types type;
	private final Object value;

	/**
	 * @param type 实例化后的记号所属的种类。
	 * @param value 该记号关联的值；除非 type 等于
	 *        Types.DATUM，否则会被忽略。
	 * @see Types
	 */
	Yytoken(final Types type, final Object value) {
		/* Sanity check. Make sure the value is ignored for the proper value unless it is a datum token. */
		switch (type) {
			case COLON:
				this.value = ":";
				break;
			case COMMA:
				this.value = ",";
				break;
			case END:
				this.value = "";
				break;
			case LEFT_BRACE:
				this.value = "{";
				break;
			case LEFT_SQUARE:
				this.value = "[";
				break;
			case RIGHT_BRACE:
				this.value = "}";
				break;
			case RIGHT_SQUARE:
				this.value = "]";
				break;
			default:
				this.value = value;
				break;
		}
		this.type = type;
	}

	/**
	 * @return 该记号属于哪一种 {@link Yytoken.Types}。
	 * @see Yytoken.Types
	 */
	Types getType() {
		return this.type;
	}

	/**
	 * @return 该记号的内容。
	 * @see Types
	 */
	Object getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(this.type.toString()).append("(").append(this.value).append(")");
		return sb.toString();
	}
}
