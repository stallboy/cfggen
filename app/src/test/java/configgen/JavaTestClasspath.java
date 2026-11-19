package configgen;

import org.jspecify.annotations.NullMarked;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;

public final class JavaTestClasspath {
    private JavaTestClasspath() {
    }

    public static String classpathWithJSpecify(Class<?>... classes) throws URISyntaxException {
        StringBuilder classpath = new StringBuilder(classLocation(NullMarked.class));
        for (Class<?> type : classes) {
            classpath.append(File.pathSeparator).append(classLocation(type));
        }
        return classpath.toString();
    }

    private static String classLocation(Class<?> type) throws URISyntaxException {
        var codeSource = java.util.Objects.requireNonNull(type.getProtectionDomain().getCodeSource());
        return Path.of(codeSource.getLocation().toURI()).toString();
    }
}
