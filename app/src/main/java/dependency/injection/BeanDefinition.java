package dependency.injection;

public interface BeanDefinition {
    String getBeanName();
    Class<?> getBeanClass();
}
