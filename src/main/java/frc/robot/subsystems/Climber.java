package frc.robot.subsystems;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;

import edu.wpi.first.wpilibj2.command.Subsystem;

public class Climber implements Subsystem{
    private static Climber instance;
    public static TalonSRX mainMotor;
    public static TalonSRX slaveMotor;

    private Climber(){
        slaveMotor.follow(mainMotor);
    }
    public static void climbUp(double speed){
        mainMotor.set(ControlMode.PercentOutput, speed);
    }
    public static Climber getInstance(){
        if(instance == null) {instance = new Climber();}
        return instance;
    }
    public static void stopMotors(){
        mainMotor.set(ControlMode.PercentOutput, 0.0f);
    }
    
}