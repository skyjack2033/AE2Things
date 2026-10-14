package com.asdflj.ae2thing.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import com.asdflj.ae2thing.integration.Mods;

import appeng.api.util.IInterfaceViewable;
import appeng.helpers.IInterfaceHost;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.interfaces.tileentity.RecipeMapWorkable;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.common.tileentities.machines.MTEHatchCraftingInputME;
import gregtech.common.tileentities.machines.MTEHatchCraftingInputSlave;

/** Resolves recipe maps from live machine relationships, never from the hatch's display name or icon. */
public final class InterfaceRecipeMaps {

    private InterfaceRecipeMaps() {}

    public static boolean isSupported() {
        return Mods.GREGTECH.isModLoaded() && GtSupport.SUPPORTED;
    }

    private static final class GtSupport {

        private static final boolean SUPPORTED = inspectStructure();
    }

    private static boolean inspectStructure() {
        ClassLoader loader = InterfaceRecipeMaps.class.getClassLoader();
        if (loader.getResource("gregtech/api/interfaces/tileentity/RecipeMapWorkable.class") == null
            || loader.getResource("gregtech/api/recipe/RecipeMap.class") == null) return false;
        // Inspect resources without loading the GT target before late mixins are applied.
        try (InputStream stream = openBaseClassResource(
            loader,
            "gregtech/api/metatileentity/implementations/MTEHatch.class")) {
            return stream != null && hasControllerFields(stream);
        } catch (IOException | RuntimeException | LinkageError e) {
            return false;
        }
    }

    static InputStream openBaseClassResource(ClassLoader loader, String name) throws IOException {
        URL resource = loader.getResource(name);
        if (resource == null) return null;
        URLConnection connection = resource.openConnection();
        if (connection instanceof JarURLConnection jar) {
            // Modern development JVMs select multi-release entries; inspect the Java 8 base class with legacy ASM.
            try (ZipFile archive = new ZipFile(jar.getJarFile().getName())) {
                ZipEntry entry = archive.getEntry(name);
                if (entry == null) return null;
                try (InputStream stream = archive.getInputStream(entry)) {
                    return new ByteArrayInputStream(stream.readAllBytes());
                }
            }
        }
        return connection.getInputStream();
    }

