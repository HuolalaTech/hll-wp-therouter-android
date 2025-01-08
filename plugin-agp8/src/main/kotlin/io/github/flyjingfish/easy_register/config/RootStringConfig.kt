package io.github.flyjingfish.easy_register.config

enum class RootStringConfig(
    val propertyName: String,
    val defaultValue: String,
) {
    /**
     * 是否启动当前插件
     */
    ENABLE("easyRegister.enable", "true"),

    /**
     * 注册模式， 取值范围看 [MODE_SET]
     * debug 是指无论你的当前编译的变体名是什么，都是使用插件的 debug 模式（debug模式是快速模式）
     * release 是指无论你的当前编译的变体名是什么，都是使用插件的 release 模式（release模式是慢速模式）
     * auto 只是根据你的变体名决定使用那种模式，如果变体名包含 debug 则启用 debug 模式，包含 release 则启用 release 模式
     *
     */
    MODE("easyRegister.mode", "debug"),

    /**
     * 配置文件，文件放置于 gradle.properties 同级目录
     */
    CONFIG_JSON("easyRegister.configJson", ""),

    /**
     * 是否已经织入锚点代码
     */
    ANCHOR("easyRegister.isAnchor", "false"),

    /**
     * 当 easyRegister.mode = debug 或 auto 时，如果当前变体为 debug 时，app 模块是否增量编译（如果是增量，则修改新增类时不能更新注册类的信息）
     */
    APP_INCREMENTAL("easyRegister.app.isIncremental", "false");

    companion object{
        val MODE_SET = setOf("debug","release","auto")
    }
}