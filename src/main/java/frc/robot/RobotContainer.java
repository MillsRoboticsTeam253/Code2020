package frc.robot;

import com.ctre.phoenix.music.Orchestra;
import com.kauailabs.navx.frc.AHRS;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj.SPI.Port;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.geometry.Pose2d;
import edu.wpi.first.wpilibj.geometry.Rotation2d;
import edu.wpi.first.wpilibj.geometry.Translation2d;
import edu.wpi.first.wpilibj.trajectory.Trajectory;
import edu.wpi.first.wpilibj.trajectory.TrajectoryConfig;
import edu.wpi.first.wpilibj.trajectory.TrajectoryGenerator;
import edu.wpi.first.wpilibj.trajectory.TrajectoryUtil;
import edu.wpi.first.wpilibj.trajectory.constraint.DifferentialDriveVoltageConstraint;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import edu.wpi.first.wpilibj2.command.button.JoystickButton;
import edu.wpi.first.wpilibj2.command.button.POVButton;
import frc.robot.Constants.DriverConstants;
import frc.robot.Constants.OrchestraConstants;
import frc.robot.autonomous.Dashboard;
import frc.robot.autonomous.TrajectoryTracker;
import frc.robot.commands.ConveyorQueue;
import frc.robot.commands.Drive;
import frc.robot.subsystems.Conveyor;
import frc.robot.subsystems.Drivetrain;
import frc.robot.subsystems.Intake;
import frc.robot.subsystems.Shooter;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

public class RobotContainer {
    public static Drivetrain drivetrain;
    public static Orchestra orchestra;
    
    private static int songIndex = 0;
    
    public static Intake intake;
    public static Conveyor conveyor;
    public static Shooter shooter;

    public static Dashboard falconDashboard;
    private static NetworkTable limelight;

    public static AHRS navX;
    
    private static final XboxController driver = new XboxController(Constants.InputPorts.xboxController);
    private static final JoystickButton driver_X = new JoystickButton(driver, 3);
    private static final POVButton DPAD_RIGHT = new POVButton(driver, 90);
    private static final POVButton DPAD_LEFT = new POVButton(driver, 270);
    
    private static RobotContainer instance;
    public static RobotContainer getInstance(){
        if (instance == null) instance = new RobotContainer();
        return instance;
    }
    
    private RobotContainer(){
        navX = new AHRS(Port.kMXP);
        limelight = NetworkTableInstance.getDefault().getTable("limelight");
        
        drivetrain = Drivetrain.getInstance();
        drivetrain.setDefaultCommand(new Drive(Drive.State.CheesyDriveOpenLoop));
        
        orchestra = new Orchestra(Drivetrain.motors);
        // set to true for competition bindings
        bindOrchestraOI(false);
    
        intake = Intake.getInstance();
    
        conveyor = Conveyor.getInstance();
        conveyor.setDefaultCommand(new ConveyorQueue());
    
        shooter = Shooter.getInstance();
    
        falconDashboard = Dashboard.getInstance();
    
        bindOI();
    }
    
    private void bindOrchestraOI(boolean competitionMode){
        if (competitionMode) {
            // bindings for competition
        } else {
            // bindings for off-competition
            loadSong();
            driver_X.whenPressed(orchestra.isPlaying() ? () -> orchestra.pause() : () -> orchestra.play(), drivetrain);
            DPAD_RIGHT.whenPressed(() -> {
                songIndex++;
                if (songIndex > OrchestraConstants.numSongs) songIndex = 0;
                loadSong();
            });
            DPAD_LEFT.whenPressed(() -> {
                if (orchestra.isPlaying()) {
                    // restart song
                    orchestra.stop();
                    orchestra.play();
                } else {
                    songIndex--;
                    if (songIndex < 0) songIndex = OrchestraConstants.numSongs;
                    loadSong();
                }
            });
        }
    }
    
    private void loadSong(){
        orchestra.loadMusic(String.format("songs/%s.chrp", OrchestraConstants.songs[songIndex]));
    }

    /**
     * Binds operator input to Commands 
     */
    private void bindOI() {

    }

