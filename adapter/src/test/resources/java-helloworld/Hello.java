import java.io.File;

public class Hello {
    public static void main(String[] args) {
        System.out.println("Hello, world. " + new File(".").getAbsoluteFile().getParent());
        System.out.println("user.dir: " + System.getProperty("user.dir"));
    }
}
