public class SubClass extends SuperClass {

    public static String bar(SubClass subClass, String bar) {
        String i = subClass.bar(bar);
        return i;
    }

    public static void staticMethod() {
        SubClass instance = new SubClass();
        String j = SubClass.bar(instance, "bar");
    }
}