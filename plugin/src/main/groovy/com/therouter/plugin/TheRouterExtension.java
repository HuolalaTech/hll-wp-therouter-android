package com.therouter.plugin;

import java.util.HashSet;
import java.util.Set;

public class TheRouterExtension {
    // 是否开启调试模式
    public boolean debug = false;
    // 编译期检查路由表合法性，可选参数 warning(仅告警)/error(编译期抛异常)/delete(每次根据注解重新生成路由表)，不配置则不校验
    public String checkRouteMap = "";
    // 检查 FlowTask 是否有循环引用，可选参数 warning(仅打印日志)/error(编译期抛异常)，不配置则不校验
    public String checkFlowDepend = "";
    // 图形化展示当前的 FlowTask 依赖图
    public boolean showFlowDepend = false;
    // 强制开启增量编译，增量编译默认只在debug模式并且有缓存时开启，如果强制开启增量编译，不论debug还是release都会启用增量
    public boolean forceIncremental = false;
    // 自定义增量编译缓存路径，取settings.gradle所在文件夹的相对路径
    public String incrementalCachePath = "";
    // 在编译期要被干掉的类（只对 jar 包生效，源码不过滤)，可以用这个功能替换一些 jar 包中的类
    // 格式： com/therouter/app/R$anim.class
    public Set<String> removeClass = new HashSet<>();
}
