class P {

	static float calculate(int x, float a, float b, float c, float d) {
		return ((a + b) * c) / d;
	}

	public static void main(String[] args) {
		float a = .015f;
		float b = .035f;
		float c = 2.5f;
		float d = 1.25f;

		float result = calculate(1, a, b, c, d);
		result += calculate(2, a, b, c, d);
		print_float(result);
	}
}