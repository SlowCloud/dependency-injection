package dependency.injection.context;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class BasicContext implements Context {

    private final Map<String, BeanDefinition> beanDefinitions = new HashMap<>();
    private final Map<String, Object> singletons = new HashMap<>();

    @Override
    public <T> T getBean(String name, Class<T> clazz) {
        BeanDefinition beanDefinition = getBeanDefinitionByName(name);
        validateBeanType(beanDefinition, clazz);
        return resolveBean(beanDefinition);
    }

    @Override
    public <T> T getBean(String name) {
        BeanDefinition beanDefinition = getBeanDefinitionByName(name);
        return resolveBean(beanDefinition);
    }

    @Override
    public <T> T getBean(Class<T> clazz) {
        BeanDefinition beanDefinition = findBeanDefinitionByType(clazz);
        return resolveBean(beanDefinition);
    }

    @Override
    public <T> List<T> getBeans(Class<T> clazz) {
        List<T> beans = new ArrayList<>();
        for (BeanDefinition beanDefinition : beanDefinitions.values()) {
            if (clazz.isAssignableFrom(beanDefinition.getBeanClass())) {
                beans.add(resolveBean(beanDefinition));
            }
        }
        return beans;
    }

    @Override
    public <T> void addBean(Class<T> clazz) {
        addBean(clazz.getName(), clazz);
    }

    @Override
    public <T> void addBean(String name, Class<T> clazz) {
        BeanDefinition beanDefinition = new BasicBeanDefinition(clazz);
        validateBeanNameUniqueness(beanDefinition.getBeanName());
        beanDefinitions.put(beanDefinition.getBeanName(), beanDefinition);
    }

    private <T> T resolveBean(BeanDefinition beanDefinition) {
        if (beanDefinition == null) {
            throw new RuntimeException("빈 정의를 찾을 수 없습니다.");
        }

        // 싱글톤 캐시 확인
        if (singletons.containsKey(beanDefinition.getBeanName())) {
            return (T) singletons.get(beanDefinition.getBeanName());
        }

        // 의존성 해결
        Object[] dependencies = resolveDependencies(beanDefinition);

        // 인스턴스 생성
        T instance = createInstance(beanDefinition, dependencies);
        
        // 싱글톤으로 저장
        singletons.put(beanDefinition.getBeanName(), instance);
        
        return instance;
    }

    private Object[] resolveDependencies(BeanDefinition beanDefinition) {
        Class<?>[] dependencyClasses = beanDefinition.getDependencyClass();
        Object[] dependencies = new Object[dependencyClasses.length];
        
        for (int i = 0; i < dependencyClasses.length; i++) {
            Class<?> dependencyClass = dependencyClasses[i];
            BeanDefinition dependencyBeanDef = findBeanDefinitionByType(dependencyClass);
            
            if (dependencyBeanDef == null) {
                throw new RuntimeException("의존성을 찾을 수 없습니다: " + dependencyClass.getName());
            }
            
            dependencies[i] = resolveBean(dependencyBeanDef);
        }
        
        return dependencies;
    }

    private <T> T createInstance(BeanDefinition beanDefinition, Object[] dependencies) {
        try {
            Constructor<?> constructor = findSuitableConstructor(beanDefinition.getBeanClass());
            return (T) constructor.newInstance(dependencies);
        } catch (Exception e) {
            throw new RuntimeException("빈 생성에 실패했습니다: " + beanDefinition.getBeanClass().getName(), e);
        }
    }

    private Constructor<?> findSuitableConstructor(Class<?> beanClass) {
        Constructor<?>[] constructors = beanClass.getDeclaredConstructors();
        if (constructors.length == 1) {
            return constructors[0];
        }
        throw new RuntimeException("생성자는 1개만 존재하여야 합니다.");
    }

    private BeanDefinition getBeanDefinitionByName(String name) {
        BeanDefinition beanDefinition = beanDefinitions.get(name);
        if (beanDefinition == null) {
            throw new RuntimeException("빈을 찾을 수 없습니다: " + name);
        }
        return beanDefinition;
    }

    private BeanDefinition findBeanDefinitionByType(Class<?> clazz) {
        for (BeanDefinition beanDefinition : beanDefinitions.values()) {
            if (clazz.isAssignableFrom(beanDefinition.getBeanClass())) {
                return beanDefinition;
            }
        }
        throw new RuntimeException("타입에 해당하는 빈을 찾을 수 없습니다: " + clazz.getName());
    }

    private void validateBeanType(BeanDefinition beanDefinition, Class<?> expectedType) {
        if (!expectedType.isAssignableFrom(beanDefinition.getBeanClass())) {
            throw new RuntimeException("빈의 타입이 일치하지 않습니다. 예상: " + expectedType.getName() + 
                                     ", 실제: " + beanDefinition.getBeanClass().getName());
        }
    }

    private void validateBeanNameUniqueness(String beanName) {
        if (beanDefinitions.containsKey(beanName)) {
            throw new RuntimeException("동일한 빈이 이미 존재합니다: " + beanName);
        }
    }
}
