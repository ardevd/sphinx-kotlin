package chat.sphinx.resources

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.view.inputmethod.InputMethodManager
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding
import chat.sphinx.wrapper_view.Dp
import chat.sphinx.wrapper_view.Px

inline val Context.inputMethodManager: InputMethodManager?
    get() = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager

@ColorInt
@Suppress("NOTHING_TO_INLINE")
inline fun Context.getRandomColor(): Int {
    return ContextCompat.getColor(this, getRandomColorRes())
}

@ColorInt
@Suppress("NOTHING_TO_INLINE")
inline fun Context.getColorForKey(colorKey: String): Int {

    this.getSharedPreferences("sphinx_colors", Context.MODE_PRIVATE).let { sharedPrefs ->
        val colorHexCode = sharedPrefs.getString(colorKey, null)?.let { hexCode ->
            hexCode
        } ?: "#" + Integer.toHexString(ContextCompat.getColor(this, this.getRandomColorRes()))

        sharedPrefs?.edit()?.let { editor ->
            editor.putString(colorKey, colorHexCode).let { editor ->
                editor.apply()
            }
        }

        return Color.parseColor(colorHexCode)
    }

    return getRandomColor()
}

@ColorRes
@Suppress("NOTHING_TO_INLINE")
inline fun Context.getRandomColorRes(): Int {
    return listOf<@ColorRes Int>(
        R.color.randomColor1,
        R.color.randomColor2,
        R.color.randomColor3,
        R.color.randomColor4,
        R.color.randomColor5,
        R.color.randomColor6,
        R.color.randomColor7,
        R.color.randomColor8,
        R.color.randomColor9,
        R.color.randomColor10,
        R.color.randomColor11,
        R.color.randomColor12,
        R.color.randomColor13,
        R.color.randomColor14,
        R.color.randomColor15,
        R.color.randomColor16,
        R.color.randomColor17,
        R.color.randomColor18,
        R.color.randomColor19,
        R.color.randomColor20,
    ).shuffled()[0]
}

@Suppress("NOTHING_TO_INLINE")
inline fun Dp.toPx(context: Context): Px =
    Px(value * context.resources.displayMetrics.density)

@Suppress("NOTHING_TO_INLINE")
inline fun Px.toDp(context: Context): Dp =
    Dp(value / context.resources.displayMetrics.density)

@Suppress("NOTHING_TO_INLINE")
@Throws(Resources.NotFoundException::class)
inline fun ViewBinding.getString(@StringRes stringRes: Int): String =
    root.context.resources.getString(stringRes)

@ColorInt
@Suppress("NOTHING_TO_INLINE")
@Throws(Resources.NotFoundException::class)
inline fun ViewBinding.getColor(@ColorRes colorRes: Int): Int {
    return ContextCompat.getColor(root.context, colorRes)
}
