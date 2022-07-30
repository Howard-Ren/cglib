package common.cglib;

import common.MyInterceptor1;
import common.MyInterceptor2;
import common.MySuperClass;
import net.sf.cglib.core.DebuggingClassWriter;
import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;
import net.sf.cglib.proxy.NoOp;
import org.junit.Assert;
import org.junit.Test;

public class MyTest {

    static {
        System.setProperty("cglib.useCache", "false");
    }

    @Test
    public void testCreateWithSetCallback() {
        Enhancer enhancer = new Enhancer();

        enhancer.setSuperclass(MySuperClass.class);

        enhancer.setCallback(new MyInterceptor1());
        MySuperClass mySuperClass = (MySuperClass) enhancer.create();

        String test1 = mySuperClass.test1();
        Assert.assertEquals("interceptor1 test1", test1);
        String test2 = mySuperClass.test2();
        Assert.assertEquals("interceptor1 test2", test2);
        String test3 = mySuperClass.test3();
        Assert.assertEquals("interceptor1 test3", test3);
    }

    @Test
    public void testCreateWithSetCallBacks() {
        Enhancer enhancer = new Enhancer();

        enhancer.setSuperclass(MySuperClass.class);
        Callback[] callbacks = new Callback[]{new MyInterceptor1()};
        enhancer.setCallbacks(callbacks);
        enhancer.setCallbackFilter(method -> 0);

        MySuperClass mySuperClass = (MySuperClass) enhancer.create();
        String test1 = mySuperClass.test1();
        Assert.assertEquals("interceptor1 test1", test1);
        String test2 = mySuperClass.test2();
        Assert.assertEquals("interceptor1 test2", test2);
        String test3 = mySuperClass.test3();
        Assert.assertEquals("interceptor1 test3", test3);
    }

    @Test
    public void testCreateWithSetCallBackType() {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(MySuperClass.class);
        enhancer.setCallbackType(MyInterceptor1.class);
        try {
            enhancer.create();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("Callbacks are required", e.getMessage());
        }
    }

    @Test
    public void testCreateWithSetMultiCallBacks() {
        Enhancer enhancer = new Enhancer();

        enhancer.setSuperclass(MySuperClass.class);
        Callback[] callbacks = new Callback[]{
                new MyInterceptor1(),
                new MyInterceptor2(),
                new NoOp() {
                }};
        enhancer.setCallbacks(callbacks);
        enhancer.setCallbackFilter(method -> {
            if (method.getName().equals("test1")) {
                return 0;
            } else if (method.getName().equals("test2")) {
                return 1;
            } else {
                return 2;
            }
        });

        MySuperClass mySuperClass = (MySuperClass) enhancer.create();
        String test1 = mySuperClass.test1();
        Assert.assertEquals("interceptor1 test1", test1);
        String test2 = mySuperClass.test2();
        Assert.assertEquals("interceptor2 test2", test2);
        String test3 = mySuperClass.test3();
        Assert.assertEquals("test3", test3);
    }

    @Test
    public void testCreateClassWithSetCallBack() {
        Enhancer enhancer = new Enhancer();

        enhancer.setSuperclass(MySuperClass.class);
        enhancer.setCallback(new MyInterceptor1());

        try {
            enhancer.createClass();
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("createClass does not accept callbacks", e.getMessage());
        }
    }

    @Test
    public void testCreateClassWithSetCallBackType() throws InstantiationException, IllegalAccessException {
        Enhancer enhancer = new Enhancer();

        enhancer.setSuperclass(MySuperClass.class);
        enhancer.setCallbackType(MyInterceptor1.class);

        Class aClass = enhancer.createClass();
        Object o = aClass.newInstance();
        MySuperClass mySuperClass = (MySuperClass) o;
        String test1 = mySuperClass.test1();
        Assert.assertEquals("test1", test1);
        String test2 = mySuperClass.test2();
        Assert.assertEquals("test2", test2);
        String test3 = mySuperClass.test3();
        Assert.assertEquals("test3", test3);
    }

    @Test
    public void testCreateClassWithSetCallBackTypeThenSetCallBack() throws InstantiationException, IllegalAccessException {
        Enhancer enhancer = new Enhancer();

        enhancer.setSuperclass(MySuperClass.class);
        enhancer.setCallbackType(MyInterceptor1.class);

        Class aClass = enhancer.createClass();
        Object o = aClass.newInstance();
        Factory factory = (Factory) o;
        factory.setCallbacks(new Callback[]{new MyInterceptor1()});
        MySuperClass mySuperClass = (MySuperClass) o;
        String test1 = mySuperClass.test1();
        Assert.assertEquals("interceptor1 test1", test1);
        String test3 = mySuperClass.test3();
        Assert.assertEquals("interceptor1 test3", test3);
    }

    @Test
    public void testCreateClassWithSetCallBackTypeThenRegisterStaticCallbacks() throws InstantiationException, IllegalAccessException {
        Enhancer enhancer = new Enhancer();

        enhancer.setSuperclass(MySuperClass.class);
        enhancer.setCallbackType(MyInterceptor1.class);

        Class aClass = enhancer.createClass();
        Enhancer.registerStaticCallbacks(aClass, new Callback[]{new MyInterceptor1()});
        Object o = aClass.newInstance();
        MySuperClass mySuperClass = (MySuperClass) o;
        String test1 = mySuperClass.test1();
        Assert.assertEquals("interceptor1 test1", test1);
        String test3 = mySuperClass.test3();
        Assert.assertEquals("interceptor1 test3", test3);
    }

    @Test
    public void testCreateClassWithSetCallBackTypeThenRegisterCallbacks() throws InstantiationException, IllegalAccessException {
        Enhancer enhancer = new Enhancer();

        enhancer.setSuperclass(MySuperClass.class);
        enhancer.setCallbackType(MyInterceptor1.class);

        Class aClass = enhancer.createClass();
        Enhancer.registerCallbacks(aClass, new Callback[]{new MyInterceptor1()});
        Object o = aClass.newInstance();
        MySuperClass mySuperClass = (MySuperClass) o;
        String test1 = mySuperClass.test1();
        Assert.assertEquals("interceptor1 test1", test1);
        String test3 = mySuperClass.test3();
        Assert.assertEquals("interceptor1 test3", test3);
    }

    @Test
    public void test() {
        System.setProperty(DebuggingClassWriter.DEBUG_LOCATION_PROPERTY, "D:/cglib");
        Enhancer enhancer = new Enhancer();

        enhancer.setSuperclass(MySuperClass.class);

        enhancer.setCallback(new MyInterceptor1());
        MySuperClass mySuperClass = (MySuperClass) enhancer.create();

        String test1 = mySuperClass.test1();
        Assert.assertEquals("interceptor1 test1", test1);
    }
}
