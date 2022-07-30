package common;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

public class MyInterceptor1 implements MethodInterceptor {
    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        Object o = proxy.invokeSuper(obj, args);
        if (o instanceof String) {
            return "interceptor1 " + o;
        } else {
            return o;
        }
    }
}
