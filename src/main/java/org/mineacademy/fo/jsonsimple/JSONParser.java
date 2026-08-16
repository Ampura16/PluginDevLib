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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import lombok.NonNull;

/**
 * Jsoner 提供 JSON 工具：将字符串转义为 JSON 兼容格式、线程安全地解析（RFC 7159）JSON
 * 字符串，以及线程安全地将数据序列化为 JSON 格式的字符串。
 *
 * @author https://cliftonlabs.github.io/json-simple/
 * @since 2.0.0
 */
public class JSONParser {
	/** 用于调整主反序列化方法行为的标志。 */
	private enum DeserializationOptions {
		/** 是否允许将多个 JSON 值反序列化为根元素。 */
		ALLOW_CONCATENATED_JSON_VALUES,
		/** 是否允许将 JsonArray 反序列化为根元素。 */
		ALLOW_JSON_ARRAYS,
		/** 是否允许将 boolean、null、Number 或 String 反序列化为根元素。 */
		ALLOW_JSON_DATA,
		/** 是否允许将 JsonObject 反序列化为根元素。 */
		ALLOW_JSON_OBJECTS;
	}

	/** 用于调整主序列化方法行为的标志。 */
	private enum SerializationOptions {
		/**
		 * 遇到非 JSON 值时不中止序列化，而是将该非 JSON 值直接序列化
		 * 到（此时已无效的）JSON 中继续进行。请注意，无效的 JSON 无法成功
		 * 反序列化。
		 */
		ALLOW_INVALIDS,
		/**
		 * 遇到实现了 Jsonable 的非 JSON 值时不中止序列化，而是把序列化
		 * 交给该 Jsonable 继续进行。
		 * @see Jsonable
		 */
		ALLOW_JSONABLES;
	}

	/** JSON 反序列化器的可能状态。 */
	private enum States {
		/** 解析后状态。 */
		DONE,
		/** 解析前状态。 */
		INITIAL,
		/** 解析错误，应抛出 ParsingException。 */
		PARSED_ERROR,
		PARSING_ARRAY,
		/** 正在解析对象内的键值对。 */
		PARSING_ENTRY,
		PARSING_OBJECT;
	}

	/**
	 * 返回 JSON 解析器的新实例
	 *
	 * @deprecated 请直接调用静态方法
	 *
	 * @return
	 */
	@Deprecated
	public static JSONParser getInstance() {
		return new JSONParser();
	}

	private JSONParser() {
		/* Jsoner is purely static so instantiation is unnecessary. */
	}

	/**
	 * @see #deserialize(String)
	 *
	 * @param reader
	 * @return
	 * @throws JSONParseException
	 * @deprecated 请改用 deserialize()
	 */
	@Deprecated
	public static Object parse(Reader reader) throws JSONParseException {
		return deserialize(reader);
	}

	/**
	 * @see #deserialize(String)
	 *
	 * @param json
	 * @return
	 * @throws JSONParseException
	 * @deprecated 请改用 {@link #deserialize(String)}
	 */
	@Deprecated
	public static Object parse(String json) throws JSONParseException {
		return deserialize(json);
	}

	/**
	 * 按照 RFC 7159 JSON 规范反序列化可读流。
	 * @param readableDeserializable 要作为 JSON 反序列化的内容。
	 * @return 最能表示该可反序列化内容的 boolean、null、Number、String、JsonObject 或 JsonArray。
	 * @throws JSONParseException 若在可反序列化内容中遇到意外的标记。恢复
	 * JsonException 的方法：修正可反序列化内容，使其不再包含意外标记，然后重试。
	 */
	public static Object deserialize(final Reader readableDeserializable) throws JSONParseException {
		return JSONParser.deserialize(readableDeserializable, EnumSet.of(DeserializationOptions.ALLOW_JSON_ARRAYS, DeserializationOptions.ALLOW_JSON_OBJECTS, DeserializationOptions.ALLOW_JSON_DATA)).get(0);
	}

