package PID;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Random;

import comms.DebugMode;
import comms.FileLoader;
import comms.SmartWriter;
import robot.IControl;

/**
 * A class used for automatically tuning PID loops using an evolutionary
 * strategy with semi-intelligent improvement, once some improvement has been
 * made.
 * 
 * @author SecondThread
 */
public class AutoPIDTuner extends IControl {

	/**
	 * The tunable thing that is suppose to be tuned
	 */
	private AutoPIDTunable toTune;

	/**
	 * The previous best PID values and the PID values that will be tested
	 */
	private PIDValues bestPIDValues, testingPIDValues, smartDashboardPIDValues=null;

	/**
	 * The PIDController that is used by the Tunable object. This is needed so
	 * that new PID values can be set when they are tested, and so that error
	 * can be reset after tests.
	 */
	private PIDController pidController;

	/**
	 * The error tolerance. The loop will count as being complete when the error
	 * is less than this for maxErrorSafeCounter frames
	 */
	private final double minError=3;

	/**
	 * The counter and required frames counting how long the tunable has been
	 * less than <i>minError</i> from its target value.
	 */
	private int errorSafeCounter, maxErrorSafeCounter=40;

	/**
	 * The number of frames that this loop has used, and the number of frames
	 * that can be used before these PID values are marked unsuccessful.
	 */
	private int currentTuneCounter, maxTuneCounter=300;

	/**
	 * The number of frames the best PID values took to run
	 */
	private int bestTuneTime;

	/**
	 * The number the last run (not necessarily the best) took to run, which is
	 * displayed in the graph, if it is being used.
	 */
	private int lastTuneCounter;

	/**
	 * The change in kp, ki, and kd to make the last random PID values, which
	 * will continue to be used if they make things better.
	 */
	private double dp, di, dd;

	/**
	 * The number of PID values tried, and the max number to be tried,
	 * respectively.
	 */
	private int timesTried, maxTries=100;

	/**
	 * A counter that is used to see what stat is changed
	 */
	private int ceterusPluribusCounter=-1;

	/**
	 * An array list of strings that will be logged after <i>maxTries</i>
	 * trials.
	 */
	private ArrayList<String> toWrite;

	private Date startTime;

	private int firstTry=-1, secondTry=-1;
	private int testOf3=0;

	/**
	 * The constructor for AutoPIDTuner, which passes in what needs to be tuned.
	 * 
	 * @param toTune
	 *            The tunable to tune
	 */
	public AutoPIDTuner(AutoPIDTunable toTune) {
		this.toTune=toTune;
		SmartWriter.putB("AutoPIDRandomTest", false, DebugMode.DEBUG);
	}

	/**
	 * The constructor for AutoPIDTuner, which passes in what needs to be tuned.
	 * <br>
	 * Also passes in the startingPIDValues
	 * 
	 * @param toTune
	 *            The tunable to tune
	 * @param kpStarting
	 * @param kiStarting
	 * @param kdStarting
	 */
	public AutoPIDTuner(AutoPIDTunable toTune, double kpStarting, double kiStarting, double kdStarting) {
		this(toTune);
		testingPIDValues=bestPIDValues=new PIDValues(kpStarting, kiStarting, kdStarting);
	}

	/**
	 * The constructor for AutoPIDTuner, which passes in what needs to be tuned.
	 * <br>
	 * Also passes in the startingPIDValues
	 * 
	 * @param toTune
	 * @param startingPIDValues
	 */
	public AutoPIDTuner(AutoPIDTunable toTune, PIDValues startingPIDValues) {
		this(toTune, startingPIDValues.kp, startingPIDValues.ki, startingPIDValues.kd);
	}

	/**
	 * This needs to be called once a frame or update cycle or everything will
	 * break.
	 */
	public void update() {
		tunePID();
		SmartWriter.putD("AutoPID Test Number", timesTried, DebugMode.DEBUG);
	}

	/**
	 * <h1>Evolutionary PID Tuning Pro Strat:</h1> <br>
	 * Start with <.1, 0, 0>, or whatever it says at the top<br>
	 * Test and record time <br>
	 * <br>
	 * Change one number a little bit.<br>
	 * Test and record the new time <br>
	 * <br>
	 * If the new one is better, start from there and keep doing the same thing,
	 * <br>
	 * otherwise, try another random mutation <br>
	 */
	private void tunePID() {
		if (doneTesting()) return;
		if (!toTune.getResetFinished()) return;

		currentTuneCounter++;
		checkError();
		if (errorSafeCounter>=maxErrorSafeCounter||currentTuneCounter>=maxTuneCounter) {
			if (shouldStartRandomTest()) {
				startRandomTest();
			}
			else {
				accountForLastTest();
				startNewTest();
			}
			return;
		}

		sendOutputToTunable();
	}

