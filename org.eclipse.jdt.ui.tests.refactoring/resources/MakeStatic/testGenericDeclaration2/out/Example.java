package package1;

public class Example<T, U> {
	private T value1;

	private U value2;

	public static <T, U> void bar(Example<T, U> example, T value1, U value2) {
		example.value1= value1; // First generic type parameter
		example.value2= value2; // Second generic type parameter
	}
}
