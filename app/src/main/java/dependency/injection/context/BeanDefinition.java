package dependency.injection.context;

public interface BeanDefinition {
    String getBeanName();
    Class<?> getBeanClass();
    Class<?>[] getDependencyClass();
}
