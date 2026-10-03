package com.asdflj.ae2thing.coremod.transform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.asdflj.ae2thing.coremod.ClassTransformer;

public class DualityInterfaceTransformerTest {

    private static final String DUALITY_INTERFACE = "appeng.helpers.DualityInterface";
    private static final String ADAPTOR = "appeng/util/InventoryAdaptor";
    private static final String HOOKS = "com/asdflj/ae2thing/coremod/hooker/CoreModHooks";
    private static final String GET_ADAPTOR = "(Ljava/lang/Object;Lnet/minecraftforge/common/util/ForgeDirection;)Lappeng/util/InventoryAdaptor;";
    private static final String ADD_STACK = "(Lappeng/api/storage/data/IAEStack;Lappeng/api/config/InsertionMode;)Lappeng/api/storage/data/IAEStack;";

    @Test
    public void hooksBothInitialPatternPushAndQueuedInputsInResolvedAe2() throws IOException {
        byte[] original = readClass(DUALITY_INTERFACE.replace('.', '/') + ".class");
        byte[] transformed = new ClassTransformer().transform(DUALITY_INTERFACE, DUALITY_INTERFACE, original);

        for (String method : new String[] { "pushPattern", "pushItemsOut" }) {
            assertEquals(1, countCalls(original, method, ADAPTOR, "getAdaptor", GET_ADAPTOR, Opcodes.INVOKESTATIC));
            assertEquals(1, countCalls(transformed, method, HOOKS, "getAdaptor", GET_ADAPTOR, Opcodes.INVOKESTATIC));
            assertEquals(0, countCalls(transformed, method, ADAPTOR, "getAdaptor", GET_ADAPTOR, Opcodes.INVOKESTATIC));
        }

        // The normal interface inventory probe must keep using AE2's original adaptor.
        assertEquals(1, countCalls(transformed, "isBusy", ADAPTOR, "getAdaptor", GET_ADAPTOR, Opcodes.INVOKESTATIC));
        assertEquals(0, countCalls(transformed, "isBusy", HOOKS, "getAdaptor", GET_ADAPTOR, Opcodes.INVOKESTATIC));
    }

    @Test
    public void essentiaWrappersKeepTheDelegatesGenericStackInsertion() throws IOException {
        for (String adaptor : new String[] { "EssentiaInventoryAdaptor", "ThaumatoriumInventoryAdapter" }) {
            byte[] code = readClass("com/asdflj/ae2thing/inventory/" + adaptor + ".class");
            for (String method : new String[] { "addStack", "simulateAddStack" }) {
                // INVOKEVIRTUAL calls the wrapped adaptor, preserving its fluid and long-count handling.
                assertEquals(1, countCalls(code, method, ADAPTOR, method, ADD_STACK, Opcodes.INVOKEVIRTUAL));
            }
        }
    }

    private static byte[] readClass(String resource) throws IOException {
        InputStream input = DualityInterfaceTransformerTest.class.getClassLoader()
            .getResourceAsStream(resource);
        assertNotNull("Missing test classpath resource: " + resource, input);
        try (InputStream in = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private static int countCalls(byte[] bytecode, String method, String owner, String calledMethod,
        String calledDescriptor, int expectedOpcode) {
        int[] calls = { 0 };
        new ClassReader(bytecode).accept(new ClassVisitor(Opcodes.ASM5) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                String[] exceptions) {
                if (!method.equals(name)) return null;
                return new MethodVisitor(Opcodes.ASM5) {

                    @Override
                    public void visitMethodInsn(int opcode, String invocationOwner, String invocationName,
                        String invocationDescriptor, boolean isInterface) {
                        if (opcode == expectedOpcode && owner.equals(invocationOwner)
                            && calledMethod.equals(invocationName)
                            && calledDescriptor.equals(invocationDescriptor)) {
                            calls[0]++;
                        }
                    }
                };
            }
        }, 0);
        return calls[0];
    }
}
