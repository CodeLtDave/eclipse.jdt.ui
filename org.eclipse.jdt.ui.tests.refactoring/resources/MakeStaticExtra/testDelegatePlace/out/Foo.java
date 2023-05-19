public class Foo {
    int i;

    public void foo(int j) {
        foo(this, j);
    }

    public static void foo(int j, Foo foo) {
    }
}
