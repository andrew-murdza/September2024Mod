import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Local compatibility transform of the pinned MIT-licensed Tiny Takeover release. */
public class PatchTinyTakeover {
    public static void main(String[] arguments) throws Exception {
        Path input = Path.of(arguments[0]);
        Path output = Path.of(arguments[1]);
        Map<String, String> renamedClasses = Map.of(
                "com/blackgear/vanillabackport/client/level/entities/model/ArmadilloModel",
                "com/blackgear/vanillabackport/client/level/model/entity/ArmadilloModel",
                "com/blackgear/vanillabackport/common/level/entities/armadillo/Armadillo",
                "com/blackgear/vanillabackport/common/level/entities/mob/animal/armadillo/Armadillo");
        int[] removedDolphinMethods = {0};
        int[] replacedAgingChecks = {0};
        Files.createDirectories(output.getParent());
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(input));
             ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(output))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                byte[] bytes = in.readAllBytes();
                if (entry.getName().endsWith(".class")) {
                    ClassReader reader = new ClassReader(bytes);
                    ClassWriter writer = new ClassWriter(0);
                    String className = reader.getClassName();
                    ClassVisitor remapper = new ClassRemapper(writer, new SimpleRemapper(renamedClasses));
                    ClassVisitor compatibility = new ClassVisitor(Opcodes.ASM9, remapper) {
                        @Override
                        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                         String signature, String[] exceptions) {
                            if (className.endsWith("/mixin/DolphinMixin")
                                    && (name.equals("isBaby") || name.equals("m_6162_")
                                    || name.equals("tiny_takeover_backport$mobInteract"))) {
                                removedDolphinMethods[0]++;
                                return null;
                            }
                            MethodVisitor method = super.visitMethod(access, name, descriptor, signature, exceptions);
                            if (className.endsWith("/mixin/MobMixin") && name.equals("onAiStep")) {
                                return new MethodVisitor(Opcodes.ASM9, method) {
                                    @Override
                                    public void visitTypeInsn(int opcode, String type) {
                                        if (opcode == Opcodes.INSTANCEOF && type.equals("net/minecraft/world/entity/animal/Dolphin")) {
                                            // AOE owns dolphin age. Preserve Tiny's particles and squid aging.
                                            type = "net/minecraft/world/entity/animal/Squid";
                                            replacedAgingChecks[0]++;
                                        }
                                        super.visitTypeInsn(opcode, type);
                                    }
                                };
                            }
                            return method;
                        }
                    };
                    reader.accept(compatibility, 0);
                    bytes = writer.toByteArray();
                }
                ZipEntry target = new ZipEntry(entry.getName());
                target.setTime(0);
                out.putNextEntry(target);
                out.write(bytes);
                out.closeEntry();
            }
        }
        if (removedDolphinMethods[0] != 2 || replacedAgingChecks[0] != 1) {
            throw new IllegalStateException("Unexpected Tiny Takeover bytecode: removed="
                    + removedDolphinMethods[0] + ", aging checks=" + replacedAgingChecks[0]);
        }
        System.out.println("Patched Tiny Takeover armadillo API and preserved AOE dolphin breeding/aging.");
    }
}
