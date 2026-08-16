<a href="https://bit.ly/3GHdIQI">
  <img src="https://i.imgur.com/AeprAug.jpg" />
</a>

[![](https://jitpack.io/v/kangarko/Foundation.svg)](https://jitpack.io/#kangarko/Foundation)


[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/kangarko/Foundation)

> 本文件为 [README.md](README.md) 的中文译文。

让 Minecraft 插件开发更快：省去样板代码，让你专注于实现创意，而不是与受限的 Spigot/Bukkit/Paper API 纠缠。

主要特性包括：

- Folia、Bukkit/Spigot/Paper 支持（1.8.8 - 26.1）- 自动版本适配（即：调用一个方法即可在所有 Minecraft 版本上发送标题/动画数据包）
- GUI 菜单 API
- 无需 plugin.yml 的高级命令
- 支持注释、可自动更新的配置文件
- 自动化的第三方库支持：数据包、Discord、Citizens、Towny 等
- 省时的封装：数据库（flatfile SQL、HikariCP、MySQL）、全息 API、自定义物品与头颅 API，等等！

自 2013 年以来，成千上万的服务器运行在 Foundation 之上。它久经沙场，已在 ChatControl、Boss、CoreArena、Confiscate、AutoPlay、Puncher、Winter、AnimeX 等插件中得到验证。

## 请完整阅读快速入门——其中有一个额外步骤，遗漏会导致插件损坏

# 快速入门

**新内容**：观看这个安装 Foundation 的视频教程：https://www.youtube.com/watch?v=gXbZnKYE7ww

1. 使用 Maven/Gradle 引入 Foundation（见下文“引入”一节）。
2. **重要——切勿遗漏**：配置 shading，只包含 Foundation 和你需要的库，否则我们的所有依赖都会被打进你的 jar！示例用法[见此链接](https://github.com/kangarko/PluginTemplate/blob/master/pom.xml#L130)。
3. 把“**extends JavaPlugin**”改为“**extends SimplePlugin**”（我们需要它来自动注册各类组件和监听器）
4. 把 **onEnable()** 改为 **onPluginStart()**，把 **onDisable()** 改为 **onPluginStop()**（这两个方法由我们占用以执行内部逻辑）
5. 如果你在插件主类中使用了 **static getInstance()** 方法，把它改为返回 **(T) SimplePlugin.getInstance()**（其中 T 是你的插件类）。如果你在类中保存了插件实例（如 `myPlugin = this`，请删除它）。

示例插件见 [PluginTemplate](https://github.com/kangarko/plugintemplate)。

关于如何使用本库的完整教程是我们 Project Orion 培训课程的一部分，见[这里](https://mineacademy.org/project-orion)

如果你只想快速上手 Minecraft 插件开发，[看看这个 gist](https://gist.github.com/kangarko/456d9cfce52dc971b93dbbd12a95f43c)。

## 引入

我们使用 JitPack 为你自动编译并托管最新版 Foundation。用 Maven 安装 Foundation 时，打开你的 pom.xml，找到 `<repositories>` 一节，把下面的仓库加进去：

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>
```

然后找到 pom.xml 的 `<dependencies>` 一节，把下面这段加进去。把其中的 "REPLACE_WITH_LATEST_VERSION" 替换为最新版本号，版本号见：https://github.com/kangarko/Foundation/releases

```xml
<dependency>
    <groupId>com.github.kangarko</groupId>
    <artifactId>Foundation</artifactId>
    <version>REPLACE_WITH_LATEST_VERSION</version>
</dependency>
```

## Shading（重要！）

先看上面快速入门指南的第 2 步。

Foundation 自带了一些可直接使用的插件 API（如 WorldEdit 等），方便你写代码时直接调用，而无需自己再把它们作为依赖引入。

Maven 有个限制：如果你没有正确配置 maven-shade-plugin 的 includes，这些插件最终会被打进你的插件 .jar 文件。

如果你是新手，只需复制粘贴下面这段，放到 pom.xml 的 `<plugins>` 一节里（如果已经有这样的一节，先删掉）。

**记得把下面的 your.plugin.main.package 换成你自己的包名。**

如果你想把某个依赖编译进你的 jar，先通过 `<dependency>` 正常引入它，把 scope 设为 "compile"，再把它 include 进来。你可以直接复制 `<include>` 改成你的依赖。

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>

    <!-- 把 version 改为最新版，见
         https://mvnrepository.com/artifact/org.apache.maven.plugins/maven-shade-plugin -->
    <version>3.5.1</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>shade</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <createDependencyReducedPom>false</createDependencyReducedPom>
        <artifactSet>
            <includes>
                <!-- 重要：这能确保只有 Foundation 被打进你的 jar。如果你还有
                     其他需要编译进来的依赖，为每一个复制一行。

                     只在这里添加你希望包含进 PLUGIN.JAR 的库
                     -->
                <include>com.github.kangarko:Foundation*</include>
            </includes>
        </artifactSet>
        <relocations>
            <!-- 把 Foundation 搬到你自己包下的 “lib” 子包，防止冲突。-->
            <relocation>
                <pattern>org.mineacademy.fo</pattern>
                <shadedPattern>your.plugin.main.package.lib</shadedPattern>
            </relocation>
        </relocations>
    </configuration>
</plugin>
```

更多信息，包括如何在 Maven 之外的工具中使用 Foundation，请访问：https://jitpack.io/#kangarko/Foundation/

# 兼容性

我们致力于提供广泛的兼容层，让 1.8.8 到最新版的 Minecraft 版本都能正常工作。

Foundation 支持 Spigot、Paper、Folia 及大多数分支服务端。

# 许可信息

© MineAcademy.org

一句话总结：只要你不把 Foundation 冒充为自己的作品、不出售或转售其中任何部分，你可以随意使用。但如果你不是 MineAcademy 的付费学员，且你的付费软件使用了 Foundation，你必须在销售页面（如 Spigot 上的 Overview 页面）放置指向本 GitHub 页面的链接。

1) **如果你是 MineAcademy.org 的付费学员**，你可以为自己、你的团队或服务器网络使用、修改和复制 Foundation，无论商用还是非商用，无需署名。

4) **如果你不是 MineAcademy.org 的付费学员**，你可以按上述方式使用本库，但必须通过链接到本 GitHub 页面，明确说明你的软件使用了 Foundation。

以上两种情况都一样：不要出售本库的任何部分，也不要声称它是你的作品。

无担保——本软件按“现状”提供，不对其功能做任何保证。我们已尽最大努力让 Foundation 成为企业级的加速编码方案，但对你使用它所取得的成功或失败不承担任何责任。

---

<i>Dave Thomas，OTI 创始人，Eclipse 战略教父：</i>

<i>整洁的代码可以被原作者之外的开发者阅读和改进。它有单元测试和验收测试，有有意义的命名，做一件事只提供一种方式而不是多种。它的依赖最少且显式声明，并提供清晰精简的 API。代码应当是可读的，因为取决于语言，并非所有必要信息都能仅用代码清晰表达。</i>
