package dependency.injection;

import java.lang.reflect.Constructor;
import java.util.HashMap;

public class BasicContext implements Context {

    HashMap<String, BeanDefinition> beanDefinitions = new HashMap<>();

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
        try {
            Class<?> beanClass = beanDefinition.getBeanClass();
            Constructor<?> constructor = beanClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (T) constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("빈 생성에 실패했습니다.");
        }
    }

}
