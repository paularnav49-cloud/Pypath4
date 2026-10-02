package com.pypath.app.data.content

import android.content.Context
import com.pypath.app.data.model.Course
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** Source of course content. Today it reads a bundled JSON file; later it could be remote. */
interface CourseRepository {
    suspend fun loadCourse(): Course
}

class AssetCourseRepository(
    private val context: Context,
    private val assetPath: String = "course/course.json",
) : CourseRepository {

    @Volatile private var cached: Course? = null

    override suspend fun loadCourse(): Course = cached ?: withContext(Dispatchers.IO) {
        val text = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        CourseParser.parse(text).also { cached = it }
    }
}

/** Shared JSON configuration for course content (also used by tests and future remote loaders). */
object CourseParser {
    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
    }

    fun parse(text: String): Course = json.decodeFromString(Course.serializer(), text)
}
