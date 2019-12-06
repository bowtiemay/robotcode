package org.team5419.frc2019.subsystems

import com.ctre.phoenix.motorcontrol.*
import com.ctre.phoenix.sensors.PigeonIMU
import edu.wpi.first.wpilibj.Notifier
import org.team5419.frc2019.DriveConstants
import org.team5419.fault.hardware.ctre.*
import org.team5419.fault.math.geometry.Rotation2d
import org.team5419.fault.math.geometry.degree
import org.team5419.fault.math.localization.TankPositionTracker
import org.team5419.fault.math.physics.DifferentialDrive
import org.team5419.fault.math.units.*
import org.team5419.fault.math.units.derived.*
import org.team5419.fault.math.units.native.nativeUnits
import org.team5419.fault.subsystems.drivetrain.AbstractTankDrive
import org.team5419.fault.trajectory.followers.RamseteFollower

object Drivetrain : AbstractTankDrive() {

    private const val kPositionSlot = 0
    private const val kVelocitySlot = 1
    private const val kTurnSlot = 2

    private val periodicIO = PeriodicIO()
    private var currentState = State.Nothing
    private var wantedState = State.Nothing

    override val differentialDrive = DriveConstants.kDriveModel

    override val trajectoryFollower = RamseteFollower(DriveConstants.kBeta, DriveConstants.kZeta)

    override val localization = TankPositionTracker(
        { angle },
        { leftDistance.value },
        { rightDistance.value }
    )

    // hardware
    override val leftMasterMotor = BerkeliumSRX(DriveConstants.kLeftMasterPort, DriveConstants.kNativeGearboxConversion)
    private val leftSlave1 = BerkeliumSPX(DriveConstants.kLeftSlave1Port, DriveConstants.kNativeGearboxConversion)
    private val leftSlave2 = BerkeliumSPX(DriveConstants.kLeftSlave2Port, DriveConstants.kNativeGearboxConversion)

    override val rightMasterMotor = BerkeliumSRX(DriveConstants.kRightMasterPort, DriveConstants.kNativeGearboxConversion)
    private val rightSlave1 = BerkeliumSPX(DriveConstants.kRightSlave1Port, DriveConstants.kNativeGearboxConversion)
    private val rightSlave2 = BerkeliumSPX(DriveConstants.kRightSlave2Port, DriveConstants.kNativeGearboxConversion)

    private val gyro = PigeonIMU(DriveConstants.kGyroPort)

