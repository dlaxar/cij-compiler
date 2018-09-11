import Benchmark;

class F extends Benchmark {
	public static void main(String[] args) {
		begin();
		int[] x = {12, 15, 0};

		println(x.length);

		expectEqual(x.length, 3);

		println(random(x[0]));

		end();
	}

	public static int random(int arg) {
		return 4;
	}
}