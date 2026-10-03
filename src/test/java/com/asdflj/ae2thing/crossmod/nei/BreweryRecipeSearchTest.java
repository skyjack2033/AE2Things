package com.asdflj.ae2thing.crossmod.nei;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.zip.ZipFile;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class BreweryRecipeSearchTest {

    @Test
    public void aContainerThatRejectsTheBrewDoesNotCrashOrMatch() {
        assertFalse(BreweryRecipeSearch.matchesItem(new ItemStack(new Item()), null));
        assertFalse(BreweryRecipeSearch.matchesItem(null, null));
    }

    @Test
    public void validContainersKeepTheNativeItemAndDamageComparison() {
        Item item = new Item().setHasSubtypes(true);
        ItemStack result = new ItemStack(item, 1, 2);
        assertTrue(BreweryRecipeSearch.matchesItem(result, result.copy()));
        assertFalse(BreweryRecipeSearch.matchesItem(result, new ItemStack(item, 1, 3)));
        assertFalse(BreweryRecipeSearch.matchesItem(result, new ItemStack(new Item(), 1, 2)));
    }

    @Test
    public void thePinnedBrewerySearchHasTheExactRedirectTargetAndRetainsItsNbtCheck() throws IOException {
        String resource = "vazkii/botania/client/integration/nei/recipe/RecipeHandlerBrewery.class";
        int[] comparisons = { 0, 0 };
        URL location = getClass().getClassLoader()
            .getResource(resource);
        assertNotNull("Missing pinned Botania test dependency", location);
        JarURLConnection connection = (JarURLConnection) location.openConnection();
        // Forge's bundled ASM reads Java 8 bytecode. Select the base entry of Botania's multi-release jar.
        try (ZipFile archive = new ZipFile(
            connection.getJarFile()
                .getName());
            InputStream input = archive.getInputStream(archive.getEntry(resource))) {
            new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM5) {

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                    String[] exceptions) {
                    if (!name.equals("loadCraftingRecipes") || !descriptor.equals("(Lnet/minecraft/item/ItemStack;)V"))
                        return null;
                    return new MethodVisitor(Opcodes.ASM5) {

                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name, String descriptor,
                            boolean isInterface) {
                            if (owner.equals("net/minecraft/item/ItemStack") && name.equals("isItemEqual")
                                && descriptor.equals("(Lnet/minecraft/item/ItemStack;)Z")) comparisons[0]++;
                            if (owner.equals("java/util/Objects") && name.equals("equals")) comparisons[1]++;
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        assertEquals("Brewery item comparison must still be interceptable", 1, comparisons[0]);
        assertEquals("Keep Botania's separate brew NBT comparison", 1, comparisons[1]);
    }
}