    init {
        leftSlave1.follow(leftMasterMotor)
        leftSlave2.follow(leftMasterMotor)

        rightSlave1.follow(rightMasterMotor)
        rightSlave2.follow(rightMasterMotor)

        leftSlave1.victorSPX.setInverted(InvertType.FollowMaster)
        leftSlave2.victorSPX.setInverted(InvertType.FollowMaster)
        leftMasterMotor.outputInverted = true

        rightSlave1.victorSPX.setInverted(InvertType.FollowMaster)
        rightSlave2.victorSPX.setInverted(InvertType.FollowMaster)
        rightMasterMotor.outputInverted = false

        leftMasterMotor.talonSRX.configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Relative, kPositionSlot, 0)
        rightMasterMotor.talonSRX.configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Relative, kPositionSlot, 0)

        leftMasterMotor.talonSRX.configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Relative, kVelocitySlot, 0)
        rightMasterMotor.talonSRX.configSelectedFeedbackSensor(FeedbackDevice.CTRE_MagEncoder_Relative, kVelocitySlot, 0)

        leftMasterMotor.encoder.encoderPhase = DriveConstants.kEncoderPhase
        rightMasterMotor.encoder.encoderPhase = DriveConstants.kEncoderPhase

        rightMasterMotor.talonSRX.configRemoteFeedbackFilter(gyro.deviceID, RemoteSensorSource.Pigeon_Yaw, 1, 0)
        rightMasterMotor.talonSRX.configSelectedFeedbackSensor(FeedbackDevice.RemoteSensor1, 1 ,0)
        rightMasterMotor.talonSRX.configSelectedFeedbackCoefficient(
            DriveConstants.kPigeonConversion.value, 1, 0
        )

        leftMasterMotor.talonSRX.setSelectedSensorPosition(0, kPositionSlot, 0)
        leftMasterMotor.talonSRX.setSelectedSensorPosition(0, kVelocitySlot, 0)
        rightMasterMotor.talonSRX.setSelectedSensorPosition(0, kPositionSlot, 0)
        rightMasterMotor.talonSRX.setSelectedSensorPosition(0, kVelocitySlot, 0)

        rightMasterMotor.talonSRX.configAuxPIDPolarity(true, 0)
        rightMasterMotor.talonSRX.configClosedLoopPeakOutput(kTurnSlot, 1.0, 0)
        rightMasterMotor.talonSRX.configAllowableClosedloopError(kTurnSlot, 0, 0)

        leftMasterMotor.talonSRX.setStatusFramePeriod(StatusFrameEnhanced.Status_2_Feedback0, 5, 0)
        rightMasterMotor.talonSRX.setStatusFramePeriod(StatusFrameEnhanced.Status_2_Feedback0, 5, 0)

        leftMasterMotor.useMotionProfileForPosition = true
        rightMasterMotor.useMotionProfileForPosition = true

        leftMasterMotor.motionProfileCruiseVelocity = DriveConstants.kMotionMagicVelocity
        leftMasterMotor.motionProfileAcceleration = DriveConstants.kMotionMagicAcceleration

        rightMasterMotor.motionProfileCruiseVelocity = DriveConstants.kMotionMagicVelocity
        rightMasterMotor.motionProfileAcceleration = DriveConstants.kMotionMagicAcceleration

        isBraking = false

        localization.reset()
        Notifier {
            localization.update()
        }.startPeriodic(1.0 / 100.0)
    }

    override val leftDistance: SIUnit<Meter>
        get() = -DriveConstants.kNativeGearboxConversion.fromNativeUnitPosition(periodicIO.leftRawSensorPosition)

    override val rightDistance: SIUnit<Meter>
        get() = DriveConstants.kNativeGearboxConversion.fromNativeUnitPosition(periodicIO.rightRawSensorPosition)

    override val leftDistanceError: SIUnit<Meter>
        get() = -DriveConstants.kNativeGearboxConversion.fromNativeUnitPosition(periodicIO.leftRawDistanceError)

    override val rightDistanceError: SIUnit<Meter>
        get() = DriveConstants.kNativeGearboxConversion.fromNativeUnitPosition(periodicIO.rightRawDistanceError)

    val angle: Rotation2d
        get() = periodicIO.gyroAngle.degree

    override val angularVelocity: AngularVelocity
        get() = periodicIO.angularVelocity.degree.velocity

    override val turnError: Rotation2d
        get() = periodicIO.turnError.degrees


    override fun setPercent(left: Double, right: Double) = setOpenLoop(left, right)

    fun setOpenLoop(left: Double, right: Double) {
        wantedState = State.OpenLoop

        periodicIO.leftDemand = left
        periodicIO.rightDemand = right

        periodicIO.leftFeedforward = 0.0.volts
        periodicIO.rightFeedforward = 0.0.volts
    }

    override fun setPosition(distance: SIUnit<Meter>) {
        wantedState = State.Position

        leftMasterMotor.talonSRX.selectProfileSlot(kPositionSlot, 0)
        rightMasterMotor.talonSRX.selectProfileSlot(kPositionSlot, 0)

        periodicIO.leftDemand = distance.value
        periodicIO.rightDemand = distance.value

        periodicIO.leftFeedforward = 0.0.volts
        periodicIO.rightFeedforward = 0.0.volts
    }

    override fun setTurn(angle: Rotation2d, type: TurnType) {
        wantedState = State.Turning

        rightMasterMotor.talonSRX.selectProfileSlot(kPositionSlot, 0)
        rightMasterMotor.talonSRX.selectProfileSlot(kTurnSlot, 1)

        leftMasterMotor.talonSRX.follow(rightMasterMotor.talonSRX, FollowerType.AuxOutput1)

        periodicIO.angleTarget = angle.degree

        periodicIO.leftDemand = 0.0
        periodicIO.rightDemand = 0.0

        periodicIO.leftFeedforward = 0.0.volts
        periodicIO.rightFeedforward = 0.0.volts
    }

    fun setVelocity(leftVelocity: SIUnit<LinearVelocity>, rightVelocity: SIUnit<LinearVelocity>, leftFF: SIUnit<Volt>, rightFF: SIUnit<Volt>) {
        wantedState = State.Velocity

        leftMasterMotor.talonSRX.selectProfileSlot(kVelocitySlot, 0)
        rightMasterMotor.talonSRX.selectProfileSlot(kVelocitySlot, 0)

        periodicIO.leftDemand = leftVelocity.value
        periodicIO.rightDemand = rightVelocity.value

        periodicIO.leftFeedforward = leftFF
        periodicIO.rightFeedforward = rightFF
    }

    override fun setOutput(wheelVelocities: DifferentialDrive.WheelState, wheelVoltages: DifferentialDrive.WheelState) {
        wantedState = State.PathFollowing

        periodicIO.leftDemand = wheelVelocities.left * differentialDrive.wheelRadius
        periodicIO.rightDemand = wheelVelocities.right * differentialDrive.wheelRadius

        periodicIO.leftFeedforward = wheelVoltages.left.volts
        periodicIO.rightFeedforward = wheelVoltages.right.volts
    }

    override fun zeroOutputs() {
        wantedState = State.Nothing

        periodicIO.leftDemand = 0.0
        periodicIO.rightDemand = 0.0

        periodicIO.leftFeedforward = 0.0
        periodicIO.rightFeedforward = 0.0
    }

    override fun periodic() {
        periodicIO.leftVoltage = leftMasterMotor.voltageOutput
        periodicIO.rightVoltage = rightMasterMotor.voltageOutput

        periodicIO.leftCurrent = leftMasterMotor.talonSRX.outputCurrent.amps
        periodicIO.rightCurrent = rightMasterMotor.talonSRX.outputCurrent.amps

        periodicIO.leftRawSensorPosition = leftMasterMotor.encoder.rawPosition
        periodicIO.rightRawSensorPosition = rightMasterMotor.encoder.rawPosition

        periodicIO.leftRawDistanceError = leftMasterMotor.talonSRX.closedLoopError.nativeUnits
        periodicIO.rightRawDistanceError = rightMasterMotor.talonSRX.closedLoopError.nativeUnits
        periodicIO.leftRawSensorVelocity = leftMasterMotor.encoder.rawVelocity
        periodicIO.rightRawSensorVelocity = rightMasterMotor.encoder.rawVelocity

        periodicIO.gyroAngle = gyro.fusedHeading

        val xyz = DoubleArray(3)
        gyro.getRawGyro(xyz)
        periodicIO.angularVelocity = xyz[1]

        when(wantedState) {
            State.Nothing -> {
                leftMasterMotor.setNeutral()
                rightMasterMotor.setNeutral()
            }
            State.OpenLoop -> {
                leftMasterMotor.setPercent(periodicIO.leftDemand)
                rightMasterMotor.setPercent(periodicIO.rightDemand)
            }
            State.PathFollowing, State.Velocity -> {
                leftMasterMotor.setVelocity(periodicIO.leftDemand, periodicIO.leftFeedforward)
                rightMasterMotor.setVelocity(periodicIO.rightDemand, periodicIO.rightFeedforward)
            }
            State.Position -> {
                leftMasterMotor.setPosition(periodicIO.leftDemand,0.0)
                rightMasterMotor.setPosition(periodicIO.rightDemand,0.0)
            }
            State.Turning -> {
                rightMasterMotor.talonSRX.set(
                        ControlMode.PercentOutput, 0.0,
                        DemandType.AuxPID, rightMasterMotor.talonSRX.getSelectedSensorPosition(1) +
                            periodicIO.angleTarget
                )
            }
        }

        if(wantedState != currentState) currentState = wantedState
    }


    class PeriodicIO {
        // input
        var leftVoltage = 0.0.volts
        var rightVoltage = 0.0.volts

        var leftCurrent = 0.0.amps
        var rightCurrent = 0.0.amps

        var leftRawSensorPosition = 0.0.nativeUnits
        var rightRawSensorPosition = 0.0.nativeUnits

        var leftRawDistanceError = 0.0.nativeUnits
        var rightRawDistanceError = 0.0.nativeUnits

        var leftRawSensorVelocity = 0.0.meters.velocity
        var rightRawSensorVelocity = 0.0.meters.velocity

        var gyroAngle = 0.0.degrees
        var angularVelocity = 0.0.radians.velocity
        var turnError = 0.0.nativeUnits

        // output
        var leftDemand: Double  = 0.0
        var rightDemand: Double = 0.0

        var leftFeedforward = 0.0.volts
        var rightFeedforward = 0.0.volts

        var angleTarget = 0.0
    }

    private enum class State { Nothing, Turning, Velocity, PathFollowing, OpenLoop, Position }

}