    static boolean hasControllerFields(InputStream stream) throws IOException {
        boolean[] fields = new boolean[2];
        new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM5) {

            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                if ("watchers".equals(name) && "Ljava/util/List;".equals(descriptor)) fields[0] = true;
                if ("ae2CraftingIconOwner".equals(name)
                    && "Lnet/minecraft/util/ChunkCoordinates;".equals(descriptor)) fields[1] = true;
                return null;
            }
        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return fields[0] && fields[1];
    }

    public static Map<Long, Set<String>> snapshot(Map<IInterfaceViewable, ?> tracked) {
        Map<Long, Set<String>> snapshot = new LinkedHashMap<>();
        boolean gregtech = isSupported();
        for (IInterfaceViewable host : tracked.keySet()) {
            Long id = InterfaceTerminalTarget.getId(tracked, host);
            if (id == null) continue;
            Set<String> maps = gregtech ? GtResolver.resolve(host) : Collections.emptySet();
            snapshot.put(id, maps);
        }
        return snapshot;
    }

    /** Supplied only by the optional GT mixin, so non-GT terminals do not link GT classes. */
    public interface HatchControllers {

        List<?> ae2thing$getRecipeMapControllers();
    }

    private static final class GtResolver {

        private static final String PROGRAMMABLE_PREFIX = "reobf.proghatches.gt.metatileentity.";
        private static final ClassValue<Optional<Method>> PROGRAMMABLE_MASTER = new ClassValue<>() {

            @Override
            protected Optional<Method> computeValue(Class<?> type) {
                if (!type.getName()
                    .startsWith(PROGRAMMABLE_PREFIX)) return Optional.empty();
                try {
                    return Optional.of(type.getMethod("getMaster"));
                } catch (ReflectiveOperationException | SecurityException e) {
                    return Optional.empty();
                }
            }
        };

        private GtResolver() {}

        private static Set<String> resolve(IInterfaceViewable host) {
            Set<String> maps = new LinkedHashSet<>();
            Set<IMetaTileEntity> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            try {
                collect(host, maps, visited);
                TileEntity tile = host.getTileEntity();
                if (host instanceof IInterfaceHost interfaceHost && tile != null && tile.getWorldObj() != null) {
                    // Covers use a synthetic tile at the machine coordinates and UNKNOWN (zero offset).
                    for (ForgeDirection side : interfaceHost.getTargets()) {
                        World world = tile.getWorldObj();
                        int x = tile.xCoord + side.offsetX;
                        int y = tile.yCoord + side.offsetY;
                        int z = tile.zCoord + side.offsetZ;
                        if (world.blockExists(x, y, z)) {
                            collect(world.getTileEntity(x, y, z), maps, visited);
                        }
                    }
                } else {
                    collect(tile, maps, visited);
                }
            } catch (RuntimeException | LinkageError ignored) {
                // Unsupported optional integrations retain the terminal's ordinary name search.
                maps.clear();
            }
            return Collections.unmodifiableSet(maps);
        }

        private static void collect(Object target, Set<String> maps, Set<IMetaTileEntity> visited) {
            IMetaTileEntity machine = target instanceof IGregTechTileEntity tile ? tile.getMetaTileEntity()
                : target instanceof IMetaTileEntity meta ? meta : null;
            if (machine == null || !(machine instanceof MetaTileEntity meta) || !isLoaded(meta)
                || !visited.add(machine)) return;

            if (machine instanceof MTEHatch hatch) {
                collectHatch(hatch, maps);
                if (hatch instanceof MTEHatchCraftingInputME input) {
                    for (MTEHatchCraftingInputSlave slave : input.getProxyHatches()) {
                        collect(slave, maps, visited);
                    }
                }
            } else {
                addMap(machine, maps);
            }

            // PH's pattern-mapping provider has its own patterns but crafts through its current master.
            IMetaTileEntity master = getMaster(machine);
            if (master != null) collect(master, maps, visited);
        }

        private static void collectHatch(MTEHatch hatch, Set<String> maps) {
            if (!(hatch instanceof HatchControllers source)) return;
            for (Object owner : source.ae2thing$getRecipeMapControllers()) {
                if (!(owner instanceof MTEMultiBlockBase controller) || !isLoaded(controller)
                    || !controller.mMachine
                    || !belongsToController(controller, hatch)) continue;
                addMap(controller, maps);
            }
        }

        private static boolean belongsToController(MTEMultiBlockBase controller, MTEHatch hatch) {
            List<IMetaTileEntity> inputs = new ArrayList<>(controller.mInputBusses);
            inputs.addAll(controller.mInputHatches);
            inputs.addAll(controller.mDualInputHatches);
            for (IMetaTileEntity input : inputs) {
                if (input == hatch || reachesMaster(input, hatch)) return true;
            }
            return false;
        }

        private static boolean reachesMaster(IMetaTileEntity input, IMetaTileEntity target) {
            Set<IMetaTileEntity> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            for (IMetaTileEntity current = input; current != null && visited.add(current); current = getMaster(current)) {
                if (current == target) return true;
                if (!(current instanceof MetaTileEntity meta) || !isLoaded(meta)) return false;
            }
            return false;
        }

        private static IMetaTileEntity getMaster(IMetaTileEntity machine) {
            if (machine instanceof MTEHatchCraftingInputSlave slave) return slave.getMaster();
            Method method = PROGRAMMABLE_MASTER.get(machine.getClass())
                .orElse(null);
            if (method == null) return null;
            try {
                Object master = method.invoke(machine);
                return master instanceof IMetaTileEntity meta ? meta : null;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return null;
            }
        }

        private static boolean isLoaded(MetaTileEntity machine) {
            if (!machine.isValid() || !(machine.getBaseMetaTileEntity() instanceof TileEntity tile)) return false;
            World world = tile.getWorldObj();
            return world != null && world.blockExists(tile.xCoord, tile.yCoord, tile.zCoord)
                && world.getTileEntity(tile.xCoord, tile.yCoord, tile.zCoord) == tile;
        }

        private static void addMap(IMetaTileEntity machine, Set<String> maps) {
            if (machine instanceof MTEMultiBlockBase controller && !controller.mMachine) return;
            if (!(machine instanceof RecipeMapWorkable workable)) return;
            RecipeMap<?> map = workable.getRecipeMap();
            if (map != null && map.unlocalizedName != null && !map.unlocalizedName.isEmpty()) {
                maps.add(map.unlocalizedName);
            }
        }
    }
}
