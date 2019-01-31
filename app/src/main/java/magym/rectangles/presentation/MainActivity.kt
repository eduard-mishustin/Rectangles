package magym.rectangles.presentation

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import magym.rectangles.R
import magym.rectangles.presentation.screen.menu.MenuAlert
import magym.rectangles.presentation.screen.surface.GameSurfaceView
import magym.rectangles.presentation.screen.surface.MainView
import magym.rectangles.util.extension.onClick

class MainActivity : AppCompatActivity(), MainView {

    private val alert: MenuAlert by lazy {
        MenuAlert(this, surface)
    }

    private lateinit var surface: GameSurfaceView

    override var titleToolbar: String
        get() = toolbar.text.toString()
        set(value) {
            toolbar.text = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        surface = surface_view.apply {
            setIActivity(this@MainActivity)
        }

        pause.onClick = alert::openPauseMenu

        alert.openMainMenu()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun createAlertDialogEndGame(message: String) = alert.openEndMenu(message)

}