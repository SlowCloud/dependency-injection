package dependency.injection.aop;

import java.lang.reflect.Method;

public interface Interceptor {
    Object invoke(Method method);
}
