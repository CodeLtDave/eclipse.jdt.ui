package package1;

import java.io.Serializable;

class Example<A extends Throwable, B extends Runnable, C extends Runnable & Serializable> {

	int j;

	public static <A extends Throwable, B extends Runnable, C extends Runnable & Serializable> void bar(Example<A, B, C> example) {
		example.j= 0;
	}
}
