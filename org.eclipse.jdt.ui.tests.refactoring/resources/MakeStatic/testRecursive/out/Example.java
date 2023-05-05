package package1;

public class Example {

    public static void toRefactor(Example bar, Example example) {
        Example.toRefactor(bar, example);
    }

    public void foo() {
        Example instance = new Example();
        Example.toRefactor(this, instance);
    }
}