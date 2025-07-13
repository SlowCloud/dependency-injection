package dependency.injection;

public interface Context {
    <E> E getBean(String name, Class<E> clazz);
    <E> E getBean(String name);
    <E> E getBean(Class<E> clazz);

    <E> void addBean(Class<E> clazz);
    <E> void addBean(String name, Class<E> clazz);
}
