package package1;

public class Example {
    public static void bar(Example example) {
        Runnable runnable = () -> {
            System.out.println(example); // Outer class instance
        };

        new Thread(runnable).start();
    }
}
