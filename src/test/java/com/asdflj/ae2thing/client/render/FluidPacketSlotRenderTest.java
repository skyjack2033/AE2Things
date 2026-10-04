package com.asdflj.ae2thing.client.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class FluidPacketSlotRenderTest {

    private static final String RENDER_PACKAGE = "com/asdflj/ae2thing/client/render/";
    private static final String GUI_DRAW_SLOT = "com/asdflj/ae2thing/client/gui/IGuiDrawSlot";
    private static final String SLOT_DESCRIPTOR = "Lnet/minecraft/inventory/Slot;";

    @Test
    public void fluidPacketRenderingRequestsBaseDrawWithoutAccessingTheGui() {
        RenderFluidPacketPatternSlot renderer = new RenderFluidPacketPatternSlot();

        // The pre-draw hook must only delegate. Null arguments fail immediately if it tries to draw recursively.
        assertTrue(renderer.drawSlot(null, null, null, false));
        assertTrue(renderer.drawSlot(null, null, null, true));
    }

    @Test
    public void registeredSlotRenderersNeverReenterTheGuiSlotDrawingEntryPoint() throws IOException {
        Set<String> renderers = new LinkedHashSet<>();
        readClass(RENDER_PACKAGE + "SlotRender").accept(new ClassVisitor(Opcodes.ASM5) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM5) {

                    @Override
                    public void visitTypeInsn(int opcode, String type) {
                        if (opcode == Opcodes.NEW && type.startsWith(RENDER_PACKAGE)) renderers.add(type);
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        assertTrue(renderers.contains(RENDER_PACKAGE + "RenderFluidPacketPatternSlot"));
        int checked = 0;
        for (String renderer : renderers) {
            ClassReader bytecode = readClass(renderer);
            if (!Arrays.asList(bytecode.getInterfaces())
                .contains(RENDER_PACKAGE + "ISlotRender")) continue;
            checked++;
            bytecode.accept(new ClassVisitor(Opcodes.ASM5) {

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                    String[] exceptions) {
                    return new MethodVisitor(Opcodes.ASM5) {

                        @Override
                        public void visitMethodInsn(int opcode, String owner, String target, String targetDescriptor,
                            boolean isInterface) {
                            boolean guiEntry = (target.equals("func_146977_a") || target.equals("drawSlot"))
                                && targetDescriptor.equals("(" + SLOT_DESCRIPTOR + ")V");
                            boolean sharedEntry = owner.equals(GUI_DRAW_SLOT) && target.equals("drawSlot")
                                && targetDescriptor.equals("(" + SLOT_DESCRIPTOR + "Ljava/lang/Runnable;)Z");
                            assertFalse(
                                renderer + "." + name + " must not reenter " + owner + "." + target,
                                guiEntry || sharedEntry);
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        assertTrue("The renderer registry must contain implementations to inspect", checked > 0);
    }

    @Test
    public void theSharedDrawPathInvokesBaseDrawingBeforeRendererCallbacks() throws IOException {
        List<String> calls = new ArrayList<>();
        readClass(GUI_DRAW_SLOT).accept(new ClassVisitor(Opcodes.ASM5) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                String[] exceptions) {
                if (!name.equals("drawSlot") || !descriptor.equals("(" + SLOT_DESCRIPTOR + "Ljava/lang/Runnable;)Z"))
                    return null;
                return new MethodVisitor(Opcodes.ASM5) {

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String target, String targetDescriptor,
                        boolean isInterface) {
                        if (owner.equals("java/lang/Runnable") && target.equals("run")) calls.add("baseDraw");
                        if (owner.equals(RENDER_PACKAGE + "ISlotRender") && target.equals("drawCallback"))
                            calls.add("callback");
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        assertEquals(Arrays.asList("baseDraw", "callback"), calls);
    }

    private static ClassReader readClass(String name) throws IOException {
        try (InputStream input = FluidPacketSlotRenderTest.class.getClassLoader()
            .getResourceAsStream(name + ".class")) {
            assertNotNull("Missing compiled class: " + name, input);
            return new ClassReader(input);
        }
    }
}
