class Calculation {
	Real l;
	Real r;

	final static int ADD = 0;

	int operator;

	public Real calculate() {
		if(operator == ADD) {
			return l.add(r);
		}
		return null;
	}
}

class Real {

	private final double value;

	Real(double value) {
		this.value = value;
	}

	public Real add(Real other) {
		return new Real(getValue() + other.getValue());
	}

	public double getValue() {
		return value;
	}
}

class Fraction extends Real {
	double nominator;
	double denominator;

	public Fraction(double nominator) {
		super(0.0);
		this.nominator = nominator;
		this.denominator = 1.0;
	}

	public Fraction(double nominator, double denominator) {
		this(nominator);
		this.denominator = denominator;
	}

	@Override
	public double getValue() {
		return 2.5;
	}
}

public class Demo {
	static int total;

	public static void main(String[] args) {
		Real a = new Real(1.5);
		print_double(a.getValue());

		Real f = new Fraction(3.0, 2.0);
		print_double(f.getValue());

		Real[] reals = new Real[48];

		// every two iterations add up to 4
		for(int i = 0; i < reals.length; i++) {
			if(i % 2 == 0) {
				reals[i] = new Real(1.5);
			} else {
				reals[i] = new Fraction(5.0, 2.0);
			}
		}

		Real sum = new Real(0.0);

		// reals.length/2 * 4
		for(int i = 0; i < reals.length; i++) {
			sum = sum.add(reals[i]);
		}

		print_double(sum.getValue());

		Calculation c = new Calculation();
		c.l = new Real(12.5);
		c.r = c.l;

		print_double(c.calculate().getValue());
	}
}
