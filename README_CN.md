Android 动态路由框架：TheRouter
---

[![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Language](https://img.shields.io/badge/Language-Kotlin-green)](https://kotlinlang.org/)
[![Wiki](https://img.shields.io/badge/Wiki-open-green)](https://therouter.cn/doc)

TheRouter Android | [iOS](https://github.com/HuolalaTech/hll-wp-therouter-ios)

### 一、功能介绍

TheRouter 具备四大能力：  

* 页面导航跳转能力（[Navigator](https://therouter.cn/docs/2022/08/28/01)） 页面跳转能力介绍 
* 跨模块依赖注入能力（[ServiceProvider](https://therouter.cn/docs/2022/08/27/01)）跨模块依赖注入 
* 单模块初始化(业务节点订阅)能力 （[FlowTaskExecutor](https://therouter.cn/docs/2022/08/26/01)）单模块自动初始化能力介绍 
* 动态化能力 ([ActionManager](https://therouter.cn/docs/2022/08/25/01)) 动态化能力支持 

* Demo:

<img src="https://github.com/HuolalaTech/hll-wp-therouter-android/wiki/uploads/image/demo.gif" width="50%" alt="demo gif" />

### 二、使用介绍

**更多详细使用文档请查看项目官网 [therouter.cn](https://therouter.cn/doc)**

#### 2.1 Gradle 引入

|module| apt                                                                                                     | router                                                                                                           | plugin                                                                                                           |
|---|---------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
|version| [![apt](https://img.shields.io/badge/apt-1.2.1-green)](https://repo1.maven.org/maven2/cn/therouter/apt) | [![router](https://img.shields.io/badge/router-1.2.1-green)](https://repo1.maven.org/maven2/cn/therouter/router) | [![plugin](https://img.shields.io/badge/plugin-1.2.1-green)](https://repo1.maven.org/maven2/cn/therouter/plugin) |

```
// 项目根目录 build.gradle 引入
classpath 'cn.therouter:plugin:1.2.1'

// app module 中引入
apply plugin: 'therouter'

// 依赖，所有使用了注解的模块都要添加
kapt "cn.therouter:apt:1.2.1"
implementation "cn.therouter:router:1.2.1"
```

#### 2.2 初始化

框架内部包含自动初始化功能，详见[单模块自动初始化能力](https://therouter.cn/docs/2022/08/26/01)
无需任何初始化代码。但推荐你根据业务设置否为`Debug`环境，用以查看日志信息。
`Application.attachBaseContext()` 方法中尽可能早设置当前是否为`Debug`环境。

```
@Override
protected void attachBaseContext(Context base) {
    TheRouter.setDebug(true or false);
    super.attachBaseContext(base);
}
```

#### 2.3 页面参数注入

在`Activity` 或 `Fragment` 的 `onCreate()`方法中调用，建议直接在`BaseActivity(BaseFragment)`中做

```
@Override
protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    TheRouter.inject(this);
}
```

#### 2.4 页面跳转

关于注解`@Route`的参数含义，请查看文档：[页面导航跳转能力](https://therouter.cn/docs/2022/08/28/01)

```
@Route(path = "http://therouter.com/home", action = "action://scheme.com",
        description = "第二个页面", params = {"hello", "world"})
public class HomeActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TheRouter.build("要跳转的目标页Path")
            .withInt("intValue", 12345678) // 传 int 值
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

#### 2.5 混淆配置  

```
# 如果使用了 Fragment 路由，需要保证类名不被混淆
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


### 三、从其他路由迁移至 TheRouter

#### 3.1 迁移工具一键迁移

可使用迁移工具一键迁移（GitHub下载比较慢）：  
迁移工具使用说明请见官网文档：[https://therouter.cn/docs/2022/09/05/01](https://therouter.cn/docs/2022/09/05/01)  

* Mac OS 迁移工具：[uploads/file/TheRouterTransfer-Mac.zip](https://github.com/HuolalaTech/hll-wp-therouter-android/wiki/uploads/file/TheRouterTransfer-Mac.zip)
* Windows 迁移工具：[uploads/file/TheRouterTransfer-Windows.zip](https://github.com/HuolalaTech/hll-wp-therouter-android/wiki/uploads/file/TheRouterTransfer-Windows.zip)

如果项目中使用了`ARouter`的`IProvider.init()`方法，可能需要手动处理初始化逻辑。

如下图：  

<img src="https://github.com/HuolalaTech/hll-wp-therouter-android/wiki/uploads/image/TheRouterTransfer.png" width="40%" />

#### 3.2 其他路由框架

如有需要，请在本项目`issue`中提出，我们评估后会尽快支持，也欢迎任何人提供迁移脚本的 `Pull Requests`   

#### 3.3 与其他路由对比

|功能|TheRouter|ARouter|WMRouter|
|---|---|---|---|
|Fragment路由|✔️|✔️|✔️|
|支持依赖注入|✔️|✔️|✔️|
|加载路由表|无运行时扫描<br>无反射|运行时扫描dex<br>反射实例类<br>性能损耗大|运行时读文件<br>反射实例类<br>性能损耗中|
|注解正则表达式|✔️|✖️|✔️|
|Activity指定拦截器|✔️（四大拦截器可根据业务定制）|✖️|✔️|
|导出路由文档|✔️（路由文档支持添加注释描述）|✔️|✖️|
|动态注册路由信息|✔️|✔️|✖️|
|APT支持增量编译|✔️|✔️（开启文档生成则无法增量编译）|✖️|
|plugin支持增量编译|✔️|✖️|✖️|
|多 Path 对应同一页面（低成本实现双端path统一）|✔️|✖️|✖️|
|远端路由表下发|✔️|✖️|✖️|
|支持单模块独立初始化|✔️|✖️|✖️|
|支持使用路由打开第三方库页面|✔️|✖️|✖️|

### 四、源码运行与调试

#### 4.1 工程模块描述  

```
TheRouter
  ├─app
  │   └──代码使用示例Demo
  ├─business-a
  │   └──用于模块化业务模块的演示模块
  ├─business-b
  │   └──用于模块化业务模块的演示模块
  ├─business-base
  │   └──用于模块化基础模块的演示模块
  │
  ├─apt
  │   └──注解处理器相关代码
  │
  ├─plugin
  │   └──编译期 Gradle 插件源码
  │
  └─router
      └──路由库核心代码
```

#### 4.2 项目运行

1. 打开`local.properties`，声明你想要调试的模块
	例如希望源码调试`apt`模块，则声明`apt=true`即可
2.  同步 Gradle 变更

#### 4.3 plugin 源码调试

`plugin`调试比较特殊，需要修改`module`名。

1. 修改`plugin`文件夹名为`buildSrc`（注意大小写）
2. 注释根目录`build.gradle`中的`classpath`引用（不需要了）
3. 同步 Gradle 变更


### 五、Change Log  

详见 releases 记录：[CHANGELOG](https://github.com/HuolalaTech/hll-wp-therouter-android/releases)

### 六、Author

<img src="https://github.com/HuolalaTech/hll-wp-therouter-android/wiki/uploads/image/hll.png" width="40%" alt="HUOLALA mobile technology team" />

加入 【TheRouter】 官方微信群：  
*如过期，请加微信：kymjs123，拉你进群*   

<img src="https://therouter.cn/assets/img/therouter_wx_group.png" width="40%" alt="TheRouter官方微信群：https://kymjs.com/therouter/wx" />

### 七、开源协议

TheRouter is licensed under the Apache License 2.0: [LICENSE](https://github.com/HuolalaTech/hll-wp-therouter-android/blob/master/LICENSE).  