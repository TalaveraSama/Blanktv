package app.blanktv.tv

import android.app.Application
import app.blanktv.tv.di.AppContainer

class BlankTvApp : Application() {

    /** Process-wide object graph, created on first access rather than at startup. */
    val container: AppContainer by lazy { AppContainer(this) }
}
