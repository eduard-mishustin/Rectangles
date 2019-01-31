package magym.rectangles.util.extension

import android.util.Log

fun String.log() = Log.d("myTag", this)

fun Any.log() = Log.d("myTag", this.toString())

fun Exception.log() = Log.e("myTag", "", this)

fun Throwable.log() = Log.e("myTag", "", this)