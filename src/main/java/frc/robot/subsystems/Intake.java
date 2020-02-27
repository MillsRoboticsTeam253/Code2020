package frc.robot.subsystems;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;
import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;

import edu.wpi.first.wpilibj.controller.ArmFeedforward;
import edu.wpi.first.wpilibj.controller.ProfiledPIDController;
import edu.wpi.first.wpilibj.trajectory.TrapezoidProfile;
import edu.wpi.first.wpilibj2.command.ProfiledPIDSubsystem;
import frc.robot.Constants.IntakeConstants;
import frc.robot.Units;
import frc.robot.Util;


public class Intake extends ProfiledPIDSubsystem {
    
    private static final TalonSRX armMotor = Util.createTalonSRX(IntakeConstants.armMotor, false);
    private static final TalonSRX spinMotor = Util.createTalonSRX(IntakeConstants.spinMotor, false);
    private static final CANSparkMax conveyorMotor = Util.createSparkMAX(IntakeConstants.conveyorMotor, MotorType.kBrushless);

    private static final ArmFeedforward FEEDFORWARD = new ArmFeedforward(IntakeConstants.kS, IntakeConstants.kCos, IntakeConstants.kV, IntakeConstants.kA);
    
    private static Intake instance;
    public static Intake getInstance(){
        if (instance == null) instance = new Intake();
        return instance;
    }

    private Intake(){
        super(new ProfiledPIDController(IntakeConstants.kP , IntakeConstants.kI, IntakeConstants.kD,
            new TrapezoidProfile.Constraints(IntakeConstants.kMaxVelocity, IntakeConstants.kMaxAcceleration)), 0);    

        
        conveyorMotor.setInverted(true);
        conveyorMotor.burnFlash();
    }

    /**
     * Sets the conveyor to spin at a percent of max speed
     * @param speed Percent speed
     */
    public void setConveyor(double value) {
        conveyorMotor.set(value);
    }

    /**
     * Sets the intake to spin at a given voltage
     * 
     * @param value Percent of maximum voltage to send to motor
     */
    public void intake(double value) {
        spinMotor.set(ControlMode.PercentOutput, value);
    }

    /**
     * Set the intake to rotate manually (overriding the position control)
     * @param value Percent of maximum voltage to send to motor
     */
    public void rotate(double value) {
        armMotor.set(ControlMode.PercentOutput, value);
    }

    /**
     * Stops the motors 
     */
    public void stopMotors() {
        armMotor.set(ControlMode.PercentOutput, 0);
        spinMotor.set(ControlMode.PercentOutput, 0);
    }

    /**
     * Resets encoder to zero
     */
    public void resetEncoder() {
        resetEncoder(0);
    }

    /**
     * Resets encoder to a specified value
     * @param value Value to set encoder position to
     */
    public void resetEncoder(int value) {
        armMotor.setSelectedSensorPosition(0);
    }

    /**
     * @return the arm's current position as an encoder value
     */
    @Override
    public double getMeasurement() {
        return Units.IntakeUnits.TicksToRadians(armMotor.getSelectedSensorPosition());
    }

    /**
     * Uses the value calculated by ProfiledPIDSubsystem
     */
    @Override
    public void useOutput(double output, TrapezoidProfile.State setpoint) {
        // Calculate feedforward from the setpoint
        double feedforward = FEEDFORWARD.calculate(setpoint.position, setpoint.velocity);
        // Set motor, converting voltage to percent voltage
        armMotor.set(ControlMode.PercentOutput, (output + feedforward)/12.0);
    }
}
