# Retrofit generic type information
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations

# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items)
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# With R8 full mode generic signatures are stripped for classes that are not kept
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Retrofit does reflection on method and parameter annotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep generic types for Retrofit
-keep class kotlin.Metadata { *; }

# If you're using Gson with Retrofit
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Keep your API interface and data classes
-keep interface com.flo.japhelper.network.** { *; }
-keep class com.flo.japhelper.model.** { *; }

# Fix for ParameterizedType casting issue with R8
-keepattributes Signature
-keep class java.lang.reflect.ParameterizedType { *; }
-keep class java.lang.reflect.Type { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keep class kotlinx.coroutines.** { *; }