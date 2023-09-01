package com.therouter.plugin;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AddCodeVisitor extends ClassVisitor {

    private List<String> serviceProvideList;
    private Map<String, String> serviceProvideMap;
    private List<String> autowiredList;
    private List<String> routeList;
    private final boolean isIncremental;

    public AddCodeVisitor(ClassVisitor cv, Map<String, String> serviceProvideMap, Set<String> autowiredSet, Set<String> routeSet, boolean incremental) {
        super(Opcodes.ASM7, cv);
        this.serviceProvideList = new ArrayList<>(serviceProvideMap.keySet());
        this.serviceProvideMap = serviceProvideMap;
        this.autowiredList = new ArrayList<>(autowiredSet);
        this.routeList = new ArrayList<>(routeSet);
        this.isIncremental = incremental;
        Collections.sort(this.serviceProvideList);
        Collections.sort(this.autowiredList);
        Collections.sort(this.routeList);
    }

    @Override
    public MethodVisitor visitMethod(int access, String methodName, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, methodName, desc, signature, exceptions);
        mv = new AdviceAdapter(Opcodes.ASM7, mv, access, methodName, desc) {
            @Override
            protected void onMethodEnter() {
                super.onMethodEnter();
                //不代理构造函数
                if (!"<init>".equals(methodName)) {
                    if ("trojan".equals(methodName)) {
                        for (String serviceProviderClassName : serviceProvideList) {
                            if (!serviceProviderClassName.startsWith("a/")) {
                                serviceProviderClassName = "a/" + serviceProviderClassName;
                            }
                            Label tryStart = new Label();
                            Label tryEnd = new Label();
                            Label labelCatch = new Label();
                            mv.visitTryCatchBlock(tryStart, tryEnd, labelCatch, "java/lang/Throwable");
                            mv.visitLabel(tryStart);

                            mv.visitMethodInsn(INVOKESTATIC, "com/therouter/TheRouter", "getRouterInject", "()Lcom/therouter/inject/RouterInject;", false);
                            mv.visitTypeInsn(NEW, serviceProviderClassName);
                            mv.visitInsn(DUP);
                            mv.visitMethodInsn(INVOKESPECIAL, serviceProviderClassName, "<init>", "()V", false);
                            mv.visitMethodInsn(INVOKEVIRTUAL, "com/therouter/inject/RouterInject", "privateAddInterceptor", "(Lcom/therouter/inject/Interceptor;)V", false);

                            mv.visitLabel(tryEnd);
                            Label tryCatchBlockEnd = new Label();
                            mv.visitJumpInsn(GOTO, tryCatchBlockEnd);
                            mv.visitLabel(labelCatch);
                            mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/lang/Throwable"});
                            mv.visitVarInsn(ASTORE, 0);
                            mv.visitVarInsn(ALOAD, 0);
                            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V", false);
                            mv.visitLabel(tryCatchBlockEnd);
                            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                        }
                    }
                    if ("addFlowTask".equals(methodName)) {
                        for (String serviceProviderClassName : serviceProvideList) {
                            if (!serviceProviderClassName.startsWith("a/")) {
                                serviceProviderClassName = "a/" + serviceProviderClassName;
                            }
                            String aptVersion = serviceProvideMap.get(serviceProviderClassName.substring(2));
                            if (aptVersion == null) {
                                aptVersion = serviceProvideMap.get(serviceProviderClassName);
                            }
                            // FlowTask 功能是从1.0.13开始引入的，
                            // 没有版本号的都是老版本，不能插入字节码，但源码引用的例外，需要插入字节码
                            if (aptVersion != null && !aptVersion.equals("0.0.0")) {
                                Label label0 = new Label();
                                Label label1 = new Label();
                                Label label2 = new Label();
                                mv.visitTryCatchBlock(label0, label1, label2, "java/lang/Throwable");
                                mv.visitLabel(label0);
                                mv.visitVarInsn(ALOAD, 0);
                                mv.visitVarInsn(ALOAD, 1);
                                mv.visitMethodInsn(INVOKESTATIC, serviceProviderClassName, "addFlowTask", "(Landroid/content/Context;Lcom/therouter/flow/Digraph;)V", false);
                                mv.visitLabel(label1);
                                Label tryCatchBlockEnd = new Label();
                                mv.visitJumpInsn(GOTO, tryCatchBlockEnd);
                                mv.visitLabel(label2);
                                mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/lang/Throwable"});
                                mv.visitVarInsn(ASTORE, 2);
                                mv.visitLabel(tryCatchBlockEnd);
                                mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                            }
                        }
                    }
                    if ("autowiredInject".equals(methodName)) {
                        for (String autowiredClassName : autowiredList) {
                            Label tryStart = new Label();
                            Label tryEnd = new Label();
                            Label labelCatch = new Label();
                            mv.visitTryCatchBlock(tryStart, tryEnd, labelCatch, "java/lang/Throwable");

//                            Label labelIf = new Label();
//                            mv.visitLabel(labelIf);
//                            mv.visitLdcInsn(autowiredClassName.replace('/', '.'));
//                            mv.visitVarInsn(ALOAD, 0);
//                            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
//                            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
//                            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "contains", "(Ljava/lang/CharSequence;)Z", false);

                            Label labelInvoke = new Label();
//                            mv.visitJumpInsn(IFEQ, labelInvoke);
                            mv.visitLabel(tryStart);
                            mv.visitVarInsn(ALOAD, 0);
                            mv.visitMethodInsn(INVOKESTATIC, autowiredClassName.replace('.', '/'), methodName, "(Ljava/lang/Object;)V", false);
                            mv.visitLabel(tryEnd);
                            mv.visitJumpInsn(GOTO, labelInvoke);
                            mv.visitLabel(labelCatch);
                            mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/lang/ClassCastException"});
                            mv.visitVarInsn(ASTORE, 1);
                            mv.visitLabel(labelInvoke);
                            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                        }
                    }
                    if ("initDefaultRouteMap".equals(methodName)) {
                        for (String route : routeList) {
                            Label tryStart = new Label();
                            Label tryEnd = new Label();
                            Label labelCatch = new Label();
                            Label tryCatchBlockEnd = new Label();
                            mv.visitTryCatchBlock(tryStart, tryEnd, labelCatch, "java/lang/Throwable");
                            mv.visitLabel(tryStart);

                            String className = route.replace(".class", "").replace('.', '/');
                            mv.visitMethodInsn(INVOKESTATIC, className, "addRoute", "()V", false);

                            mv.visitLabel(tryEnd);
                            mv.visitJumpInsn(GOTO, tryCatchBlockEnd);
                            mv.visitLabel(labelCatch);
                            mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/lang/Throwable"});
                            mv.visitVarInsn(ASTORE, 0);
                            mv.visitVarInsn(ALOAD, 0);
                            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V", false);
                            mv.visitLabel(tryCatchBlockEnd);
                            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                        }
                    }
                    if (!isIncremental) {
                        mv.visitInsn(RETURN);
                    }
                }
            }
        };
        return mv;
    }
}