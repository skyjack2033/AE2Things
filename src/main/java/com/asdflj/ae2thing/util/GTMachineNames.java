package com.asdflj.ae2thing.util;

import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;

/** Optional GT integration, kept separate so ordinary interface names do not load GT classes. */
final class GTMachineNames {

    private static final String MACHINE_PREFIX = "gt.blockmachines.";

    private GTMachineNames() {}

    static String getDisplayName(String rawName) {
        if (!rawName.startsWith(MACHINE_PREFIX)) return rawName;
        String metaName = rawName.substring(MACHINE_PREFIX.length());
        for (IMetaTileEntity machine : GregTechAPI.METATILEENTITIES) {
            if (machine != null && metaName.equals(machine.getMetaName())) {
                // Some GT machines format their names at runtime and have no <rawName>.name translation.
                return machine.getLocalName();
            }
        }
        return rawName;
    }
}
