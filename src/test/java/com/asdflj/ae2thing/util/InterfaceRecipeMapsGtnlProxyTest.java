package com.asdflj.ae2thing.util;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class InterfaceRecipeMapsGtnlProxyTest {

    private static final String GTNL_PROXY = "com.science.gtnl.common.machine.hatch.SuperCraftingInputProxy";

    @Test
    public void followsTheActiveSuperOrOrdinaryCraftingMaster() throws ReflectiveOperationException {
        Object proxy = proxy();
        Object craftingMaster = new Object();
        Object superMaster = new Object();
        proxy.getClass()
            .getField("craftingMaster")
            .set(proxy, craftingMaster);
        assertSame(craftingMaster, InterfaceRecipeMaps.getGtnlProxyMaster(proxy, Object.class));

        proxy.getClass()
            .getField("superMaster")
            .set(proxy, superMaster);
        assertSame(superMaster, InterfaceRecipeMaps.getGtnlProxyMaster(proxy, Object.class));

        proxy.getClass()
            .getField("superMaster")
            .set(proxy, null);
        proxy.getClass()
            .getField("craftingMaster")
            .set(proxy, null);
        assertNull(InterfaceRecipeMaps.getGtnlProxyMaster(proxy, Object.class));
    }

    @Test
    public void acceptsInheritedProxyMethodsWithoutLoadingGtnl() throws ReflectiveOperationException {
        FixtureLoader loader = new FixtureLoader();
        loader.define(GTNL_PROXY, "java.lang.Object", true);
        Object proxy = loader.define("fixture.GtnlProxySubclass", GTNL_PROXY, false)
            .getConstructor()
            .newInstance();
        Object master = new Object();
        proxy.getClass()
            .getField("superMaster")
            .set(proxy, master);
        assertSame(master, InterfaceRecipeMaps.getGtnlProxyMaster(proxy, Object.class));
    }

    @Test
    public void ignoresUnrelatedWatchersWithTheSameGetterNames() throws ReflectiveOperationException {
        Object unrelated = new FixtureLoader().define("fixture.UnrelatedWatcher", "java.lang.Object", true)
            .getConstructor()
            .newInstance();
        unrelated.getClass()
            .getField("superMaster")
            .set(unrelated, new Object());
        assertNull(InterfaceRecipeMaps.getGtnlProxyMaster(unrelated, Object.class));
    }

    @Test
    public void optionalGetterFailureOrWrongReturnTypeDoesNotBlockTheOtherMaster() throws ReflectiveOperationException {
        Object proxy = proxy();
        String master = "craftingMaster";
        proxy.getClass()
            .getField("craftingMaster")
            .set(proxy, master);
        proxy.getClass()
            .getField("superMaster")
            .set(proxy, new Object());
        assertSame(master, InterfaceRecipeMaps.getGtnlProxyMaster(proxy, String.class));

        proxy.getClass()
            .getField("failSuper")
            .setBoolean(proxy, true);
        assertSame(master, InterfaceRecipeMaps.getGtnlProxyMaster(proxy, String.class));
    }

    private static Object proxy() throws ReflectiveOperationException {
        return new FixtureLoader().define(GTNL_PROXY, "java.lang.Object", true)
            .getConstructor()
            .newInstance();
    }

    private static final class FixtureLoader extends ClassLoader {

        private FixtureLoader() {
            super(InterfaceRecipeMapsGtnlProxyTest.class.getClassLoader());
        }

        private Class<?> define(String name, String parent, boolean masters) {
            String type = name.replace('.', '/');
            String superType = parent.replace('.', '/');
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, type, null, superType, null);
            MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            constructor.visitCode();
            constructor.visitVarInsn(Opcodes.ALOAD, 0);
            constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, superType, "<init>", "()V", false);
            constructor.visitInsn(Opcodes.RETURN);
            constructor.visitMaxs(0, 0);
            constructor.visitEnd();
            if (masters) {
                writer.visitField(Opcodes.ACC_PUBLIC, "superMaster", "Ljava/lang/Object;", null, null)
                    .visitEnd();
                writer.visitField(Opcodes.ACC_PUBLIC, "craftingMaster", "Ljava/lang/Object;", null, null)
                    .visitEnd();
                writer.visitField(Opcodes.ACC_PUBLIC, "failSuper", "Z", null, null)
                    .visitEnd();
                getter(writer, type, "getMasterSuper", "superMaster", true);
                getter(writer, type, "getCraftingMaster", "craftingMaster", false);
            }
            writer.visitEnd();
            byte[] code = writer.toByteArray();
            return defineClass(name, code, 0, code.length);
        }

        private static void getter(ClassWriter writer, String type, String name, String field, boolean mayFail) {
            MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC, name, "()Ljava/lang/Object;", null, null);
            method.visitCode();
            if (mayFail) {
                Label valid = new Label();
                method.visitVarInsn(Opcodes.ALOAD, 0);
                method.visitFieldInsn(Opcodes.GETFIELD, type, "failSuper", "Z");
                method.visitJumpInsn(Opcodes.IFEQ, valid);
                method.visitTypeInsn(Opcodes.NEW, "java/lang/IllegalStateException");
                method.visitInsn(Opcodes.DUP);
                method
                    .visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/IllegalStateException", "<init>", "()V", false);
                method.visitInsn(Opcodes.ATHROW);
                method.visitLabel(valid);
            }
            method.visitVarInsn(Opcodes.ALOAD, 0);
            method.visitFieldInsn(Opcodes.GETFIELD, type, field, "Ljava/lang/Object;");
            method.visitInsn(Opcodes.ARETURN);
            method.visitMaxs(0, 0);
            method.visitEnd();
        }
    }
}
