package dependency.injection;

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

}
