/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.data.demo.di

import kpt.core.base.data.infra.NetworkMonitor
import kpt.core.base.store.infra.impl.RoomBookkeeper
import kpt.core.base.store.infra.impl.RoomSubmitOutbox
import kpt.core.base.store.submit.OfflineSubmitSyncer
import kpt.core.base.store.submit.SubmitOutbox
import kpt.core.data.demo.alerts.AlertsRepository
import kpt.core.data.demo.alerts.impl.AlertsRepositoryImpl
import kpt.core.data.demo.banking.BillReminderRepository
import kpt.core.data.demo.banking.LoanRepository
import kpt.core.data.demo.banking.impl.BillReminderRepositoryImpl
import kpt.core.data.demo.banking.impl.LoanRepositoryImpl
import kpt.core.data.demo.cloudtodo.CloudTodoRepository
import kpt.core.data.demo.cloudtodo.impl.CloudTodoRepositoryImpl
import kpt.core.data.demo.crypto.CryptoRepository
import kpt.core.data.demo.crypto.impl.CryptoRepositoryImpl
import kpt.core.data.demo.currency.CurrencyRepository
import kpt.core.data.demo.currency.impl.CurrencyRepositoryImpl
import kpt.core.data.demo.economic.EconomicRatesRepository
import kpt.core.data.demo.economic.MacroIndicatorsRepository
import kpt.core.data.demo.economic.impl.EconomicRatesRepositoryImpl
import kpt.core.data.demo.economic.impl.MacroIndicatorsRepositoryImpl
import kpt.core.data.demo.watchlist.WatchlistRepository
import kpt.core.data.demo.watchlist.impl.WatchlistRepositoryImpl
import kpt.core.data.di.OutboxQualifiers
import kpt.core.data.visit.VisitRepository
import kpt.core.data.visit.impl.VisitRepositoryImpl
import kpt.core.database.AppDatabase
import kpt.core.model.demo.alerts.PriceAlert
import kpt.core.model.demo.banking.BillReminder
import kpt.core.model.demo.banking.Loan
import kpt.core.store.AppStoreRegistry
import kpt.core.store.demo.cloudtodo.impl.CloudTodoKey
import org.koin.dsl.module
import org.mobilenativefoundation.store.store5.Bookkeeper

val ProjectRepositoryModule = module {
    single { get<AppDatabase>().visitDao }
    single<VisitRepository> {
        VisitRepositoryImpl(visitDao = get())
    }

    single { get<AppDatabase>().watchlistDao }
    single<WatchlistRepository> {
        WatchlistRepositoryImpl(
            watchlistStore = get(AppStoreRegistry.Watchlist),
            dao = get(),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }

    single { get<AppDatabase>().loanDao }
    single { get<AppDatabase>().billReminderDao }
    single<LoanRepository> {
        LoanRepositoryImpl(
            loansStore = get(AppStoreRegistry.Loans),
            loanDao = get(),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }
    single<BillReminderRepository> {
        BillReminderRepositoryImpl(
            billRemindersStore = get(AppStoreRegistry.BillReminders),
            billReminderDao = get(),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }

    single<CurrencyRepository> {
        CurrencyRepositoryImpl(
            exchangeRatesStore = get(AppStoreRegistry.ExchangeRates),
            rateHistoryStore = get(AppStoreRegistry.RateHistory),
            spotRateStore = get(AppStoreRegistry.SpotRate),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }
    single<CryptoRepository> {
        CryptoRepositoryImpl(
            coinMarketsStore = get(AppStoreRegistry.CoinMarkets),
            coinDetailStore = get(AppStoreRegistry.CoinDetail),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }

    single<Bookkeeper<CloudTodoKey>> {
        RoomBookkeeper(dao = get(), keySerializer = { "cloudTodo:${it.id}" })
    }
    single<CloudTodoRepository> {
        CloudTodoRepositoryImpl(
            readStore = get(AppStoreRegistry.CloudTodo),
            writeStore = get(AppStoreRegistry.CloudTodoMutable),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }

    single<EconomicRatesRepository> {
        EconomicRatesRepositoryImpl(
            interestRateSeriesStore = get(AppStoreRegistry.InterestRateSeries),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }
    single<MacroIndicatorsRepository> {
        MacroIndicatorsRepositoryImpl(
            macroIndicatorStore = get(AppStoreRegistry.MacroIndicator),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }

    single { get<AppDatabase>().alertDao }
    single<AlertsRepository> {
        AlertsRepositoryImpl(
            alertsStore = get(AppStoreRegistry.Alerts),
            alertDao = get(),
            networkMonitor = get(),
            fetchedAtRepository = get(),
        )
    }
}
