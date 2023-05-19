import java.util.List;
import java.util.function.BiConsumer;

class Foo {
	public void walk(I e) {
		e.getChildren().forEach(this::walk);
		final BiConsumer<IWalker, I> walk= Foo::walk;
	}

	interface I {
		List<I> getChildren();
	}
}
