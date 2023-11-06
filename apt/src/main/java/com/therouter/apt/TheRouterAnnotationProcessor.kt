package com.therouter.apt

import com.google.gson.Gson
import com.therouter.app.flowtask.lifecycle.FlowTask
import com.therouter.inject.NewInstance
import com.therouter.inject.ServiceProvider
import com.therouter.inject.Singleton
import com.therouter.router.Autowired
import com.therouter.router.Route
import com.therouter.router.Routes
import com.therouter.router.action.ActionInterceptor
import java.io.File
import java.io.FileInputStream
import java.io.PrintStream
import java.util.*
import java.util.regex.Pattern
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.element.ElementKind.*
import kotlin.math.abs

/**
 * Created by ZhangTao on 17/8/11.
 */

const val POINT = "."
const val KEY_USE_EXTEND = "USE_EXTENSION"
const val PROPERTY_FILE = "gradle.properties"
const val STR_TRUE = "true"
const val KEY_PARAMS = "params"
const val KEY_RETURNTYPE = "returnType"
const val KEY_CLASS = "clazz="
const val CLASS = "class"
const val PACKAGE = "a"
const val PREFIX_SERVICE_PROVIDER = "ServiceProvider__TheRouter__"
const val PREFIX_ROUTER_MAP = "RouterMap__TheRouter__"
const val SUFFIX_AUTOWIRED = "__TheRouter__Autowired"
val gson = Gson()