	/**
	 * 反序列化一个流，所有反序列化得到的 JSON 值都包装在 JsonArray 中。
	 * @param deserializable 要作为 JSON 反序列化的内容。
	 * @param flags 反序列化的允许项与限制。
	 * @return 最能表示该可反序列化内容的允许对象。
	 * @throws JsonException 若在可反序列化内容中遇到不允许或意外的标记。恢复
	 *         JsonException 的方法：修正可反序列化内容，使其不再包含不允许或意外的标记，然后
	 * 重试。
	 */
	private static JSONArray deserialize(final Reader deserializable, final Set<DeserializationOptions> flags) throws JSONParseException {
		final Yylex lexer = new Yylex(deserializable);
		Yytoken token;
		States currentState;
		int returnCount = 1;
		final LinkedList<States> stateStack = new LinkedList<>();
		final LinkedList<Object> valueStack = new LinkedList<>();
		stateStack.addLast(States.INITIAL);
		do {
			/* Parse through the parsable string's tokens. */
			currentState = JSONParser.popNextState(stateStack);
			token = JSONParser.lexNextToken(lexer);
			switch (currentState) {
				case DONE:
					/* The parse has finished a JSON value. */
					if (!flags.contains(DeserializationOptions.ALLOW_CONCATENATED_JSON_VALUES) || Yytoken.Types.END.equals(token.getType()))
						/* Break if concatenated values are not allowed or if an END token is read. */
						break;
					/* Increment the amount of returned JSON values and treat the token as if it were a fresh parse. */
					returnCount += 1;
					/* Fall through to the case for the initial state. */
					//$FALL-THROUGH$
				case INITIAL:
					/* The parse has just started. */
					switch (token.getType()) {
						case DATUM:
							/* A boolean, null, Number, or String could be detected. */
							if (flags.contains(DeserializationOptions.ALLOW_JSON_DATA)) {
								valueStack.addLast(token.getValue());
								stateStack.addLast(States.DONE);
							} else
								throw new JSONParseException(lexer.getPosition(), JSONParseException.Problems.DISALLOWED_TOKEN, token);
							break;
						case LEFT_BRACE:
							/* An object is detected. */
							if (flags.contains(DeserializationOptions.ALLOW_JSON_OBJECTS)) {
								valueStack.addLast(new JSONObject());
								stateStack.addLast(States.PARSING_OBJECT);
							} else
								throw new JSONParseException(lexer.getPosition(), JSONParseException.Problems.DISALLOWED_TOKEN, token);
							break;
						case LEFT_SQUARE:
							/* An array is detected. */
							if (flags.contains(DeserializationOptions.ALLOW_JSON_ARRAYS)) {
								valueStack.addLast(new JSONArray());
								stateStack.addLast(States.PARSING_ARRAY);
							} else
								throw new JSONParseException(lexer.getPosition(), JSONParseException.Problems.DISALLOWED_TOKEN, token);
							break;
						default:
							/* Neither a JSON array or object was detected. */
							throw new JSONParseException(lexer.getPosition(), JSONParseException.Problems.UNEXPECTED_TOKEN, token);
					}
					break;
				case PARSED_ERROR:
					/* The parse could be in this state due to the state stack not having a state to pop off. */
					throw new JSONParseException(lexer.getPosition(), JSONParseException.Problems.UNEXPECTED_TOKEN, token);
				case PARSING_ARRAY:
					switch (token.getType()) {
						case COMMA:
							/* The parse could detect a comma while parsing an array since it separates each element. */
							stateStack.addLast(currentState);
							break;
						case DATUM:
							/* The parse found an element of the array. */
							JSONArray val = (JSONArray) valueStack.getLast();
							val.add(token.getValue());
							stateStack.addLast(currentState);
							break;
						case LEFT_BRACE:
							/* The parse found an object in the array. */
							val = (JSONArray) valueStack.getLast();
							final JSONObject object = new JSONObject();
							val.add(object);
							valueStack.addLast(object);
							stateStack.addLast(currentState);
							stateStack.addLast(States.PARSING_OBJECT);
							break;
						case LEFT_SQUARE:
							/* The parse found another array in the array. */
							val = (JSONArray) valueStack.getLast();
							final JSONArray array = new JSONArray();
							val.add(array);
							valueStack.addLast(array);
							stateStack.addLast(currentState);
							stateStack.addLast(States.PARSING_ARRAY);
							break;
						case RIGHT_SQUARE:
							/* The parse found the end of the array. */
							if (valueStack.size() > returnCount)
								valueStack.removeLast();
							else
								/* The parse has been fully resolved. */
								stateStack.addLast(States.DONE);
							break;
						default:
							/* Any other token is invalid in an array. */
							throw new JSONParseException(lexer.getPosition(), JSONParseException.Problems.UNEXPECTED_TOKEN, token);
					}
					break;
				case PARSING_OBJECT:
					/* The parse has detected the start of an object. */
					switch (token.getType()) {
						case COMMA:
							/* The parse could detect a comma while parsing an object since it separates each key value
							 * pair. Continue parsing the object. */
							stateStack.addLast(currentState);
							break;
						case DATUM:
							/* The token ought to be a key. */
							if (token.getValue() instanceof String) {
								/* JSON keys are always strings, strings are not always JSON keys but it is going to be
								 * treated as one. Continue parsing the object. */
								final String key = (String) token.getValue();
								valueStack.addLast(key);
								stateStack.addLast(currentState);
								stateStack.addLast(States.PARSING_ENTRY);
							} else
								/* Abort! JSON keys are always strings and it wasn't a string. */
								throw new JSONParseException(lexer.getPosition(), JSONParseException.Problems.UNEXPECTED_TOKEN, token);
							break;
						case RIGHT_BRACE:
							/* The parse has found the end of the object. */
							if (valueStack.size() > returnCount)
								/* There are unresolved values remaining. */
								valueStack.removeLast();
							else
								/* The parse has been fully resolved. */
								stateStack.addLast(States.DONE);
							break;
						default:
							/* The parse didn't detect the end of an object or a key. */
							throw new JSONParseException(lexer.getPosition(), JSONParseException.Problems.UNEXPECTED_TOKEN, token);
					}
					break;
				case PARSING_ENTRY:
					switch (token.getType()) {
						/* Parsed pair keys can only happen while parsing objects. */
						case COLON:
							/* The parse could detect a colon while parsing a key value pair since it separates the key
							 * and value from each other. Continue parsing the entry. */
							stateStack.addLast(currentState);
							break;
						case DATUM:
							/* The parse has found a value for the parsed pair key. */
							String key = (String) valueStack.removeLast();
							JSONObject parent = (JSONObject) valueStack.getLast();
							parent.put(key, token.getValue());
							break;
						case LEFT_BRACE:
							/* The parse has found an object for the parsed pair key. */
							key = (String) valueStack.removeLast();
							parent = (JSONObject) valueStack.getLast();
							final JSONObject object = new JSONObject();
							parent.put(key, object);
							valueStack.addLast(object);
							stateStack.addLast(States.PARSING_OBJECT);
							break;
						case LEFT_SQUARE:
							/* The parse has found an array for the parsed pair key. */
							key = (String) valueStack.removeLast();
							parent = (JSONObject) valueStack.getLast();
							final JSONArray array = new JSONArray();
							parent.put(key, array);
							valueStack.addLast(array);
							stateStack.addLast(States.PARSING_ARRAY);
							break;
						default:
							/* The parse didn't find anything for the parsed pair key. */
							throw new JSONParseException(lexer.getPosition(), JSONParseException.Problems.UNEXPECTED_TOKEN, token);
					}
					break;
				default:
					break;
			}
			/* If we're not at the END and DONE then do the above again. */
		} while (!(States.DONE.equals(currentState) && Yytoken.Types.END.equals(token.getType())));
		return new JSONArray(valueStack);
	}

