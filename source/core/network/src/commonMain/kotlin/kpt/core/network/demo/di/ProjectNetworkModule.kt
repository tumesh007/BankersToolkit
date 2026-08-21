/*
 * Copyright 2025 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
package kpt.core.network.demo.di

import kpt.core.base.network.restApi
import kpt.core.network.BuildKonfig
import kpt.core.network.demo.cloudtodo.api.createJsonPlaceholderApi
import kpt.core.network.demo.crypto.api.createCoinGeckoApi
import kpt.core.network.demo.currency.api.createFrankfurterApi
import kpt.core.network.demo.economic.api.createFredApi
import kpt.core.network.demo.economic.api.createWorldBankApi
import kpt.core.network.demo.economic.config.FredApiConfig
import org.koin.dsl.module

val ProjectNetworkModule = module {
    single<FredApiConfig> {
        FredApiConfig(apiKey = null)
    }

    restApi("frankfurter") { it.createFrankfurterApi() }
    restApi("coingecko") { it.createCoinGeckoApi() }
    restApi("fred") { it.createFredApi() }
    restApi("worldbank") { it.createWorldBankApi() }
    restApi("jsonplaceholder") { it.createJsonPlaceholderApi() }
}
