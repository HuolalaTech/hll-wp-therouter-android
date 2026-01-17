package com.therouter.ksp

import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.FunctionKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.therouter.app.flowtask.lifecycle.FlowTask
import com.therouter.apt.ActionInterceptorItem
import com.therouter.apt.AutowiredItem
import com.therouter.apt.BuildConfig
import com.therouter.apt.ComposeItem
import com.therouter.apt.ComposeParameter
import com.therouter.apt.FlowTaskItem
import com.therouter.apt.KEY_USE_EXTEND
import com.therouter.apt.PACKAGE
import com.therouter.apt.PREFIX_ROUTER_MAP
import com.therouter.apt.PREFIX_SERVICE_PROVIDER
import com.therouter.apt.PROPERTY_FILE
import com.therouter.apt.RouteItem
import com.therouter.apt.STR_TRUE
import com.therouter.apt.SUFFIX_AUTOWIRED
import com.therouter.apt.ServiceProviderItem
import com.therouter.apt.duplicateRemove
import com.therouter.apt.gson
import com.therouter.brick.DataProviderItem
import com.therouter.inject.ServiceProvider
import com.therouter.router.Autowired
import com.therouter.router.Route
import com.therouter.router.action.ActionInterceptor
import java.io.FileInputStream
import java.io.PrintStream
import java.util.HashSet
import java.util.Locale
import java.util.Properties
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.text.StringBuilder

/**
 * Created by ZhangTao on 17/8/11.
 */
class TheRouterSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    private var sourcePath = ""

    private val routeDependencies = mutableSetOf<KSFile>()
    private val autoWiredDependencies = mutableSetOf<KSFile>()
    private val serviceProviderDependencies = mutableSetOf<KSFile>()
    private val brickDependencies = mutableSetOf<KSFile>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        routeDependencies.clear()
        autoWiredDependencies.clear()
        serviceProviderDependencies.clear()
        brickDependencies.clear()

        val brickList = parseBrick(resolver)
        parseRoute(resolver, brickList)
        parseAutowired(resolver)
        val providerItemList = parseServiceProvider(resolver)
        val flowTaskList = parseFlowTask(resolver)
        val actionInterceptorList = parseActionInterceptor(resolver)
        genServiceProviderFile(providerItemList, flowTaskList, actionInterceptorList)
        return emptyList()
    }

    private fun parseBrick(resolver: Resolver): ArrayList<DataProviderItem> {
        val list = ArrayList<DataProviderItem>()
        resolver.getSymbolsWithAnnotation(com.therouter.brick.annotation.DataProvider::class.java.name).forEach {
            it.containingFile?.let { file ->
                brickDependencies.add(file)
            }
            it.accept(ModelVisitor(list), Unit)
        }
        return list
    }

    inner class ModelVisitor(private val list: ArrayList<DataProviderItem>) : TheRouterVisitor(logger) {

        override fun visitFunctionDeclaration(f: KSFunctionDeclaration, data: Unit) {
            super.visitFunctionDeclaration(f, data)
            sourcePath = getSourcePath(f)
            if (f.functionKind != FunctionKind.STATIC && f.functionKind != FunctionKind.TOP_LEVEL) {
                logger.error(
                    "\n=========================\n"
                            + "The modifiers of the " + f.qualifiedName?.asString() + "() must be TOP_LEVEL Method!"
                            + "\n=========================\n\n\n\n"
                )
            }
            if (f.parameters.size != 1) {
                logger.error(
                    "\n=========================\n"
                            + f.qualifiedName?.asString() + "() must only has Navigator parameter"
                            + "\n=========================\n\n\n\n"
                )
            }
            f.parameters.forEach {
                if (it.type.resolve().declaration.qualifiedName?.asString() != "com.therouter.router.Navigator") {
                    logger.error(
                        "\n=========================\n"
                                + f.qualifiedName?.asString() + "(" + it.type.resolve().declaration.qualifiedName?.asString()
                                + ") must only has Navigator parameter"
                                + "\n=========================\n\n\n\n"
                    )
                }
            }
            f.annotations.forEach { annotation ->
                val item = DataProviderItem()
                item.methodName = f.simpleName.asString()
                item.className = f.parentDeclaration?.qualifiedName?.asString() ?: f.packageName.asString()
                item.returnType = f.returnType?.resolve()?.declaration?.qualifiedName?.asString() ?: ""
                item.returnTypeWithParams = getParameterType(f.returnType?.resolve())
                annotation.arguments.forEach { arg ->
                    when (arg.name?.asString()) {
                        "path" -> {
                            item.path = arg.value.toString()
                        }

                        "priority" -> {
                            if (arg.value != null) {
                                item.priority = arg.value as Int
                            }
                        }

                        "fieldName" -> {
                            if (arg.value != null) {
                                item.fieldName = arg.value as String
                            } else {
                                item.fieldName = item.className.lowercase()
                            }
                        }
                    }
                }
                list.add(item)
            }
        }
    }

    private fun parseRoute(resolver: Resolver, brickList: ArrayList<DataProviderItem>) {
        val routeList: ArrayList<RouteItem> = ArrayList()
        val composeRouteList: ArrayList<ComposeItem> = ArrayList()
        resolver.getSymbolsWithAnnotation(Route::class.java.name).forEach {
            it.containingFile?.let { file ->
                routeDependencies.add(file)
            }
            it.accept(RouteVisitor(routeList, composeRouteList), Unit)
        }
        genRouterMapFile(routeList, composeRouteList, brickList)
    }

    inner class RouteVisitor(
        private val routeList: ArrayList<RouteItem>,
        private val composeRouteList: ArrayList<ComposeItem>
    ) :
        TheRouterVisitor(logger) {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            super.visitClassDeclaration(classDeclaration, data)
            sourcePath = getSourcePath(classDeclaration)
            classDeclaration.annotations.forEach { annotation ->
                val routeItem = RouteItem()
                routeItem.className = classDeclaration.qualifiedName?.asString()
                annotation.arguments.forEach { arg ->
                    when (arg.name?.asString()) {
                        "path" -> routeItem.path = "${arg.value}"
                        "action" -> routeItem.action = "${arg.value}"
                        "description" -> routeItem.description = "${arg.value}"
                        "params" -> {
                            if (arg.value is Array<*>) {
                                require((arg.value as Array<*>).size % 2 == 0) { "${routeItem.className} @Route(params) is not key value pairs" }
                                var k: String? = null
                                for (kv in arg.value as Array<*>) {
                                    if (k == null) {
                                        require(kv != null) { "${routeItem.className} @Route(params) key is null" }
                                        k = "$kv"
                                    } else {
                                        routeItem.params[k] = "$kv"
                                        k = null
                                    }
                                }
                            } else if (arg.value is java.util.ArrayList<*>) {
                                require((arg.value as java.util.ArrayList<*>).size % 2 == 0) { "${routeItem.className} @Route(params) is not key value pairs" }
                                var k: String? = null
                                for (kv in arg.value as java.util.ArrayList<*>) {
                                    if (k == null) {
                                        require(kv != null) { "${routeItem.className} @Route(params) key is null" }
                                        k = "$kv"
                                    } else {
                                        routeItem.params[k] = "$kv"
                                        k = null
                                    }
                                }
                            }
                        }
                    }
                }
                if (routeItem.path.isNotEmpty() && !routeItem.className.isNullOrEmpty()) {
                    routeList.add(routeItem)
                }
            }
        }

        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
            super.visitFunctionDeclaration(function, data)
            sourcePath = getSourcePath(function)
            var fileText: List<String> = ArrayList()

            if (function.functionKind != FunctionKind.STATIC && function.functionKind != FunctionKind.TOP_LEVEL) {
                logger.error("The modifiers of the " + function.qualifiedName?.asString() + "() must be top level function!")
            }
            val composeRoute = ComposeItem()
            function.annotations.forEach { annotation ->
                composeRoute.methodName = function.simpleName.asString()
                composeRoute.className =
                    function.parentDeclaration?.qualifiedName?.asString() ?: function.packageName.asString()

                annotation.arguments.forEach { arg ->
                    when (arg.name?.asString()) {
                        "path" -> composeRoute.path = "${arg.value}"
                        "action" -> {
                            if ("${arg.value}".isNotEmpty()) {
                                logger.error("@Composable function " + function.qualifiedName?.asString() + "() cannot support action!")
                            }
                        }

                        "description" -> composeRoute.description = "${arg.value}"
                        "params" -> {
                            if ("${arg.value}".isNotEmpty() && "${arg.value}".replace(" ", "") != "[]") {
                                logger.error("@Composable function " + function.qualifiedName?.asString() + "() cannot support params!")
                            }
                        }
                    }
                }
            }

            function.parameters.forEach {
                val parameter = ComposeParameter()
                parameter.hasDefault = it.hasDefault
                parameter.parameterName = it.name?.getShortName() ?: ""
                parameter.fieldName = parameter.parameterName
                // 使用新函数获取完整的参数类型，包括泛型嵌套
                parameter.parameterClassName = getParameterType(it.type.resolve())
                parameter.parameterSimpleClassName = it.type.resolve().declaration.qualifiedName?.asString() ?: ""

                it.annotations.forEach { parameterAnnotation ->
                    if ("Autowired" == parameterAnnotation.shortName.asString()) {
                        parameterAnnotation.arguments.forEach { arg ->
                            when (arg.name?.asString()) {
                                "name" -> {
                                    val rename = "${arg.value}"
                                    if (rename.isNotBlank()) {
                                        parameter.parameterName = rename
                                    }
                                }

                                "args" -> {
                                    val rename = "${arg.value}"
                                    if (rename.isNotBlank()) {
                                        parameter.parameterClassName = rename
                                    }
                                }
                            }
                        }
                    }
                }
                if (!composeRoute.params.contains(parameter)) {
                    composeRoute.params.add(parameter)
                }
            }

            if (composeRoute.path.isNotEmpty() && !composeRoute.className.isNullOrEmpty() && !composeRoute.methodName.isNullOrEmpty()) {
                composeRouteList.add(composeRoute)
            }
        }
    }

    private fun genRouterMapFile(
        pageList: List<RouteItem>,
        composeRouteList: ArrayList<ComposeItem>,
        brickList: ArrayList<DataProviderItem>
    ) {
        if (pageList.isEmpty() && composeRouteList.isEmpty() && brickList.isEmpty()) {
            return
        }
        val routePagelist = duplicateRemove(pageList)
        val routePagelist2 = ArrayList(HashSet(composeRouteList)).apply { sort() }
        // 确保只要编译的软硬件环境不变，类名就不会改变
        val className = PREFIX_ROUTER_MAP + kotlin.math.abs(
            if (sourcePath.isEmpty()) {
                if (routePagelist.isEmpty()) {
                    routePagelist2[0].className?.hashCode() ?: routePagelist2[0].path.hashCode()
                } else {
                    routePagelist[0].className?.hashCode() ?: routePagelist[0].path.hashCode()
                }
            } else {
                sourcePath.hashCode()
            },
        )
        val json = gson.toJson(routePagelist)
        var ps: PrintStream? = null
        try {
            val dependencies = Dependencies(aggregating = true, *routeDependencies.toTypedArray())
            ps = PrintStream(codeGenerator.createNewFile(dependencies, PACKAGE, className))
            ps.println("@file:JvmName(\"$className\")")
            ps.println("package $PACKAGE")
            ps.println()
            ps.println("/**")
            ps.println(" * Generated code, Don't modify!!!")
            ps.println(" * Created by kymjs, and KSP Version is ${BuildConfig.VERSION}.")
            ps.println(" * JDK Version is ${System.getProperty("java.version")}.")
            ps.println(" */")
            ps.println("@androidx.annotation.Keep")
            ps.println("class $className : com.therouter.router.IRouterMapAPT {")
            ps.println()
            ps.println("\toverride fun init() { $className.addRoute() }")
            ps.println()
            ps.println("\tcompanion object { ")
            ps.println()
            ps.println("\tconst val TAG = \"Created by kymjs, and KSP Version is ${BuildConfig.VERSION}.\"")
            ps.println("\tconst val THEROUTER_APT_VERSION = \"${BuildConfig.VERSION}\"")
            val routeMapJson = json.replace("\"", "\\\"")
            val max = 50000  // 65535
            var count = 0
            if (routeMapJson.length > max) {
                var content = routeMapJson
                while (content.length > max) {
                    val stringBuilder = StringBuilder("\tconst val ROUTERMAP$count = \"")
                    var index = max
                    var sub = content.substring(0, index)
                    var safe = !sub.endsWith('\\')
                    while (!safe) {
                        index--
                        sub = content.substring(0, index)
                        safe = !sub.endsWith('\\')
                    }
                    stringBuilder.append(sub).append("\"")
                    ps.println(stringBuilder.toString())
                    count++
                    content = content.substring(index, content.length)
                }
                ps.println("\tconst val ROUTERMAP$count = \"$content\"")
                count++
            } else {
                ps.println("\tconst val ROUTERMAP$count = \"$routeMapJson\"")
                count++
            }
            ps.println("\tconst val COUNT = $count")
            ps.println()
            ps.println("\t@JvmStatic")
            ps.println("\tfun addRoute() {")
            var i = 0
            for (item in routePagelist) {
                i++
                ps.println("\t\tval item$i = com.therouter.router.RouteItem(\"${item.path}\",\"${item.className}\",\"${item.action}\",\"${item.description}\")")
                item.params.keys.forEach {
                    ps.println("\t\titem$i.addParams(\"$it\", \"${item.params[it]}\")")
                }
                ps.println("\t\tcom.therouter.router.addRouteItem(item$i)")
            }

            for (item in brickList){
                i++
                ps.println("\t\tvar x$i = com.therouter.app.brick.DataRepository.mapping.get(\"${item.path}\")")
                ps.println("\t\tif (x$i == null) {")
                ps.println("\t\t\tx$i = ArrayList<com.therouter.app.brick.DataProvider<*>>()")
                ps.println("\t\t}")
                ps.println("\t\tval dp$i = com.therouter.app.brick.DataProvider<${item.returnTypeWithParams}>()")
                ps.println("\t\tdp$i.priority = ${item.priority}")
                ps.println("\t\tdp$i.fieldName = \"${item.fieldName}\"")
                ps.println("\t\tdp$i.path = \"${item.path}\"")
                ps.println("\t\tdp$i.returnType = ${item.returnType}::class.java")
                ps.println("\t\tdp$i.make = { nav -> ${item.className}.${item.methodName}(nav)}")
                ps.println("\t\tx$i.add(dp$i)")
                ps.println("\t\tcom.therouter.app.brick.DataRepository.mapping.set(\"${item.path}\", x$i)")
                ps.println("")
            }

            for (item in composeRouteList) {
                if (item.description.isNotEmpty()) {
                    ps.println("\t\t// ${item.description} ")
                }

                val hasDefaultComposeParameter = ArrayList<ComposeParameter>()

                item.params.forEach {
                    if (it.hasDefault) {
                        hasDefaultComposeParameter.add(it)
                    } else {
                        i++
                        ps.println("\t\tvar map$i = com.therouter.app.brick.DataRepository.composeMapping.get(\"${item.path}\")")
                        ps.println("\t\tif (map$i == null) {")
                        ps.println("\t\t\tmap$i = HashMap<String, Class<*>>()")
                        ps.println("\t\t}")
                        ps.println("\t\tmap$i.put(\"${it.parameterName}\", ${it.parameterSimpleClassName}::class.java)")
                        ps.println("\t\tcom.therouter.app.brick.DataRepository.composeMapping.put(\"${item.path}\", map$i)")
                    }
                }
                val lambdaText = StringBuilder()
                var paramsText = ""
                if (hasDefaultComposeParameter.isEmpty()) {
                    paramsText = ""
                    if (item.params.isNotEmpty()) {
                        var isFirst = true
                        item.params.forEach {
                            if (it.parameterName.isNotBlank()) {
                                val startFix = if (isFirst) "" else ", "
                                paramsText += "${startFix}it?.get(\"${it.parameterName}\") as ${it.parameterClassName}"
                                isFirst = false
                            }
                        }
                    }
                    lambdaText.append("${item.className}.${item.methodName}($paramsText)")
                } else {
                    // 111 每一位表示一个带默认值的参数，1表示判断有值，0表示判断没值
                    val binaryCount = (2 shl (hasDefaultComposeParameter.size - 1)) - 1
                    for (index in 0..binaryCount) {
                        lambdaText.append("if (")
                        // 3 遍历每一个带默认值的参数
                        for (number in 0 until hasDefaultComposeParameter.size) {
                            val bit = (index shr number) and 1
                            lambdaText.append("(it?.containsKey(\"${hasDefaultComposeParameter.get(number).parameterName}\")?:false) == ${bit == 1} &&")
                        }
                        // 写在这里，可以少一次循环
                        paramsText = ""
                        if (item.params.isNotEmpty()) {
                            var isFirst = true
                            item.params.forEach {
                                if (it.parameterName.isNotBlank()) {
                                    var bit = 1
                                    if (it.hasDefault) {
                                        for (number in 0 until hasDefaultComposeParameter.size) {
                                            if (hasDefaultComposeParameter.get(number).parameterName == it.parameterName) {
                                                bit = (index shr number) and 1
                                            }
                                        }
                                    }
                                    if (bit == 1) {
                                        val startFix = if (isFirst) "" else ", "
                                        val defaultFieldPrefix = if (it.hasDefault) "${it.fieldName} = " else ""
                                        paramsText += "${startFix}${defaultFieldPrefix}it?.get(\"${it.parameterName}\") as ${it.parameterClassName}"
                                        isFirst = false
                                    }
                                }
                            }
                        }
                        // delete last " &&"
                        lambdaText.deleteAt(lambdaText.length - 1)
                        lambdaText.deleteAt(lambdaText.length - 1)
                        lambdaText.deleteAt(lambdaText.length - 1)
                        lambdaText.append(") {")
                        lambdaText.append("${item.className}.${item.methodName}($paramsText)")
                            .append("} else ")
                    }

                    lambdaText.append("{")
                    paramsText = ""
                    if (item.params.isNotEmpty()) {
                        var isFirst = true
                        item.params.forEach {
                            if (it.parameterName.isNotBlank() && !it.hasDefault) {
                                val startFix = if (isFirst) "" else ", "
                                paramsText += "${startFix}it?.get(\"${it.parameterName}\") as ${it.parameterClassName}"
                                isFirst = false
                            }
                        }
                    }
                    lambdaText.append("${item.className}.${item.methodName}($paramsText)")
                        .append("}")
                }
                ps.println("\t\tcom.therouter.compose.composable(\"${item.path}\", {$lambdaText})")
            }

            ps.println("\t}")
            ps.println("\t}")
            ps.println("}")
            ps.flush()
        } finally {
            ps?.close()
        }
    }

    private fun parseAutowired(resolver: Resolver) {
        val map = HashMap<String, ArrayList<AutowiredItem>>()
        resolver.getSymbolsWithAnnotation(Autowired::class.java.name).forEach {
            it.containingFile?.let { file ->
                autoWiredDependencies.add(file)
            }
            it.accept(AutowiredVisitor(map), Unit)
        }
        genAutowiredFile(map)
    }

    inner class AutowiredVisitor(private val map: HashMap<String, ArrayList<AutowiredItem>>) :
        TheRouterVisitor(logger) {

        override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Unit) {
            super.visitPropertyDeclaration(property, data)
            sourcePath = getSourcePath(property)
            property.annotations.forEach { annotation ->
                val autowiredItem = AutowiredItem()
                autowiredItem.fieldName = property.simpleName.asString()
                autowiredItem.className =
                    property.parentDeclaration?.qualifiedName?.asString() ?: property.packageName.asString()
                autowiredItem.classNameAndTypeParameters = autowiredItem.className
                property.parentDeclaration?.typeParameters?.size?.let { size ->
                    if (size > 0) {
                        val classNameBuilder = StringBuilder(autowiredItem.className).append("<")
                        for (i in 0 until size) {
                            classNameBuilder.append("*")
                            if (i != size - 1) {
                                classNameBuilder.append(",")
                            }
                        }
                        classNameBuilder.append(">")
                        autowiredItem.classNameAndTypeParameters = classNameBuilder.toString()
                    }
                }

                autowiredItem.type = getParameterType(property.type.resolve())

                annotation.arguments.forEach { arg ->
                    when (arg.name?.asString()) {
                        "name" -> {
                            var key = "${arg.value}"
                            if (key.isBlank()) {
                                key = property.simpleName.asString()
                            }
                            autowiredItem.key = key
                        }

                        "args" -> autowiredItem.args = "${arg.value}"
                        "id" -> autowiredItem.id = "${arg.value ?: 0}".toInt()
                        "required" -> {
                            autowiredItem.required = "${arg.value}".equals("true", ignoreCase = true)
                        }

                        "description" -> autowiredItem.description = "${arg.value}"
                    }
                }
                var list = map[autowiredItem.className]
                if (list == null) {
                    list = ArrayList()
                }
                list.add(autowiredItem)
                list.sort()
                map[autowiredItem.className] = list
            }
        }
    }

    /**
     * 获取参数类型的完整字符串表示，包括泛型嵌套
     * @param type 参数的类型
     * @return 完整的类型字符串，如 "kotlin.collections.List<kotlin.String>"
     */
    private fun getParameterType(type: KSType?): String {
        if (type == null) {
            return ""
        }
        // 如果没有类型参数，直接返回基础类型名
        if (type.arguments.isEmpty()) {
            return type.declaration.qualifiedName?.asString() ?: ""
        }
        // 有类型参数，递归处理泛型嵌套
        val classNameBuilder = StringBuilder(type.declaration.qualifiedName?.asString()).append("<")
        type.arguments.forEachIndexed { index, typeArgument ->
            if (index > 0) {
                classNameBuilder.append(", ")
            }
            // 递归处理嵌套的泛型类型
            classNameBuilder.append(getParameterType(typeArgument.type?.resolve()))
        }
        classNameBuilder.append(">")
        return classNameBuilder.toString()
    }

    private fun genAutowiredFile(pageMap: Map<String, List<AutowiredItem>>) {
        val keyList = ArrayList(pageMap.keys)
        // 做一次排序，确保只要map成员没有变化，输出文件内容的顺序就没有变化
        keyList.sort()
        for (key in keyList) {
            val fullClassName = key + SUFFIX_AUTOWIRED
            val fullClassNameAndTypeParameters = pageMap[key]?.get(0)?.classNameAndTypeParameters ?: key
            val simpleName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1)
            val pkgName = fullClassName.substring(0, fullClassName.lastIndexOf('.'))
            var ps: PrintStream? = null
            try {
                val dependencies = Dependencies(aggregating = false, *autoWiredDependencies.toTypedArray())
                ps = PrintStream(codeGenerator.createNewFile(dependencies, pkgName, simpleName))
                ps.println("@file:JvmName(\"$simpleName\")")
                ps.println(String.format("package %s", pkgName))
                ps.println()
                ps.println("/**")
                ps.println(" * Generated code, Don't modify!!!")
                ps.println(" * Created by kymjs, and KSP Version is ${BuildConfig.VERSION}.")
                ps.println(" * JDK Version is ${System.getProperty("java.version")}.")
                ps.println(" */")
                ps.println("@androidx.annotation.Keep")
                ps.println(String.format("object %s {", simpleName))
                ps.println("\t@JvmStatic")
                ps.println("\tval TAG = \"Created by kymjs, and KSP Version is ${BuildConfig.VERSION}.\"")
                ps.println("\t@JvmStatic")
                ps.println("\tval THEROUTER_APT_VERSION = \"${BuildConfig.VERSION}\"")
                ps.println()
                ps.println("\t@JvmStatic")
                ps.println("\tfun autowiredInject(obj: Any) {")
                ps.println(String.format("\t\tif (obj is %s) {", fullClassNameAndTypeParameters))
                ps.println(String.format("\t\tval target = obj as %s", fullClassNameAndTypeParameters))
                ps.println()
                ps.println("\t\tfor (parser in com.therouter.TheRouter.parserList) {")
                for ((i, item) in pageMap[key]!!.withIndex()) {
                    var type = transformNumber(item.type)
                    if (!type.endsWith('?')) {
                        type += "?"
                    }
                    val variableName = "variableName$i"
                    ps.println("\t\t\ttry {")
                    ps.println(
                        String.format(
                            "\t\t\t\tval %s: %s = parser.parse(\"%s\", target, com.therouter.router.AutowiredItem(\"%s\",\"%s\",%s,\"%s\",\"%s\",\"%s\",%s,\"%s\"))",
                            variableName, type,
                            item.type,
                            item.type,
                            item.key,
                            item.id,
                            item.args,
                            item.className,
                            item.fieldName,
                            item.required,
                            item.description,
                        ),
                    )
                    ps.println("\t\t\t\tif ($variableName != null){")
                    ps.println("\t\t\t\t\t// ${item.description}")
                    ps.println(String.format("\t\t\t\t\ttarget.%s = $variableName", item.fieldName))
                    ps.println("\t\t\t\t}")
                    ps.println("\t\t\t} catch (e: Exception) {")
                    ps.println("\t\t\t\tif (com.therouter.TheRouter.isDebug) { e.printStackTrace() }")
                    ps.println("\t\t\t}")
                }
                ps.println("\t\t} // for end")

                for ((i, item) in pageMap[key]!!.withIndex()) {
                    if (item.required) {
                        ps.println(
                            String.format(
                                "\t\tif (target.%s == null && com.therouter.TheRouter.isDebug){",
                                item.fieldName,
                            ),
                        )
                        ps.println("\t\t\tthrow NullPointerException(\"@Autowired(required = true) ${key}.${item.fieldName} is null\")")
                        ps.println("\t\t}")
                    }
                }

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

    private fun parseServiceProvider(resolver: Resolver): ArrayList<ServiceProviderItem> {
        val list: ArrayList<ServiceProviderItem> = ArrayList()
        resolver.getSymbolsWithAnnotation(ServiceProvider::class.java.name).forEach {
            it.containingFile?.let { file ->
                serviceProviderDependencies.add(file)
            }
            it.accept(ServiceProviderVisitor(list), Unit)
        }
        return list
    }

    inner class ServiceProviderVisitor(private val list: ArrayList<ServiceProviderItem>) :
        TheRouterVisitor(logger) {

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            super.visitClassDeclaration(classDeclaration, data)
            sourcePath = getSourcePath(classDeclaration)
            classDeclaration.annotations.forEach { annotation ->
                if (annotation.annotationType.toString() != "ServiceProvider") {
                    return@forEach
                }
                val serviceProviderItem = ServiceProviderItem(false)
                serviceProviderItem.className =
                    classDeclaration.qualifiedName?.asString().toString()
                serviceProviderItem.methodName = ""
                annotation.arguments.forEach { arg ->
                    when (arg.name?.asString()) {
                        "returnType" -> {
                            if (arg.value is KSType) {
                                serviceProviderItem.returnType =
                                    "${(arg.value as KSType).declaration.qualifiedName?.asString()}"
                                if (serviceProviderItem.returnType == ServiceProvider::class.java.name) {
                                    serviceProviderItem.returnType = ""
                                }
                            }
                        }

                        "params" -> {
                            val params = ArrayList<String>()
                            for (kv in arg.value as List<*>) {
                                if (kv is KSType) {
                                    params.add(
                                        transformNumber(
                                            kv.declaration.qualifiedName?.asString() ?: "",
                                        ),
                                    )
                                }
                            }
                            serviceProviderItem.params = params
                        }
                    }
                }

                if (serviceProviderItem.returnType.isBlank()) {
                    val list = classDeclaration.superTypes.toList()
                    if (list.isEmpty()) {
                        serviceProviderItem.returnType = serviceProviderItem.className
                    } else if (list.size == 1) {
                        list.forEach {
                            serviceProviderItem.returnType =
                                it.resolve().declaration.qualifiedName?.asString().toString()
                        }
                    } else {
                        val prop = Properties()
                        try {
                            val gradleProperties = FileInputStream(PROPERTY_FILE)
                            prop.load(gradleProperties)
                        } catch (e: Exception) {
                        }
                        if (!STR_TRUE.equals(
                                prop.getProperty(KEY_USE_EXTEND),
                                ignoreCase = true,
                            )
                        ) {
                            throw IllegalArgumentException(
                                serviceProviderItem.className +
                                        " has multiple interfaces. Must to be specified returnType=XXX," +
                                        " or configuration KEY_USE_EXTEND=true in gradle.properties",
                            )
                        } else {
                            serviceProviderItem.returnType = serviceProviderItem.className
                        }
                    }
                }
                list.add(serviceProviderItem)
            }
        }

        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
            super.visitFunctionDeclaration(function, data)
            sourcePath = getSourcePath(function)

            if (function.functionKind != FunctionKind.STATIC && function.functionKind != FunctionKind.TOP_LEVEL) {
                logger.error("The modifiers of the " + function.qualifiedName?.asString() + "() must have static!")
            }
            function.annotations.forEach { annotation ->
                val serviceProviderItem = ServiceProviderItem(true)
                serviceProviderItem.methodName = function.simpleName.asString()
                serviceProviderItem.className =
                    function.parentDeclaration?.qualifiedName?.asString() ?: function.packageName.asString()

                annotation.arguments.forEach { arg ->
                    when (arg.name?.asString()) {
                        "returnType" -> {
                            if (arg.value is KSType) {
                                serviceProviderItem.returnType =
                                    "${(arg.value as KSType).declaration.qualifiedName?.asString()}"
                                if (serviceProviderItem.returnType == ServiceProvider::class.java.name) {
                                    serviceProviderItem.returnType =
                                        function.returnType?.resolve()?.declaration?.qualifiedName?.asString()
                                            ?: ""
                                }
                            }
                        }

                        "params" -> {
                            val params = ArrayList<String>()
                            for (kv in arg.value as List<*>) {
                                if (kv is KSType) {
                                    params.add(
                                        transformNumber(
                                            kv.declaration.qualifiedName?.asString() ?: "",
                                        ),
                                    )
                                }
                            }
                            if (params.size == 0) {
                                function.parameters.forEach {
                                    params.add(
                                        transformNumber(
                                            it.type.resolve().declaration.qualifiedName?.asString()
                                                ?: "",
                                        ),
                                    )
                                }
                            } else if (params.size != function.parameters.size) {
                                val log = StringBuilder(function.qualifiedName?.asString())
                                    .append("() parameter list required (")
                                function.parameters.forEach {
                                    log.append(it.type.resolve().declaration.qualifiedName?.asString())
                                        .append(",")
                                }
                                log.append("), But the declared parameter is")
                                repeat(params.size) {
                                    log.append("$it,")
                                }
                                log.append(")")
                                logger.error(log.toString())
                            }
                            serviceProviderItem.params = params
                        }
                    }
                }
                list.add(serviceProviderItem)
            }
        }
    }

    private fun parseFlowTask(resolver: Resolver): ArrayList<FlowTaskItem> {
        val list = ArrayList<FlowTaskItem>()
        resolver.getSymbolsWithAnnotation(FlowTask::class.java.name).forEach {
            it.containingFile?.let { file ->
                serviceProviderDependencies.add(file)
            }
            it.accept(FlowTaskVisitor(list), Unit)
        }
        return list
    }

    inner class FlowTaskVisitor(private val list: ArrayList<FlowTaskItem>) :
        TheRouterVisitor(logger) {

        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
            super.visitFunctionDeclaration(function, data)
            sourcePath = getSourcePath(function)

            if (function.functionKind != FunctionKind.STATIC && function.functionKind != FunctionKind.TOP_LEVEL) {
                logger.error("The modifiers of the " + function.qualifiedName?.asString() + "() must have static!")
            }

            val type = function.returnType?.resolve()?.declaration?.qualifiedName?.asString() ?: ""
            if (type != Unit.javaClass.name) {
                logger.error("The return type of the " + function.qualifiedName?.asString() + "() must be void")
            }
            if (function.parameters.size != 1) {
                logger.error("=========================\n\n\n\n" + function.qualifiedName?.asString() + "() must only has Context parameter")
            }
            function.parameters.forEach {
                if (it.type.resolve().declaration.qualifiedName?.asString() != "android.content.Context") {
                    logger.error(
                        "=========================\n\n\n\n" + function.qualifiedName?.asString() + "(" + it.type.resolve().declaration.qualifiedName?.asString() + ") must only has Context parameter",
                    )
                }
            }

            function.annotations.forEach { annotation ->
                val flowTaskItem = FlowTaskItem()
                flowTaskItem.methodName = function.simpleName.asString()
                flowTaskItem.className =
                    function.parentDeclaration?.qualifiedName?.asString() ?: function.packageName.asString()
                annotation.arguments.forEach { arg ->
                    when (arg.name?.asString()) {
                        "taskName" -> flowTaskItem.taskName = "${arg.value}"
                        "async" -> flowTaskItem.async = "${arg.value}".toBoolean()
                        "dependsOn" -> flowTaskItem.dependencies = "${arg.value}"
                    }
                }
                list.add(flowTaskItem)
            }
        }
    }

    private fun parseActionInterceptor(resolver: Resolver): ArrayList<ActionInterceptorItem> {
        val list = ArrayList<ActionInterceptorItem>()
        resolver.getSymbolsWithAnnotation(ActionInterceptor::class.java.name).forEach {
            it.containingFile?.let { file ->
                serviceProviderDependencies.add(file)
            }
            it.accept(ActionInterceptorVisitor(list), Unit)
        }
        return list
    }

    inner class ActionInterceptorVisitor(private val list: ArrayList<ActionInterceptorItem>) :
        TheRouterVisitor(logger) {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            super.visitClassDeclaration(classDeclaration, data)
            sourcePath = getSourcePath(classDeclaration)
            classDeclaration.annotations.forEach { annotation ->
                val item = ActionInterceptorItem()
                item.className = classDeclaration.qualifiedName?.asString().toString()
                annotation.arguments.forEach { arg ->
                    when (arg.name?.asString()) {
                        "actionName" -> item.actionName = "${arg.value}"
                    }
                }
                list.add(item)
            }
        }
    }

    private fun genServiceProviderFile(
        pageList: ArrayList<ServiceProviderItem>,
        flowTaskList: ArrayList<FlowTaskItem>,
        actionInterceptorList: ArrayList<ActionInterceptorItem>,
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
            stringBuilder.append("\\\"").append(it.taskName).append("\\\":\\\"")
                .append(it.dependencies).append("\\\"")
            isFirst = false
        }
        stringBuilder.append("}")

        // 确保只要编译的软硬件环境不变，类名就不会改变
        val className = PREFIX_SERVICE_PROVIDER + kotlin.math.abs(
            if (sourcePath.isEmpty()) {
                if (pageList.isNotEmpty()) {
                    pageList[0].className.hashCode()
                } else {
                    flowTaskList[0].className.hashCode()
                }
            } else {
                sourcePath.hashCode()
            },
        )
        var ps: PrintStream? = null
        try {
            val dependencies = Dependencies(aggregating = true, *serviceProviderDependencies.toTypedArray())
            ps = PrintStream(codeGenerator.createNewFile(dependencies, PACKAGE, className))
            ps.println("@file:JvmName(\"$className\")")
            ps.println(String.format("package %s", PACKAGE))
            ps.println()
            ps.println("/**")
            ps.println(" * Generated code, Don't modify!!!")
            ps.println(" * Created by kymjs, and KSP Version is ${BuildConfig.VERSION}.")
            ps.println(" * JDK Version is ${System.getProperty("java.version")}.")
            ps.println(" */")
            ps.println("@androidx.annotation.Keep")
            ps.println(
                String.format(
                    "public class %s : com.therouter.inject.Interceptor {",
                    className,
                ),
            )
            ps.println()
            ps.println("\toverride fun initFlowTask(context: android.content.Context, digraph: com.therouter.flow.Digraph) {")
            ps.println(String.format("\t\t%s.addFlowTask(context, digraph)", className))
            ps.println("\t}")
            ps.println()
            ps.println("\toverride fun <T> interception(clazz: Class<T>?, vararg params: Any?): T? {")
            ps.println("\t\tvar obj: T? = null")
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
                    ps.print(
                        String.format(
                            "if (%s::class.javaObjectType.isAssignableFrom(clazz)",
                            serviceProviderItem.returnType,
                        ),
                    )
                } else {
                    ps.print(
                        String.format(
                            "if (%s::class.java.equals(clazz)",
                            serviceProviderItem.returnType,
                        ),
                    )
                }
                // 多参数判断
                ps.print(" && params.size == ")
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
                                "\n\t\t\t\t&& params[%d] is %s",
                                count,
                                serviceProviderItem.params[count],
                            ),
                        )
                    }
                }
                ps.println(") {")
                if (serviceProviderItem.isMethod) {
                    ps.print(
                        String.format(
                            "\t\t\tval returnType: %s = %s.%s(",
                            serviceProviderItem.returnType,
                            serviceProviderItem.className,
                            serviceProviderItem.methodName,
                        ),
                    )
                } else {
                    ps.print(
                        String.format(
                            "\t\t\tval returnType: %s = %s(",
                            serviceProviderItem.returnType,
                            serviceProviderItem.className,
                        ),
                    )
                }

                for (count in serviceProviderItem.params.indices) {
                    if (!serviceProviderItem.params[count].trim { it <= ' ' }.isEmpty()) {
                        //参数强转
                        ps.print(
                            String.format(
                                Locale.getDefault(),
                                "params[%d] as %s",
                                count,
                                serviceProviderItem.params[count],
                            ),
                        )
                        if (count != serviceProviderItem.params.size - 1) {
                            ps.print(", ")
                        }
                    }
                }
                ps.println(")")
                ps.println("\t\t\tobj = returnType as T?")
                ps.print("\t\t} else ")
            }
            ps.println("{\n")
            ps.println("        }")
            ps.println("        return obj")
            ps.println("    }")
            ps.println()
            ps.println("\tcompanion object { ")
            ps.println()
            ps.println("\tconst val TAG = \"Created by kymjs, and KSP Version is ${BuildConfig.VERSION}.\"")
            ps.println("\tconst val THEROUTER_APT_VERSION = \"${BuildConfig.VERSION}\"")
            ps.println("\tconst val FLOW_TASK_JSON = \"${stringBuilder}\"")
            ps.println()
            ps.println("\t\t@kotlin.jvm.JvmStatic")
            ps.println("\t\tfun addFlowTask(context: android.content.Context, digraph: com.therouter.flow.Digraph) {")
            for (item in flowTaskList) {
                ps.println("\t\t\tdigraph.addTask(com.therouter.flow.Task(${item.async}, \"${item.taskName}\", \"${item.dependencies}\", object : com.therouter.flow.FlowTaskRunnable {")
                ps.println("\t\t\t\toverride fun run() = ${item.className}.${item.methodName}(context)")
                ps.println()
                ps.println("\t\t\t\toverride fun log() = \"${item.className}.${item.methodName}(context)\"")
                ps.println("\t\t\t}))")
            }
            for (item in actionInterceptorList) {
                ps.println("\t\t\tcom.therouter.TheRouter.addActionInterceptor(\"${item.actionName}\", ${item.className}());")
            }
            ps.println("\t\t}")
            ps.println("\t}")
            ps.println("}")
            ps.flush()
        } finally {
            ps?.close()
        }
    }
}

fun transformNumber(type: String): String {
    return when (type) {
        "byte" -> "kotlin.Byte"
        "short" -> "kotlin.Short"
        "int" -> "kotlin.Integer"
        "long" -> "kotlin.Long"
        "float" -> "kotlin.Float"
        "double" -> "kotlin.Double"
        "boolean" -> "kotlin.Boolean"
        "char" -> "kotlin.Character"
        "String" -> "kotlin.String"
        "java.lang.String" -> "kotlin.String"
        else -> type
    }
}

fun getSourcePath(ks: KSDeclaration): String {
    val pkgPath = ks.packageName.asString().replace(".", "/")
    val filePath = ks.containingFile?.filePath
    var sourcePath = filePath?.replace("/$pkgPath", "") ?: ""
    val endIndex = sourcePath.indexOfLast { it == '/' }
    if (endIndex > 0) {
        sourcePath = sourcePath.substring(0, endIndex)
    }
    return sourcePath
}