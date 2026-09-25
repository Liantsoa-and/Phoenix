package utils;

import java.io.File;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClassScanner {

    public static List<Class<?>> scanAll() {
        List<Class<?>> classes = new ArrayList<>();
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL rootUrl = classLoader.getResource("");
            if (rootUrl != null) {
                File rootDir = new File(rootUrl.getFile());
                scanDirectory(rootDir, rootDir, classLoader, classes);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return classes;
    }

    private static void scanDirectory(File root, File current, ClassLoader classLoader, List<Class<?>> classes) {
        File[] files = current.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(root, file, classLoader, classes);
            } else if (file.getName().endsWith(".class")) {
                String relativePath = root.toURI().relativize(file.toURI()).getPath();
                String className = relativePath.replace(File.separatorChar, '.')
                        .replace(".class", "");
                try {
                    classes.add(Class.forName(className, false, classLoader));
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    // ignore unresolvable classes
                }
            }
        }
    }

    public static List<Class<?>> filterByPackage(List<Class<?>> classes, String packageName) {
        return classes.stream()
                .filter(c -> c.getPackageName().equals(packageName))
                .collect(Collectors.toList());
    }

    public static List<Class<?>> filterByAnnotation(List<Class<?>> classes, Class<? extends Annotation> annotation) {
        return classes.stream()
                .filter(c -> c.isAnnotationPresent(annotation))
                .collect(Collectors.toList());
    }

    public static List<Class<?>> getClasses(String packageName, Class<? extends Annotation> annotation) {
        List<Class<?>> all = scanAll();
        List<Class<?>> filtered = filterByPackage(all, packageName);
        return filterByAnnotation(filtered, annotation);
    }
}
