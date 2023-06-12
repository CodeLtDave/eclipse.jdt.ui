import java.util.Map;

class Foo<T extends Map<? extends Runnable, ? extends Throwable>> {

	int j;

	public static <T extends Map<? extends Runnable, ? extends Throwable>> void bar(Foo<T> foo) {
		foo.j= 0;
	}
}
