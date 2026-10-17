package com.asdflj.ae2thing.util;

import java.util.concurrent.TimeUnit;

import com.asdflj.ae2thing.integration.Mods;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import appeng.api.util.IInterfaceViewable;
import appeng.helpers.ICustomNameObject;

public final class InterfaceTerminalNameCache {

    private static final String PH_PATTERN_PREFIX = "reobf.proghatches.gt.metatileentity.PatternDualInputHatch";
    private static final String GTNL_SUPER_HATCH = "com.science.gtnl.common.machine.hatch.SuperCraftingInputHatchME";
    private static final long FALLBACK_NANOS = TimeUnit.SECONDS.toNanos(5);
    private static final InterfaceTerminalNameCache SHARED = new InterfaceTerminalNameCache();
    private static final ClassValue<Provider> PROVIDERS = new ClassValue<>() {

        @Override
        protected Provider computeValue(Class<?> type) {
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                String name = current.getName();
                if (name.startsWith(PH_PATTERN_PREFIX)) return Provider.PH;
                if (GTNL_SUPER_HATCH.equals(name)) return Provider.GTNL;
            }
            return Provider.NONE;
        }
    };

    private final Cache<Object, CachedName> names = CacheBuilder.newBuilder()
        .weakKeys()
        .maximumSize(4096)
        .expireAfterAccess(5, TimeUnit.MINUTES)
        .build();

    InterfaceTerminalNameCache() {}

    public static String getRawName(IInterfaceViewable host) {
        if (host instanceof ICustomNameObject named && named.hasCustomName()) {
            SHARED.names.invalidate(host);
            return named.getCustomName();
        }
        String rawName = host.getRawName();
        Provider provider = PROVIDERS.get(host.getClass());
        if (provider == Provider.NONE || !Mods.isGt5Loaded()) return rawName;
        return GTNames.getRawName(host, rawName, provider);
    }

    synchronized String resolve(Object host, String rawName, String selfName, String selfKey, boolean legacy,
        long nowNanos) {
        if (rawName == null || rawName.isEmpty()) return rawName;
        CachedName known = this.names.getIfPresent(host);
        String suffix = selfSuffix(rawName, selfName, legacy);
        if (suffix == null) suffix = selfSuffix(rawName, selfKey, legacy);
        if (suffix == null) suffix = selfSuffix(rawName, selfKey + ".name", legacy);
        if (suffix != null) {
            if (known == null) return rawName;
            if (!known.fallback) {
                known = new CachedName(known.base, true, nowNanos);
                this.names.put(host, known);
            } else if (nowNanos - known.fallbackSince >= FALLBACK_NANOS) {
                this.names.invalidate(host);
                return rawName;
            }
            return known.base + suffix;
        }
        String base = legacy ? rawName.substring(0, suffixStart(rawName)) : rawName;
        if (known == null || known.fallback || !base.equals(known.base)) {
            this.names.put(host, new CachedName(base, false, 0));
        }
        return rawName;
    }

    private static String selfSuffix(String rawName, String selfName, boolean legacy) {
        if (selfName == null || selfName.isEmpty() || !rawName.startsWith(selfName)) return null;
        if (rawName.length() == selfName.length()) return "";
        if (legacy && rawName.startsWith(" -", selfName.length())) {
            return rawName.substring(selfName.length());
        }
        return null;
    }

    private static int suffixStart(String name) {
        int start = name.indexOf(" -");
        return start < 0 ? name.length() : start;
    }

    private enum Provider {
        NONE,
        PH,
        GTNL
    }

    private record CachedName(String base, boolean fallback, long fallbackSince) {}

    private static final class GTNames {

        private static String getRawName(IInterfaceViewable host, String rawName, Provider provider) {
            if (!(host instanceof gregtech.api.metatileentity.MetaTileEntity machine)) return rawName;
            return SHARED.resolve(
                host,
                rawName,
                machine.getLocalName(),
                "gt.blockmachines." + machine.getMetaName(),
                provider == Provider.PH,
                System.nanoTime());
        }
    }
}