	/**
	 * 假定使用 StringReader 来反序列化字符串的便捷方法。
	 * @param deserializable 要作为 JSON 反序列化的内容。
	 * @return 最能表示该可反序列化内容的 boolean、null、Number、String、JsonObject 或 JsonArray。
	 * @throws JSONParseException 若在可反序列化内容中遇到意外的标记。恢复
	 *         JsonException 的方法：修正可反序列化内容，使其不再包含意外标记，然后重试。
	 *
	 * @see StringReader
	 */
	public static Object deserialize(@NonNull final String deserializable) throws JSONParseException {

		final String trimmed = deserializable.trim();

		// Assume it's just a normal string
		if (!trimmed.startsWith("{") || !trimmed.endsWith("}"))
			return deserializable;

		Object returnable;
		StringReader readableDeserializable = null;
		try {
			readableDeserializable = new StringReader(deserializable);
			returnable = JSONParser.deserialize(readableDeserializable);
		} catch (final NullPointerException caught) {
			/* They both have the same recovery scenario.
			 * See StringReader.
			 * If deserializable is null, it should be reasonable to expect null back. */
			returnable = null;
		} finally {
			if (readableDeserializable != null)
				readableDeserializable.close();
		}
		return returnable;
	}

	/**
	 * 假定必须反序列化出 JsonArray 的便捷方法。
	 * @param deserializable 要作为 JsonArray 反序列化的内容。
	 * @param defaultValue 当可反序列化内容不是 JsonArray，或反序列化过程中发生 IOException、
	 *        NullPointerException 或 JsonException 时返回的值。
	 * @return 表示该可反序列化内容的 JsonArray；若没有能表示它的 JsonArray，
	 *         则返回 defaultValue。
	 */
	public static JSONArray deserialize(final String deserializable, final JSONArray defaultValue) {
		StringReader readable = null;
		JSONArray returnable;
		try {
			readable = new StringReader(deserializable);
			returnable = JSONParser.deserialize(readable, EnumSet.of(DeserializationOptions.ALLOW_JSON_ARRAYS)).getArray(0);
		} catch (NullPointerException | JSONParseException caught) {
			/* Don't care, just return the default value. */
			returnable = defaultValue;
		} finally {
			if (readable != null)
				readable.close();
		}
		return returnable;
	}

