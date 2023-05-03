package p;

public class Input {
    private int counter;
    private InnerClass inner;
    private static int staticInt = 0;

    public static void toRefactor(Input input) {
    	staticMethod();
    	staticInt=staticInt+1;
    	int localInt = 0;
        input.counter=input.counter+1;
        input.instanceMethod();
        input.inner.printHello();
    }

    public void instanceMethod() {
    	Input.toRefactor(this);
    }

    public static void staticMethod() {
        Input foo = new Input();
        Input.toRefactor(foo);
    }
    
    private class InnerClass {
        public void printHello() {
            System.out.println("Hello from InnerClass!");
        }
    }
}