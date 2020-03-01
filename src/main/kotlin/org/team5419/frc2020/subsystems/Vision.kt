package org.team5419.frc2020.subsystems

import org.team5419.frc2020.tab
import org.team5419.frc2020.VisionConstants
import org.team5419.fault.subsystems.Subsystem
import org.team5419.fault.math.units.*
import org.team5419.fault.math.units.derived.*
import org.team5419.fault.math.geometry.Rotation2d
import org.team5419.fault.hardware.Limelight
import org.team5419.fault.hardware.Limelight.LightMode
import edu.wpi.first.wpilibj.shuffleboard.*
import edu.wpi.first.wpilibj.controller.PIDController
import org.team5419.fault.input.DriveSignal


object Vision : Subsystem("Vision") {
    // config limelight
    val limelight = Limelight(
        networkTableName = "limelight",
        inverted = false,

        mTargetHeight = VisionConstants.TargetHeight,
        mCameraHeight = VisionConstants.CameraHeight,
        mCameraAngle = Rotation2d( VisionConstants.CameraAngle )
    )

    // settings

    public var offset = VisionConstants.TargetOffset

    public val maxSpeed = VisionConstants.MaxAutoAlignSpeed

    // PID loop controller
    public val controller: PIDController =
        PIDController(
            VisionConstants.PID.P,
            VisionConstants.PID.I,
            VisionConstants.PID.D
        ).apply {
            setTolerance( VisionConstants.Tolerance )
        }

    // add the pid controller to shuffleboard
    init {
        tab.addNumber("Offset", { limelight.horizontalOffset })
        tab.addBoolean("Aligned", { aligned() })
    }

    // auto alignment

    public val targetFound
        get() = limelight.targetFound && limelight.verticalOffset != 0.0

    public fun aligned(): Boolean{
        // println("at set point: ${controller.atSetpoint()}")
        // println("target found: ${limelight.targetFound}")
        // println("verticle offset: ${limelight.verticalOffset}")
        return targetFound && controller.atSetpoint() && Drivetrain.averageSpeed.value < 0.1
    }

    public fun getShotSetpoint() = Lookup.get(limelight.horizontalDistance.inMeters())

    public fun calculate() = controller.calculate(limelight.horizontalOffset + offset)

    public fun autoAlign() : DriveSignal {
        // println("Error: ${limelight.horizontalOffset + offset}")
        // if(
        //     limelight.zoom == 1 &&
        //     Math.abs(limelight.horizontalOffset) < VisionConstants.MaxOffsetFor2XZoom
        // ) {
        //     // println("zoom in")
        //     limelight.zoom = 2
        //     controller.setPID(
        //         VisionConstants.PID.P,
        //         VisionConstants.PID.I,
        //         VisionConstants.PID.D
        //     )
        // } else if( !targetFound ){
        //     // println("zoom out")
        //     limelight.zoom = 1
        //     controller.setPID(
        //         VisionConstants.PID2.P,
        //         VisionConstants.PID2.I,
        //         VisionConstants.PID2.D
        //     )
        // }

        // get the pid loop output
        var output = calculate()

        // println( limelight.horizontalOffset )

        // can we align / do we need to allign?
        if ( !targetFound || aligned() )
            return DriveSignal(0.0, 0.0)

        // limit the output
        if (output >  maxSpeed) output =  maxSpeed
        if (output < -maxSpeed) output = -maxSpeed

        return DriveSignal(output, -output)
    }

    public fun on() {
        limelight.lightMode = LightMode.On
    }

    public fun off() {
        limelight.lightMode = LightMode.Off
    }
}
