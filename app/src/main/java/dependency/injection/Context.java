package dependency.injection;

public interface Context {
    <T> T getBean(String name, Class<T> clazz);
    <T> T getBean(String name);
    <T> T getBean(Class<T> clazz);

    <T> void addBean(Class<T> clazz);
    <T> void addBean(String name, Class<T> clazz);
}
