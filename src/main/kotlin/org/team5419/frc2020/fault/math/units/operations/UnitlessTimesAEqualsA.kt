/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright 2019, Green Hope Falcons
 */

package org.team5419.frc2020.fault.math.units.operations

import org.team5419.frc2020.fault.math.units.SIKey
import org.team5419.frc2020.fault.math.units.SIUnit
import org.team5419.frc2020.fault.math.units.Unitless

operator fun <A : SIKey> SIUnit<Unitless>.times(other: SIUnit<A>) = SIUnit<A>(value.times(other.value))
