// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;


import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

// https://software-metadata.revrobotics.com/REVLib-2024.json
import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkLowLevel.MotorType;
import com.revrobotics.SparkAbsoluteEncoder.Type;
import com.revrobotics.AbsoluteEncoder;
import com.revrobotics.RelativeEncoder;

//import edu.wpi.first.wpilibj.ADXRS450_Gyro; // small FRC gyro in SPI slot
// https://dev.studica.com/releases/2024/NavX.json
import com.kauailabs.navx.frc.AHRS;
import edu.wpi.first.wpilibj.SPI;

//dunno
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj2.command.SubsystemBase;


/**
 * This is a demo program showing the use of the DifferentialDrive class,
 * specifically it contains
 * the code necessary to operate a robot with tank drive.
 */
public class Robot extends TimedRobot {

  ////////////////////////////////////////////////////////////////////////////
  // Constants we use //
  ////////////////////////////////////////////////////////////////////////////
  // We use 2 Xbox game pads to run the robot, 1 for the driver, 1 for the
  // operator. The driver usually just maneuvers the robot around the arena.
  // The operator manipulates the extra stuff for that year's robot (like
  // shooters, arms, winches ...).
  // "raw" mappings for the Xbox controller (and the clones we bought) below.
  // Following are for buttons that are either
  // pressed (getRawButton returns true) or not (getRawButton returns false).
  // XboxController has unique names for everything, but using the
  // lower level GenericHID raw reads lets us switch buttons quicker.
  public static final int kAButton = 1;
  public static final int kBButton = 2;
  public static final int kXButton = 3;
  public static final int kYButton = 4;
  public static final int kLeftBumper = 5;
  public static final int kRightBumper = 6;
  public static final int kBackButton = 7;
  public static final int kStartButton = 8;
  public static final int kLeftStickPress = 9;
  public static final int kRighttStickPress = 10;

  // following are for triggers/joy sticks
  // getRawAxis returns values from -1.0000 to 1.0000
  public static final int kLeftStickXaxis = 0;
  public static final int kleftStickYaxis = 1;
  public static final int kLeftTrigger = 2; // 0.00 to 1.00
  public static final int kRightTrigger = 3; // 0.00 to 1.00
  public static final int kRightStickXaxis = 4;
  public static final int kRightStickYaxis = 5;

  public static final int kIntakeCurrentLimit = 5;
  

  public static final double kDeadZone = 0.2;

  // The following constants are array indices for tuning values used in auto
  public static final int kClimberSpd = 0;
  public static final int kRotateSpd = 1;
  public static final int kShooterSpd = 2;
  public static final int kIntakeSpd = 3;
  public static final int kItems = 4; // set to last one above + 1

  // Sometimes the game controller triggers and joy sticks lie and give a non-0
  // value even though they aren't being touched. Filter out any reading less
  // than the kDeadZone.
  
  double power = 0.2;

   ////////////////////////////////////////////////////////////////////////////
  // Global variables //
  ////////////////////////////////////////////////////////////////////////////
  // following is the current index (will range from 0 to kItems - 1
  public int tCurIndex = 0;

  // the arrays that get indexed by tCurIndex
  public double[] taCurValue = new double[kItems];
  public double[] taDelta = new double[kItems];
  public String[] taLabel = new String[kItems]; // seen only on SmartDashboard

  boolean aEnabled = true; // state data for auto values tuning routines
  boolean bEnabled = true;
  boolean xEnabled = true;
  boolean yEnabled = true;

  int quadrant = 1;
  double direction = 1; // otherwise -1
  int whichMotor = 0; // 0 is pivot 1 is roll motor

  int allow5 = 0;
  int allow6 = 0;

  public static final int kDriver = 0;
  public static final int kOperator = 1;
  
  
  
  CANSparkMax climberArm = new CANSparkMax(0, MotorType.kBrushless);
  CANSparkMax shooter = new CANSparkMax(0, MotorType.kBrushless);
  CANSparkMax intake = new CANSparkMax(0, MotorType.kBrushless);
  CANSparkMax rotater = new CANSparkMax(0, MotorType.kBrushless);



  //reed switch thingymabob doing doing hoing soing boing
  //false = closed. Use in if statements. false by default
  DigitalInput climberSwitchClosed = new DigitalInput(0);
  DigitalInput rotaterSwitchClosed = new DigitalInput(0);

//replace with roborio DIO

//encoders or whatever -----------------------------------
  //RelativeEncoder climberArmLocation;
  

  // Gyro gyro = new ADXRS450_Gyro();
  //AHRS gyro = new AHRS(SPI.Port.kMXP);

  // double m_chassisAngularOffset = 0;
  // SwerveModuleState m_desiredState = new SwerveModuleState(0.0, new
  // Rotation2d());

  XboxController driverController = new XboxController(0); // USB port 0
  XboxController operatorController = new XboxController(1); // USB port 1

  // Use the following array so we can easily map the operator controls to
  // either controller. Doing this so it is easy to map both to one
  // controller in an emergency (like operator gets sick). Of course you
  // need to not use the same buttons, sliders/sticks on both pads.
  // The mapping to the array happens down in robotInit.
  public XboxController[] Controller = new XboxController[2];

  int last_press = 0; 
  int intake_counter = 0;

