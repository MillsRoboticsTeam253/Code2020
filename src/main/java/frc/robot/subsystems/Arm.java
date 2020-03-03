package frc.robot.subsystems;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;
import edu.wpi.first.wpilibj.DutyCycleEncoder;
import edu.wpi.first.wpilibj.controller.ArmFeedforward;
import edu.wpi.first.wpilibj.controller.ProfiledPIDController;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.trajectory.TrapezoidProfile;
import edu.wpi.first.wpilibj2.command.ProfiledPIDSubsystem;
import frc.robot.Constants.IntakeConstants;
import frc.robot.Util;

public class Arm extends ProfiledPIDSubsystem {
    
    private static final TalonSRX motor = Util.createTalonSRX(IntakeConstants.armMotor, false);
    
    private static final DutyCycleEncoder armEncoder = new DutyCycleEncoder(1);
    
    private static final ArmFeedforward FEEDFORWARD = new ArmFeedforward(IntakeConstants.kS, IntakeConstants.kCos, IntakeConstants.kV, IntakeConstants.kA);
    
    private static Arm instance;
    public static Arm getInstance() {
        if (instance == null) instance = new Arm();
        return instance;
    }
    
    /**
     * Enum class representing the two possible positions of the intake arm, UP and DOWN
     */
    public enum State {
        UP(4.22), DOWN(2.68);
        
        public double position;
        
        /**
         * @param position the value of the arm position in radians
         */
        private State(double position) {
            this.position = position;
        }
    }
    
    private Arm() {
        super(new ProfiledPIDController(IntakeConstants.kP , IntakeConstants.kI, IntakeConstants.kD,
                new TrapezoidProfile.Constraints(IntakeConstants.kMaxVelocity, IntakeConstants.kMaxAcceleration)), 0);
        
        armEncoder.setDistancePerRotation(2 * Math.PI);
    
        motor.configContinuousCurrentLimit(1);
        motor.configPeakCurrentLimit(0);
        motor.enableCurrentLimit(true);
        
        setGoal(0);
        disable();
        register();
    }
    
    /**
     * Set the intake to rotate manually (overriding the position control)
     * @param value Percent of maximum voltage to send to motor
     */
    public void rotate(double value) {
        motor.set(ControlMode.PercentOutput, value);
    }
    
    public void stopArm() {
        disable();
        motor.set(ControlMode.PercentOutput, 0);
    }
    
    /**
     * Resets encoders to zero
     */
    public void resetEncoders() {
        armEncoder.reset();
    }
    
    @Override
    public void periodic() {
        super.periodic();
        SmartDashboard.putNumber("encoder value", armEncoder.getDistance());
        SmartDashboard.putNumber("measurement", getMeasurement());
    }
    
    /**
     * @return the arm's current position as a radian measure
     */
    @Override
    public double getMeasurement() {
        return armEncoder.getDistance() - IntakeConstants.kInitialPosition;
    }
    
    /**
     * Uses the value calculated by ProfiledPIDSubsystem
     */
    @Override
    public void useOutput(double output, TrapezoidProfile.State setpoint) {
        // Calculate feedforward from the setpoint
        double feedforward = FEEDFORWARD.calculate(setpoint.position, setpoint.velocity);
        // Set motor, converting voltage to percent voltage
        motor.set(ControlMode.PercentOutput, (output + feedforward)/12.0);
        SmartDashboard.putNumber("pos", setpoint.position);
        SmartDashboard.putNumber("output", output/12);
        SmartDashboard.putNumber("feedforward + output", (output+feedforward)/12);
    }
}
