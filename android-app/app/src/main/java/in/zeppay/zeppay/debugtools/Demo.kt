package `in`.zeppay.zeppay.debugtools

import `in`.zeppay.zeppay.BuildConfig
import `in`.zeppay.zeppay.ZepViewModel
import `in`.zeppay.zeppay.ui.Screen
import `in`.zeppay.zeppay.ussd.Engine

/**
 * Debug builds only: `adb shell am start -n in.zeppay.zeppay.debug/in.zeppay.zeppay.MainActivity --es demo sim:ok`
 * turns on the simulated carrier with a script (ok | ambiguous | fail | needs_account),
 * or `--es demo skip-setup` jumps past onboarding for screenshots. For screenshots of each
 * screen: `tab:split`, `go:settings` (see [screens]), `theme:light|dark|auto`.
 */
object Demo {
    fun apply(vm: ZepViewModel, cmd: String) {
        if (!BuildConfig.DEBUG) return
        when {
            cmd.startsWith("sim:") -> { Engine.simulate = true; Engine.simScript = cmd.removePrefix("sim:") }
            cmd == "skip-setup" -> vm.finishSetup()
            cmd.startsWith("tab:") -> vm.setTab(cmd.removePrefix("tab:"))
            cmd.startsWith("theme:") -> vm.setTheme(cmd.removePrefix("theme:"))
            cmd.startsWith("go:") -> screens[cmd.removePrefix("go:")]?.let { vm.home(); vm.go(it()) }
        }
    }

    private val screens: Map<String, () -> Screen> = mapOf(
        "settings" to { Screen.Settings }, "help" to { Screen.Help }, "activity" to { Screen.Activity },
        "update" to { Screen.Update }, "helper" to { Screen.Helper }, "banksetup" to { Screen.BankSetup },
        "menucheck" to { Screen.MenuCheck }, "licenses" to { Screen.Licenses }, "setup" to { Screen.Setup() },
        "profile" to { Screen.Profile() }, "receive" to { Screen.Receive }, "newsplit" to { Screen.NewSplit(duo = true) },
        "payto-upi" to { Screen.PayTo("upi") }, "payto-bank" to { Screen.PayTo("bank") },
    )
}
