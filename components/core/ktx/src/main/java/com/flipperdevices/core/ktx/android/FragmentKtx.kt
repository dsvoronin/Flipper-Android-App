package com.flipperdevices.core.ktx.android

import android.os.Bundle
import androidx.fragment.app.Fragment

inline fun Fragment.withArgs(argsBuilder: Bundle.() -> Unit): Fragment {
    val bundle = arguments ?: Bundle()
    argsBuilder.invoke(bundle)
    arguments = bundle
    return this
}
