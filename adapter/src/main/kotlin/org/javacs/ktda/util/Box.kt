package org.javacs.ktda.util

/** A simple boxing wrapper. Useful for captured local variables that have to be mutated. */
class Box<T>(var value: T)
