package ru.pdd.academy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.pdd.academy.ui.AcademyApp
import ru.pdd.academy.ads.AdConsentController

class MainActivity : ComponentActivity() {
    private var ads: AdConsentController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (AppConstants.adsEnabled) ads = AdConsentController(applicationContext).also { it.refresh(this) }
        setContent { val vm: StudyViewModel = viewModel(); AcademyApp(vm, ads) }
    }

    override fun onDestroy() {
        ads?.dispose()
        ads = null
        super.onDestroy()
    }
}
