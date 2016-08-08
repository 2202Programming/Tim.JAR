package robot;

import java.util.Map;
import java.util.Map.Entry;

import comms.DebugMode;
import comms.SmartWriter;
import edu.wpi.first.wpilibj.IterativeRobot;
import robotDefinitions.IDefinition;
import robotDefinitions.RobotName;
import robotDefinitions.Tim;

/**
 * The main robot class that calls the IControl methods of each IControl object
 * associated with the robot
 */
public class Robot extends IterativeRobot {

	private Map<String, IControl> controlObjects;
	IDefinition robotDefinition;

	/**
	 * We usually only would want to print the first error that occurred, as
	 * that tends to cause other ones. Once an error is printed, the rest won't
	 * be so that the first one can be fixed.
	 */
	private boolean stopPrintingErrors=false;

	public void robotInit() {

		// String to say which robot we are using could later be made into a XML
		// property getter
		RobotName name=RobotName.TIM; // TODO Can we get this from the robot so
										// it automatically knows what robot it
										// is?

		// Switch to decide which robot definition to use
		switch (name) {
		case TIM:
			robotDefinition=new Tim();
			break;
		case PIPER:
			break;
		default:
			break;
		}

		// Load all the properties in the currently selected definition
		controlObjects=robotDefinition.loadControlObjects();
	}

	public void autonomousInit() {
		SmartWriter.putS("Robot State", "Autonomous Init", DebugMode.COMPETITION);

		for (Entry<String, IControl> entry : controlObjects.entrySet()) {
			try {
				entry.getValue().autonomousInit();
			} catch (Exception e) {
				outputError(e, "Auto Init");
			}
		}
	}

	public void autonomousPeriodic() {
		SmartWriter.putS("Robot State", "Autonomous Periodic", DebugMode.COMPETITION);

		for (Entry<String, IControl> entry : controlObjects.entrySet()) {
			try {
				entry.getValue().autonomousPeriodic();
			} catch (Exception e) {
				outputError(e, "Auto Periodic");
			}
		}
	}

	public void teleopInit() {
		SmartWriter.putS("Robot State", "Teleop Init", DebugMode.COMPETITION);

		for (Entry<String, IControl> entry : controlObjects.entrySet()) {
			try {
				entry.getValue().teleopInit();
			} catch (Exception e) {
				outputError(e, "Teleop Init");
			}
		}
	}

	public void teleopPeriodic() {
		SmartWriter.putS("Robot State", "Teleop Periodic", DebugMode.COMPETITION);

		for (Entry<String, IControl> entry : controlObjects.entrySet()) {
			try {
				entry.getValue().teleopPeriodic();
			} catch (Exception e) {
				outputError(e, "Teleop Periodic");
			}
		}
	}

	public void disabledInit() {
		SmartWriter.putS("Robot State", "Disabled Init", DebugMode.COMPETITION);

		for (Entry<String, IControl> entry : controlObjects.entrySet()) {
			try {
				entry.getValue().disabledInit();
			} catch (Exception e) {
				outputError(e, "Disabled Init");
			}
		}
	}

	public void disabledPeriodic() {
		SmartWriter.putS("Robot State", "Disabled Periodic", DebugMode.COMPETITION);

		for (Entry<String, IControl> entry : controlObjects.entrySet()) {
			try {
				entry.getValue().disabledPeriodic();
			} catch (Exception e) {
				outputError(e, "Disabled Periodic");
			}
		}
	}

	/**
	 * Prints the error to standard output so it can be identified and debugged
	 * 
	 * @param The
	 *            exception that occurred
	 * @param The
	 *            name of the time period that the exception occurred (i. e.
	 *            Auto init) as a string to be printed
	 */
	private void outputError(Exception e, String timeOccured) {
		// We are going to try to print to Stdout, but I think this isn't going
		// to work.
		// If this doesn't work, then we can try:
		// -printing to System.out instead of System.err
		// -printing to using SmartWriter (This would be more difficult because
		// Excetion.StackTrace would have to be converted to Strings)
		if (!stopPrintingErrors) {
			System.err.println("Exception occured in: "+timeOccured+".");
			e.printStackTrace(System.err);
			stopPrintingErrors=true;
		}
	}
}
