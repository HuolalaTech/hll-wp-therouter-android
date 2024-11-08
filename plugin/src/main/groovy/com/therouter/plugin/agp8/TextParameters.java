package com.therouter.plugin.agp8;

import com.android.build.api.instrumentation.InstrumentationParameters;

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;

import java.io.File;

public interface TextParameters extends InstrumentationParameters {
    @Input
    Property<File> getAsmTargetFile();

    @Input
    Property<File> getAllClassFile();

    @Input
    Property<File> getFlowTaskFile();

    @Input
    Property<File> getRouteFile();

    @Input
    Property<String> getAllClassText();

    @Input
    Property<String> getAsmTargetText();

    @Input
    Property<Boolean> getDebugValue();

    // 编译期检查路由表合法性，可选参数 warning(仅告警)/error(编译期抛异常)/delete(每次根据注解重新生成路由表)，不配置则不校验
    @Input
    Property<String> getCheckRouteMapValue();

    // 检查 FlowTask 是否有循环引用，可选参数 warning(仅打印日志)/error(编译期抛异常)，不配置则不校验
    @Input
    Property<String> getCheckFlowDependValue();
}
