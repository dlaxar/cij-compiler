class M0 {
	static int THOUSAND = 1000;
	int x = 500000;

	void foo() {
		x += stat();
		bar();
	}

	void foo(int i) {
		x = i;
	}

	void bar() {
		THOUSAND += 20;
	}

	public static int stat() {
		return 10;
	}

	public static M0 instance() {
		THOUSAND += 100;
		return new M0();
	}
}

class M01 extends M0 {
	static final int THOUSAND = 2000;
	int y;

	public M01(int y) {
		this.y = y;
	}

	void foo() {
		x += 11;
	}

	public static int stat() {
		THOUSAND += 30;
		return 400;
	}

	int noOverride() {
		return x + 50;
	}
}

class M02 extends M0 {
	int y;
	M0 parent;
}

class M {

	public static int test() {
		return 1;
	}

	public static void main(String[] args) {
		test();
		M0 m0 = new M0();
		m0.foo(0); // m0.x = 0
		m0.foo(); // m0.x = 10; M0.THOUSAND = 1020;


		M0 m1 = new M01(12);
		m1.foo(200); // m1.x = 200
		m1.foo(); // m1.x = 211;
		m1.stat(); // M0.THOUSAND = 1020; M01.THOUSAND = 2000; // this is not virtual so we use the stat of the declared type

		int x = M0.instance().instance().stat(); // x = 10; M0.THOUSAND = 1220

		int y = (m1.x + M0.stat() + M0.THOUSAND); // y = (211 + 10 + 1220) = 1441

		M01 M0 = new M01(13);
		int z = M0.stat(); // z = 400; M01.THOUSAND = 2030)

		//         M0 referrs to the object of type M01
		//         |
		int result = y + M0.THOUSAND + M0.noOverride() + z; // 1441 + 2030 + 500050 + 400 = 503921;
		VirtualMachine.exit(result);
	}
}
