import java.util.List;
import java.util.function.BiConsumer;

class Foo {
	public static void walk(I e) {
		e.getChildren().forEach(Foo::walk);
		final BiConsumer<IWalker, I> walk= (iWalker, e1) -> Foo.walk(e1);
	}

	interface I {
		List<I> getChildren();
	}
}
