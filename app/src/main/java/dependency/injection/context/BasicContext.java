package dependency.injection.context;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BasicContext implements Context {

    private final Map<String, BeanDefinition> beanDefinitions = new HashMap<>();
    private final Map<String, Object> singletons = new HashMap<>();
    private final ThreadLocal<Set<String>> beansInCreation = ThreadLocal.withInitial(HashSet::new);

    @Override
    public <T> T getBean(String name, Class<T> clazz) {
        BeanDefinition beanDefinition = getBeanDefinitionByName(name);
        validateBeanType(beanDefinition, clazz);
        return getOrCreateSingleton(beanDefinition);
    }

    @Override
    public <T> T getBean(Class<T> clazz) {
        BeanDefinition beanDefinition = findUniqueBeanDefinitionByType(clazz);
        return getOrCreateSingleton(beanDefinition);
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
        return findBeanDefinitionsByType(type).stream()
            .map(this::<T>getOrCreateSingleton)
            .collect(Collectors.toMap(
                bean -> bean.getClass().getName(), // Consider a more robust naming strategy
                bean -> bean
            ));
    }

    @Override
    public void close() {
        singletons.clear();
        beanDefinitions.clear();
        beansInCreation.remove();
        System.out.println("Context closed. All beans destroyed.");
    }

    @SuppressWarnings("unchecked")
    private <T> T getOrCreateSingleton(BeanDefinition beanDefinition) {
        Object singleton = singletons.get(beanDefinition.getBeanName());
        if (singleton != null) {
            return (T) singleton;
        }
        return createBeanInstance(beanDefinition);
    }

    private <T> T createBeanInstance(BeanDefinition beanDefinition) {
        String beanName = beanDefinition.getBeanName();
        if (beansInCreation.get().contains(beanName)) {
            throw new RuntimeException("순환 참조가 감지되었습니다: " + beanName);
        }

        beansInCreation.get().add(beanName);
        try {
            Object[] dependencies = resolveDependencies(beanDefinition);
            T instance = instantiate(beanDefinition, dependencies);
            singletons.put(beanName, instance);
            return instance;
        } finally {
            beansInCreation.get().remove(beanName);
        }
    }

    private Object[] resolveDependencies(BeanDefinition beanDefinition) {
        return Arrays.stream(beanDefinition.getDependencies())
            .map(this::getBean)
            .toArray();
    }

    @SuppressWarnings("unchecked")
    private <T> T instantiate(BeanDefinition beanDefinition, Object[] dependencies) {
        try {
            Constructor<?> constructor = findSuitableConstructor(beanDefinition.getBeanClass(), dependencies);
            constructor.setAccessible(true);
            return (T) constructor.newInstance(dependencies);
        } catch (Exception e) {
            throw new RuntimeException("빈 생성에 실패했습니다: " + beanDefinition.getBeanClass().getName(), e);
        }
    }

    private Constructor<?> findSuitableConstructor(Class<?> beanClass, Object[] dependencies) {
        return Arrays.stream(beanClass.getDeclaredConstructors())
            .filter(c -> isConstructorMatching(c, dependencies))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("적합한 생성자를 찾을 수 없습니다: " + beanClass.getName()));
    }

    private boolean isConstructorMatching(Constructor<?> constructor, Object[] dependencies) {
        if (constructor.getParameterCount() != dependencies.length) {
            return false;
        }
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (!parameterTypes[i].isAssignableFrom(dependencies[i].getClass())) {
                return false;
            }
        }
        return true;
    }

    private BeanDefinition getBeanDefinitionByName(String name) {
        BeanDefinition beanDefinition = beanDefinitions.get(name);
        if (beanDefinition == null) {
            throw new RuntimeException("빈을 찾을 수 없습니다: " + name);
        }
        return beanDefinition;
    }

    private BeanDefinition findUniqueBeanDefinitionByType(Class<?> clazz) {
        List<BeanDefinition> matchingDefs = findBeanDefinitionsByType(clazz);
        if (matchingDefs.size() > 1) {
            throw new RuntimeException("해당 타입의 빈이 1개 이상 존재합니다: " + clazz.getName());
        }
        return matchingDefs.get(0);
    }

    private List<BeanDefinition> findBeanDefinitionsByType(Class<?> clazz) {
        List<BeanDefinition> matchingDefs = beanDefinitions.values().stream()
            .filter(bd -> clazz.isAssignableFrom(bd.getBeanClass()))
            .collect(Collectors.toList());
        if (matchingDefs.isEmpty()) {
            throw new RuntimeException("타입에 해당하는 빈을 찾을 수 없습니다: " + clazz.getName());
        }
        return matchingDefs;
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
