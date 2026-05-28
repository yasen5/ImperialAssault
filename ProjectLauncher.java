import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

public final class ProjectLauncher {
    private static final Path SOURCE_ROOT = Path.of("src", "main", "java");
    private static final Path RESOURCES_ROOT = Path.of("src", "main", "resources");
    private static final Path OUTPUT_ROOT = Path.of("build", "javac-main", "classes");

    private ProjectLauncher() {
    }

    public static void run(String mainClassName, String[] args) throws Exception {
        compileProjectSources();
        URL[] urls = Files.isDirectory(RESOURCES_ROOT)
                ? new URL[] { OUTPUT_ROOT.toUri().toURL(), RESOURCES_ROOT.toUri().toURL() }
                : new URL[] { OUTPUT_ROOT.toUri().toURL() };
        try (URLClassLoader loader = new URLClassLoader(urls, ProjectLauncher.class.getClassLoader())) {
            Class<?> mainClass = Class.forName(mainClassName, true, loader);
            Method main = mainClass.getMethod("main", String[].class);
            try {
                main.invoke(null, (Object) args);
            } catch (InvocationTargetException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof Exception exception) {
                    throw exception;
                }
                if (cause instanceof Error error) {
                    throw error;
                }
                throw ex;
            }
        }
    }

    private static void compileProjectSources() throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            System.err.println("A JDK is required. Run these commands with a JDK, not a JRE.");
            return;
        }
        if (!Files.isDirectory(SOURCE_ROOT)) {
            System.err.println("Missing source directory: " + SOURCE_ROOT.toAbsolutePath());
            return;
        }

        Files.createDirectories(OUTPUT_ROOT);
        List<String> options = new ArrayList<>();
        options.add("-encoding");
        options.add("UTF-8");
        options.add("-d");
        options.add(OUTPUT_ROOT.toString());
        options.add("-classpath");
        options.add(OUTPUT_ROOT + File.pathSeparator + System.getProperty("java.class.path"));

        try (var stream = Files.walk(SOURCE_ROOT)) {
            stream.filter(path -> path.toString().endsWith(".java"))
                    .sorted(Comparator.comparing(Path::toString))
                    .map(Path::toString)
                    .forEach(options::add);
        }

        int result = compiler.run(null, null, null, options.toArray(String[]::new));
        if (result != 0) {
            System.err.println("javac failed with exit code " + result);
        }
    }
}
