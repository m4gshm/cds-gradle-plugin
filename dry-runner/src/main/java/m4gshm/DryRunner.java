package m4gshm;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static java.lang.Class.forName;
import static java.lang.System.arraycopy;
import static java.lang.System.exit;

public class DryRunner {
    public static void main(String[] args) throws NoSuchMethodException, ClassNotFoundException,
            InvocationTargetException, IllegalAccessException {
        if (args.length == 0) throw new IllegalArgumentException("mainClass must be first argument");
        String mainClassName = args[0];
        String[] mainArgs = new String[args.length - 1];
        arraycopy(args, 1, mainArgs, 0, mainArgs.length);

        Class<?> mainClass = forName(mainClassName, false, Thread.currentThread().getContextClassLoader());
        Method mainMethod = mainClass.getDeclaredMethod("main", String[].class);
        mainMethod.setAccessible(true);

        try {
            mainMethod.invoke(null, new Object[]{mainArgs});
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw e;
        } catch (Exception ignored) {
        }
        exit(0);
    }
}
