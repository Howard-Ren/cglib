/*
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
 */
package net.sf.cglib.core;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.CodeVisitor;

/**
 * @author Juozas Baliuka, Chris Nokleberg
 */
abstract public class Emitter {
    private String className;
    private Class superclass;

    private String methodName;
    private Class returnType;
    private Class[] parameterTypes;
    private boolean isStatic;
    private int nextLocal;

    private Map fieldInfo = new HashMap();
    private Block curBlock;
    private Map finalizeCallbacks = new HashMap();

    private ClassVisitor classv;
    private CodeVisitor codev;

//     public Emitter(String className, Class superclass) {
//         this.className = className;
//         this.superclass = superclass;
//     }

    public Emitter() {
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public Class getSuperclass() {
        return superclass;
    }
    
    public void setSuperclass(Class superclass) {
        if (superclass == null) {
            superclass = Object.class;
        }
        this.superclass = superclass;
    }

    public void setClassVisitor(ClassVisitor v) {
        classv = v;
    }

    public void setCodeVisitor(CodeVisitor v) {
        codev = v;
    }

    // TODO: SOURCE_FILE argument?
    public void begin_class(int modifiers,
                            String className,
                            Class superclass,
                            Class[] interfaces) {
        setClassName(className);
        setSuperclass(superclass);
        classv.visit(modifiers,
                     getInternalName(getClassName()),
                     getInternalName(getSuperclass()),
                     getInternalNames(interfaces),
                     Constants.SOURCE_FILE);
    }

    public void end_class() {
        // TODO: any checking?
        for (Iterator it = finalizeCallbacks.values().iterator(); it.hasNext();) {
            ((FinalizeCallback)it.next()).process();
        }
        classv.visitEnd();
        classv = null;
    }

//     private Class generate(ClassLoader loader) {
//         String className = getClassName();
//         ClassWriter cw = new ClassWriter();
//         cw.visit(Modifier.PUBLIC,
//                  ReflectUtils.getInternalName(className),
//                  ReflectUtils.getInternalName(superclass),
//                  ReflectUtils.getInternalNames(interfaces),
//                  Constants.SOURCE_FILE);
//         accept(cw);
//         cw.visitEnd();
//         byte[] b = cw.toByteArray();
//         return defineClass(source.defineClass, className, b, loader);
//     }
    

    // TODO: what if end_class will not be called?
    public void register(Object key, FinalizeCallback callback) {
        finalizeCallbacks.put(key, callback);
    }

//     public byte[] getBytes() throws Exception {
//         for (Iterator it = finalizeCallbacks.values().iterator(); it.hasNext();) {
//             ((FinalizeCallback)it.next()).process();
//         }
//         return this.getBytes();
//     }

    public Class[] getParameterTypes() {
        return parameterTypes;
    }

//     public Class getReturnType() {
//         return returnType;
//     }
    
    public void begin_method(int modifiers, Class returnType, String methodName,
                             Class[] parameterTypes, Class[] exceptionTypes) {
        this.methodName = methodName;
        this.returnType = returnType;
        this.parameterTypes = parameterTypes;
        isStatic = Modifier.isStatic(modifiers);

        codev = classv.visitMethod(modifiers, // ASM modifier constants are same as JDK
                                   methodName,
                                   getInternalName(ReflectUtils.getMethodDescriptor(returnType, parameterTypes)),
                                   getInternalNames(exceptionTypes));
        setNextLocal();
    }
    
    public void begin_method(Method method) {
        begin_method(method, ReflectUtils.getDefaultModifiers(method));
    }
    
    public void begin_method(Method method, int modifiers) {
        begin_method(modifiers, method.getReturnType(), method.getName(),
        method.getParameterTypes(), method.getExceptionTypes());
    }
    
    public void begin_constructor(Constructor constructor) {
        begin_constructor(constructor.getParameterTypes());
    }
    
    public void begin_constructor() {
        begin_constructor(Constants.TYPES_EMPTY);
    }
    
    public void begin_constructor(Class[] parameterTypes) {
        this.returnType = Void.TYPE;
        this.parameterTypes = parameterTypes;

        codev = classv.visitMethod(Modifier.PUBLIC,
                            Constants.CONSTRUCTOR_NAME,
                            getInternalName(ReflectUtils.getMethodDescriptor(Void.TYPE, parameterTypes)),
                            null);

        setNextLocal();
    }
    
    public void begin_static() {
        this.returnType = Void.TYPE;
        this.parameterTypes = Constants.TYPES_EMPTY;

        codev = classv.visitMethod(Modifier.STATIC,
                            Constants.STATIC_NAME,
                            getInternalName(ReflectUtils.getMethodDescriptor(Void.TYPE, Constants.TYPES_EMPTY)),
                            null);
        setNextLocal();
    }

    private void setNextLocal() {
        nextLocal = getLocalOffset() + getStackSize(parameterTypes);
    }
    
    public void end_method() {
        codev.visitMaxs(0, 0); // values are ignored
        
        parameterTypes = null;
        returnType = null;
        methodName = null;
        if (curBlock != null) {
            throw new IllegalStateException("unclosed exception block");
        }
    }

    public Block begin_block() {
        Block newBlock = new Block(curBlock, mark());
        curBlock = newBlock;
        return curBlock;
    }

    public void end_block() {
        if (curBlock == null) {
            throw new IllegalStateException("mismatched block boundaries");
        }
        curBlock.setEnd(mark());
        curBlock = curBlock.getParent();
    }
    
    public void catch_exception(Block block, Class exceptionType) {
        // TODO
        // throw new IllegalArgumentException("block belongs to a different method");

        if (block.getEnd() == null) {
            throw new IllegalStateException("end of block is unset");
        }
        codev.visitTryCatchBlock(convertLabel(block.getStart()),
                              convertLabel(block.getEnd()),
                              convertLabel(mark()),
                              getInternalName(exceptionType));
    }
    
    public void ifeq(Label label) { emit(Opcodes.IFEQ, label); }
    public void ifne(Label label) { emit(Opcodes.IFNE, label); }
    public void iflt(Label label) { emit(Opcodes.IFLT, label); }
    public void ifge(Label label) { emit(Opcodes.IFGE, label); }
    public void ifgt(Label label) { emit(Opcodes.IFGT, label); }
    public void ifle(Label label) { emit(Opcodes.IFLE, label); }
    public void goTo(Label label) { emit(Opcodes.GOTO, label); }
    public void ifnull(Label label) { emit(Opcodes.IFNULL, label); }
    public void ifnonnull(Label label) { emit(Opcodes.IFNONNULL, label); }
    public void if_icmplt(Label label) { emit(Opcodes.IF_ICMPLT, label); }
    public void if_icmpgt(Label label) { emit(Opcodes.IF_ICMPGT, label); }
    public void if_icmpne(Label label) { emit(Opcodes.IF_ICMPNE, label); }
    public void if_icmpeq(Label label) { emit(Opcodes.IF_ICMPEQ, label); }
    public void if_acmpeq(Label label) { emit(Opcodes.IF_ACMPEQ, label); }
    public void if_acmpne(Label label) { emit(Opcodes.IF_ACMPNE, label); }

    public void pop() { emit(Opcodes.POP); }
    public void pop2() { emit(Opcodes.POP2); }
    public void dup() { emit(Opcodes.DUP); }
    public void dup2() { emit(Opcodes.DUP2); }
    public void dup_x1() { emit(Opcodes.DUP_X1); }
    public void dup_x2() { emit(Opcodes.DUP_X2); }
    public void swap() { emit(Opcodes.SWAP); }
    public void aconst_null() { emit(Opcodes.ACONST_NULL); }

    public void monitorenter() { emit(Opcodes.MONITORENTER); }
    public void monitorexit() { emit(Opcodes.MONITOREXIT); }

    public void if_cmpeq(Class type, Label label) {
        cmpHelper(type, label, Opcodes.IF_ICMPEQ, Opcodes.IFEQ);
    }
    public void if_cmpne(Class type, Label label) {
        cmpHelper(type, label, Opcodes.IF_ICMPNE, Opcodes.IFNE);
    }
    public void if_cmplt(Class type, Label label) {
        cmpHelper(type, label, Opcodes.IF_ICMPLT, Opcodes.IFLT);
    }
    public void if_cmpgt(Class type, Label label) {
        cmpHelper(type, label, Opcodes.IF_ICMPGT, Opcodes.IFGT);
    }
    
    private void cmpHelper(Class type, Label label, int intOp, int numOp) {
        if (type == Long.TYPE) {
            emit(Opcodes.LCMP);
        } else if (type == Double.TYPE) {
            emit(Opcodes.DCMPG);
        } else if (type == Float.TYPE) {
            emit(Opcodes.FCMPG);
        } else {
            emit(intOp, label);
            return;
        }
        emit(numOp, label);
    }

    public void add(Class type) {
        if (type == Long.TYPE) {
            emit(Opcodes.LADD);
        } else if (type == Double.TYPE) {
            emit(Opcodes.DADD);
        } else if (type == Float.TYPE) {
            emit(Opcodes.FADD);
        } else if (type == Integer.TYPE) {
            emit(Opcodes.IADD);
        }
    }

    public void mul(Class type) {
        if (type == Long.TYPE) {
            emit(Opcodes.LMUL);
        } else if (type == Double.TYPE) {
            emit(Opcodes.DMUL);
        } else if (type == Float.TYPE) {
            emit(Opcodes.FMUL);
        } else {
            emit(Opcodes.IMUL);
        }
    }

    public void xor(Class type) {
        if (type == Long.TYPE) {
            emit(Opcodes.LXOR);
        } else {
            emit(Opcodes.IXOR);
        }
    }

    public void ushr(Class type) {
        if (type == Long.TYPE) {
            emit(Opcodes.LUSHR);
        } else {
            emit(Opcodes.IUSHR);
        }
    }

    public void sub(Class type) {
        if (type == Long.TYPE) {
            emit(Opcodes.LSUB);
        } else if (type == Double.TYPE) {
            emit(Opcodes.DSUB);
        } else if (type == Float.TYPE) {
            emit(Opcodes.FSUB);
        } else {
            emit(Opcodes.ISUB);
        }
    }

    public void div(Class type) {
        if (type == Long.TYPE) {
            emit(Opcodes.LDIV);
        } else if (type == Double.TYPE) {
            emit(Opcodes.DDIV);
        } else if (type == Float.TYPE) {
            emit(Opcodes.FDIV);
        } else {
            emit(Opcodes.IDIV);
        }
    }

    public void neg(Class type) {
        if (type == Long.TYPE) {
            emit(Opcodes.LNEG);
        } else if (type == Double.TYPE) {
            emit(Opcodes.DNEG);
        } else if (type == Float.TYPE) {
            emit(Opcodes.FNEG);
        } else {
            emit(Opcodes.INEG);
        }
    }

    public void rem(Class type) {
        if (type == Long.TYPE) {
            emit(Opcodes.LREM);
        } else if (type == Double.TYPE) {
            emit(Opcodes.DREM);
        } else if (type == Float.TYPE) {
            emit(Opcodes.FREM);
        } else {
            emit(Opcodes.IREM);
        }
    }

    public void and(Class type) {
        if (type == Long.TYPE) {
            emit(Opcodes.LAND);
        } else {
            emit(Opcodes.IAND);
        }
    }
    
    public void or(Class type) {
        if (type == Long.TYPE) {
            emit(Opcodes.LOR);
        } else {
            emit(Opcodes.IOR);
        }
    }

    /**
     * Casts from one primitive numeric type to another
     */
    public void cast_numeric(Class from, Class to) {
        if (from != to) {
            if (from == Double.TYPE) {
                if (to == Float.TYPE) {
                    emit(Opcodes.D2F);
                } else if (to == Long.TYPE) {
                    emit(Opcodes.D2L);
                } else {
                    emit(Opcodes.D2I);
                    cast_numeric(Integer.TYPE, to);
                }
            } else if (from == Float.TYPE) {
                if (to == Double.TYPE) {
                    emit(Opcodes.F2D);
                } else if (to == Long.TYPE) {
                    emit(Opcodes.F2L);
                } else {
                    emit(Opcodes.F2I);
                    cast_numeric(Integer.TYPE, to);
                }
            } else if (from == Long.TYPE) {
                if (to == Double.TYPE) {
                    emit(Opcodes.L2D);
                } else if (to == Float.TYPE) {
                    emit(Opcodes.L2F);
                } else {
                    emit(Opcodes.L2I);
                    cast_numeric(Integer.TYPE, to);
                }
            } else {
                if (to == Byte.TYPE) {
                    emit(Opcodes.I2B);
                } else if (to == Character.TYPE) {
                    emit(Opcodes.I2C);
                } else if (to == Double.TYPE) {
                    emit(Opcodes.I2D);
                } else if (to == Float.TYPE) {
                    emit(Opcodes.I2F);
                } else if (to == Long.TYPE) {
                    emit(Opcodes.I2L);
                } else if (to == Short.TYPE) {
                    emit(Opcodes.I2S);
                }
            }
        }
    }

    public void push(int i) {
        if (i < -1) {
            emit_ldc(new Integer(i));
        } else if (i <= 5) {
            emit(Opcodes.iconst(i));
        } else if (i <= Byte.MAX_VALUE) {
            emit_int(Opcodes.BIPUSH, i);
        } else if (i <= Short.MAX_VALUE) {
            emit_int(Opcodes.SIPUSH, i);
        } else {
            emit_ldc(new Integer(i));
        }
    }
    
    public void push(long value) {
        if (value == 0L || value == 1L) {
            emit(Opcodes.lconst(value));
        } else {
            emit_ldc(new Long(value));
        }
    }
    
    public void push(float value) {
        if (value == 0f || value == 1f || value == 2f) {
            emit(Opcodes.fconst(value));
        } else {
            emit_ldc(new Float(value));
        }
    }
    public void push(double value) {
        if (value == 0d || value == 1d) {
            emit(Opcodes.dconst(value));
        } else {
            emit_ldc(new Double(value));
        }
    }
    
    public void push(String value) {
        emit_ldc(value);
    }

    public void newarray() {
        newarray(Object.class);
    }
    
    public void newarray(Class type) {
        if (type.isPrimitive()) {
            emit_int(Opcodes.NEWARRAY, Opcodes.newarray(type));
        } else {
            emit_type(Opcodes.ANEWARRAY, type.getName());
        }
    }
    
    public void arraylength() {
        emit(Opcodes.ARRAYLENGTH);
    }
    
    public void array_load(Class type) {
        if (type.isPrimitive()) {
            if (type.equals(Long.TYPE)) {
                emit(Opcodes.LALOAD);
            } else if (type.equals(Double.TYPE)) {
                emit(Opcodes.DALOAD);
            } else if (type.equals(Float.TYPE)) {
                emit(Opcodes.FALOAD);
            } else if (type.equals(Short.TYPE)) {
                emit(Opcodes.SALOAD);
            } else if (type.equals(Character.TYPE)) {
                emit(Opcodes.CALOAD);
            } else if (type.equals(Integer.TYPE)) {
                emit(Opcodes.IALOAD);
            } else {
                emit(Opcodes.BALOAD);
            }
        } else {
            emit(Opcodes.AALOAD);
        }
    }

    public void array_store(Class type) {
        if (type.isPrimitive()) {
            if (type.equals(Long.TYPE)) {
                emit(Opcodes.LASTORE);
            } else if (type.equals(Double.TYPE)) {
                emit(Opcodes.DASTORE);
            } else if (type.equals(Float.TYPE)) {
                emit(Opcodes.FASTORE);
            } else if (type.equals(Short.TYPE)) {
                emit(Opcodes.SASTORE);
            } else if (type.equals(Character.TYPE)) {
                emit(Opcodes.CASTORE);
            } else if (type.equals(Integer.TYPE)) {
                emit(Opcodes.IASTORE);
            } else {
                emit(Opcodes.BASTORE);
            }
        } else {
            emit(Opcodes.AASTORE);
        }
    }
    
    public void load_this() {
        if (isStatic) {
            throw new IllegalStateException("no 'this' pointer within static method");
        }
        emit_var(Opcodes.ALOAD, 0);
    }
    
    /**
     * Pushes all of the arguments of the current method onto the stack.
     */
    public void load_args() {
        load_args(0, parameterTypes.length);
    }
    
    /**
     * Pushes the specified argument of the current method onto the stack.
     * @param index the zero-based index into the argument list
     */
    public void load_arg(int index) {
        load_local(parameterTypes[index], getLocalOffset() + skipArgs(index));
    }
    
    // zero-based (see load_this)
    public void load_args(int fromArg, int count) {
        int pos = getLocalOffset() + skipArgs(fromArg);
        for (int i = 0; i < count; i++) {
            Class t = parameterTypes[fromArg + i];
            load_local(t, pos);
            pos += getStackSize(t);
        }
    }

    private int getLocalOffset() {
        return isStatic ? 0 : 1;
    }
    
    private int skipArgs(int numArgs) {
        int amount = 0;
        for (int i = 0; i < numArgs; i++) {
            amount += getStackSize(parameterTypes[i]);
        }
        return amount;
    }
    
    private void load_local(Class t, int pos) {
        if (t != null && t.isPrimitive()) {
            if (t.equals(Long.TYPE)) {
                emit_var(Opcodes.LLOAD, pos);
            } else if (t.equals(Double.TYPE)) {
                emit_var(Opcodes.DLOAD, pos);
            } else if (t.equals(Float.TYPE)) {
                emit_var(Opcodes.FLOAD, pos);
            } else {
                emit_var(Opcodes.ILOAD, pos);
            }
        } else {
            emit_var(Opcodes.ALOAD, pos);
        }
    }

    private void store_local(Class t, int pos) {
        if (t != null && t.isPrimitive()) {
            if (t.equals(Long.TYPE)) {
                emit_var(Opcodes.LSTORE, pos);
            } else if (t.equals(Double.TYPE)) {
                emit_var(Opcodes.DSTORE, pos);
            } else if (t.equals(Float.TYPE)) {
                emit_var(Opcodes.FSTORE, pos);
            } else {
                emit_var(Opcodes.ISTORE, pos);
            }
        } else {
            emit_var(Opcodes.ASTORE, pos);
        }
    }
    
    public void iinc(Local local, int amount) {
        emit_iinc(local.getIndex(), amount);
    }
    
    public void store_local(Local local) {
        store_local(local.getType(), local.getIndex());
    }
    
    public void load_local(Local local) {
        load_local(local.getType(), local.getIndex());
    }
    
    public void return_value() {
        if (returnType.isPrimitive()) {
            if (returnType.equals(Void.TYPE)) {
                emit(Opcodes.RETURN);
            } else if (returnType.equals(Long.TYPE)) {
                emit(Opcodes.LRETURN);
            } else if (returnType.equals(Double.TYPE)) {
                emit(Opcodes.DRETURN);
            } else if (returnType.equals(Float.TYPE)) {
                emit(Opcodes.FRETURN);
            } else {
                emit(Opcodes.IRETURN);
            }
        } else {
            emit(Opcodes.ARETURN);
        }
    }

    public void declare_field(int modifiers, Class type, String name) {
        if (fieldInfo.get(name) != null) {
            throw new IllegalArgumentException("Field \"" + name + "\" already exists");
        }
        classv.visitField(modifiers, name, getInternalName(ReflectUtils.getDescriptor(type)), null);
        fieldInfo.put(name, new FieldInfo(Modifier.isStatic(modifiers), type));
    }
    
    private FieldInfo getFieldInfo(String name) {
        FieldInfo field = (FieldInfo)fieldInfo.get(name);
        if (field == null) {
            throw new IllegalArgumentException("Field " + name + " is not declared");
        }
        return field;
    }
    
    private static class FieldInfo {
        private boolean staticFlag;
        private Class type;
        
        public FieldInfo(boolean staticFlag, Class type) {
            this.staticFlag = staticFlag;
            this.type = type;
        }
        
        public boolean isStatic() {
            return staticFlag;
        }
        
        public Class getType() {
            return type;
        }
    }
    
    public void getfield(String name) {
        FieldInfo info = getFieldInfo(name);
        int opcode = info.isStatic() ? Opcodes.GETSTATIC : Opcodes.GETFIELD;
        emit_field(opcode, className, name, info.getType());
    }
    
    public void putfield(String name) {
        FieldInfo info = getFieldInfo(name);
        int opcode = info.isStatic() ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD;
        emit_field(opcode, className, name, info.getType());
    }
    
    public void super_getfield(String name) throws NoSuchFieldException {
        // TODO: search up entire superclass chain?
        getfield(superclass.getDeclaredField(name));
    }
    
    public void super_putfield(String name) throws NoSuchFieldException {
        putfield(superclass.getDeclaredField(name));
    }

    public void getfield(Field field) {
        int opcode = isStatic(field) ? Opcodes.GETSTATIC : Opcodes.GETFIELD;
        fieldHelper(opcode, field);
    }
    
    public void putfield(Field field) {
        int opcode = isStatic(field) ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD;
        fieldHelper(opcode, field);
    }

    private boolean isStatic(Field field) {
        return Modifier.isStatic(field.getModifiers());
    }

    private boolean isStatic(Method method) {
        return Modifier.isStatic(method.getModifiers());
    }

    private void fieldHelper(int opcode, Field field) {
        emit_field(opcode,
                           field.getDeclaringClass().getName(),
                           field.getName(),
                           field.getType());
    }

    public void invoke(Method method) {
        int opcode;
        if (method.getDeclaringClass().isInterface()) {
            opcode = Opcodes.INVOKEINTERFACE;
        } else if (isStatic(method)) {
            opcode = Opcodes.INVOKESTATIC;
        } else {
            opcode = Opcodes.INVOKEVIRTUAL;
        }
        emit_invoke(opcode,
                            method.getDeclaringClass().getName(),
                            method.getName(),
                            method.getReturnType(),
                            method.getParameterTypes());
    }
    
    public void super_invoke(Method method) {
        emit_invoke(Opcodes.INVOKESPECIAL,
                            superclass.getName(),
                            method.getName(),
                            method.getReturnType(),
                            method.getParameterTypes());
    }

    public void invoke_virtual_this(String methodName, Class returnType, Class[] parameterTypes) {
        emit_invoke(Opcodes.INVOKEVIRTUAL, className, methodName, returnType, parameterTypes);
    }

    public void invoke_static_this(String methodName, Class returnType, Class[] parameterTypes) {
        emit_invoke(Opcodes.INVOKESTATIC, className, methodName, returnType, parameterTypes);
    }

    public void super_invoke() {
        emit_invoke(Opcodes.INVOKESPECIAL,
                            superclass.getName(),
                            methodName,
                            returnType,
                            parameterTypes);
    }
    
    public void invoke_constructor(String className, Class[] parameterTypes) {
        emit_invoke(Opcodes.INVOKESPECIAL,
                            className,
                            Constants.CONSTRUCTOR_NAME,
                            Void.TYPE,
                            parameterTypes);
    }
    
    public void invoke_constructor(Class type) {
        invoke_constructor(type, Constants.TYPES_EMPTY);
    }
    
    public void invoke(Constructor constructor) {
        invoke_constructor(constructor.getDeclaringClass(), constructor.getParameterTypes());
    }
    
    public void invoke_constructor(Class type, Class[] parameterTypes) {
        invoke_constructor(type.getName(), parameterTypes);
    }

    // needed by Virt
    public static int getStackSize(Class type) {
        return (type.equals(Double.TYPE) || type.equals(Long.TYPE)) ? 2 : 1;
    }

    private static int getStackSize(Class[] classes) {
        int size = 0;
        if (classes != null) {
            for (int i = 0; i < classes.length; i++) {
                size += getStackSize(classes[i]);
            }
        }
        return size;
    }

    public void super_invoke(Constructor constructor) {
        super_invoke_constructor(constructor.getParameterTypes());
    }
    
    public void super_invoke_constructor() {
        invoke_constructor(superclass.getName(), Constants.TYPES_EMPTY);
    }
    
    public void super_invoke_constructor(Class[] parameterTypes) {
        invoke_constructor(superclass.getName(), parameterTypes);
    }
    
    public void invoke_constructor_this() {
        invoke_constructor_this(Constants.TYPES_EMPTY);
    }
    
    public void invoke_constructor_this(Class[] parameterTypes) {
        invoke_constructor(className, parameterTypes);
    }
    
    public void new_instance_this() {
        emit_type(Opcodes.NEW, className);
    }
    
    public void new_instance(String className) {
        emit_type(Opcodes.NEW, className);
    }
    
    public void new_instance(Class type) {
        new_instance(type.getName());
    }
    
    public void aaload(int index) {
        push(index);
        aaload();
    }

    public void aaload() { emit(Opcodes.AALOAD); }
    public void aastore() { emit(Opcodes.AASTORE); }
    public void athrow() { emit(Opcodes.ATHROW); }
    
    public Label make_label() {
        return new ASMLabel();
    }
    
    public Local make_local() {
        return make_local(null);
    }
    
    public Local make_local(Class type) {
        int index = nextLocal;
        nextLocal += (type == null) ? 1 : getStackSize(type);
        return new Local(type, index);
    }

    public void checkcast_this() {
        emit_type(Opcodes.CHECKCAST, className);
    }
    
    public void checkcast(Class type) {
        if (type.equals(Object.class)) {
            // ignore
        } else {
            emit_type(Opcodes.CHECKCAST, type.getName());
        }
    }
    
    public void instance_of(Class type) {
        emit_type(Opcodes.INSTANCEOF, type.getName());
    }
    
    public void instance_of_this() {
        emit_type(Opcodes.INSTANCEOF, className);
    }

    public void mark(Label label) {
        emit(label);
    }

    public void process_switch(int[] keys, ProcessSwitchCallback callback) throws Exception {
        float density;
        if (keys.length == 0) {
            density = 0;
        } else {
            density = (float)keys.length / (keys[keys.length - 1] - keys[0] + 1);
        }
        process_switch(keys, callback, density >= 0.5f);
    }

    public void process_switch(int[] keys, ProcessSwitchCallback callback, boolean useTable) throws Exception {
        if (!isSorted(keys))
            throw new IllegalArgumentException("keys to switch must be sorted ascending");
        Label def = make_label();
        Label end = make_label();

        if (keys.length > 0) {
            int len = keys.length;
            int min = keys[0];
            int max = keys[len - 1];
            int range = max - min + 1;

            if (useTable) {
                Label[] labels = new Label[range];
                Arrays.fill(labels, def);
                for (int i = 0; i < len; i++) {
                    labels[keys[i] - min] = make_label();
                }
                emit_switch(min, max, labels, def);
                for (int i = 0; i < range; i++) {
                    Label label = labels[i];
                    if (label != def) {
                        mark(label);
                        callback.processCase(i + min, end);
                    }
                }
            } else {
                Label[] labels = new Label[len];
                for (int i = 0; i < len; i++) {
                    labels[i] = make_label();
                }
                emit_switch(keys, labels, def);
                for (int i = 0; i < len; i++) {
                    mark(labels[i]);
                    callback.processCase(keys[i], end);
                }
            }
        }

        mark(def);
        callback.processDefault();
        mark(end);
    }

    public interface ProcessSwitchCallback {
        void processCase(int key, Label end) throws Exception;
        void processDefault() throws Exception;
    }

    public interface FinalizeCallback {
        void process();
    }

    private static boolean isSorted(int[] keys) {
        for (int i = 1; i < keys.length; i++) {
            if (keys[i] < keys[i - 1])
                return false;
        }
        return true;
    }

    ///////////////// METHODS FORMERLY IN THE BACKEND //////////////////


    private void emit(int opcode) {
        codev.visitInsn(opcode);
    }

    private void emit(Label label) {
        codev.visitLabel(convertLabel(label));
    }

    private void emit(int opcode, Label label) {
        codev.visitJumpInsn(opcode, convertLabel(label));
    }
    
    private void emit_var(int opcode, int index) {
        codev.visitVarInsn(opcode, index);
    }
    
    private void emit_type(int opcode, String className) {
        codev.visitTypeInsn(opcode, getInternalName(className));
    }
    
    private void emit_int(int opcode, int value) {
        codev.visitIntInsn(opcode, value);
    }
    
    private void emit_field(int opcode, String className, String fieldName, Class type) {
        codev.visitFieldInsn(opcode,
                          getInternalName(className),
                          fieldName,
                          getInternalName(ReflectUtils.getDescriptor(type)));
    }

    private static class ASMLabel implements Label {
        org.objectweb.asm.Label label = new org.objectweb.asm.Label();
    }

    private Label mark() {
        Label label = make_label();
        emit(label);
        return label;
    }

    private void emit_invoke(int opcode,
                            String className,
                            String methodName,
                            Class returnType,
                            Class[] parameterTypes) {
        codev.visitMethodInsn(opcode,
                           getInternalName(className),
                           methodName,
                           getInternalName(ReflectUtils.getMethodDescriptor(returnType, parameterTypes)));
    }

    private void emit_iinc(int index, int amount) {
        codev.visitIincInsn(index, amount);
    }

    private void emit_ldc(Object value) {
        codev.visitLdcInsn(value);
    }

    private void emit_switch(int[] keys, Label[] labels, Label def) {
        codev.visitLookupSwitchInsn(convertLabel(def),
                                 keys,
                                 convertLabels(labels));
    }

    private void emit_switch(int min, int max, Label[] labels, Label def) {
        codev.visitTableSwitchInsn(min, max,
                                convertLabel(def),
                                convertLabels(labels));
    }

    private org.objectweb.asm.Label convertLabel(Label label) {
        return ((ASMLabel)label).label;
    }

    private org.objectweb.asm.Label[] convertLabels(Label[] labels) {
        org.objectweb.asm.Label[] converted = new org.objectweb.asm.Label[labels.length];
        for (int i = 0; i < labels.length; i++) {
            converted[i] = convertLabel(labels[i]);
        }
        return converted;
    }

    // TODO: use ASM methods instead
    private static String getInternalName(Class type) {
        return (type == null) ? null : getInternalName(type.getName());
    }

    private static String getInternalName(String className) {
        return (className == null) ? null : className.replace('.', '/');
    }

    private static String[] getInternalNames(Class[] classes) {
        if (classes == null) {
            return null;
        }
        String[] copy = new String[classes.length];
        for (int i = 0; i < copy.length; i++) {
            copy[i] = getInternalName(classes[i]);
        }
        return copy;
    }
}