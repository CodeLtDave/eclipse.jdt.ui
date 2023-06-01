package package1;

public class Example<T> {
	private T value;

	public static <T> void bar(Example<T> example, T value) {
		example.value= value; // Class generic parameter
	}
}