	/**
	 * 假定必须反序列化出 JsonObject 的便捷方法。
	 * @param deserializable 要作为 JsonObject 反序列化的内容。
	 * @param defaultValue 当可反序列化内容不是 JsonObject，或反序列化过程中发生 IOException、
	 *        NullPointerException 或 JsonException 时返回的值。
	 * @return 表示该可反序列化内容的 JsonObject；若没有能表示它的 JsonObject，
	 *         则返回 defaultValue。
	 */
	public static JSONObject deserialize(final String deserializable, final JSONObject defaultValue) {
		StringReader readable = null;
		JSONObject returnable;
		try {
			readable = new StringReader(deserializable);
			returnable = JSONParser.deserialize(readable, EnumSet.of(DeserializationOptions.ALLOW_JSON_OBJECTS)).<JSONObject>getMap(0);
		} catch (NullPointerException | JSONParseException caught) {
			/* Don't care, just return the default value. */
			returnable = defaultValue;
		} finally {
			if (readable != null)
				readable.close();
		}
		return returnable;
	}

	/**
	 * 假定多个 RFC 7159 JSON 值（数字除外）被拼接在一起进行反序列化的便捷方法，
	 * 结果会统一包装在一个 JsonArray 中返回。
	 * 其中可以包含数字，但数字之间不能直接拼接，因为这容易引发
	 * NumberFormatException（从而导致 JsonException），或使数字不再表示
	 * 各自原本的值。
	 * 示例：
	 * "123null321" returns [123, null, 321]
	 * "nullnullnulltruefalse\"\"{}[]" returns [null, null, null, true, false, "", {}, []]
	 * "123" appended to "321" returns [123321]
	 * "12.3" appended to "3.21" throws JsonException(NumberFormatException)
	 * "123" appended to "-321" throws JsonException(NumberFormatException)
	 * "123e321" appended to "-1" throws JsonException(NumberFormatException)
	 * "null12.33.21null" throws JsonException(NumberFormatException)
	 * @param deserializable 在同一个 reader 中要作为 JSON 反序列化的拼接内容。其内容
	 *        不能包含两个直接拼接在一起的数字。
	 * @return 以各个拼接对象为元素的 JsonArray。每个拼接元素都是最能表示
	 *         deserializable 中对应拼接内容的 boolean、null、Number、String、JsonArray 或 JsonObject。
	 * @throws JSONParseException 若在可反序列化内容中遇到意外的标记。恢复
	 * JsonException 的方法：修正可反序列化内容，使其不再包含意外标记，然后重试。
	 */
	public static JSONArray deserializeMany(final Reader deserializable) throws JSONParseException {
		return JSONParser.deserialize(deserializable, EnumSet.of(DeserializationOptions.ALLOW_JSON_ARRAYS, DeserializationOptions.ALLOW_JSON_OBJECTS, DeserializationOptions.ALLOW_JSON_DATA, DeserializationOptions.ALLOW_CONCATENATED_JSON_VALUES));
	}

	/**
	 * 转义所给字符串中可能引起混淆或重要的字符。
	 * @param escapable 未转义的字符串。
	 * @return 可用于 JSON 的已转义字符串；已转义字符串是指所有引号 (")、
	 *         反斜杠 (\)、回车符 (\r)、换行符 (\n)、制表符 (\t)、
	 *         退格符 (\b)、换页符 (\f) 以及其他控制字符 [u0000..u001F] 或
	 *         字符 [u007F..u009F]、[u2000..u20FF] 都已用
	 * 反斜杠 (\) 转义的字符串，而在 java 字符串中该反斜杠本身也必须用反斜杠转义。
	 */
	public static String escape(final String escapable) {
		final StringBuilder builder = new StringBuilder();
		final int characters = escapable.length();
		for (int i = 0; i < characters; i++) {
			final char character = escapable.charAt(i);
			switch (character) {
				case '"':
					builder.append("\\\"");
					break;
				case '\\':
					builder.append("\\\\");
					break;
				case '\b':
					builder.append("\\b");
					break;
				case '\f':
					builder.append("\\f");
					break;
				case '\n':
					builder.append("\\n");
					break;
				case '\r':
					builder.append("\\r");
					break;
				case '\t':
					builder.append("\\t");
					break;
				default:
					/* The many characters that get replaced are benign to software but could be mistaken by people
					 * reading it for a JSON relevant character. */
					if (((character >= '\u0000') && (character <= '\u001F')) || ((character >= '\u007F') && (character <= '\u009F')) || ((character >= '\u2000') && (character <= '\u20FF'))) {
						final String characterHexCode = Integer.toHexString(character);
						builder.append("\\u");
						for (int k = 0; k < (4 - characterHexCode.length()); k++)
							builder.append("0");
						builder.append(characterHexCode.toUpperCase());
					} else
						/* Character didn't need escaping. */
						builder.append(character);
			}
		}
		return builder.toString();
	}

