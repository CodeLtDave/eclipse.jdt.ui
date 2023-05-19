class Foo {
    void test() {
        Runnable f = Foo::yyy;
    }
    
    String myField = "";
    static void yyy() {
        System.out.println(myField);
    }
}