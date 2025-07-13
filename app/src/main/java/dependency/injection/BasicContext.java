package dependency.injection;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class BasicContext implements Context {

    HashMap<String, BeanDefinition> beanDefinitions = new HashMap<>();
    HashMap<String, Object> singletons = new HashMap<>();

    @Override
    public <T> T getBean(String name, Class<T> clazz) {
        BeanDefinition beanDefinition = beanDefinitions.get(name);
        if(beanDefinition.getBeanClass().isAssignableFrom(clazz)) {
            return resolveBean(beanDefinition);
        }
        throw new RuntimeException("타입이 일치하지 않습니다.");
    }

    @Override
    public <T> T getBean(String name) {
        return resolveBean(beanDefinitions.get(name));
    }

    @Override
    public <T> T getBean(Class<T> clazz) {
        for(BeanDefinition beanDefinition : beanDefinitions.values()) {
            if(clazz.isAssignableFrom(beanDefinition.getBeanClass())) {
                return resolveBean(beanDefinition);
            }
        }
        throw new RuntimeException("타입이 일치하지 않습니다.");
    }

    @Override
    public <T> void addBean(Class<T> clazz) {
        addBean(clazz.getName(), clazz);
    }

    @Override
    public <T> void addBean(String name, Class<T> clazz) {

        BeanDefinition beanDefinition = new BasicBeanDefinition(clazz);

        if (beanDefinitions.containsKey(beanDefinition.getBeanName())) {
            throw new RuntimeException("동일한 빈이 이미 존재합니다.");
        }

        beanDefinitions.put(beanDefinition.getBeanName(), beanDefinition);

    }

    private <T> T resolveBean(BeanDefinition beanDefinition) {
        if(singletons.containsKey(beanDefinition.getBeanName())) {
            return (T) singletons.get(beanDefinition.getBeanName());
        }

        Class<?>[] dependencies = beanDefinition.getDependencyClass();

        Object[] dependencyObjects = new Object[dependencies.length];
        for(int i = 0; i < dependencies.length; i++) {
            Class<?> dependency = dependencies[i];
            BeanDefinition dependencyBeanDef = getBeanDefinition(dependency);
            if(dependencyBeanDef == null) {
                throw new RuntimeException("의존성을 찾을 수 없습니다: " + dependency.getName());
            }
            dependencyObjects[i] = resolveBean(dependencyBeanDef);
        }

        try {
            Constructor<?> constructor = getAllArgsConstructor(beanDefinition.getBeanClass());
            Object instance = constructor.newInstance(dependencyObjects);
            singletons.put(beanDefinition.getBeanName(), instance);
            return (T) instance;
        } catch(Exception e) {
            throw new RuntimeException("빈 생성에 실패했습니다.", e);
        }
    }

    private static Constructor<?> getAllArgsConstructor(Class<?> beanClass) {
        Constructor<?>[] constructors = beanClass.getDeclaredConstructors();
        if(constructors.length == 1) {
            return constructors[0];
        }

        if(constructors.length > 2) {
            throw new RuntimeException("생성자가 너무 많습니다.");
        }

        int parameteredConstructorCount = 0;
        for(Constructor<?> constructor : constructors) {
            if(constructor.getParameterCount() > 0) {
                parameteredConstructorCount++;
            }
        }
        if(parameteredConstructorCount > 1) {
            throw new RuntimeException("파라미터가 있는 생성자는 1개로 제한되어야 합니다.");
        }

        for(Constructor<?> constructor : constructors) {
            if(constructor.getParameterCount() > 0) {
                return constructor;
            }
        }

        return null;
    }

    private BeanDefinition getBeanDefinition(Class<?> dependency) {
        for(Entry<String, BeanDefinition> entry : beanDefinitions.entrySet()) {
            BeanDefinition beanDef = entry.getValue();
            if(dependency.isAssignableFrom(beanDef.getBeanClass())) {
                return beanDef;
            }
        }
        return null;
    }

    @Override
    public <T> List<T> getBeans(Class<T> clazz) {
        List<T> beans = new ArrayList<>();
        for(BeanDefinition beanDefinition : beanDefinitions.values()) {
            if(clazz.isAssignableFrom(beanDefinition.getBeanClass())) {
                beans.add(resolveBean(beanDefinition));
            }
        }
        return beans;
    }

}
