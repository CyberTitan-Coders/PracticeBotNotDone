// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.XboxController;
import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkLowLevel.MotorType;
import com.revrobotics.SparkAbsoluteEncoder.Type;
import com.revrobotics.AbsoluteEncoder;
import com.revrobotics.CANSparkBase;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.SparkAbsoluteEncoder;
import com.revrobotics.SparkRelativeEncoder;
import com.ctre.phoenix6.controls.DutyCycleOut;
import com.ctre.phoenix6.controls.compound.Diff_DutyCycleOut_Position;
//import edu.wpi.first.wpilibj.ADXRS450_Gyro; // small FRC gyro in SPI slot
// https://dev.studica.com/releases/2024/NavX.json
import com.kauailabs.navx.frc.AHRS;
import edu.wpi.first.wpilibj.SPI;

//dunno
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.DutyCycle;
import edu.wpi.first.wpilibj.DutyCycleEncoder;
import edu.wpi.first.wpilibj2.command.SubsystemBase;


/**
 * The VM is configured to automatically run this class, and to call the functions corresponding to
 * each mode, as described in the TimedRobot documentation. If you change the name of this class or
 * the package after creating this project, you must also update the build.gradle file in the
 * project.
 */
public class Robot extends TimedRobot {

  //motor speeds
  public static final int kClimberMotorSpd = 0;
  public static final int kItems = 1;
  
  //controllers
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
  
  public static final int kDriver = 0;
  public static final int kOperator = 1;
  

  CANSparkMax climberA = new CANSparkMax(34, MotorType.kBrushless);
//CANSparkMax climberB = new CANSparkMax(0, MotorType.kBrushless);

  
  
  
  

   //false = closed. Use in if statements. false by default
   
   //DigitalInput climberSwitchMin = new DigitalInput(0);

   //replace with roborio DIO

   XboxController driverController = new XboxController(0); // USB port 0
   XboxController operatorController = new XboxController(1); // USB port 1

  /**
   * This function is run when the robot is first started up and should be used for any
   * initialization code.
   */

   //back up and easy mapping or smth
   public XboxController[] Controller = new XboxController[2];

   RelativeEncoder climberEncoderA=climberA.getEncoder(SparkRelativeEncoder.Type.kHallSensor,42);
// RelativeEncoder climberEncoderB=climberB.getEncoder(SparkRelativeEncoder.Type.kHallSensor,42);


  @Override
  public void robotInit() {
    Controller[kDriver] = driverController;
    Controller[kOperator] = operatorController;

    
     
    

    
    int index;
  //SmartDashboard.putNumber("yaw", gyro.getAngle());
  // initialize arrays used to tune values used in auto
  index = kClimberMotorSpd;
  taCurValue[index] = .20;
  taDelta[index] = 0.01;
  taLabel[index] = "arm motor speed";
  }

  /**
   * This function is called every 20 ms, no matter the mode. Use this for items like diagnostics
   * that you want ran during disabled, autonomous, teleoperated and test.
   *
   * <p>This runs after the mode specific periodic functions, but before LiveWindow and
   * SmartDashboard integrated updating.
   */

   
  @Override
  public void robotPeriodic() {
    SmartDashboard.putNumber("Climber Encoder A", climberEncoderA.getPosition());
    //SmartDashboard.putNumber("Climber Encoder B", climberEncoderB.getPosition());

  }

  

  /** This function is called periodically during operator control. */
  @Override
  public void teleopPeriodic() {

    if (Controller[kDriver].getRawButton(kXButton))
    climberA.set(kClimberMotorSpd);
   else
    climberA.set(0);

    if (Controller[kDriver].getRawButton(kYButton) && climberEncoderA.getPosition()<=0)
    climberA.set(-kClimberMotorSpd);
   else
    climberA.set(0);

  }

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
        xEnabled = false;
      tweakTheIndex(-1); // decrease by 1
      
    } else {
      if (!Controller[kDriver].getRawButton(kXButton)) {
        xEnabled = true;
      }
    }
  }

  /** This function is called periodically when disabled. */

  
}
