/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */
package net.sf.cglib.proxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

public class Delegator
implements ClassFileConstants
{
    private static final Class OBJECT_CLASS = Object.class;
    private static final String CLASS_NAME = "net.sf.cglib.proxy.Delegator$$CreatedByCGLIB$$";
    private static final String PARENT_CLASS = "java.lang.Object";
    private static final String PARENT_SIGNATURE = Type.getMethodSignature(Type.VOID, null);
    private static final Type[] EMPTY_TYPE_ARRAY = {};
    private static final String FIELD_NAME = "delegates";
    private static final Class ARG_CLASS = Object[].class;
    private static final Class[] ARG_CLASS_ARRAY = { ARG_CLASS };
    private static final Type ARG_TYPE = Type.getType(ARG_CLASS.getName());
    private static final Type[] ARG_TYPE_ARRAY = { ARG_TYPE };
    private static final String ARG_SIGNATURE = ARG_TYPE.getSignature();
    private static int index = 0;

    private static final Map factories = new HashMap();
    private static final Map infoCache = new HashMap();
    
    // Inner and private because if the makeDelegator(Object[]) constructor
    // was used, the required order of the delegates may be different from
    // what was originally specified, which would be confusion. There is
    // not much overhead associated with going through the cache, anyway.
    private interface Factory
    {
        public Object newInstance(Object[] delegates);
    }

    /**
     * Returns an object that implements all of the specified
     * interfaces. For each interface, all methods are delegated to the
     * respective object in the delegates argument array.
     * @param interfaces the array of interfaces to implement
     * @param delegates The array of delegates. Must be the same length
     * as the interface array, and each delegates must implements the
     * corresponding interface.
     * @return the dynamically created object
     */
    public static Object makeDelegator(Class[] interfaces, Object[] delegates)
    throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException
    {
        return makeDelegatorHelper(new ArrayKey(interfaces), interfaces, delegates);
    }

    /**
     * Returns an object that implements all of the interfaces
     * implemented by the specified objects. For each interface, all
     * methods are delegated to the first object in the argument array
     * which implements the interface.
     * <p>
     * <b>Note:</b> interfaces which have no methods (marker interfaces)
     * are not implemented by the returned object.
     * @param delegates the array of delegates
     * @return the dynamically created object
     * @see #getInterfaceMap(Object[])
     */
    public static Object makeDelegator(Object[] delegates)
    throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException
    {
        Info info = getInfo(delegates);
        Object[] remapped = new Object[info.interfaces.length];
        for (int i = 0; i < remapped.length; i++) {
            remapped[i] = delegates[info.indexes[i]];
        }
        return makeDelegatorHelper(info.key, info.interfaces, remapped);
    }

    /**
     * Returns a Map that describes how interfaces would be delegated.
     * The keys are interfaces (Class objects) that would be implemented
     * by the object returned by <code>makeDelegator(delegates)</code>. For each
     * interface, the Map value is the objects from the argument array
     * that would be used as a delegate.
     * @param delegates the array of delegates
     * @return the Map of interfaces -> delegates
     * @see #makeDelegator(Object[])
     */
    public static Map getInterfaceMap(Object[] delegates)
    {
        Info info = getInfo(delegates);
        Map map = new HashMap();
        for (int i = 0; i < info.interfaces.length; i++) {
            map.put(info.interfaces[i], delegates[info.indexes[i]]);
        }
        return map;
    }

    synchronized private static Info getInfo(Object[] delegates)
    {
        Object key = new ClassesKey(delegates);
        Info info = (Info)infoCache.get(key);
        if (info == null)
            infoCache.put(key, info = new Info(delegates));
        return info;
    }

    synchronized private static Object makeDelegatorHelper(ArrayKey key, Class[] interfaces, Object[] delegates)
    throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException
    {
        Object factory = (Factory)factories.get(key);
        if (factory == null) {
            Class clazz = makeDelegatorClass(interfaces);
            factory = clazz.getConstructor(ARG_CLASS_ARRAY).newInstance(new Object[]{ delegates });
            factories.put(key, factory);
            return factory;
        } else {
            return ((Factory)factory).newInstance(delegates);
        }
    }

    private static Class makeDelegatorClass(Class[] interfaces)
    throws NoSuchMethodException    
    {
        ClassGen cg = getClassGen(interfaces);
        int fieldref = addField(cg);
        int cstructref = addConstructor(cg, fieldref);
        ConstantPoolGen cp = cg.getConstantPool();
        for (int i = 0; i < interfaces.length; i++) {
            Class iface = interfaces[i];
            int ifaceref = cp.addClass(iface.getName());
            if (!iface.isInterface())
                throw new IllegalArgumentException(iface + " is not an interface");
            Method[] methods = iface.getMethods();
            for (int j = 0; j < methods.length; j++) {
                addProxy(cg, methods[j], ifaceref, fieldref, i);
            }
        }
        addFactory(cg, cstructref);
        return ClassFileUtils.defineClass(Thread.currentThread().getContextClassLoader(),
                                          cg.getClassName(),
                                          cg.getJavaClass().getBytes());
    }

    private static ClassGen getClassGen(Class[] interfaces)
    {
        ClassGen cg = new ClassGen(CLASS_NAME + index++,
                                   PARENT_CLASS,
                                   SOURCE_FILE,
                                   ACC_PUBLIC | ACC_FINAL,
                                   null);
        for (int i = 0; i < interfaces.length; i++) {
            cg.addInterface(interfaces[i].getName());
        }
        cg.addInterface(Factory.class.getName());
        return cg;
    }

    private static int addField(ClassGen cg)
    {
        ConstantPoolGen cp = cg.getConstantPool();
        FieldGen fg = new FieldGen(ACC_PRIVATE, ARG_TYPE, FIELD_NAME, cp);
        cg.addField(fg.getField());
        return cp.addFieldref(cg.getClassName(), FIELD_NAME, ARG_SIGNATURE);
    }

    private static int addConstructor(ClassGen cg, int fieldref)
    {
        InstructionList il = new InstructionList();
        ConstantPoolGen cp = cg.getConstantPool();
        MethodGen cstruct =
            new MethodGen(ACC_PUBLIC,
                          Type.VOID,
                          ARG_TYPE_ARRAY, null,
                          CONSTRUCTOR_NAME, cg.getClassName(),
                          il, cp);
        int cstructref = cp.addMethodref(cstruct);
        int superref = cp.addMethodref(PARENT_CLASS, CONSTRUCTOR_NAME, PARENT_SIGNATURE);
        il.append(new ALOAD(0));
        il.append(new INVOKESPECIAL(superref));
        il.append(new ALOAD(0));
        il.append(new ALOAD(1));
        il.append(new PUTFIELD(fieldref));
        il.append(new RETURN());
        cg.addMethod(ClassFileUtils.getMethod(cstruct));
        return cstructref;
    }

    private static void addProxy(ClassGen cg, Method method, int ifaceref, int fieldref, int arrayref)
    {
        InstructionList il = new InstructionList();
        ConstantPoolGen cp = cg.getConstantPool();
        MethodGen mg = ClassFileUtils.toMethodGen(method, cg.getClassName(), il, cp);
        Type[] args = mg.getArgumentTypes();
        il.append(new ALOAD(0));
        il.append(new GETFIELD(fieldref));
        pushEfficientInt(il, cp, arrayref);
        il.append(new AALOAD());
        il.append(new CHECKCAST(ifaceref));
        int pos = 1;
        for (int i = 0; i < args.length; i++) {
            pos = ClassFileUtils.loadArg(il, args[i], pos);
        }
        int methodRef = cp.addInterfaceMethodref(method.getDeclaringClass().getName(), mg.getName(), mg.getSignature());
        il.append(new INVOKEINTERFACE(methodRef, args.length + 1));
        il.append(ClassFileUtils.newReturn(mg.getReturnType()));
        cg.addMethod(ClassFileUtils.getMethod(mg));
    }

    private static void addFactory(ClassGen cg, int cstructref)
    throws NoSuchMethodException    
    {
        InstructionList il = new InstructionList();
        ConstantPoolGen cp = cg.getConstantPool();
        Method m = Factory.class.getMethod("newInstance", ARG_CLASS_ARRAY);
        MethodGen newInstance = ClassFileUtils.toMethodGen(m, cg.getClassName(), il, cp);
        il.append(new NEW(cp.addClass(cg.getClassName())));
        il.append(new DUP());
        il.append(new ALOAD(1));
        il.append(new INVOKESPECIAL(cstructref));
        il.append(new ARETURN());
        cg.addMethod(ClassFileUtils.getMethod(newInstance));
    }

    private static void pushEfficientInt(InstructionList il, ConstantPoolGen cp, int value)
    {
        if (value < -1) {
            il.append(new LDC(cp.addInteger(value)));
        } else if (value <= 5) {
            il.append(new ICONST(value));
        } else if (value <= Byte.MAX_VALUE) {
            il.append(new BIPUSH((byte)value));
        } else if (value <= Short.MAX_VALUE) {
            il.append(new SIPUSH((short)value));
        } else {
            il.append(new LDC(cp.addInteger(value)));
        }
    }

    private static class ArrayKey
    {
        private Object[] array;
        private int hash = 23; // should be odd
    
        public ArrayKey(Object[] array)
        {
            this.array = array;
            for (int i = 0; i < array.length; i++) {
                hash = hash * 57 + filter(array[i]).hashCode();
            }
        }

        public int hashCode()
        {
            return hash;
        }

        protected Object filter(Object obj)
        {
            return obj;
        }

        public boolean equals(Object obj)
        {
            ArrayKey other = (ArrayKey)obj;
            if (array.length != other.array.length)
                return false;
            for (int i = 0; i < array.length; i++) {
                if (!filter(array[i]).equals(filter(other.array[i])))
                    return false;
            }
            return true;
        }
    }

    private static class ClassesKey
    extends ArrayKey
    {
        public ClassesKey(Object[] array)
        {
            super(array);
        }

        protected Object filter(Object obj)
        {
            return obj.getClass();
        }
    }

    private static class Info
    {
        private Class[] interfaces;
        private int[] indexes;
        private ArrayKey key;

        public Info(Object[] incoming)
        {
            Set seenInterfaces = new HashSet();
            List interfaceList = new LinkedList();
            List indexList = new LinkedList();
            for (int i = 0; i < incoming.length; i++) {
                Object delegate = incoming[i];
                Class[] delegateInterfaces = getAllInterfaces(delegate.getClass());
                for (int j = 0; j < delegateInterfaces.length; j++) {
                    Class iface = delegateInterfaces[j];
                    // get first superinterface that has declared methods
                    // this weeds out interfaces with no methods and helps caching
                    while (iface != null && iface.getDeclaredMethods().length == 0) {
                        iface = iface.getSuperclass();
                    }
                    if (iface != null && !seenInterfaces.contains(iface)) {
                        interfaceList.add(iface);
                        indexList.add(new Integer(i));
                        seenInterfaces.add(iface);
                    }
                }
            }
            interfaces = (Class[])interfaceList.toArray(new Class[interfaceList.size()]);
            indexes = new int[interfaces.length];
            Iterator it = indexList.iterator();
            for (int i = 0; it.hasNext(); i++) {
                indexes[i] = ((Integer)it.next()).intValue();
            }
            key = new ArrayKey(interfaces);
        }
    }

    private static Class[] getAllInterfaces(Class clazz)
    {
        List interfaces = new ArrayList();
        while (!clazz.equals(OBJECT_CLASS)) {
            interfaces.addAll(Arrays.asList(clazz.getInterfaces()));
            clazz = clazz.getSuperclass();
        }
        return (Class[])interfaces.toArray(new Class[interfaces.size()]);
    }
}    