	/**
	 * 处理词法分析器的 reader，获取下一个标记。
	 * @param lexer 反序列化过程中使用的文本处理器。
	 * @return 表示词法分析器遇到的有意义元素的标记。
	 * @throws JsonException 若处理文本时遇到意外字符。
	 */
	private static Yytoken lexNextToken(final Yylex lexer) throws JSONParseException {
		Yytoken returnable;
		/* Parse through the next token. */
		try {
			returnable = lexer.yylex();
		} catch (final IOException caught) {
			throw new JSONParseException(-1, JSONParseException.Problems.UNEXPECTED_EXCEPTION, caught);
		}
		if (returnable == null)
			/* If there isn't another token, it must be the end. */
			returnable = new Yytoken(Yytoken.Types.END, null);
		return returnable;
	}

	/**
	 * 用于反序列化时的状态转换。
	 * @param stateStack 为后续处理保存的反序列化状态。
	 * @return 反序列化上下文的状态，使其知道如何消费下一个标记。
	 */
	private static States popNextState(final LinkedList<States> stateStack) {
		if (stateStack.size() > 0)
			return stateStack.removeLast();
		else
			return States.PARSED_ERROR;
	}

	/**
	 * 使用调用者选择的缩进和换行，让 JSON 输入更易于阅读。这意味着
	 * 此方法输出的 JSON 是否有效取决于调用者选择的缩进和换行。
	 * @param readable 不含多余字符的 JSON 格式字符串，例如由
	 *        Jsoner#serialize(Object) 返回的字符串。
	 * @param writable 美化后的 JSON 应写入的位置。
	 * @param indentation 用于格式化 JSON 字符串的缩进。不会校验其是否为合法的
	 *        缩进。推荐使用制表符 ("\t")，3 或 4 个空格也是常见的替代方案。
	 * @param newline 用于格式化 JSON 字符串的换行符。不会校验其是否为合法的换行符。
	 *        推荐使用 "\n"，"\r" 或 "/r/n" 也是常见的替代方案。
	 * @throws IOException 若所提供的 writer 遇到 IO 问题。
	 * @throws JSONParseException 若所提供的 reader 遇到 IO 问题。
	 *
	 * @since 3.1.0 改为 public，以支持大型 JSON 输入和更灵活的美化输出控制。
	 */
	public static void prettyPrint(final Reader readable, final Writer writable, final String indentation, final String newline) throws IOException, JSONParseException {
		final Yylex lexer = new Yylex(readable);
		Yytoken lexed;
		int level = 0;
		do {
			lexed = JSONParser.lexNextToken(lexer);
			switch (lexed.getType()) {
				case COLON:
					writable.append(lexed.getValue().toString());
					writable.append(' ');
					break;
				case COMMA:
					writable.append(lexed.getValue().toString());
					writable.append(newline);
					for (int i = 0; i < level; i++)
						writable.append(indentation);
					break;
				case END:
					break;
				case LEFT_BRACE:
				case LEFT_SQUARE:
					writable.append(lexed.getValue().toString());
					writable.append(newline);
					level++;
					for (int i = 0; i < level; i++)
						writable.append(indentation);
					break;
				case RIGHT_BRACE:
				case RIGHT_SQUARE:
					writable.append(newline);
					level--;
					for (int i = 0; i < level; i++)
						writable.append(indentation);
					writable.append(lexed.getValue().toString());
					break;
				default:
					if (lexed.getValue() == null)
						writable.append("null");
					else if (lexed.getValue() instanceof String) {
						writable.append("\"");
						writable.append(JSONParser.escape((String) lexed.getValue()));
						writable.append("\"");
					} else
						writable.append(lexed.getValue().toString());
					break;
			}
		} while (!lexed.getType().equals(Yytoken.Types.END));
		writable.flush();
	}

