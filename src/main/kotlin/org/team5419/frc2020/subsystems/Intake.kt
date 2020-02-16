package org.team5419.frc2020.subsystems

import org.team5419.frc2020.IntakeConstants
import org.team5419.fault.subsystems.Subsystem
import org.team5419.fault.math.units.native.NativeUnitRotationModel
import org.team5419.fault.hardware.ctre.BerkeliumSRX

object Intake : Subsystem("Intake") {
    // motors

    val intakeModel = NativeUnitRotationModel(IntakeConstants.IntakeTicksPerRotation)
    val deployModel = NativeUnitRotationModel(IntakeConstants.DeployTicksPerRotation)

    val intakeMotor = BerkeliumSRX(IntakeConstants.IntakePort, intakeModel)
    val deployMotor = BerkeliumSRX(IntakeConstants.DeployPort, deployModel)

    // intake mode

    public enum class IntakeMode {
        STORED,
        DEPLOY,

        INTAKE,
        OUTTAKE,

        OFF
    }

    public var mode = IntakeMode.STORED
        set(mode: IntakeMode) {
            println(mode)

            if ( mode == field ) return

            if ( mode == IntakeMode.INTAKE ) {
                intakeMotor.setPercent( 1.0 )
                deployMotor.setPercent( 0.0 )
            }

            if ( mode == IntakeMode.OUTTAKE ) {
                intakeMotor.setPercent( -1.0 )
                deployMotor.setPercent( 0.0 )
            }

            if ( mode == IntakeMode.STORED ) {
                intakeMotor.setPercent( 0.0 )
                deployMotor.setPercent( 0.4 )
            }

            if ( mode == IntakeMode.DEPLOY ) {
                intakeMotor.setPercent( 0.0 )
                deployMotor.setPercent( -0.4 )
            }

            if ( mode == IntakeMode.OFF ) {
                intakeMotor.setPercent( 0.0 )
                deployMotor.setPercent( 0.0 )
            }


            field = mode
        }

    // public api

    public fun intake() { mode == IntakeMode.INTAKE }

    public fun outtake() { mode == IntakeMode.OUTTAKE }

    public fun turnOff() { mode == IntakeMode.OFF }

    public fun store() { mode = IntakeMode.STORED }

    public fun isActive() = mode == IntakeMode.INTAKE

    // subsystem functions

    fun reset() {
        mode = IntakeMode.STORED
    }

    override fun autoReset() = reset()
    override fun teleopReset() = reset()

    override fun periodic() {
    }
}
