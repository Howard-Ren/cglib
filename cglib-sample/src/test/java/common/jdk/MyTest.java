package common.jdk;

import common.MyInterface;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Proxy;

public class MyTest {

    @Test
    public void test() {
        MyInterface target = () -> "target";

        Object o = Proxy.newProxyInstance(MyTest.class.getClassLoader(),
                new Class[]{MyInterface.class},
                (proxy, method, args) -> {
                    Object result = method.invoke(target, args);
                    if (result instanceof String) {
                        return "invoke " + result;
                    }
                    return result;
                });

        MyInterface proxy = (MyInterface) o;
        String result = proxy.interfaceTest();
        System.out.println(proxy.getClass());
        Assert.assertEquals("invoke target", result);
    }
}