class TheRouterAnnotationProcessor : AbstractProcessor() {
    private var isProcess = false
    override fun getSupportedAnnotationTypes(): Set<String> {
        val supportTypes: MutableSet<String> = HashSet()
        supportTypes.add(ServiceProvider::class.java.canonicalName)
        supportTypes.add(Singleton::class.java.canonicalName)
        supportTypes.add(NewInstance::class.java.canonicalName)
        supportTypes.add(Autowired::class.java.canonicalName)
        supportTypes.add(Routes::class.java.canonicalName)
        supportTypes.add(Route::class.java.canonicalName)
        supportTypes.add(FlowTask::class.java.canonicalName)
        supportTypes.add(ActionInterceptor::class.java.canonicalName)
        return supportTypes
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun process(set: Set<TypeElement?>, roundEnvironment: RoundEnvironment): Boolean {
        if (!isProcess) {
            isProcess = true
            checkSingleton(roundEnvironment)
            genRouterMapFile(parseRoute(roundEnvironment))
            val providerItemList = parseServiceProvider(roundEnvironment)
            val autowiredItems = parseAutowired(roundEnvironment)
            genAutowiredJavaFile(autowiredItems)
            val actionInterceptorList = parseActionInterceptor(roundEnvironment)
            val flowTaskList = parseFlowTask(roundEnvironment)
            genJavaFile(providerItemList, flowTaskList, actionInterceptorList)
        }
        return isProcess
    }

    private fun parseActionInterceptor(roundEnv: RoundEnvironment): MutableList<ActionInterceptorItem> {
        val list: MutableList<ActionInterceptorItem> = ArrayList()
        val set = roundEnv.getElementsAnnotatedWith(ActionInterceptor::class.java)
        for (element in set) {
            val annotation = element.getAnnotation(ActionInterceptor::class.java)
            val actionInterceptorItem = ActionInterceptorItem()
            actionInterceptorItem.actionName = annotation.actionName
            actionInterceptorItem.className = element.toString()
            list.add(actionInterceptorItem)
        }
        return list
    }

    private fun parseFlowTask(roundEnv: RoundEnvironment): MutableList<FlowTaskItem> {
        val list: MutableList<FlowTaskItem> = ArrayList()
        val set = roundEnv.getElementsAnnotatedWith(FlowTask::class.java)
        for (element in set) {
            require(element.kind == METHOD) { element.simpleName.toString() + " is not method" }
            var isStatic = false
            for (m in element.modifiers) {
                if (m == Modifier.STATIC) {
                    isStatic = true
                    break
                }
            }
            require(isStatic) {
                ("The modifiers of the" + element.enclosingElement.toString()
                        + "." + element.simpleName + "() must have static!")
            }
            val annotation = element.getAnnotation(FlowTask::class.java)
            val flowTaskItem = FlowTaskItem()
            flowTaskItem.taskName = annotation.taskName
            flowTaskItem.async = annotation.async
            flowTaskItem.dependencies = annotation.dependsOn
            flowTaskItem.methodName = element.simpleName.toString()
            flowTaskItem.className = element.enclosingElement.toString()
            if (element is ExecutableElement) {
                val returnType = element.returnType.toString()
                require(returnType.equals("void", ignoreCase = true)) {
                    ("The return type of the" + element.getEnclosingElement().toString()
                            + "." + element.getSimpleName() + "() must be void")
                }
                val parameters = element.parameters
                if (parameters != null && parameters.size == 1) {
                    val type = transformNumber(parameters[0].asType().toString())
                    require(type == "android.content.Context") {
                        ("=========================\n\n\n\n" + element.getEnclosingElement().toString()
                                + "." + element.getSimpleName() + "(" + type + ") must only has Context parameter")
                    }
                } else {
                    throw IllegalArgumentException(
                        "=========================\n\n\n\n" + element.getEnclosingElement().toString()
                                + "." + element.getSimpleName() + "() must only has Context parameter"
                    )
                }
            }
            list.add(flowTaskItem)
        }
        return list
    }

    private fun parseAutowired(roundEnv: RoundEnvironment): Map<String, MutableList<AutowiredItem>> {
        val map: MutableMap<String, MutableList<AutowiredItem>> = HashMap()
        val set = roundEnv.getElementsAnnotatedWith(Autowired::class.java)
        for (element in set) {
            require(element.kind == FIELD) { element.simpleName.toString() + " is not field" }
            val annotation = element.getAnnotation(Autowired::class.java)
            val autowiredItem = AutowiredItem()
            autowiredItem.key = annotation.name.trim { it <= ' ' }
            if (autowiredItem.key == "") {
                autowiredItem.key = element.toString()
            }
            autowiredItem.args = annotation.args
            autowiredItem.fieldName = element.toString()
            autowiredItem.required = annotation.required
            autowiredItem.id = annotation.id
            autowiredItem.description = annotation.description
            autowiredItem.type = element.asType().toString()
            autowiredItem.className = element.enclosingElement.toString()
            var list = map[autowiredItem.className]
            if (list == null) {
                list = ArrayList()
            }
            list.add(autowiredItem)
            list.sort()
            map[autowiredItem.className] = list
        }
        return map
    }

    private fun parseRoute(roundEnv: RoundEnvironment): List<RouteItem> {
        val list: MutableList<RouteItem> = ArrayList()
        val set = roundEnv.getElementsAnnotatedWith(Route::class.java)
        val arraySet = roundEnv.getElementsAnnotatedWith(Routes::class.java)
        if (arraySet != null && arraySet.isNotEmpty()) {
            for (element in arraySet) {
                require(element.kind == ElementKind.CLASS) { element.simpleName.toString() + " is not class" }
                val annotation = element.getAnnotation(Routes::class.java)
                annotation.value.forEach {
                    val routeItem = RouteItem()
                    routeItem.className = element.toString()
                    routeItem.path = it.path
                    routeItem.action = it.action
                    routeItem.description = it.description
                    require(it.params.size % 2 == 0) { "$element params is not key value pairs" }
                    var key: String? = null
                    for (kv in it.params) {
                        if (key == null) {
                            key = kv
                        } else {
                            routeItem.params[key!!] = kv
                            key = null
                        }
                    }
                    list.add(routeItem)
                }
            }
        }
        if (set != null && set.isNotEmpty()) {
            for (element in set) {
                require(element.kind == ElementKind.CLASS) { element.simpleName.toString() + " is not class" }
                val annotation = element.getAnnotation(Route::class.java)
                val routeItem = RouteItem()
                routeItem.className = element.toString()
                routeItem.path = annotation.path
                routeItem.action = annotation.action
                routeItem.description = annotation.description
                require(annotation.params.size % 2 == 0) { "$element params is not key value pairs" }
                var key: String? = null
                for (kv in annotation.params) {
                    if (key == null) {
                        key = kv
                    } else {
                        routeItem.params[key!!] = kv
                        key = null
                    }
                }
                list.add(routeItem)
            }
        }
        return list
    }

    private fun checkSingleton(roundEnv: RoundEnvironment) {
        val set1 = roundEnv.getElementsAnnotatedWith(
            Singleton::class.java
        )
        for (element in set1) {
            require(!(element.kind != ElementKind.CLASS && element.kind != INTERFACE)) { element.simpleName.toString() + " is not class or interface" }
        }
        val set2 = roundEnv.getElementsAnnotatedWith(
            NewInstance::class.java
        )
        for (element in set2) {
            require(!(element.kind != ElementKind.CLASS && element.kind != INTERFACE)) { element.simpleName.toString() + " is not class or interface" }
            require(!set1.contains(element)) { "Error in class " + element.simpleName + ", @Singleton and @NewInstance are mutually exclusive" }
        }
    }

    private fun parseServiceProvider(roundEnv: RoundEnvironment): ArrayList<ServiceProviderItem> {
        val list: ArrayList<ServiceProviderItem> = ArrayList()
        val set = roundEnv.getElementsAnnotatedWith(ServiceProvider::class.java)
        for (element in set) {
            val serviceProviderItem = if (element.kind == METHOD) {
                handleMethodServiceProviderItem(element)
            } else {
                handleClassServiceProviderItem(element)
            }
            list.add(checkServiceProviderItemInvalidData(serviceProviderItem))
        }
        return list
    }

    private fun handleClassServiceProviderItem(element: Element): ServiceProviderItem {
        val serviceProviderItem = ServiceProviderItem(false)
        serviceProviderItem.element = element
        val annotation = element.getAnnotation(ServiceProvider::class.java)
        serviceProviderItem.className = element.toString()
        serviceProviderItem.methodName = ""

        val annotationStr = annotation.toString()
        val matcher = Pattern.compile("(returnType|params)=([\\w\\.]+[\\w\\.,]*)").matcher(annotationStr)
        while (matcher.find()) {
            val key = matcher.group(1)
            val value = matcher.group(2)
            when (key) {
                KEY_RETURNTYPE -> if (!value.startsWith("com.therouter.inject.ServiceProvider")) {
                    serviceProviderItem.returnType = value
                }

                KEY_PARAMS -> serviceProviderItem.params = transform(value.split(",").toCollection(ArrayList()))

                else -> {}
            }
        }

        if (serviceProviderItem.returnType.isEmpty()) {
            if (element is TypeElement) {
                if (element.interfaces.size == 0) {
                    serviceProviderItem.returnType = serviceProviderItem.className
                } else if (element.interfaces.size == 1) {
                    serviceProviderItem.returnType = element.interfaces[0].toString()
                } else {
                    val prop = Properties()
                    try {
                        val gradleProperties = FileInputStream(PROPERTY_FILE)
                        prop.load(gradleProperties)
                    } catch (e: Exception) {
                    }
                    if (!STR_TRUE.equals(prop.getProperty(KEY_USE_EXTEND), ignoreCase = true)) {
                        throw IllegalArgumentException(
                            element.enclosingElement.toString() + "." + element.simpleName +
                                    " has multiple interfaces. Must to be specified returnType=XXX," +
                                    " or configuration KEY_USE_EXTEND=true in gradle.properties"
                        )
                    } else {
                        serviceProviderItem.returnType = serviceProviderItem.className
                    }
                }
            }
        }
        return serviceProviderItem
    }

    private fun handleMethodServiceProviderItem(element: Element): ServiceProviderItem {
        var isStatic = false
        for (m in element.modifiers) {
            if (m == Modifier.STATIC) {
                isStatic = true
                break
            }
        }
        require(isStatic) {
            (element.enclosingElement.toString()
                    + "." + element.simpleName + "() is not static method")
        }
        val serviceProviderItem = ServiceProviderItem(true)
        serviceProviderItem.element = element
        serviceProviderItem.methodName = element.simpleName.toString()
        serviceProviderItem.className = element.enclosingElement.toString()
        if (element is ExecutableElement) {
            serviceProviderItem.returnType = element.returnType.toString()
            val parameters = element.parameters
            if (parameters.size > 0) {
                val params: ArrayList<String> = ArrayList<String>()
                repeat(parameters.size) { i ->
                    params.add(transformNumber(parameters[i].asType().toString()))
                }
                serviceProviderItem.params = params
            }
        }
        val annotationStr = element.getAnnotation(ServiceProvider::class.java).toString()
        val matcher = Pattern.compile("(returnType|params)=([\\w\\.]+[\\w\\.,]*)").matcher(annotationStr)
        while (matcher.find()) {
            val key = matcher.group(1)
            val value = matcher.group(2)
            when (key) {
                KEY_RETURNTYPE -> if (!value.startsWith("com.therouter.inject.ServiceProvider")) {
                    serviceProviderItem.returnType = value
                }

                KEY_PARAMS -> serviceProviderItem.params = transform(value.split(",").toCollection(ArrayList()))

                else -> {}
            }
        }
        return serviceProviderItem
    }

    private fun checkServiceProviderItemInvalidData(item: ServiceProviderItem): ServiceProviderItem {
        if (item.returnType.endsWith(".class")) {
            item.returnType = item.returnType.replace(".class", "")
        }

        if (item.params.size == 1) {
            if (item.params[0] == "{}") {
                item.params = ArrayList<String>()
            }
        } else if (item.params.size > 1) {
            ArrayList(item.params).forEach {
                if (it == "{}") {
                    item.params.remove(it)
                }
            }
        }

        return item
    }

    private fun genRouterMapFile(pageList: List<RouteItem>) {
        if (pageList.isEmpty()) {
            return
        }
        val path = processingEnv.filer.createSourceFile(PACKAGE + POINT + PREFIX_ROUTER_MAP + "temp").toUri().toString()
        // As long as the compiled software and hardware environment remains unchanged, the class name will not change
        val className = PREFIX_ROUTER_MAP + abs(path.hashCode()).toString()
        val routePagelist = duplicateRemove(pageList)
        val json = gson.toJson(routePagelist)
        var ps: PrintStream? = null
        try {
            val jfo = processingEnv.filer.createSourceFile(PACKAGE + POINT + className)
            val genJavaFile = File(jfo.toUri().toString())
            if (genJavaFile.exists()) {
                genJavaFile.delete()
            }

            ps = PrintStream(jfo.openOutputStream(), false, "UTF-8")
            ps.println(String.format("package %s;", PACKAGE))
            ps.println()
            ps.println("/**")
            ps.println(" * Generated code, Don't modify!!!")
            ps.println(" * Created by kymjs, and APT Version is ${BuildConfig.VERSION}.")
            ps.println(" * JDK Version is ${System.getProperty("java.version")}.")
            ps.println(" */")
            ps.println("@androidx.annotation.Keep")
            ps.println(
                String.format(
                    "public class %s implements com.therouter.router.IRouterMapAPT {",
                    className
                )
            )
            ps.println()
            ps.println("\tpublic static final String TAG = \"Created by kymjs, and APT Version is ${BuildConfig.VERSION}.\";")
            ps.println("\tpublic static final String THEROUTER_APT_VERSION = \"${BuildConfig.VERSION}\";")
            ps.println(String.format("\tpublic static final String ROUTERMAP = \"%s\";", json.replace("\"", "\\\"")))
            ps.println()

            ps.println("\tpublic static void addRoute() {")
            var i = 0
            for (item in routePagelist) {
                i++
                ps.println("\t\tcom.therouter.router.RouteItem item$i = new com.therouter.router.RouteItem(\"${item.path}\",\"${item.className}\",\"${item.action}\",\"${item.description}\");")
                item.params.keys.forEach {
                    ps.println("\t\titem$i.addParams(\"$it\", \"${item.params[it]}\");")
                }
                ps.println("\t\tcom.therouter.router.RouteMapKt.addRouteItem(item$i);")
            }
            ps.println("\t}")

            ps.println("}")
            ps.flush()
        } finally {
            ps?.close()
        }
    }

    private fun genAutowiredJavaFile(pageMap: Map<String, MutableList<AutowiredItem>>) {
        val keyList = ArrayList(pageMap.keys)
        // 做一次排序，确保只要map成员没有变化，输出文件内容的顺序就没有变化
        keyList.sort()
        for (key in keyList) {
            val fullClassName = key + SUFFIX_AUTOWIRED
            val simpleClassName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1)
            val packageName = fullClassName.substring(0, fullClassName.lastIndexOf('.'))
            var ps: PrintStream? = null
            try {
                val jfo = processingEnv.filer.createSourceFile(fullClassName)
                ps = PrintStream(jfo.openOutputStream(), false, "UTF-8")
                ps.println(String.format("package %s;", packageName))
                ps.println()
                ps.println("/**")
                ps.println(" * Generated code, Don't modify!!!")
                ps.println(" * Created by kymjs, and APT Version is ${BuildConfig.VERSION}.")
                ps.println(" * JDK Version is ${System.getProperty("java.version")}.")
                ps.println(" */")
                ps.println("@androidx.annotation.Keep")
                ps.println(String.format("public class %s {", simpleClassName))
                ps.println()
                ps.println("\tpublic static final String TAG = \"Created by kymjs, and APT Version is ${BuildConfig.VERSION}.\";")
                ps.println("\tpublic static final String THEROUTER_APT_VERSION = \"${BuildConfig.VERSION}\";")
                ps.println()
                ps.println("\tpublic static void autowiredInject(Object obj) {")
                ps.println()
                ps.println("\t\tif (obj instanceof $key) {")
                ps.println()
                ps.println("\t\t$key target = ($key) obj;")
                ps.println("\t\tfor (com.therouter.router.interceptor.AutowiredParser parser : com.therouter.TheRouter.getParserList()) {")
                for ((i, item) in pageMap[key]!!.withIndex()) {
                    val variableName = "variableName$i"
                    ps.println("\t\t\ttry {")
                    ps.println(
                        String.format(
                            "\t\t\t\t%s %s = parser.parse(\"%s\", target, new com.therouter.router.AutowiredItem(\"%s\",\"%s\",%s,\"%s\",\"%s\",\"%s\",%s,\"%s\"));",
                            transformNumber(item.type), variableName,
                            item.type,
                            item.type,
                            item.key,
                            item.id,
                            item.args,
                            item.className,
                            item.fieldName,
                            item.required,
                            item.description
                        )
                    )
                    ps.println("\t\t\t\tif ($variableName != null){")
                    ps.println("\t\t\t\t\t// ${item.description}")
                    ps.println(String.format("\t\t\t\t\ttarget.%s = $variableName;", item.fieldName))
                    ps.println("\t\t\t\t}")
                    ps.println("\t\t\t} catch (Exception e) {")
                    ps.println("\t\t\t\tif (com.therouter.TheRouter.isDebug()) { e.printStackTrace(); }")
                    ps.println("\t\t\t}")
                }
                ps.println("\t\t}")
                ps.println()
                ps.println("\t\t}")
                ps.println("\t}")
                ps.println("}")
                ps.flush()
            } finally {
                ps?.close()
            }
        }
    }

