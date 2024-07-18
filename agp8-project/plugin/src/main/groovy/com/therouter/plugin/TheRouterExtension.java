package com.therouter.plugin;

public class TheRouterExtension {
    // 是否开启调试模式(开启后会跳过部分合法性检查，提高编译速度，输出编译日志)
    public boolean debug = false;
    // 编译期检查路由表合法性，可选参数 warning(仅告警)/error(抛异常)/delete(每次根据注解重新生成路由表)
    public String checkRouteMap = "";
    // 检查 FlowTask 是否有循环引用
    public String checkFlowDepend = "";
    // 图形化展示当前的 FlowTask 依赖图
    public String showFlowDepend = "";
    // 表示，仅扫描当前工程中源码依赖的模块，即 build.gradle 中 implementation project(":xxxx") 引入的模块
    public boolean sourceOnly = false;
    // 表示仅扫描列表内的依赖（相当于白名单），匹配规则为  aarName.contains(item)
    public String[] scan;
    // 表示扫描除了列表内aar以外的全部依赖（相当于黑名单），匹配规则为  aarName.contains(item)
    public String[] ignore;
}
