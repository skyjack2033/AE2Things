package com.asdflj.ae2thing.coremod.transform;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.asdflj.ae2thing.coremod.ClassTransformer;

public class DualityInterfaceTransformer extends ClassTransformer.ClassMapper {

    public static final DualityInterfaceTransformer INSTANCE = new DualityInterfaceTransformer();

    private static final String ADAPTOR = "appeng/util/InventoryAdaptor";
    private static final String GET_ADAPTOR = "(Ljava/lang/Object;Lnet/minecraftforge/common/util/ForgeDirection;)Lappeng/util/InventoryAdaptor;";

    @Override
    protected ClassVisitor getClassMapper(ClassVisitor downstream) {
        return new ClassVisitor(Opcodes.ASM5, downstream) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                String[] exceptions) {
                MethodVisitor parent = super.visitMethod(access, name, desc, signature, exceptions);
                if (!name.equals("pushPattern") && !name.equals("pushItemsOut")) {
                    return parent;
                }
                // AE2 now dispatches both item and fluid pattern inputs; FC's converting adaptor was removed.
                return new MethodVisitor(api, parent) {

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        if (opcode == Opcodes.INVOKESTATIC && owner.equals(ADAPTOR)
                            && name.equals("getAdaptor")
                            && desc.equals(GET_ADAPTOR)) {
                            owner = "com/asdflj/ae2thing/coremod/hooker/CoreModHooks";
                        }
                        super.visitMethodInsn(opcode, owner, name, desc, itf);
                    }
                };
            }
        };
    }
}
