package magym.rectangles.presentation.base.surface

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import magym.rectangles.util.extension.log

open class BaseSurfaceView(context: Context, attributeSet: AttributeSet) : SurfaceView(context, attributeSet),
    SurfaceHolder.Callback {

    private val parentJob = Job()
    private val coroutineContext get() = parentJob + Dispatchers.Default
    val scope = CoroutineScope(coroutineContext)

    private lateinit var renderingThread: RenderingThread

    init {
        isFocusable = true
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        renderingThread = RenderingThread(this, getHolder()).apply {
            threadRunning = true
            start()
        }
    }

    override fun surfaceChanged(arg0: SurfaceHolder, arg1: Int, arg2: Int, arg3: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        coroutineContext.cancel()

        var retry = true
        renderingThread.threadRunning = false

        while (retry) {
            try {
                renderingThread.join()
                retry = false
            } catch (e: InterruptedException) {
                e.log()
            }
        }
    }

}