	/**
	 * 使用制表符 ("\t") 和换行符 "\n" 美化输出字符串的便捷方法。
	 * @param printable 不含多余字符的 JSON 格式字符串，例如由
	 *        Jsoner#serialize(Object) 返回的字符串。
	 * @return 与 printable 相同，但会在 JSON 中的 '['、'{'、',' 之后以及 ']' '}' 标记之前
	 * 插入 '\n' 和 '\t' 字符。若 printable 不是 JSON 字符串则返回 null。
	 */
	public static String prettyPrint(final String printable) {
		final StringWriter writer = new StringWriter();
		try {
			JSONParser.prettyPrint(new StringReader(printable), writer, "\t", "\n");
		} catch (final IOException caught) {
			/* See java.io.StringReader.
			 * See java.io.StringWriter. */
		} catch (final JSONParseException caught) {
			/* Would have been caused by a an IO exception while lexing, but the StringReader does not throw them. See
			 * java.io.StringReader. */
		}
		return writer.toString();
	}

	/**
	 * 假定使用 StringWriter 的便捷方法。
	 * @param jsonSerializable 应序列化为 JSON 格式字符串的对象。
	 * @return 表示所给对象的 JSON 格式字符串。
	 * @throws IllegalArgumentException 若 jsonSerializable 无法序列化为 JSON。
	 *
	 * @see StringWriter
	 */
	public static String serialize(final Object jsonSerializable) {
		final StringWriter writableDestination = new StringWriter();
		try {
			JSONParser.serialize(jsonSerializable, writableDestination);
		} catch (final IOException caught) {
			/* See java.io.StringWriter. */
		}
		return writableDestination.toString();
	}

	/**
	 * 按照 RFC 7159 JSON 规范序列化值。对于其序列化的任何 Jsonable，
	 * 也会信任它们提供的序列化结果。
	 * @param jsonSerializable 应序列化为 JSON 格式的对象。
	 * @param writableDestination 生成的 JSON 文本写入的位置。
	 * @throws IOException 若 writableDestination 遇到 I/O 问题，例如在使用中被关闭。
	 * @throws IllegalArgumentException 若 jsonSerializable 无法序列化为 JSON。
	 */
	public static void serialize(final Object jsonSerializable, final Writer writableDestination) throws IOException {
		JSONParser.serialize(jsonSerializable, writableDestination, EnumSet.of(SerializationOptions.ALLOW_JSONABLES));
	}

