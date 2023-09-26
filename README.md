TheRouter: *Android componentization solution*
---

[![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Language](https://img.shields.io/badge/Language-Kotlin-green)](https://kotlinlang.org/)
[![Wiki](https://img.shields.io/badge/Wiki-open-green)](https://github.com/HuolalaTech/hll-wp-therouter-android/wiki)  

HomePage | [iOS](https://github.com/HuolalaTech/hll-wp-therouter-ios) | [中文官网](https://therouter.cn)  

### A. Features

TheRouter core functions have four functionalities:  

* Page Navigation（[Navigator](https://github.com/HuolalaTech/hll-wp-therouter-android/wiki/Navigator)）
* Cross-module dependency injection（[ServiceProvider](https://github.com/HuolalaTech/hll-wp-therouter-android/wiki/ServiceProvider)） 
* Single module automatic initialization （[FlowTaskExecutor](https://github.com/HuolalaTech/hll-wp-therouter-android/wiki/FlowTaskExecutor)） 
* Enable client apps to remotely load method dynamically ([ActionManager](https://github.com/HuolalaTech/hll-wp-therouter-android/wiki/ActionManager))

* Demo:

<img src="https://github.com/HuolalaTech/hll-wp-therouter-android/wiki/uploads/image/demo.gif" width="50%" alt="demo gif" />

### B. Introduction

**For more detailed documentation, please check the project wiki**：[Wiki](https://github.com/HuolalaTech/hll-wp-therouter-android/wiki)  

#### B1. Gradle configuration

|module| apt                                                                                                      | router                                                                                                            | plugin                                                                                                            |
|---|----------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|
|version| [![apt](https://img.shields.io/badge/apt-1.2.0-green)](https://repo1.maven.org/maven2/cn/therouter/apt/) | [![router](https://img.shields.io/badge/router-1.2.0-green)](https://repo1.maven.org/maven2/cn/therouter/router/) | [![plugin](https://img.shields.io/badge/plugin-1.2.0-green)](https://repo1.maven.org/maven2/cn/therouter/plugin/) |

```
// root build.gradle 
classpath 'cn.therouter:plugin:1.2.0'

// app module 
apply plugin: 'therouter'

// dependencies
kapt "cn.therouter:apt:1.2.0"
implementation "cn.therouter:router:1.2.0"
```

#### B2. initialization library

The library contains the automatic initialization function inside，link to: [Single module automatic initialization](https://github.com/HuolalaTech/hll-wp-therouter-android/wiki/FlowTaskExecutor); therefore, there's no need for any initialization code. However, it is recommended that you set the `Debug` environment according to your business settings to view log information.  

```
@Override
protected void attachBaseContext(Context base) {
    TheRouter.setDebug(true or false);
    super.attachBaseContext(base);
}
```

#### B3. page parameter injection

Called in the `onCreate()` method of `Activity` or `Fragment`:   
(*It is recommended to do it directly in `BaseActivity(BaseFragment)`*)

```
@Override
protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    TheRouter.inject(this);
}
```

#### B4. page navigation

For the meaning of the annotation `@Route`, please check the documentation: [Page Navigation](https://github.com/HuolalaTech/hll-wp-therouter-android/wiki/Navigator)

```
@Route(path = "http://therouter.com/home", action = "action://scheme.com",
        description = "second page", params = {"hello", "world"})
public class HomeActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TheRouter.build("Path")
            .withInt("intValue", 12345678)
            .withString("str_123_Value", "传中文字符串")
            .withBoolean("boolValue", true)
            .withLong("longValue", 123456789012345L)
            .withChar("charValue", 'c')
            .withDouble("double", 3.14159265358972)
            .withFloat("floatValue", 3.14159265358972F)
            .navigation();
    }
}
```

### C. proguard rules configuration  

```
# need add for Fragment page route
# -keep public class * extends android.app.Fragment
# -keep public class * extends androidx.fragment.app.Fragment
# -keep public class * extends android.support.v4.app.Fragment

-keep class androidx.annotation.Keep
-keep @androidx.annotation.Keep class * {*;}
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <methods>;
}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <fields>;
}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <init>(...);
}
-keepclasseswithmembers class * {
    @com.therouter.router.Autowired <fields>;
}
```

### D. Build and Debug

#### D1. project module description  

```
TheRouter
  ├─app
  │   └──sample
  ├─business-a
  │   └──modular business demo
  ├─business-b
  │   └──modular business demo
  ├─business-base
  │   └──modular business demo
  │
  ├─apt
  │   └──Annotation processor tool code
  │
  ├─plugin
  │   └──Gradle plugin
  │
  └─router
      └──library code
```

#### D2. run Project 

1. Open `local.properties` and declare the modules you want to debug. For example, if you want the source code to debug the `apt` module, you can declare `apt=true`
2.  sync Gradle change

#### D3. plugin source code debugging

`plugin` debugging is special; you'll need to modify the `module` name to enable plugin debugging.  

1. Modify the `plugin` folder name to `buildSrc` (Case sensitive)
2. Remove `classpath` reference in root `build.gradle`  
3. sync Gradle change  


### E. Change Log  

link to Releases：[Github Releases](https://github.com/HuolalaTech/hll-wp-therouter-android/releases)

### F. Author 
<img src="https://github.com/HuolalaTech/hll-wp-therouter-android/wiki/uploads/image/hll.png" width="40%" alt="HUOLALA mobile technology team" />

### G. LICENSE

TheRouter is licensed under the Apache License 2.0: [LICENSE](https://github.com/HuolalaTech/hll-wp-therouter-android/blob/main/LICENSE).