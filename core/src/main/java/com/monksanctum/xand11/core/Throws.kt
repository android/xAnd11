package org.monksanctum.xand11.core

import org.monksanctum.xand11.comm.XProtoReader
import org.monksanctum.xand11.comm.XProtoWriter
import kotlin.reflect.KClass

annotation class Throws(val kClass: KClass<*>,
                        val nClass: KClass<*> = Throws::class,
                        val dClass: KClass<*> = Throws::class) {
}