    private fun genJavaFile(
        pageList: ArrayList<ServiceProviderItem>,
        flowTaskList: MutableList<FlowTaskItem>,
        actionInterceptorList: MutableList<ActionInterceptorItem>
    ) {
        if (pageList.isEmpty() && flowTaskList.isEmpty() && actionInterceptorList.isEmpty()) {
            return
        }
        val stringBuilder = StringBuilder()
        stringBuilder.append("{")
        var isFirst = true
        flowTaskList.sort()
        pageList.sort()
        actionInterceptorList.sort()
        flowTaskList.forEach {
            if (!isFirst) {
                stringBuilder.append(",")
            }
            stringBuilder.append("\\\"").append(it.taskName).append("\\\":\\\"").append(it.dependencies).append("\\\"")
            isFirst = false
        }
        stringBuilder.append("}")
        val path =
            processingEnv.filer.createSourceFile(PACKAGE + POINT + PREFIX_SERVICE_PROVIDER + "temp").toUri().toString()
        val className = PREFIX_SERVICE_PROVIDER + abs(path.hashCode()).toString()
        var ps: PrintStream? = null
        try {
            val jfo = processingEnv.filer.createSourceFile(PACKAGE + POINT + className)
            val genJavaFile = File(jfo.toUri().toString())
            if (genJavaFile.exists()) {
                genJavaFile.delete()
            }
            ps = PrintStream(jfo.openOutputStream(), false, "UTF-8")
            ps.println(String.format("package %s;", PACKAGE))
            ps.println()
            ps.println("/**")
            ps.println(" * Generated code, Don't modify!!!")
            ps.println(" * Created by kymjs, and APT Version is ${BuildConfig.VERSION}.")
            ps.println(" * JDK Version is ${System.getProperty("java.version")}.")
            ps.println(" */")
            ps.println("@androidx.annotation.Keep")
            ps.println(String.format("public class %s implements com.therouter.inject.Interceptor {", className))
            ps.println()
            ps.println("\tpublic static final String TAG = \"Created by kymjs, and APT Version is ${BuildConfig.VERSION}.\";")
            ps.println("\tpublic static final String THEROUTER_APT_VERSION = \"${BuildConfig.VERSION}\";")
            ps.println("\tpublic static final String FLOW_TASK_JSON = \"${stringBuilder.toString()}\";")
            ps.println()
            ps.println("\tpublic <T> T interception(Class<T> clazz, Object... params) {")
            ps.println("\t\tT obj = null;")
            ps.print("\t\t")
            val prop = Properties()
            try {
                val gradleProperties = FileInputStream(PROPERTY_FILE)
                prop.load(gradleProperties)
            } catch (e: Exception) {
            }
            for (serviceProviderItem in pageList) {
                //处理 USE_EXTEND 开关
                if (STR_TRUE.equals(prop.getProperty(KEY_USE_EXTEND), ignoreCase = true)) {
                    ps.print(String.format("if (%s.class.isAssignableFrom(clazz)", serviceProviderItem.returnType))
                } else {
                    ps.print(String.format("if (%s.class.equals(clazz)", serviceProviderItem.returnType))
                }
                // 多参数判断
                ps.print(" && params.length == ")
                if (serviceProviderItem.params.size == 1) {
                    if (serviceProviderItem.params[0].trim { it <= ' ' }.isEmpty()) {
                        ps.print(0)
                    } else {
                        ps.print(1)
                    }
                } else {
                    ps.print(serviceProviderItem.params.size)
                }
                //参数类型判断
                for (count in serviceProviderItem.params.indices) {
                    if (!serviceProviderItem.params[count].trim { it <= ' ' }.isEmpty()) {
                        ps.print(
                            String.format(
                                Locale.getDefault(),
                                "\n\t\t\t\t&& params[%d] instanceof %s",
                                count,
                                serviceProviderItem.params[count]
                            )
                        )
                    }
                }
                ps.println(") {")
                ps.println("\t\t\t//type verification during compilation prevents the actual return type of the method from mismatching with the return type declared by the annotation")

                if (serviceProviderItem.isMethod) {
                    ps.print(
                        String.format(
                            "\t\t\t%s returnType = %s.%s(",
                            serviceProviderItem.returnType,
                            serviceProviderItem.className,
                            serviceProviderItem.methodName
                        )
                    )
                } else {
                    ps.print(
                        String.format(
                            "\t\t\t%s returnType = new %s(",
                            serviceProviderItem.returnType,
                            serviceProviderItem.className
                        )
                    )
                }

                for (count in serviceProviderItem.params.indices) {
                    if (!serviceProviderItem.params[count].trim { it <= ' ' }.isEmpty()) {
                        //参数强转
                        ps.print(
                            String.format(
                                Locale.getDefault(),
                                "(%s) params[%d]",
                                serviceProviderItem.params[count],
                                count
                            )
                        )
                        if (count != serviceProviderItem.params.size - 1) {
                            ps.print(", ")
                        }
                    }
                }
                ps.println(");")
                ps.println("\t\t\tobj = (T) returnType;")
                ps.print("\t\t} else ")
            }
            ps.println("{\n")
            ps.println("        }")
            ps.println("        return obj;")
            ps.println("    }")
            ps.println()
            ps.println("\tpublic static void addFlowTask(android.content.Context context, com.therouter.flow.Digraph digraph) {")
            for (item in flowTaskList) {
                ps.println("\t\tdigraph.addTask(new com.therouter.flow.Task(${item.async}, \"${item.taskName}\", \"${item.dependencies}\", new com.therouter.flow.FlowTaskRunnable() {")
                ps.println("\t\t\t@Override")
                ps.println("\t\t\tpublic void run() {")
                ps.println("\t\t\t\t${item.className}.${item.methodName}(context);")
                ps.println("\t\t\t}")
                ps.println()
                ps.println("\t\t\t@Override")
                ps.println("\t\t\tpublic String log() {")
                ps.println("\t\t\t\treturn \"${item.className}.${item.methodName}(context);\";")
                ps.println("\t\t\t}")
                ps.println("\t\t}));")
            }
            for (item in actionInterceptorList) {
                ps.println("\t\tcom.therouter.TheRouter.addActionInterceptor(\"${item.actionName}\", new ${item.className}());")
            }
            ps.println("\t}")
            ps.println("}")
            ps.flush()
        } finally {
            ps?.close()
        }
    }

    private fun transform(type: ArrayList<String>): ArrayList<String> {
        val list = ArrayList<String>()
        for (i in type.indices) {
            val item = transformNumber(type[i])
            if (item.isNotBlank()) {
                list.add(item)
            }
        }
        return list
    }
}

fun transformNumber(type: String): String {
    return when (type) {
        "byte" -> "java.lang.Byte"
        "short" -> "java.lang.Short"
        "int" -> "java.lang.Integer"
        "long" -> "java.lang.Long"
        "float" -> "java.lang.Float"
        "double" -> "java.lang.Double"
        "boolean" -> "java.lang.Boolean"
        "char" -> "java.lang.Character"
        else -> type
    }
}

fun duplicateRemove(pageList: List<RouteItem>) =
    ArrayList(HashSet(pageList)).apply { sort() }
