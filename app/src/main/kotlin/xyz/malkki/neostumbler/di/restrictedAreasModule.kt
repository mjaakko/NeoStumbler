package xyz.malkki.neostumbler.di

import kotlinx.coroutines.flow.first
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import xyz.malkki.neostumbler.data.restrictedarea.DataStoreRestrictedAreaManager
import xyz.malkki.neostumbler.data.restrictedarea.RestrictedAreaManager
import xyz.malkki.neostumbler.report.postprocessor.ReportPostProcessor
import xyz.malkki.neostumbler.report.postprocessor.RestrictedAreaFilterer
import xyz.malkki.neostumbler.ui.viewmodel.RestrictedAreasViewModel

val restrictedAreasModule = module {
    single<RestrictedAreaManager> { DataStoreRestrictedAreaManager(context = get()) }

    factory {
        val restrictedAreaManager = get<RestrictedAreaManager>()

        RestrictedAreaFilterer(
            restrictedAreasProvider = {
                restrictedAreaManager.restrictedAreas.first().map { it.circle }
            }
        )
    } bind ReportPostProcessor::class

    viewModel<RestrictedAreasViewModel> { RestrictedAreasViewModel(restrictedAreaManager = get()) }
}
