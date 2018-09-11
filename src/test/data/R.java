class R {
	public static void bubbleSort(int[] array)
	{
		for(int j = array.length - 1; j > 0; --j)
		{
			for(int i = 0; i < j; ++i)
			{
				if(array[i] > array[i+1]) {
					int tmp = array[i + 1];
					array[i + 1] = array[i];
					array[i] = tmp;
				}
			}
		}
	}

	public static int[] random(int seed, int max, int count)
	{
		int[] result = new int[count];
		for (int i = 0; i < count; ++i) {
			result[i] = randomImpl(seed, max);
			seed = result[i];
		}
		return result;
	}

	private static int randomImpl(int seed, int max)
	{
		int x = 1103515245 * seed;
		if (x < 0)
			x *= -1;
		return (((x + 12345) % 2147483647)) % (max+1);
	}

	public static void main(String[] args)
	{
		int[] b = random(42, 10000, 10);

		print_array_int(b);

		benchmark_start();
		bubbleSort(b);
		benchmark_end();
		print_array_int(b);
	}
}