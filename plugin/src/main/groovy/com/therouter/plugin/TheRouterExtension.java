package com.therouter.plugin;

public class TheRouterExtension {
    // 是否开启调试模式
    public boolean debug = false;
    // 异常时提示日志输出语言，默认cn，可选en
    public String lang = "cn";
    // 编译期检查路由表合法性，可选参数 warning(仅告警)/error(编译期抛异常)/delete(每次根据注解重新生成路由表)，不配置则不校验
    public String checkRouteMap = "";
    // 检查 FlowTask 是否有循环引用，可选参数 warning(仅打印日志)/error(编译期抛异常)，不配置则不校验
    public String checkFlowDepend = "";
    // 图形化展示当前的 FlowTask 依赖图
    public boolean showFlowDepend = false;
}
