package xyz.malkki.neostumbler.di

import org.koin.dsl.module
import xyz.malkki.neostumbler.R
import xyz.malkki.neostumbler.StumblerApplication
import xyz.malkki.neostumbler.ichnaeaupload.AndroidIchnaeaAutoUploadToggler
import xyz.malkki.neostumbler.ichnaeaupload.AndroidIchnaeaReportUploadStarter
import xyz.malkki.neostumbler.ichnaeaupload.IchnaeaAutoUploadToggler
import xyz.malkki.neostumbler.ichnaeaupload.IchnaeaClientProvider
import xyz.malkki.neostumbler.ichnaeaupload.IchnaeaReportUploadStarter

val uploadModule = module {
    single<IchnaeaAutoUploadToggler> { AndroidIchnaeaAutoUploadToggler(get()) }

    single<IchnaeaReportUploadStarter> {
        AndroidIchnaeaReportUploadStarter(
            get(),
            notificationChannelId = StumblerApplication.REPORT_UPLOAD_NOTIFICATION_CHANNEL_ID,
            notificationIcon = R.drawable.upload_24px,
            notificationTitle = R.string.notification_sending_reports,
        )
    }

    single<IchnaeaClientProvider> { IchnaeaClientProvider(get(), get()) }
}
