package com.asdflj.ae2thing.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public class InterfaceRecipeMapsGateTest {

    @Test
    public void acceptsTheRequiredControllerFieldsWithoutLoadingTheClass() throws IOException {
        assertTrue(supports("Ljava/util/List;", "Lnet/minecraft/util/ChunkCoordinates;"));
    }

    @Test
    public void olderHatchesWithoutTheIconOwnerUseNameFallback() throws IOException {
        assertFalse(supports("Ljava/util/List;", null));
    }

    @Test
    public void rejectsFieldsWithIncompatibleShadowDescriptors() throws IOException {
        assertFalse(supports("Ljava/util/Set;", "Lnet/minecraft/util/ChunkCoordinates;"));
        assertFalse(supports("Ljava/util/List;", "Ljava/lang/Object;"));
    }

    private static boolean supports(String watchers, String owner) throws IOException {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(
            Opcodes.V1_8,
            Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
            "fixture/Hatch",
            null,
            "java/lang/Object",
            null);
        writer.visitField(Opcodes.ACC_PROTECTED | Opcodes.ACC_FINAL, "watchers", watchers, null, null)
            .visitEnd();
        if (owner != null) {
            writer.visitField(Opcodes.ACC_PRIVATE, "ae2CraftingIconOwner", owner, null, null)
                .visitEnd();
        }
        writer.visitEnd();
        try (ByteArrayInputStream stream = new ByteArrayInputStream(writer.toByteArray())) {
            return InterfaceRecipeMaps.hasControllerFields(stream);
        }
    }
}
