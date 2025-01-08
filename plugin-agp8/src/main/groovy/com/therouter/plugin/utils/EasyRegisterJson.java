package com.therouter.plugin.utils;

import java.util.ArrayList;
import java.util.List;

public class EasyRegisterJson {

    public static final String json = """
            [
              {
                "wovenClass": "a.TheRouterServiceProvideInjecter",
                "wovenMethod": "void addFlowTask(android.content.Context,com.therouter.flow.Digraph)",
                "searchClass": {
                  "regex": "^a.ServiceProvider__TheRouter__[^$]+$",
                  "extendsClass": "",
                  "callType": "caller",
                  "callClass": "",
                  "callMethod": "void addFlowTask(android.content.Context,com.therouter.flow.Digraph)",
                  "callMethodValue": "$0,$1",
                  "useType": "className"
                }
                        
              },
              {
                "wovenClass": "a.TheRouterServiceProvideInjecter",
                "wovenMethod": "void autowiredInject(Object)",
                "searchClass": {
                  "regex": "^[^$]+__TheRouter__Autowired$",
                  "extendsClass": "",
                  "callType": "caller",
                  "callClass": "",
                  "callMethod": "void autowiredInject(Object)",
                  "callMethodValue": "$0",
                  "useType": "className"
                }
                        
              },
              {
                "wovenClass": "a.TheRouterServiceProvideInjecter",
                "wovenMethod": "void initDefaultRouteMap()",
                "searchClass": {
                  "regex": "^a.RouterMap__TheRouter__[^$]+$",
                  "extendsClass": "",
                  "callType": "caller",
                  "callClass": "",
                  "callMethod": "void addRoute()",
                  "callMethodValue": "",
                  "useType": "className"
                }
                        
              },
              {
                "wovenClass": "a.TheRouterServiceProvideInjecter",
                "wovenMethod": "void trojan()",
                "searchClass": {
                  "regex": "^a.ServiceProvider__TheRouter__[^$]+$",
                  "extendsClass": "",
                  "callType": "callee",
                  "callClass": "com.therouter.TheRouter",
                  "callMethod": "com.therouter.inject.RouterInject getRouterInject()#void privateAddInterceptor(com.therouter.inject.Interceptor)",
                  "callMethodValue": "searchClass",
                  "useType": "new"
                }
                        
              }
            ]
            """;

    public static final List<String> jsons = new ArrayList<>(List.of(json));
}
