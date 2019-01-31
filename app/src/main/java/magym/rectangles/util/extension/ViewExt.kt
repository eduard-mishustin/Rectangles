package magym.rectangles.util.extension

import android.view.View
import android.widget.EditText

/**
 * Удобная проперти для установки в качестве онклика — лямбды
 */
var View.onClick: () -> Unit
    get() = {}
    set(value) = setOnClickListener { value() }

/**
 * Видимость View-виджета
 * @return true, если видимость View.VISIBLE
 * Если определить, как true, то View станет View.VISIBLE,
 * если как false, то View.GONE
 */
var View.isVisible: Boolean
    get() = visibility == View.VISIBLE
    set(value) {
        visibility = if (value) View.VISIBLE else View.GONE
    }

fun EditText.text(): String {
    val text = this.text.toString()

    return if (text == "") this.hint.toString()
    else text
}