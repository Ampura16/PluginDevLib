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
package top.brmc.devlib.jsonsimple;

import java.io.IOException;
import java.io.Writer;

/**
 * Jsonable 可以序列化为 JavaScript 对象表示法（JSON）。对 Jsonable 生成的字符串进行反序列化，
 * 应得到该 Jsonable 的 JSON 形式表示。
 * @since 2.0.0
 */
public interface Jsonable {
	/**
	 * 序列化为 JSON 格式的字符串。
	 * @return 以 JSON 格式表示该 Jsonable 的字符串。
	 */
	String toJson();

	/**
	 * 序列化为 JSON 格式的流。
	 * @param writable 生成的 JSON 文本要写入的目标。
	 * @throws IOException 当 writable 发生 I/O 错误时抛出。
	 */
	void toJson(Writer writable) throws IOException;
}