	/**
	 * 根据行为标志将值序列化为 JSON，并写入所提供的 writer。
	 * @param jsonSerializable 应序列化为 JSON 格式字符串的对象。
	 * @param writableDestination 生成的 JSON 文本写入的位置。
	 * @param flags 序列化的允许项与限制。
	 * @throws IOException 若 writableDestination 遇到 I/O 问题。
	 * @throws IllegalArgumentException 若 jsonSerializable 无法序列化为 JSON。
	 * @see SerializationOptions
	 */
	private static void serialize(final Object jsonSerializable, final Writer writableDestination, final Set<SerializationOptions> flags) throws IOException {
		if (jsonSerializable == null)
			/* When a null is passed in the word null is supported in JSON. */
			writableDestination.write("null");
		else if (((jsonSerializable instanceof Jsonable) && flags.contains(SerializationOptions.ALLOW_JSONABLES)))
			/* Writes the writable as defined by the writable. */
			((Jsonable) jsonSerializable).toJson(writableDestination);
		else if (jsonSerializable instanceof String) {
			/* Make sure the string is properly escaped. */
			writableDestination.write('"');
			writableDestination.write(JSONParser.escape((String) jsonSerializable));
			writableDestination.write('"');
		} else if (jsonSerializable instanceof Character)
			/* Make sure the string is properly escaped.
			 * Quotes for some reason are necessary for String, but not Character. */
			writableDestination.write(JSONParser.escape(jsonSerializable.toString()));
		else if (jsonSerializable instanceof Double) {
			if (((Double) jsonSerializable).isInfinite() || ((Double) jsonSerializable).isNaN())
				/* Infinite and not a number are not supported by the JSON specification, so null is used instead. */
				writableDestination.write("null");
			else
				writableDestination.write(jsonSerializable.toString());
		} else if (jsonSerializable instanceof Float) {
			if (((Float) jsonSerializable).isInfinite() || ((Float) jsonSerializable).isNaN())
				/* Infinite and not a number are not supported by the JSON specification, so null is used instead. */
				writableDestination.write("null");
			else
				writableDestination.write(jsonSerializable.toString());
		} else if (jsonSerializable instanceof Number)
			writableDestination.write(jsonSerializable.toString());
		else if (jsonSerializable instanceof Boolean)
			writableDestination.write(jsonSerializable.toString());
		else if (jsonSerializable instanceof Map) {
			/* Writes the map in JSON object format. */
			boolean isFirstEntry = true;
			@SuppressWarnings("rawtypes")
			final Iterator entries = ((Map) jsonSerializable).entrySet().iterator();
			writableDestination.write('{');
			while (entries.hasNext()) {
				if (isFirstEntry)
					isFirstEntry = false;
				else
					writableDestination.write(',');
				@SuppressWarnings("rawtypes")
				final Map.Entry entry = (Map.Entry) entries.next();
				JSONParser.serialize(entry.getKey(), writableDestination, flags);
				writableDestination.write(':');
				JSONParser.serialize(entry.getValue(), writableDestination, flags);
			}
			writableDestination.write('}');
		} else if (jsonSerializable instanceof Collection) {
			/* Writes the collection in JSON array format. */
			boolean isFirstElement = true;
			@SuppressWarnings("rawtypes")
			final Iterator elements = ((Collection) jsonSerializable).iterator();
			writableDestination.write('[');
			while (elements.hasNext()) {
				if (isFirstElement)
					isFirstElement = false;
				else
					writableDestination.write(',');
				JSONParser.serialize(elements.next(), writableDestination, flags);
			}
			writableDestination.write(']');
		} else if (jsonSerializable instanceof byte[]) {
			/* Writes the array in JSON array format. */
			final byte[] writableArray = (byte[]) jsonSerializable;
			final int numberOfElements = writableArray.length;
			writableDestination.write('[');
			for (int i = 0; i < numberOfElements; i++)
				if (i == (numberOfElements - 1))
					JSONParser.serialize(writableArray[i], writableDestination, flags);
				else {
					JSONParser.serialize(writableArray[i], writableDestination, flags);
					writableDestination.write(',');
				}
			writableDestination.write(']');
		} else if (jsonSerializable instanceof short[]) {
			/* Writes the array in JSON array format. */
			final short[] writableArray = (short[]) jsonSerializable;
			final int numberOfElements = writableArray.length;
			writableDestination.write('[');
			for (int i = 0; i < numberOfElements; i++)
				if (i == (numberOfElements - 1))
					JSONParser.serialize(writableArray[i], writableDestination, flags);
				else {
					JSONParser.serialize(writableArray[i], writableDestination, flags);
					writableDestination.write(',');
				}
			writableDestination.write(']');
		} else if (jsonSerializable instanceof int[]) {
			/* Writes the array in JSON array format. */
			final int[] writableArray = (int[]) jsonSerializable;
			final int numberOfElements = writableArray.length;
			writableDestination.write('[');
			for (int i = 0; i < numberOfElements; i++)
				if (i == (numberOfElements - 1))
					JSONParser.serialize(writableArray[i], writableDestination, flags);
				else {
					JSONParser.serialize(writableArray[i], writableDestination, flags);
					writableDestination.write(',');
				}
			writableDestination.write(']');
		} else if (jsonSerializable instanceof long[]) {
			/* Writes the array in JSON array format. */
			final long[] writableArray = (long[]) jsonSerializable;
			final int numberOfElements = writableArray.length;
			writableDestination.write('[');
			for (int i = 0; i < numberOfElements; i++)
				if (i == (numberOfElements - 1))
					JSONParser.serialize(writableArray[i], writableDestination, flags);
				else {
					JSONParser.serialize(writableArray[i], writableDestination, flags);
					writableDestination.write(',');
				}
			writableDestination.write(']');
		} else if (jsonSerializable instanceof float[]) {
			/* Writes the array in JSON array format. */
			final float[] writableArray = (float[]) jsonSerializable;
			final int numberOfElements = writableArray.length;
			writableDestination.write('[');
			for (int i = 0; i < numberOfElements; i++)
				if (i == (numberOfElements - 1))
					JSONParser.serialize(writableArray[i], writableDestination, flags);
				else {
					JSONParser.serialize(writableArray[i], writableDestination, flags);
					writableDestination.write(',');
				}
			writableDestination.write(']');
		} else if (jsonSerializable instanceof double[]) {
			/* Writes the array in JSON array format. */
			final double[] writableArray = (double[]) jsonSerializable;
			final int numberOfElements = writableArray.length;
			writableDestination.write('[');
			for (int i = 0; i < numberOfElements; i++)
				if (i == (numberOfElements - 1))
					JSONParser.serialize(writableArray[i], writableDestination, flags);
				else {
					JSONParser.serialize(writableArray[i], writableDestination, flags);
					writableDestination.write(',');
				}
			writableDestination.write(']');
		} else if (jsonSerializable instanceof boolean[]) {
			/* Writes the array in JSON array format. */
			final boolean[] writableArray = (boolean[]) jsonSerializable;
			final int numberOfElements = writableArray.length;
			writableDestination.write('[');
			for (int i = 0; i < numberOfElements; i++)
				if (i == (numberOfElements - 1))
					JSONParser.serialize(writableArray[i], writableDestination, flags);
				else {
					JSONParser.serialize(writableArray[i], writableDestination, flags);
					writableDestination.write(',');
				}
			writableDestination.write(']');
		} else if (jsonSerializable instanceof char[]) {
			/* Writes the array in JSON array format. */
			final char[] writableArray = (char[]) jsonSerializable;
			final int numberOfElements = writableArray.length;
			writableDestination.write("[\"");
			for (int i = 0; i < numberOfElements; i++)
				if (i == (numberOfElements - 1))
					JSONParser.serialize(writableArray[i], writableDestination, flags);
				else {
					JSONParser.serialize(writableArray[i], writableDestination, flags);
					writableDestination.write("\",\"");
				}
			writableDestination.write("\"]");
		} else if (jsonSerializable instanceof Object[]) {
			/* Writes the array in JSON array format. */
			final Object[] writableArray = (Object[]) jsonSerializable;
			final int numberOfElements = writableArray.length;
			writableDestination.write('[');
			for (int i = 0; i < numberOfElements; i++)
				if (i == (numberOfElements - 1))
					JSONParser.serialize(writableArray[i], writableDestination, flags);
				else {
					JSONParser.serialize(writableArray[i], writableDestination, flags);
					writableDestination.write(",");
				}
			writableDestination.write(']');
		} else /* It cannot by any measure be safely serialized according to specification. */
		if (flags.contains(SerializationOptions.ALLOW_INVALIDS))
			/* Can be helpful for debugging how it isn't valid. */
			writableDestination.write(jsonSerializable.toString());
		else
			/* Notify the caller the cause of failure for the serialization. */
			throw new IllegalArgumentException("Encountered a: " + jsonSerializable.getClass().getName() + " as: " + jsonSerializable.toString() + " (" + jsonSerializable.getClass().getSimpleName() + ") "
					+ " that isn't JSON serializable.\n  Try:\n    1) Implementing the Jsonable interface for the object to return valid JSON. If it already does it probably has a bug.\n    2) If you cannot edit the source of the object or couple it with this library consider wrapping it in a class that does implement the Jsonable interface.\n    3) Otherwise convert it to a boolean, null, number, JsonArray, JsonObject, or String value before serializing it.\n    4) If you feel it should have serialized you could use a more tolerant serialization for debugging purposes.");
	}

