class I {
	public static void main(String[] args) {
//		int x = 0;
//		int y = 0;
//		if(0 == 0) {
//			x = 1;
//		} else if(1 == 1) {
//			x = 2;
//			if(2 == 2) {
//				y = 3+x;
//				y = 4+x;
//			} else {
//				y = 2;
//			}
//		} else {
//			x = 3;
//		}
//		x = x+2;
//		y = y+1;

		int x = 0;
		int y = 1;

		if(x < y) {
			if(2 == 2) {
				x = 3;
			}
		}

		x = x+2;

		return x; // x == 5
	}
}
