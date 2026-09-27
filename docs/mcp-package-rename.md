# 软件包命名空间迁移记录

## 改动范围

- 将 Java 根包从 `org.mineacademy.fo` 迁移到 `top.brmc.devlib`，并将源文件目录从 `src/main/java/org/mineacademy/fo/` 移至 `src/main/java/top/brmc/devlib/`。
- 更新源码中的 package 声明、导入和项目自身的完全限定类名，共 277 个 Java 源文件。
- 将 Maven 项目的 `groupId` 更新为 `top.brmc.devlib`；同步更新 `Library` 中展示的 groupId 示例以及 README 中对应的包名引用。

## 有意保留的外部命名

- `org.mineacademy.plugin:*` 是 pom.xml 中外部插件 API 的 Maven 坐标，并非本项目的 Java 包，因此保持不变。
- `org.mineacademy.boss.*` 是运行时反射访问的外部 Boss 插件 API，保持不变。
- 指向上游 PluginTemplate 的 URL 和引用不属于本项目包名，也保持不变。

## 验证

- 使用 JDK 21 和 Maven 3.9.9 执行 `mvn -B -Dmaven.javadoc.skip=true -DskipTests package`，构建成功并生成 `Foundation-6.10.1.jar`。
- 源码 package 声明与各自目录核对：0 个不匹配。
- 在源码、README、pom.xml、jitpack.yml 和 `.github` 配置中检查旧的 `org.mineacademy.fo` / `org/mineacademy/fo` 名称：无残留。
- Maven 构建按命令参数跳过测试；本项目未配置测试源码。

## 推送状态

更改已准备提交；按用户要求，推送到 GitHub 前等待用户明确确认。
