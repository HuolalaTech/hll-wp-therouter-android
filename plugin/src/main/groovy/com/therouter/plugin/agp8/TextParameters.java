package com.therouter.plugin.agp8;

import com.android.build.api.instrumentation.InstrumentationParameters;

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;

import java.io.File;

public interface TextParameters extends InstrumentationParameters {
    @Input
    Property<File> getBuildCacheFile();
    @Input
    Property<String> getBuildDataText();
    @Input
    Property<Boolean> getDebugValue();
}
