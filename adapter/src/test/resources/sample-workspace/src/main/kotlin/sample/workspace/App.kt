package sample.workspace

class App {
    private val member: String = "test"
    private val foo: String? = System.getProperty("foo")
    private val cwd: String = System.getProperty("user.dir")
    private val msg: String? = System.getenv("MSG")
    val greeting: String
        get() {
            val local: Int = 123
            return "Hello world."
        }
    
    override fun toString(): String = "App"
}

fun main(args: Array<String>) {
    println(App().greeting)
}
