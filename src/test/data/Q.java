public class Q {

	public static int recurse(int level) {
		if(level == 2) {
			return 100;
		}

		int a = 0, b = 0, c = 0, d = 0, e = 0, f = 0, g = 0, h = 0, i = 0, j = 0, k = 0, l = 0, m = level;

		int z = recurse(level+1);

		return a+b+c+d+e+f+g+h+i+j+k+l+m+z;
	}


	public static void main(String[] args) {
		return recurse(0);
	}
}