package team2202.robot.components.hoenheim;

import comms.XboxController;
import edu.wpi.first.wpilibj.DigitalInput;
import physicalOutput.SolenoidController;
import physicalOutput.motors.IMotor;
import robot.IControl;

public class Intake extends IControl {
	private IMotor motor;
	private int isOn;
	private boolean shouldBeOpen, shooting;
	private XboxController controller;
	private SolenoidController solenoidController;
	/**
	 * Get will return true when it is open
	 */
	private DigitalInput limitSwitch;
	
	public Intake(IMotor motor, DigitalInput limitSwitch) {
		this.motor = motor;
		controller=XboxController.getXboxController();
		solenoidController=SolenoidController.getInstance();
		this.limitSwitch=limitSwitch;
	}
	
	/**
	 * Call this and pass in true when you start shooting, and then when openingInProgress returns false, you can shoot.
	 * Make sure to call it and pass in false when done shooting.
	 * @param shooting Are we shooting?
	 * @return true if we are at the place we are told to be
	 */
	public boolean setShooting(boolean shooting) {
		this.shooting=shooting;
		shouldBeOpen=shooting;
		isOn=0;
		return limitSwitch.get()==shouldBeOpen;
	}

	public void teleopInit() {
		isOn = 0;
		shooting=false;
		shouldBeOpen=false;
	}

	public void teleopPeriodic() {
		motor.set(isOn);
		if (!shooting) {
			if (controller.getAHeld()) {
				if (shouldBeOpen) {
					isOn=1;
				}
				else {
					isOn=-1;
				}
			}
			else {
				isOn=0;
			}
		}
		else {
			isOn=0;
		}
		try {
			solenoidController.getSolenoid("intakeSolenoid").set(shouldBeOpen);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
}
