package dependency.injection.context;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BasicBeanDefinition implements BeanDefinition {

    private final Class<?> clazz;
    private final String name;

    public BasicBeanDefinition(Class<?> clazz) {
        this(clazz.getName(), clazz);
    }

    public BasicBeanDefinition(String name, Class<?> clazz) {
        this.name = name;
        this.clazz = clazz;
    }

    @Override
    public String getBeanName() {
        return name;
    }

    @Override
    public Class<?> getBeanClass() {
        return clazz;
    }

    @Override
    public Class<?>[] getDependencies() {
        Constructor<?>[] constructors = clazz.getConstructors();
        int cnt = 0;
        for(var constructor : constructors) {
            if(constructor.getParameterTypes().length > 0) cnt++;
        }
        if(cnt > 1) throw new RuntimeException("인자가 있는 생성자는 1개만 존재하여야 합니다.");
        for(var constructor : constructors) {
            if(constructor.getParameterTypes().length > 0) return constructor.getParameterTypes();
        }
        return new Class<?>[0];
    }

}
