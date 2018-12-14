package com.test.factory;

import com.test.config.Bean;
import com.test.config.Property;
import com.test.manager.ConfigManager;
import org.apache.commons.beanutils.BeanUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassPathXmlApplicationContext implements BeanFactory{

    // 获得读取的配置文件中的Map信息
    private Map<String, Bean> map;
    // 作为IOC容器使用,放置sring放置的对象
    private Map<String, Object> context = new HashMap<String, Object>();


    public ClassPathXmlApplicationContext(String path) {
        map = ConfigManager.getConfig(path);
        for (Map.Entry<String, Bean> en : map.entrySet()) {
            String beanName = en.getKey();
            Bean bean = en.getValue();

            Object existBean = context.get(beanName);
            if(existBean == null && bean.getScope().equals("singleton")){
                existBean = createBean(bean);
            }
        }
    }

    // 通过反射创建对象
    private Object createBean(Bean bean) {
        Class clazz = null;
        try {
            clazz = Class.forName(bean.getClassName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("没有找到该类" + bean.getClassName());
        }
        Object beanObj = null;
        try {
            beanObj = clazz.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("没有提供无参构造器");
        }
        if(bean.getProperties() != null){
            for (Property prop : bean.getProperties()){
                String name = prop.getName();
                String value = prop.getValue();
                String ref = prop.getRef();
                if(value != null){
                    Map<String, String[]> parmMap = new HashMap<String, String[]>();
                    parmMap.put(name, new String[] { value });
                    try {
                        BeanUtils.populate(beanObj, parmMap);
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException("请检查你的" + name + "属性");
                    }
                }

                if (ref != null) {
                    Object refBean = context.get(ref);
                    if(refBean == null){
                        refBean = createBean(map.get(ref));
                        if("singleton".equals(map.get(ref).getScope())){
                            context.put(ref, refBean);
                        }
                    }

                    try {
                        BeanUtils.setProperty(beanObj, name, refBean);
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException("您的bean的属性" + name
                                + "没有对应的set方法");
                    }

                }


            }
        }

        return beanObj;

    }

    @Override
    public Object getBean(String name) {
        Object bean = context.get(name);
        // 如果为空说明scope不是singleton,那么容器中是没有的,这里现场创建
        if (bean == null) {
            bean = createBean(map.get(name));
        }

        return bean;
    }
}
