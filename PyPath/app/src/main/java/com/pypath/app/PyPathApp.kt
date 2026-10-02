package com.pypath.app

import android.app.Application
import com.pypath.app.data.content.AssetCourseRepository
import com.pypath.app.data.content.CourseRepository
import com.pypath.app.data.progress.LocalProgressRepository
import com.pypath.app.data.progress.ProgressRepository

/** Simple manual DI container. Swap implementations here (e.g. remote content, cloud sync). */
class AppContainer(app: Application) {
    val courseRepository: CourseRepository = AssetCourseRepository(app)
    val progressRepository: ProgressRepository = LocalProgressRepository(app)
}

class PyPathApp : Application() {
    /**
     * Created on first use. The `:python` process (Practical tab runner) also instantiates this
     * Application class but never touches the container, so it never opens the progress store.
     */
    val container: AppContainer by lazy { AppContainer(this) }
}
