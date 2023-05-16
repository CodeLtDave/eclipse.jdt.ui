package package1;

public class Example<T> {
	private T value;

	public static <T> void bar(T value, Example<T> example) {
		example.value= value; // Class generic parameter
	}
}
