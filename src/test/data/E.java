class E {
	public static void main(String[] args) {
		int x = 0;

		if(x > 0 && x > 2) {
			x = 2;
		} else {
			x = 1;
		}

		if(x < 0 || x > 2) {
			x = 1;
		}
	}
}