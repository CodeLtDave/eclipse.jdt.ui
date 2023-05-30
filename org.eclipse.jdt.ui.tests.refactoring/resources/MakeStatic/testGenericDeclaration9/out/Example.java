package package1;

import java.util.List;

class Foo<T extends List<?>> {

	int j;

	public static <T extends List<?>> void bar(Foo<T> foo) {
		foo.j= 0;
	}
}
