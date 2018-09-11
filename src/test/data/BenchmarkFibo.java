public class BenchmarkFibo {

	public static int fib(int n) {
		if(n <= 2) {
			return 1;
		}

		return fib(n - 1) + fib(n - 2);
	}

	public static void main(String[] args) {
		benchmark_start();
		int f = fib(50);
		benchmark_end();
		return f;
	}
}