	/**
	 * 以本库第一个版本的方式进行序列化。
	 * 它已改为使用 Jsonable 序列化自定义对象，其余行为与旧的 JSON 字符串
	 * 序列化器相同。与旧版一样，它允许输出中包含非 JSON 值。它可用于作为最后手段的日志
	 * 语句，以及调试自行生成的 JSON 中的错误。使用此方法序列化的任何内容都不保证
	 * 能够被反序列化。
	 * @param jsonSerializable 应序列化为 JSON 格式的对象。
	 * @param writableDestination 生成的 JSON 文本写入的位置。
	 * @throws IOException 若 writableDestination 遇到 I/O 问题，例如在使用中被关闭。
	 */
	public static void serializeCarelessly(final Object jsonSerializable, final Writer writableDestination) throws IOException {
		JSONParser.serialize(jsonSerializable, writableDestination, EnumSet.of(SerializationOptions.ALLOW_JSONABLES, SerializationOptions.ALLOW_INVALIDS));
	}

	/**
	 * 按照 RFC 7159 JSON 规范序列化 JSON 值，且只序列化 JSON 值。
	 * @param jsonSerializable 应序列化为 JSON 格式的对象。
	 * @param writableDestination 生成的 JSON 文本写入的位置。
	 * @throws IOException 若 writableDestination 遇到 I/O 问题，例如在使用中被关闭。
	 * @throws IllegalArgumentException 若 jsonSerializable 无法序列化为原始 JSON。
	 */
	public static void serializeStrictly(final Object jsonSerializable, final Writer writableDestination) throws IOException {
		JSONParser.serialize(jsonSerializable, writableDestination, EnumSet.noneOf(SerializationOptions.class));
	}
}
