package dependency.injection.aop;

public interface ProxyFactory {
    void addInterceptor(Interceptor interceptor);
    Object getProxy();
}
