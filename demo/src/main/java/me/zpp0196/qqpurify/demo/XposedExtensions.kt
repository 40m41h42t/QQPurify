package me.zpp0196.qqpurify.demo

import androidx.annotation.IntRange

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import me.zpp0196.reflectx.proxy.ProxyClass
import me.zpp0196.reflectx.proxy.ProxyFactory

import java.lang.NullPointerException
import java.lang.reflect.Member
import java.lang.reflect.Method

inline fun <reified T> proxy(obj: Any? = null): T {
    return when (obj) {
        null -> ProxyFactory.proxyClass(T::class.java)
        is Class<*> -> {
            ProxyFactory.proxyClass(obj) as T
        }
        else -> ProxyFactory.proxyObject(T::class.java, obj)
    }
}

inline fun <reified T> checkAndProxy(obj: Any): T? {
    val assignable = obj.javaClass.isAssignableFrom(ProxyClass.findClass(T::class.java))
    return if (assignable) ProxyFactory.proxyObject(T::class.java, obj) else null
}

val XC_LoadPackage.LoadPackageParam.isMainProcess: Boolean
    get() = processName == packageName

inline fun <reified T> XC_MethodHook.MethodHookParam.arg(): T? {
    args.forEach {
        if (it != null && T::class.java.isAssignableFrom(it::class.java)) {
            return it as T
        }
    }
    return null
}

inline fun <reified T> XC_MethodHook.MethodHookParam.arg(@IntRange(from = 0) i: Int): T {
    return args[i] as T ?: throw NullPointerException()
}

inline fun <reified T> XC_MethodHook.MethodHookParam.arg(@IntRange(from = 0) i: Int, def: T?): T? {
    return args[i] as T ?: def
}

typealias Callback = (param: XC_MethodHook.MethodHookParam) -> Unit
typealias Replacement = (param: XC_MethodHook.MethodHookParam) -> Any?

fun replaceMethod(method: Method, result: Any? = null) {
    XposedBridge.hookMethod(method, object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            super.beforeHookedMethod(param)
            param.result = result
        }
    })
}

fun replaceMethod(method: Method, replacement: Replacement) {
    XposedBridge.hookMethod(method, object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            super.beforeHookedMethod(param)
            param.result = replacement(param)
        }
    })
}

fun hookBefore(method: Member, before: Callback) {
    XposedBridge.hookMethod(method, object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            super.beforeHookedMethod(param)
            before(param)
        }
    })
}

fun hookAfter(method: Member, after: Callback) {
    XposedBridge.hookMethod(method, object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            super.afterHookedMethod(param)
            after.invoke(param)
        }
    })
}

fun hookMethod(method: Member, before: Callback? = null, after: Callback? = null) {
    XposedBridge.hookMethod(method, object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            super.beforeHookedMethod(param)
            before?.invoke(param)
        }

        override fun afterHookedMethod(param: MethodHookParam) {
            super.afterHookedMethod(param)
            after?.invoke(param)
        }
    })
}
