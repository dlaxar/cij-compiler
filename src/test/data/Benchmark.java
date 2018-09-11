public class Benchmark
{
	private static long before;

	public static void begin()
	{
		System.out.println("running benchmark ...");
		before = System.currentTimeMillis();
	}

	public static void end()
	{
		long after = System.currentTimeMillis();
		long diff = after - before;
		System.out.printf("done! benchmark took %s ms\n", diff);
	}

	public static void println(long l)
	{
		System.out.println(l);
	}

	public static void expectEqual(long a, long b)
	{
		if(a != b)
		{
			System.err.printf("benchmark failed: expected equal values, got %s and %s", a, b);
			System.exit(1);
		}
	}
}