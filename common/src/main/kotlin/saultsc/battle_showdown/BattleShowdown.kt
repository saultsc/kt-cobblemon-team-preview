/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package saultsc.battle_showdown

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object BattleShowdown {
    const val MODID = "battle_showdown"

    @JvmField
    val LOGGER: Logger = LogManager.getLogger(MODID)

    fun init() {
        LOGGER.info("Launching {}...", MODID)
    }
}