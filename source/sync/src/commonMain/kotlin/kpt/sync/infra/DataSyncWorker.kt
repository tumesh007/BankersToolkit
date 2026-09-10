/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See See https://github.com/openMF/kmp-project-template/blob/main/LICENSE
 */
@file:OptIn(io.github.mobilebytelabs.worker.ExperimentalWorkerApi::class)

package kpt.sync.infra

import io.github.mobilebytelabs.worker.CoroutineWorker
import io.github.mobilebytelabs.worker.WorkResult
import io.github.mobilebytelabs.worker.WorkerContext
import kpt.core.base.data.infra.Synchronizer
import kpt.core.base.datastore.infra.ChangeListVersions
import kpt.core.base.datastore.infra.SyncStatePersister

public class DataSyncWorker(
    context: WorkerContext,
    private val persister: SyncStatePersister,
) : CoroutineWorker(context), Synchronizer {

    private var workingVersions: ChangeListVersions = ChangeListVersions()

    override suspend fun getChangeListVersions(): ChangeListVersions = workingVersions

    override suspend fun updateChangeListVersions(update: ChangeListVersions.() -> ChangeListVersions) {
        workingVersions = workingVersions.update()
    }

    override suspend fun doWork(): WorkResult {
        workingVersions = persister.read()
        persister.write(workingVersions)
        return WorkResult.success()
    }
}