	/**
	 * Records the old PID values with their results both in the <i>toWrite</i>
	 * and sends them to the AutoPIDTesterWindow, if its main is currently
	 * running.
	 */
	public void disabledInit() {
		String total="";
		total+="Best PID Values: "+bestPIDValues.toString()+"\n";
		total+="Best Tune Time: "+bestTuneTime+"\n";
		FileLoader.writeToFile("/home/lvuser/AutoPIDHistory"+startTime.toString()+".txt", total);
		SmartWriter.putS("Sent file", "fdjkdf", DebugMode.DEBUG);
		/*
		 * if (AutoPIDTesterWindow.shouldSetValues&&AutoPIDTesterWindow.window!=
		 * null) { AutoPIDTesterWindow.window.setInfo(testingPIDValues+"",
		 * bestPIDValues+"", timesTried, bestTuneTime+"", lastTuneCounter); }
		 */// comment this in for demos
	}

	/**
	 * Adjusts errorSafeCounter by incrementing it if the tunable's error is
	 * within the min error range, or setting it to zero if it isn't
	 */
	private void checkError() {
		if (Math.abs(toTune.getError())<minError) {
			errorSafeCounter++;
		}
		else {
			errorSafeCounter=0;
		}
	}

	/**
	 * Makes a slight variant of the current PID values which will be tried next
	 * in evolutionary tuning.<br>
	 * It also stores the change made to kp, ki, and kd in dp, di, and dd
	 * 
	 * @param lastValues
	 *            The PIDValues that the variant will be based on.
	 * @return A variant of <i>lastValues</i> with a slight random change to
	 *         each constant
	 */
	private PIDValues getVariant(PIDValues lastValues) {
		Random r=new Random();

		if (timesTried<20) {// r.nextInt(2)==0) {
			return getCeterusParibusVarient(lastValues);
		}

		double divider=bestTuneTime/300.0*.05;// 3;// Math.log(timesTried+2);
		dp=Math.pow(r.nextDouble()-0.5, 3)*4*divider;
		di=Math.pow(r.nextDouble()-0.5, 3)/4*divider;
		dd=Math.pow(r.nextDouble()-0.5, 3)*40*divider;

		return new PIDValues(Math.max(lastValues.kp+dp, 0), Math.max(lastValues.ki+di, 0),
				Math.max(Math.min(lastValues.kd+dd, Math.max(lastValues.kp+dp, 0)), 0));
	}

	/**
	 * Gets a variant of the last PID values which IS NOT randomized. It changes
	 * only one variable, depending on how many times this method has been
	 * called in the past
	 * 
	 * @param lastValues
	 *            The PID values that a small adjustment should be made to
	 * @return The adjusted PID values
	 */
	private PIDValues getCeterusParibusVarient(PIDValues lastValues) {
		ceterusPluribusCounter++;
		dp=di=dd=0;
		switch (ceterusPluribusCounter) {
		case 0:
			dp=0.005;
			break;
		case 1:
			di=0.0005;
			break;
		case 2:
			dd=0.1;
			break;
		case 3:
			dp=-0.005;
			break;
		case 4:
			di=-0.0005;
			break;
		case 5:
			dd=-0.1;
			break;
		default:
			ceterusPluribusCounter=-1;
			return getCeterusParibusVarient(lastValues);// YAY FOR RECURSION!
		}

		return new PIDValues(Math.max(lastValues.kp+dp, 0), Math.max(lastValues.ki+di, 0),
				Math.max(lastValues.kd+dd, 0));
	}

	/**
	 * If the last PID values were super great, then this can be called to
	 * continue the pattern they started. This is useful because it drastically
	 * speeds up evolution once randomness is onto something.
	 * 
	 * @param lastValues
	 *            The previously used values that were good, which will be
	 *            extrapolated from
	 * @return The new PID values which are extrapolated from <i>lastValues</i>
	 */
	private PIDValues extrapolate(PIDValues lastValues) {
		return new PIDValues(Math.max(lastValues.kp+dp, 0), Math.max(lastValues.ki+di, 0),
				Math.max(lastValues.kd+dd, 0));
	}

	/**
	 * Checks to see if the maximum number of tests have been performed. <br>
	 * If they have, the data in <i>toWrite</i> is writen in the .csv file.
	 * 
	 * @return true if the maximum number of tests have been reached, false
	 *         otherwise.
	 */
	private boolean doneTesting() {
		if (timesTried>maxTries) {
			System.out.println("done.");
			String total="";
			for (String s : toWrite) {
				total+=s+"\n";
			}
			FileLoader.writeToFile("/home/lvuser/AutoPIDHistory"+startTime.toString()+".txt", total);
			return true;
		}
		return false;
	}

