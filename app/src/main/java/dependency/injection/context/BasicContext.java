package dependency.injection.context;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
    public <T> T getBean(Class<T> clazz) {
        BeanDefinition beanDefinition = findBeanDefinitionByType(clazz);
        return resolveBean(beanDefinition);
    }

    @Override
    public <T> void register(Class<T> clazz) {
        register(clazz.getName(), clazz);
    }

    @Override
    public <T> void register(String name, Class<T> clazz) {
        validateBeanNameUniqueness(name);
        BeanDefinition beanDefinition = new BasicBeanDefinition(name, clazz);
        beanDefinitions.put(beanDefinition.getBeanName(), beanDefinition);
    }

    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> type) {
        return beanDefinitions.values().stream()
            .filter(bd -> type.isAssignableFrom(bd.getBeanClass()))
            .map(this::<T>resolveBean)
            .collect(Collectors.toMap(
                bean -> bean.getClass().getName(), // Or some other naming strategy
                bean -> bean
            ));
    }

    @Override
    public void close() {
        // For example, call destroy methods on disposable beans
        singletons.clear();
        beanDefinitions.clear();
        System.out.println("Context closed. All beans destroyed.");
    }

    private <T> T resolveBean(BeanDefinition beanDefinition) {
        if (beanDefinition == null) {
            throw new RuntimeException("빈 정의를 찾을 수 없습니다.");
        }

        // 싱글톤 캐시 확인
        Object singleton = singletons.get(beanDefinition.getBeanName());
        if (singleton != null) {
            return (T) singleton;
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
        Class<?>[] dependencyClasses = beanDefinition.getDependencies();
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
            // Use the constructor that matches the dependencies
            Constructor<?> constructor = findSuitableConstructor(beanDefinition.getBeanClass(), dependencies);
            constructor.setAccessible(true); // Allow access to non-public constructors
            return (T) constructor.newInstance(dependencies);
        } catch (Exception e) {
            throw new RuntimeException("빈 생성에 실패했습니다: " + beanDefinition.getBeanClass().getName(), e);
        }
    }

    private Constructor<?> findSuitableConstructor(Class<?> beanClass, Object[] dependencies) {
        Class<?>[] dependencyTypes = Arrays.stream(dependencies)
            .map(Object::getClass)
            .toArray(Class<?>[]::new);

        return Arrays.stream(beanClass.getDeclaredConstructors())
            .filter(constructor -> {
                if (constructor.getParameterCount() != dependencies.length) {
                    return false;
                }
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                for (int i = 0; i < parameterTypes.length; i++) {
                    // Check for assignable types (e.g., interface implementation)
                    if (!parameterTypes[i].isAssignableFrom(dependencyTypes[i])) {
                        return false;
                    }
                }
                return true;
            })
            .findFirst()
            .orElseThrow(() -> new RuntimeException("적합한 생성자를 찾을 수 없습니다: " + beanClass.getName() +
                " with dependencies " + Arrays.toString(dependencyTypes)));
    }


    private BeanDefinition getBeanDefinitionByName(String name) {
        BeanDefinition beanDefinition = beanDefinitions.get(name);
        if (beanDefinition == null) {
            throw new RuntimeException("빈을 찾을 수 없습니다: " + name);
        }
        return beanDefinition;
    }

    private BeanDefinition findBeanDefinitionByType(Class<?> clazz) {
        return beanDefinitions.values().stream()
            .filter(bd -> clazz.isAssignableFrom(bd.getBeanClass()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("타입에 해당하는 빈을 찾을 수 없습니다: " + clazz.getName()));
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
