package io.goodforgod.dummymapper;

import com.intellij.lang.jvm.JvmModifier;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.LibraryUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ! NO DESCRIPTION !
 *
 * @author GoodforGod
 * @since 27.11.2019
 */
public class PsiJavaFileScanner {

    private static final Map<String, Class> SIMPLE_FIELDS = getSimpleFields();

    private final Map<String, Map> scanned = new HashMap<>();

    @SuppressWarnings("unchecked")
    public @NotNull
    Map<String, Object> scan(@Nullable PsiJavaFile file) {
        if (file == null)
            return Collections.emptyMap();

        final Map<String, Object> structure = new HashMap<>();
        scanned.put(getFullName(file), structure);
        PsiClassType superType = ((PsiClassType)file.getClasses()[0].getSuperClassType());
        Project project = superType.getResolveScope().getProject();
        PsiFile[] filesByName = FilenameIndex.getFilesByName(project,
                superType.getCanonicalText() + ".class",
                GlobalSearchScope.allScope(project));

        VirtualFile[] libraryRoots = LibraryUtil.getLibraryRoots(file.getProject());
        // IS WORKING
        PsiClass superClass = file.getClasses()[0].getSuperClass();
        // IS WORKING
        PsiClass[] classesByName = PsiShortNamesCache.getInstance(project).getClassesByName(superType.rawType().getPresentableText(), GlobalSearchScope.allScope(project));

        if (file.getClasses().length > 0) {
            final PsiClass target = file.getClasses()[0];
            final PsiField[] fields = target.getAllFields();
            for (PsiField field : fields) {
                if (isFieldEnum(field)) {
                    ((List) structure.computeIfAbsent(field.getType().getPresentableText(), k -> new ArrayList<String>()))
                            .add(field.getName());
                } else if (isFieldSimple(field) && isFieldValid(field)) {
                    structure.put(field.getName(), SIMPLE_FIELDS.get(field.getType().getCanonicalText()));
                } else {
                    getTypeResolvedJavaFile(field.getType()).ifPresent(f -> {
                        final Map map = scanned.get(getFullName(f));
                        if (map == null) {
                            final Map<String, Object> scanned = scan(f);
                            final Object values = scanned.get(field.getType().getPresentableText());
                            if (values instanceof Collection) {
                                structure.put(field.getName(), values);
                            } else {
                                structure.put(field.getName(), scanned);
                            }
                        } else {
                            structure.put(field.getName(), map);
                        }
                    });
                }
            }
        }

        return structure;
    }

    private static Optional<PsiJavaFile> getTypeResolvedJavaFile(@NotNull PsiType type) {
        return Optional.ofNullable(type.getResolveScope())
                .map(GlobalSearchScope::getProject)
                .flatMap(p -> Arrays.stream(FilenameIndex.getFilesByName(p,
                        type.getCanonicalText() + ".class",
                        GlobalSearchScope.allScope(p)))
                        .findFirst()
                        .filter(f -> f instanceof PsiJavaFile)
                        .map(f -> ((PsiJavaFile) f)));
    }

    private static @NotNull
    String getFullName(@NotNull PsiJavaFile file) {
        return file.getPackageName() + "." + file.getName();
    }

    private static boolean isFieldEnum(@NotNull PsiField field) {
        final PsiType[] superTypes = field.getType().getSuperTypes();
        return superTypes.length > 0 && "java.lang.Enum".equals(superTypes[0].getCanonicalText());
    }

    private static boolean isFieldSimple(@NotNull PsiField field) {
        return SIMPLE_FIELDS.containsKey(field.getType().getCanonicalText());
    }

    private static boolean isFieldValid(@NotNull PsiField field) {
        return !field.hasModifier(JvmModifier.STATIC)
                && !field.hasModifier(JvmModifier.VOLATILE)
                && !field.hasModifier(JvmModifier.NATIVE)
                && !field.hasModifier(JvmModifier.STATIC)
                && !field.hasModifier(JvmModifier.SYNCHRONIZED)
                && !field.hasModifier(JvmModifier.TRANSITIVE);
    }

    private static Map<String, Class> getSimpleFields() {
        final List<Class<?>> simpleClasses = Stream.of(
                UUID.class,
                String.class,
                Integer.class,
                Byte.class,
                Short.class,
                Integer.class,
                Long.class,
                Float.class,
                Double.class,
                Boolean.class,
                Character.class,
                byte.class,
                int.class,
                long.class,
                float.class,
                double.class,
                boolean.class,
                char.class,
                Object.class,
                BigInteger.class,
                BigDecimal.class,
                Timestamp.class,
                Date.class,
                LocalDate.class,
                LocalTime.class,
                LocalDateTime.class)
                .collect(Collectors.toList());

        final Map<String, Class> map = new HashMap<>();
        simpleClasses.forEach(c -> {
            map.put(c.getName(), c);
            map.put(c.getSimpleName(), c);
        });

        return map;
    }
}