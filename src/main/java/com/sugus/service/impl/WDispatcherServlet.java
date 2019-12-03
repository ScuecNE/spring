package com.sugus.service.impl;

import com.sugus.annotation.WAutowired;
import com.sugus.annotation.WController;
import com.sugus.annotation.WRequestMapping;
import com.sugus.annotation.WService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class WDispatcherServlet extends HttpServlet {

    private Properties properties = new Properties();

    private List<String> beanNames = new ArrayList<>();

    private Map<String, Object> ioc = new HashMap<>();

    private Map<String, Method> urlMapping = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPost(req, resp);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        //加载配置,这里会获取到web.xml中init-param节点中的值
        String contextConfigLocation = config.getInitParameter("contextConfigLocation");
        loadConfig(contextConfigLocation);
        //获取要扫描的包地址
        String dirPath = properties.getProperty("scanner.package");
        //扫描要加载的类
        doScanner(dirPath);
        //实例化要加载的类
        doInstance();
        //加载依赖注入，给属性赋值
        doAutoWrited();
        //加载映射地址
        doRequestMapping();
    }

    private void doRequestMapping() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> obj : ioc.entrySet()) {
            if (!obj.getValue().getClass().isAnnotationPresent(WController.class)) {
                continue;
            }
            Method[] methods = obj.getValue().getClass().getMethods();
            for (Method method : methods) {
                if (!method.isAnnotationPresent(WRequestMapping.class)) {
                    continue;
                }
                String baseUrl = "";
                if (obj.getValue().getClass().isAnnotationPresent(WRequestMapping.class)) {
                    baseUrl = obj.getValue().getClass().getAnnotation(WRequestMapping.class).value();
                }
                WRequestMapping wRequestMapping = method.getAnnotation(WRequestMapping.class);
                if ("".equals(wRequestMapping.value())) {
                    continue;
                }
                String url = (baseUrl + "/" + wRequestMapping.value()).replaceAll("/+", "/");
                urlMapping.put(url, method);
                System.out.println(url);
            }
        }
    }

    private void doAutoWrited() {
        for (Map.Entry<String, Object> obj : ioc.entrySet()) {
            for (Field field : obj.getValue().getClass().getDeclaredFields()) {
                if (!field.isAnnotationPresent(WAutowired.class)) {
                    continue;
                }
                WAutowired wAutowired = field.getAnnotation(WAutowired.class);
                String beanName = wAutowired.value();
                if ("".equals(beanName)) {
                    beanName = field.getType().getSimpleName();
                }
                field.setAccessible(true);
                try {
                    field.set(obj.getValue(), ioc.get(firstLowerCase(beanName)));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 可以用工厂实现
    private void doInstance() {
        if (beanNames.isEmpty()) {
            return;
        }
        for (String beanName : beanNames) {
            try {
                Class cls = Class.forName(beanName);
                if (cls.isAnnotationPresent(WController.class)) {
                    Object instance = cls.newInstance();
                    beanName = firstLowerCase(cls.getSimpleName());
                    ioc.put(beanName, instance);
                } else if (cls.isAnnotationPresent(WService.class)) {
                    Object instance = cls.newInstance();
                    WService wService = (WService) cls.getAnnotation(WService.class);
                    String alisName = wService.value();
                    if (alisName.trim().length() == 0) {
                        beanName = cls.getSimpleName();
                    } else {
                        beanName = alisName;
                    }
                    beanName = firstLowerCase(beanName);
                    ioc.put(beanName, instance);

                    Class<?>[] interfaces = cls.getInterfaces();
                    for (Class<?> clazz : interfaces) {
                        ioc.put(firstLowerCase(clazz.getSimpleName()), instance);
                    }
                }
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
            }
        }
    }

    private String firstLowerCase(String str) {
        char[] chars = str.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    private void doScanner(String dirPath) {
        URL url = this.getClass().getClassLoader().getResource("/" + dirPath.replaceAll("\\.", "/"));
        if (null == url) {
            return;
        }
        File dir = new File(url.getFile());
        File[] files = dir.listFiles();
        if (null == files) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                doScanner(file.getPath());
                continue;
            }
            //拿到所扫描的包的所有类全路径和类名
            String beanName = dirPath + "." + file.getName().replaceAll(".class", "");
            beanNames.add(beanName);
        }
    }

    private void loadConfig(String contextConfigLocation) {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            properties.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
