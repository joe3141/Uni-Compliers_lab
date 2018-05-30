
class tst{
	private int j;
}

public class Test {
	private int x;
	private Test v;
	public tst q;
	
	public Test(int x) {
		super();
		this.x = x;
		q = new tst();
	}

	public void vl() {
		System.out.println(v.x);
	}

	public static void main(String[] args){
		Test a = new Test(1), b = new Test(0);
		a.v = b;
		a.vl();
	}
}
