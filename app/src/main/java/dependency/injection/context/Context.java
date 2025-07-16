package dependency.injection.context;

import java.util.List;

public interface Context {
    <T> T getBean(String name, Class<T> clazz);
    <T> T getBean(String name);
    <T> T getBean(Class<T> clazz);

    <T> void addBean(Class<T> clazz);
    <T> void addBean(String name, Class<T> clazz);
    <T> List<T> getBeans(Class<T> clazz);
}
