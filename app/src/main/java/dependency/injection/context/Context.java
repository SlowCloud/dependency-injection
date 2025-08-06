package dependency.injection.context;

import java.util.Map;

public interface Context extends AutoCloseable {
    <T> T getBean(String name, Class<T> clazz);
    <T> T getBean(Class<T> clazz);

    <T> void register(Class<T> clazz);
    <T> void register(String name, Class<T> clazz);

    <T> Map<String, T> getBeansOfType(Class<T> type);

    @Override
    void close();
}
