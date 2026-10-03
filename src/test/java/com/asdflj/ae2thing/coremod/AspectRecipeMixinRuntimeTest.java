package com.asdflj.ae2thing.coremod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class AspectRecipeMixinRuntimeTest {

    private static final String PROJECT_PACKAGE = "com/asdflj/ae2thing/";
    private static final String MIXIN_PACKAGE = PROJECT_PACKAGE + "coremod/mixin/";

    @Test
    public void injectedRecipeMethodsOnlyReferenceAccessibleRuntimeHelpers() throws IOException {
        int[] checkedCalls = { 0 };
        for (String mixin : new String[] { "MixinLoadCraftingRecipes", "MixinTemplateRecipeHandler" }) {
            readClass(MIXIN_PACKAGE + "tc/nei/" + mixin).accept(new ClassVisitor(Opcodes.ASM5) {

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                    String[] exceptions) {
                    if (!name.startsWith("ae2thing$")) return null;
                    return new MethodVisitor(Opcodes.ASM5) {

                        @Override
                        public void visitMethodInsn(int opcode, String owner, String calledName,
                            String calledDescriptor, boolean isInterface) {
                            if (!owner.startsWith(PROJECT_PACKAGE)) return;
                            // These calls are copied into ARI classes. Mixin rejects direct loading of its package.
                            assertFalse(
                                "Injected method references the reserved Mixin package: " + owner,
                                owner.startsWith(MIXIN_PACKAGE));
                            assertEquals(Opcodes.INVOKESTATIC, opcode);
                            assertPublicStaticMethod(owner, calledName, calledDescriptor);
                            checkedCalls[0]++;
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        assertEquals("Cover both recipe searches and the aspect-containing-items search", 3, checkedCalls[0]);
    }

    private static void assertPublicStaticMethod(String owner, String name, String descriptor) {
        boolean[] accessible = { false };
        try {
            ClassReader helper = readClass(owner);
            assertTrue("Runtime helper class must be public: " + owner, (helper.getAccess() & Opcodes.ACC_PUBLIC) != 0);
            helper.accept(new ClassVisitor(Opcodes.ASM5) {

                @Override
                public MethodVisitor visitMethod(int access, String methodName, String methodDescriptor,
                    String signature, String[] exceptions) {
                    if (name.equals(methodName) && descriptor.equals(methodDescriptor)) {
                        int requiredAccess = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
                        accessible[0] = (access & requiredAccess) == requiredAccess;
                    }
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        } catch (IOException e) {
            throw new AssertionError("Cannot read runtime helper: " + owner, e);
        }
        assertTrue("Injected recipe handler cannot call " + owner + "." + name, accessible[0]);
    }

    private static ClassReader readClass(String name) throws IOException {
        String resource = name + ".class";
        try (InputStream input = AspectRecipeMixinRuntimeTest.class.getClassLoader()
            .getResourceAsStream(resource)) {
            assertNotNull("Missing test classpath resource: " + resource, input);
            return new ClassReader(input);
        }
    }
}
