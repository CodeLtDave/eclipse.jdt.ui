package package1;

public class Example<T, U, Z> {
    private T value1;
    private U value2;

    public static <Z, T, U> void bar(Example<T, U, Z> foo, T value1, U value2) {
    	foo.value1 = value1; // First generic type parameter
    	foo.value2 = value2; // Second generic type parameter
    }
}
