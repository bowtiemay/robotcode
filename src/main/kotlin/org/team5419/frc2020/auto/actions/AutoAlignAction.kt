package org.team5419.frc2020.auto.actions

import org.team5419.frc2020.subsystems.Vision
import org.team5419.frc2020.subsystems.Drivetrain
import org.team5419.fault.auto.Action

class AutoAlignAction() : Action() {
    init {
        Vision.calculate()
        finishCondition.set({ Vision.aligned })
    }

    override public fun start(){
        Vision.calculate()
    }

    override public fun update() {
        Drivetrain.setPercent(Vision.autoAlign())
    }

    override public fun finish() {
        Vision.off()
        Drivetrain.setPercent(0.0, 0.0)
    }
}
