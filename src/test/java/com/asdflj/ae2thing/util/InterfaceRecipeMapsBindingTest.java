package com.asdflj.ae2thing.util;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class InterfaceRecipeMapsBindingTest {

    @Test
    public void pinnedGtExposesTheLiveControllerAndSlaveRelationships() throws IOException {
        Set<String> hatch = members("gregtech/api/metatileentity/implementations/MTEHatch");
        assertTrue(hatch.contains("watchers:Ljava/util/List;"));
        assertTrue(hatch.contains("ae2CraftingIconOwner:Lnet/minecraft/util/ChunkCoordinates;"));
        assertTrue(
            members("gregtech/common/tileentities/machines/MTEHatchCraftingInputME")
                .contains("getProxyHatches()Ljava/util/List;"));
        assertTrue(
            members("gregtech/common/tileentities/machines/MTEHatchCraftingInputSlave")
                .contains("getMaster()Lgregtech/common/tileentities/machines/MTEHatchCraftingInputME;"));
        assertTrue(
            members("gregtech/api/interfaces/tileentity/RecipeMapWorkable")
                .contains("getRecipeMap()Lgregtech/api/recipe/RecipeMap;"));
    }

    private static Set<String> members(String type) throws IOException {
        Set<String> members = new HashSet<>();
        try (InputStream stream = InterfaceRecipeMaps.openBaseClassResource(
            InterfaceRecipeMapsBindingTest.class.getClassLoader(),
            type + ".class")) {
            if (stream == null) throw new IOException("Missing pinned GT class: " + type);
            new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM5) {

                @Override
                public FieldVisitor visitField(int access, String name, String descriptor, String signature,
                    Object value) {
                    members.add(name + ":" + descriptor);
                    return null;
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                    String[] exceptions) {
                    members.add(name + descriptor);
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        return members;
    }
}