    /**
     * Constructs and returns the Command to run during the autonomous period
     * 
     * @return the Command to run during autonomous
     */
    public Command getAutonomousCommand() {
        // TODO: Replace this code with the selector logic to select the proper autonomousit  sequence (aka, decide how we want to select auto)
        Trajectory initialTrajectory;
        try {
            initialTrajectory = TrajectoryUtil.fromPathweaverJson(Paths.get("/home/lvuser/deploy/paths/InitiationToTarget.json"));
        } catch(IOException e) {
            initialTrajectory = TrajectoryGenerator.generateTrajectory(
            // Start at the origin facing the +X direction
            new Pose2d(0, 5, new Rotation2d(0)),
            // Pass through these two interior waypoints, making an 's' curve path
            List.of(
                new Translation2d(1, 5.5),
                new Translation2d(2, 4.5)
            ),
            // End 3 meters straight ahead of where we started, facing forward
            new Pose2d(3, 5, new Rotation2d(0)),
            // Pass config
            new TrajectoryConfig(2, 4)
                .setKinematics(Drivetrain.KINEMATICS)
                .addConstraint(new DifferentialDriveVoltageConstraint(Drivetrain.FEEDFORWARD, Drivetrain.KINEMATICS, 10))
            );
        }

        // Workaround for try-catch block "initialTrajectory may have already been assigned"
        final Trajectory initialAutonomousTrajectory = initialTrajectory;

        /* Commands common to all autonomous sequences */
        SequentialCommandGroup initializeAutonomous = new SequentialCommandGroup(
            new InstantCommand(navX::reset),
            new InstantCommand(() -> Drivetrain.ODOMETRY.resetPosition(initialAutonomousTrajectory.sample(0).poseMeters, 
                                     Rotation2d.fromDegrees(navX.getAngle()))),
            new InstantCommand(drivetrain::resetEncoders)
        );

        /* Adding all the autonomous commands into a single sequence */
        return new SequentialCommandGroup(initializeAutonomous, new TrajectoryTracker(initialTrajectory));
    }
    
    /**
     * Returns the deadbanded throttle input from the main driver controller
     * 
     * @return the deadbanded throttle input from the main driver controller
     */
    public static double getThrottleValue() {
        // Controllers y-axes are natively up-negative, down-positive. This method corrects that by returning the opposite of the y-value
        return -deadbandX(driver.getY(GenericHID.Hand.kLeft), DriverConstants.kJoystickDeadband);
    }
    
    /**
     * Returns the deadbanded turn input from the main driver controller
     * 
     * @return the deadbanded turn input from the main driver controller 
     */
    public static double getTurnValue() {
        return deadbandX(driver.getX(GenericHID.Hand.kRight), DriverConstants.kJoystickDeadband);
    }
    
    /**
     * Deadbands an input to [-1, -deadband], [deadband, 1], rescaling inputs to be linear from (deadband, 0) to (1,1)
     * 
     * @param input The input value to rescale
     * @param deadband The deadband 
     * @return the input rescaled and to fit [-1, -deadband], [deadband, 1]
     */
    public static double deadbandX(double input, double deadband) {
        if (Math.abs(input) <= deadband) {
            return 0;
        } else if (Math.abs(input) == 1) {
            return input;
        } else {
            return (1 / (1 - deadband) * (input + Math.signum(-input) * deadband));
        }
    }

    /**
     * Set the LED mode on the Limelight
     * 
     * @param ledMode The mode to set the Limelight LEDs to
     */
    public void setLEDMode(LEDMode ledMode) {
        limelight.getEntry("ledMode").setNumber(ledMode.val);
    }

    /**
     * Sets the appearance of the Limelight camera stream
     * 
     * @param stream Stream mode to set the Limelight to
     */
    public void setStreamMode(StreamMode stream) {
        limelight.getEntry("stream").setNumber(stream.val);
    }

    /**
     * Sets Limelight vision pipeline
     * 
     * @param pipeline The pipeline to use
     */
    public void setPipeline(VisionPipeline pipeline) {
        limelight.getEntry("pipeline").setNumber(pipeline.val);
    }

    /**
     * Returns the horizontal offset between the target and the crosshair in degrees
     * 
     * @return the horizontal offset between the target and the crosshair in degrees
     */
    public static double getXOffset() {
        return -limelight.getEntry("tx").getDouble(0);
    }

    /**
     * Returns the vertical offset between the target and the crosshair in degrees
     * 
     * @return the vertical offset between the target and the crosshair in degrees
     */
    public static double getYOffset() {
        return -limelight.getEntry("ty").getDouble(0.0);
    }

    /**
     * Enum representing the different possible Limelight LED modes
     */
    public static enum LEDMode {
        PIPELINE(0), OFF(1), BLINK(2), ON(3);

        public int val;

        private LEDMode(int val) {
            this.val = val;
        }
    }

    /**
     * Enum representing the different possible Limelight stream modes
     */
    public static enum StreamMode {
        SIDE_BY_SIDE(0), PIP_MAIN(1), PIP_SECONDARY(2);

        public int val;
        
        private StreamMode(int val){
            this.val = val;
        }
    }

    /**
     * Enum representing the different possible Limelight vision pipelines
     */
    public static enum VisionPipeline {
        VISION(0), DRIVER(1);

        public int val;

        private VisionPipeline(int val){
            this.val = val;
        }
    }
}
