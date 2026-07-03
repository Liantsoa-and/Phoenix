package utils;

import java.io.File;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ClassScanner {

    /**
     * 1. Scanne tout le classpath (récursif) à partir de sa racine et retourne
     * la liste de toutes les classes trouvées. Aucun chemin en dur : la racine
     * est résolue dynamiquement via le ClassLoader.
     */
    public static List<Class<?>> scanAll() throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL root = cl.getResource("");

        if (root == null) {
            throw new IllegalStateException("Impossible de résoudre la racine du classpath.");
        }
        if (!"file".equals(root.getProtocol())) {
            throw new UnsupportedOperationException("Scan supporté uniquement sur classes physiques (exploded), URL=" + root);
        }

        File rootDir = new File(root.toURI());
        scanDirectory(rootDir, rootDir, cl, classes);

        return classes;
    }

    private static void scanDirectory(File rootDir, File current, ClassLoader cl, List<Class<?>> classes) throws Exception {
        File[] files = current.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(rootDir, file, cl, classes);
            } else if (file.getName().endsWith(".class")) {
                // Nom de classe reconstruit à partir du chemin relatif à la racine
                String relativePath = rootDir.toURI().relativize(file.toURI()).getPath();
                String className = relativePath.replace('/', '.').replace(".class", "");
                classes.add(cl.loadClass(className));
            }
        }
    }

    /**
     * 2. Filtre une liste de classes pour ne garder que celles appartenant
     * au package demandé.
     */
    public static List<Class<?>> filterByPackage(List<Class<?>> classes, String packageName) {
        List<Class<?>> result = new ArrayList<>();
        for (Class<?> clazz : classes) {
            Package pkg = clazz.getPackage();
            if (pkg != null && pkg.getName().equals(packageName)) {
                result.add(clazz);
            }
        }
        return result;
    }

    /**
     * 3. Filtre une liste de classes pour ne garder que celles portant
     * l'annotation demandée.
     */
    public static List<Class<?>> filterByAnnotation(List<Class<?>> classes, Class<? extends Annotation> annotation) {
        List<Class<?>> result = new ArrayList<>();
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(annotation)) {
                result.add(clazz);
            }
        }
        return result;
    }

    /**
     * 4. Fonction d'assemblage générique : scan complet → filtre package → filtre annotation.
     * Réutilisable pour n'importe quel package / annotation (pas seulement @Controller).
     */
    public static List<Class<?>> getClasses(String packageName, Class<? extends Annotation> annotation) throws Exception {
        List<Class<?>> all = scanAll();
        List<Class<?>> inPackage = filterByPackage(all, packageName);
        return filterByAnnotation(inPackage, annotation);
    }
}