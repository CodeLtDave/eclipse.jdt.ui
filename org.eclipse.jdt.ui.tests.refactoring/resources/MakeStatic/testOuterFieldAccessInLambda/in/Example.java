package package1;

public class Example {
    public void bar() {
        Runnable runnable = () -> {
            System.out.println(Example.this); // Outer class instance
        };

        new Thread(runnable).start();
    }
}
