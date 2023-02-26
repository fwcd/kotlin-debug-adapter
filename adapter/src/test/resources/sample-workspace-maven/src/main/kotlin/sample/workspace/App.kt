package sample.workspace

class App {
    private val member: String? = System.getProperty("test")
    val greeting: String
        get() {
            val local: Int = 123
            return "Hello world."
        }
    
    override fun toString(): String = "App"
}

fun main(args: Array<String>) {
    println(App().greeting)
    testClassPath(System.getProperty("TestClassPath").toBoolean())
}

fun testClassPath(enabled: Boolean){
    if(enabled){
        // Check if the test-class is present on the jvm class-path
        Class.forName("sample.workspace.ExampleTest");
    }
}