  @Override
  public void robotInit() {
    //gyro.reset();

    // We need to invert one side of the drivetrain so that positive voltages
    // result in both sides moving forward. Depending on how your robot's
    // gearbox is constructed, you might have to invert the left side instead.
    // m_rightMotor.setInverted(true);

    Controller[kDriver] = driverController;
    Controller[kOperator] = operatorController;

      int index;
    //SmartDashboard.putNumber("yaw", gyro.getAngle());
    // initialize arrays used to tune values used in auto
   

    index = kClimberSpd;
    taCurValue[index] = 1;
    taDelta[index] = .01;
    taLabel[index] = "Climber Speed";

    index = kRotateSpd;
    taCurValue[index] = .5;
    taDelta[index] = .01;
    taLabel[index] = "shooter rotate speed";

    index = kShooterSpd;
    taCurValue[index] = 1.0;
    taDelta[index] = 0.01;
    taLabel[index] = "shooter speed";

    index = kIntakeSpd;
    taCurValue[index] = 1.0;
    taDelta[index] = 0.01;
    taLabel[index] = "intake speed";

  }

    
    
  

  @Override
  public void robotPeriodic() {
    
  }
  @Override
  public void teleopPeriodic() {

    

    if (Math.abs(Controller[kDriver].getRawAxis(kleftStickYaxis))  > kDeadZone){
      shooter.set(Controller[kDriver].getRawAxis(kleftStickYaxis) * taCurValue[kShooterSpd]);
      intake.set(Controller[kDriver].getRawAxis(kleftStickYaxis) * taCurValue[kIntakeSpd]);
  }else{
      shooter.set(0);
  }
    if (Math.abs(Controller[kDriver].getRawAxis(kRightStickYaxis))  > kDeadZone)
      intake.set(-(Controller[kDriver].getRawAxis(kRightStickYaxis) * (taCurValue[kIntakeSpd]*2)));
    else
      intake.set(0);

      if (Controller[kDriver].getRawButton(kAButton)) 
       rotater.set(0);
      else
       rotater.set(0);

    if (Controller[kDriver].getRawButton(kBButton) && rotaterSwitchClosed.get()==false) 
       rotater.set(kRotateSpd);
      else
       rotater.set(0);

      if (Controller[kDriver].getRawButton(kXButton) && climberSwitchClosed.get() == false)
       climberArm.set(kClimberSpd);
      else
       climberArm.set(0);

       if (Controller[kDriver].getRawButton(kYButton) && climberSwitchClosed.get() == false)
       climberArm.set(-kClimberSpd);
      else
       climberArm.set(0);
  }
    
/* 
    if (Controller[kDriver].getRawButton(kAButton) && intake_counter <= 0){
      last_press ++;
      intake_counter = 40;
      if (last_press > 3)
        last_press = 0;
    }
    

    if (!Controller[kDriver].getRawButton(kAButton))
      intake_counter --;


    if (last_press == 0)
      intake.set(0);
    else if (last_press == 1)
      intake.set(taCurValue[kIntakeMotorSpd]);
    else if (last_press == 2)
      intake.set(0);
    else if (last_press == 3 )
      intake.set(-taCurValue[kIntakeMotorSpd]);

    SmartDashboard.putNumber("last_press",last_press);
    SmartDashboard.putNumber("intake_counter",intake_counter);    
  } 
*/
   /////////////////////////////////////////////////////////////////////////////
  // start of auto tuning routines
  /////////////////////////////////////////////////////////////////////////////
  // delta is either 1 or -1
  public void tweakTheIndex(int delta) {
    // calculate new index, look for wrap
    tCurIndex += delta;
    if (tCurIndex >= kItems)
      tCurIndex = 0;
    if (tCurIndex < 0)
      tCurIndex = kItems - 1;
    SmartDashboard.putString("changing", taLabel[tCurIndex]);
    SmartDashboard.putNumber("curValue", taCurValue[tCurIndex]);
  }

  // multiplier is either 1 or -1
  public void tweakValueAtIndex(double multiplier) {
    taCurValue[tCurIndex] += taDelta[tCurIndex] * multiplier;
    SmartDashboard.putNumber("curValue", taCurValue[tCurIndex]);
  }

  // Only tweak test values using A, B, X, Y buttons if we are disabled
  // Y increments value, A decrements value
  // X goes backwards through list of values, B goes forward
  /** This function is called periodically when disabled. */
  @Override
  public void disabledPeriodic() {

    // we want distinct press and release of the X-box buttons
    if (yEnabled && Controller[kDriver].getRawButton(kYButton)) {
      yEnabled = false;
      tweakValueAtIndex(1.0); // increase
    } else { // debounce the button
      if (!Controller[kDriver].getRawButton(kYButton)) {
        yEnabled = true;
      }
    }
    if (aEnabled && Controller[kDriver].getRawButton(kAButton)) {
      aEnabled = false;
      tweakValueAtIndex(-1.0); // decrease
    } else { // debounce the button
      if (!Controller[kDriver].getRawButton(kAButton)) {
        aEnabled = true;
      }
    }
    // playing with the index
    if (bEnabled && Controller[kDriver].getRawButton(kBButton)) {
      bEnabled = false;
      tweakTheIndex(1); // increase
    } else {
      if (!Controller[kDriver].getRawButton(kBButton)) {
        bEnabled = true;
      }
    }
    if (xEnabled && Controller[kDriver].getRawButton(kXButton)) {
      if (climberSwitchClosed.get() == false) {
        xEnabled = false;
      tweakTheIndex(-1); // decrease
      }
      
    } else {
      if (!Controller[kDriver].getRawButton(kXButton)) {
        xEnabled = true;
      }
    }
  }
  
}
