package magym.rectangles.presentation.screen.menu

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.alert_menu.view.*
import magym.rectangles.R
import magym.rectangles.presentation.screen.surface.GameSurfaceView
import magym.rectangles.util.enum.MenuAlertMode
import magym.rectangles.util.extension.isVisible
import magym.rectangles.util.extension.onClick


class MenuAlert(
    private val activity: Activity,
    private val surface: GameSurfaceView
) {

    private val seekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onStopTrackingTouch(seekBar: SeekBar) {}
        override fun onStartTrackingTouch(seekBar: SeekBar) {}
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            surface.cell = progress + MIN_CELL_SIZE
            setTextSeekBar()
        }
    }

    private var alertMenuLayout = initConstraintLayout(R.layout.alert_menu)
    private lateinit var alertDialog: AlertDialog

    fun openMainMenu() = createAlertDialogMenu(MenuAlertMode.MENU)

    fun openPauseMenu() = createAlertDialogMenu(MenuAlertMode.PAUSE)

    fun openEndMenu(results: String = "") {
        createAlertDialogMenu(MenuAlertMode.END)
        alertMenuLayout.end_text.text = results
    }

    private fun createAlertDialogMenu(mode: MenuAlertMode) {
        alertDialog = AlertDialog.Builder(activity).setCancelable(false).setView(alertMenuLayout).show()

        when (mode) {
            MenuAlertMode.MENU -> alertMenuLayout.menu.isVisible = true
            MenuAlertMode.PAUSE -> alertMenuLayout.pause.isVisible = true
            MenuAlertMode.END -> alertMenuLayout.end.isVisible = true
        }

        alertMenuLayout.menu_initial_start.onClick = ::openStartMenu
        alertMenuLayout.menu_initial_info.onClick = ::openInfoMenu
        alertMenuLayout.menu_settings_back.onClick = ::closeSettings
        alertMenuLayout.menu_play.onClick = ::openGame
        alertMenuLayout.menu_info_back.onClick = ::closeInfo
        alertMenuLayout.map_size_seekBar.setOnSeekBarChangeListener(seekBarChangeListener)

        alertMenuLayout.pause_continue.onClick = ::continueGame
        alertMenuLayout.pause_again.onClick = ::againStartGame
        alertMenuLayout.pause_surrender.onClick = ::surrender
        alertMenuLayout.pause_menu.onClick = ::closeGameFromPause

        alertMenuLayout.end_again.onClick = ::againStartGame
        alertMenuLayout.end_menu.onClick = ::closeGameFromEnd
    }

    private fun openStartMenu() {
        alertMenuLayout.map_size_seekBar.progress = surface.cell - MIN_CELL_SIZE
        setTextSeekBar()
        alertMenuLayout.menu_initial.isVisible = false
        alertMenuLayout.menu_settings.isVisible = true
    }

    private fun openInfoMenu() {
        alertMenuLayout.menu_initial.isVisible = false
        alertMenuLayout.menu_info.isVisible = true
    }

    private fun closeSettings() {
        alertMenuLayout.menu_settings.isVisible = false
        alertMenuLayout.menu_initial.isVisible = true
    }

    private fun openGame() {
        val checkedIndex = alertMenuLayout.menu_radio.checkedIndex()
        alertDialog.close()
        surface.createNewMap(checkedIndex)
    }

    private fun closeInfo() {
        alertMenuLayout.menu_info.isVisible = false
        alertMenuLayout.menu_initial.isVisible = true
    }

    private fun continueGame() {
        alertDialog.close()
    }

    private fun againStartGame() {
        alertDialog.close()
        surface.recreateMap()
    }

    private fun surrender() {
        alertMenuLayout.end_text.text = surface.findResults(true)
        alertMenuLayout.pause.isVisible = false
        alertMenuLayout.end.isVisible = true
    }

    private fun closeGameFromPause() {
        surface.clearMap()
        alertMenuLayout.pause.isVisible = false
        alertMenuLayout.menu.isVisible = true
    }

    private fun closeGameFromEnd() {
        surface.clearMap()
        alertMenuLayout.end.isVisible = false
        alertMenuLayout.menu.isVisible = true
    }

    private fun AlertDialog.close() {
        (alertMenuLayout.parent as ViewGroup).removeAllViews()
        this.cancel()
        alertMenuLayout = initConstraintLayout(R.layout.alert_menu)
    }

    @SuppressLint("SetTextI18n")
    private fun setTextSeekBar(x: Int = surface.nX, y: Int = surface.nY) {
        alertMenuLayout.map_size.text = "Размер карты: ${x - 2}x${y - 2}"
    }

    private fun initConstraintLayout(res: Int) = activity.layoutInflater.inflate(res, null) as ConstraintLayout

    fun RadioGroup.checkedIndex(): Int {
        val checkedId = this.checkedRadioButtonId
        val checkedRadioButton = this.findViewById(checkedId) as RadioButton
        return this.indexOfChild(checkedRadioButton)
    }

    companion object {
        const val MIN_CELL_SIZE = 50
    }

}