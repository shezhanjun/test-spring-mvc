package com.jd.web.test;

import com.jd.web.test.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DispatcherSerlvet extends HttpServlet{

    private List<String> classNames = new ArrayList<String>();

    private Map<String, Object> beans = new HashMap<String, Object>();

    private Map<String, Object> hanlderMap = new HashMap<String, Object>();

    public void init(ServletConfig config) throws ServletException {
        scannPackage("com.jd.web.test"); // 扫描包，这里面的逻辑可以配置到properties里面

        doInstance(); // 生成实例

        doIoc(); // 注入对应的bean

        buildUrlMaping(); //绑定url
    }


    private void scannPackage(String basePackageName) {
        URL url = this.getClass().getClassLoader().getResource("/" + basePackageName.replaceAll("\\.", "/"));
        String filestr = url.getFile();
        File file = new File(filestr);
        String fileStrArr[] = file.list();
        for (String path : fileStrArr) {
            File currentFile = new File(filestr + path);
            if (currentFile.isDirectory()) {
                scannPackage(basePackageName + "." + path);
            } else {
                classNames.add(basePackageName + "." + path);
            }
        }
    }

    private void doInstance() {
        if (classNames.size() <1) {
            System.out.println("没有扫描初始化！！");
        } else {
            for(String cn : classNames) {
                String newcn = cn.replace(".class", "");
                try {
                    Class<?> clazz = Class.forName(newcn);
                    if (clazz.isAnnotationPresent(JDController.class)) {
                        Object instance = clazz.newInstance();

                        JDRequestMapping rm = clazz.getAnnotation(JDRequestMapping.class);

                        beans.put(rm.value(), instance);
                    } else if(clazz.isAnnotationPresent(JDService.class)) {
                        JDService service = clazz.getAnnotation(JDService.class);
                        beans.put(service.name(), clazz.newInstance());
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doIoc() {
        if (beans.entrySet().size()<1) {
            System.out.println("没有实例化对象..");
        } else {
            for(Map.Entry<String, Object> entry : beans.entrySet()) {
                Object instance = entry.getValue();

                Class<?> clazz = instance.getClass();

                if(clazz.isAnnotationPresent(JDController.class)) {
                    Field[] fields = clazz.getDeclaredFields();
                    for(Field field : fields) {
                        if(field.isAnnotationPresent(JDAutowired.class)) {
                            JDAutowired autowired = field.getAnnotation(JDAutowired.class);

                            field.setAccessible(true);
                            try {
                                field.set(instance, beans.get(autowired.value()));
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        } else {
                            continue;
                        }
                    }

                }
            }
        }
    }

    private void buildUrlMaping() {
        if(beans.entrySet().size()<1) {
            return;
        } else {
            for(Map.Entry<String, Object> entry: beans.entrySet()) {
                Object instatnce = entry.getValue();
                Class<?> clazz = instatnce.getClass();
                if(clazz.isAnnotationPresent(JDRequestMapping.class)) {
                    Method[] methods = clazz.getMethods();
                    JDRequestMapping parentPath = clazz.getAnnotation(JDRequestMapping.class);
                    for (Method method : methods) {
                        if (method.isAnnotationPresent(JDRequestMapping.class)) {
                            JDRequestMapping childPath = method.getAnnotation(JDRequestMapping.class);
                            hanlderMap.put(parentPath.value()+childPath.value(), method);
                        }
                    }
                }

            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        super.doPost(req, resp);
        System.out.println("in  aaaa.................aaaaaa");
       String uri =  req.getRequestURI();//test-root/test/query
       String context = req.getContextPath();//test-root/
       String path = uri.replace(context, "");//  /test/query
       Method method = (Method) hanlderMap.get(path);
       Object instance =  beans.get(path.substring(0,path.lastIndexOf("/")));
        try {
            method.invoke(instance, handl(req,resp,method));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    /**
     * 获取请求里面的参数
     * @param req
     * @param resp
     * @param method
     * @return
     */
    private Object[] handl(HttpServletRequest req, HttpServletResponse resp, Method method) {
        Class<?>[] patameterTypes = method.getParameterTypes();
        Object args[] = new Object[patameterTypes.length];
        for (int i=0;i<patameterTypes.length;i++) {
            Class<?> clazz  = patameterTypes[i];
            if (ServletRequest.class.isAssignableFrom(clazz)) {
                args[i] = req;
            }
            if (ServletResponse.class.isAssignableFrom(clazz)) {
                args[i] = resp;
            }
            Annotation[] annotations = method.getParameterAnnotations()[i];
            if (annotations.length > 0) {
                for (Annotation annotation : annotations) {
                    if(JDRequestParam.class.isAssignableFrom(annotation.getClass())) {
                        JDRequestParam requestParam = (JDRequestParam) annotation;
                        args[i] = req.getParameter(requestParam.name());

                    }
                }
            }
        }
        return args;
    }

    public static void main(String[] args) {
        DispatcherSerlvet ds = new DispatcherSerlvet();
        //System.out.println("com.jd.web".replaceAll("\\.","/"));
//        URL url = DispatcherSerlvet.class.getClassLoader().getResource("" ); // /D:/project/git/MySpringMVC/target/classes/
//        URL url = DispatcherSerlvet.class.getResource("" ); // /D:/project/git/MySpringMVC/target/classes/com/jd/web/test/
        URL url = DispatcherSerlvet.class.getResource("/" ); // /D:/project/git/MySpringMVC/target/classes/
        String filestr = url.getFile();
        System.out.println(filestr);
    }

}
