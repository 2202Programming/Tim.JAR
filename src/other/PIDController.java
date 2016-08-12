package other;

public class PIDController {
	private double kp;
	private double ki;
	private double kd;
	private double totalError;
	private double lastError;
	private boolean advanced;

	/**
	 * A default constructor for a PIDController<br>
	 * Must set pid values before using calculate
	 */
	public PIDController(){
		kp = 0;
		ki = 0;
		kd = 0;
		totalError = 0;
		lastError = 0;
		advanced = false;
	}
	/**
	 * A basic PIDController for random stuff(Can be improved greatly)
	 * 
	 * @param kp
	 *            Proportional Constant
	 * @param ki
	 *            Integral Constant
	 * @param kd
	 *            Derivative Constant
	 * @param advanced
	 *            If the controller should use the extra features(If you don't
	 *            know, don't use it)
	 */
	public PIDController(double kp, double ki, double kd, boolean advanced) {
		this.kp = kp;
		this.ki = ki;
		this.kd = kd;
		totalError = 0;
		lastError = 0;
		this.advanced = advanced;
	}
	
	/**
	 * Sets new values for the pid variables
	 * @param kpIn
	 * @param kiIn
	 * @param kdIn
	 */
	public void setPID(double kpIn, double kiIn, double kdIn){
		kp = kpIn;
		ki = kiIn;
		kd = kdIn;
	}

	/**
	 * calculates the output using the PIDController
	 * 
	 * @param setValue
	 *            The desired number
	 * @param input
	 *            the current value of whatever you're using (Gyro, Encoder,
	 *            etc.)
	 * @return The output using the p, i, and d values set in the constructor
	 */
	public double calculate(double setValue, double input) {

		double error = input - setValue;

		if (advanced) if ((error > 0 && lastError < 0) || (error < 0 && lastError > 0)) totalError = 0;
		totalError += error;

		double pChange = kp * error;
		double iChange = ki * totalError;
		double dChange = kd * (error - lastError);

		lastError = error;

		return pChange + iChange + dChange;
	}
}
