class Data {
	int y;
}

class L {
	public static void main(String[] args) {
		Data x = new Data();
		x.y = 1000;
		Data y = x;
		y.y = 2;
		x = new Data();
		x.y = 3;

		(new Data()).y = 0;

		return y.y + x.y;
	}
}