	/**
	 * Starts a new test, either extrapolating from the past successful
	 * PIDValues, or starting with new ones from the previous best PID values.
	 */
	private void startNewTest() {
		toTune.startReset(testOf3);
		timesTried++;
	}

	private void startRandomTest() {
		pidController.resetError();
		currentTuneCounter=0;
		errorSafeCounter=0;
		pidController.setValues(bestPIDValues);
		toTune.setToRandomState();
	}

	/**
	 * Keeps track of the last test's information and resets values. It does not
	 * start a new test yet though.
	 */
	public void accountForLastTest() {
		if (smartDashboardPIDValues!=null) {
			pidController.setValues(smartDashboardPIDValues);
			smartDashboardPIDValues=null;
			pidController.resetError();
			lastTuneCounter=currentTuneCounter;
			currentTuneCounter=0;
			errorSafeCounter=0;
			testOf3=0;
			return;
		}
		
		if (testOf3==0) {
			testOf3=1;
			if (currentTuneCounter<bestTuneTime) {
				firstTry=currentTuneCounter;
				return;
			}
			else {
				//let control flow through, we failed
			}
		}
		else if (testOf3==1) {
			testOf3=2;
			toTune.giveInfo(bestPIDValues, bestTuneTime, testingPIDValues, currentTuneCounter);			
			
			secondTry=currentTuneCounter;
			pidController.resetError();
			lastTuneCounter=currentTuneCounter;
			currentTuneCounter=0;
			errorSafeCounter=0;
			return;
		}
		else {
			testOf3=0;
			int average=(firstTry+secondTry+currentTuneCounter)/3;
			currentTuneCounter=average;// let control flow through
		}
		
		
		
		
		testOf3=0;

		if (currentTuneCounter<bestTuneTime) {// NEW BEST!!!
			bestTuneTime=currentTuneCounter;
			bestPIDValues=testingPIDValues;
			testingPIDValues=extrapolate(bestPIDValues);
		}
		else {
			testingPIDValues=getVariant(bestPIDValues);
		}

		toTune.giveInfo(bestPIDValues, bestTuneTime, testingPIDValues, currentTuneCounter);

		pidController.setValues(testingPIDValues);
		pidController.resetError();
		lastTuneCounter=currentTuneCounter;
		currentTuneCounter=0;
		errorSafeCounter=0;
	}

	/**
	 * Sends the output from <i>pidController</i> to <i>toTune</i>
	 */
	private void sendOutputToTunable() {
		// Always want 0 error
		double output=pidController.calculate(0, toTune.getError());
		SmartWriter.putD("TuningPIDError", toTune.getError(), DebugMode.DEBUG);
		toTune.setValue(output);
	}

	private boolean shouldStartRandomTest() {
		return SmartWriter.getB("AutoPIDRandomTest");// AutoPIDTesterWindow.shouldSetValues&&AutoPIDTesterWindow.window.setToRandomState();
	}

	public void autonomousInit() {
		toTune.startReset(testOf3);
		startTime=new Date(System.currentTimeMillis());
		bestPIDValues=testingPIDValues=new PIDValues(.01, .0005, 0);
		pidController=new PIDController(testingPIDValues);
		errorSafeCounter=0;
		currentTuneCounter=0;
		bestTuneTime=Integer.MAX_VALUE;
		lastTuneCounter=0;
		dp=0;
		di=0;
		dd=0;
		timesTried=0;
		ceterusPluribusCounter=-1;
		toWrite=new ArrayList<String>();
		SmartWriter.putB("AutoTuning", true, DebugMode.DEBUG);
		SmartWriter.putS("SuggestedPIDValues", "", DebugMode.DEBUG);
	}

	public void autonomousPeriodic() {
		if (SmartWriter.getB("AutoTuning")) {
			update();
			
			
			String input=SmartWriter.getS("SuggestedPIDValues").replaceAll(" ", "");
			String[] sections=input.split(",");
			if (sections.length==3&&!sections[0].isEmpty()&&!sections[1].isEmpty()&&!sections[2].isEmpty()) {
				try {
					double p=Double.parseDouble(sections[0]);
					double i=Double.parseDouble(sections[1]);
					double d=Double.parseDouble(sections[2]);
					smartDashboardPIDValues=new PIDValues(p, i, d);
					SmartWriter.putS("SuggestedPIDValues", "");
				} catch(Exception e) {
					SmartWriter.putS("SuggestedPIDValues", "Invalid Values");
				}
			}
		}
	}

}
