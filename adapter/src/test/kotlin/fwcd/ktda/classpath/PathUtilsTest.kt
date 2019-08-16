package org.javacs.ktda.classpath

import org.junit.Test
import org.junit.Assert.assertEquals

class PathUtilsTest {
	@Test
	fun testFilePathToJVMClassNames() {
		assertEquals(
			listOf("com.abc.MyClass", "com.abc.MyClassKt"),
			toJVMClassNames("/project/src/main/kotlin/com/abc/MyClass.kt")
		)
	}
}
