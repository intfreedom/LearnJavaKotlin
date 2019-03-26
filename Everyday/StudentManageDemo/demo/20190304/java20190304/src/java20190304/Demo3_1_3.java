package java20190304;

class A
{
	static int x = 2;//静态成员不被继承，可以被所有子类访问
	//不会有多个静态副本
	public void setx(int i)
	{
		x = i;	
	}
	void printa()
	{
		System.out.println(x);
	}
}
class B extends A
{
	int x = 100;
	void printb()
	{
		super.x = super.x + 10;
		System.out.println("super.x="+super.x+" x="+x);
	}
}
public class Demo3_1_3 {
	public static void main(String[] args) {
		A a1 = new A();
		a1.setx(4);
		a1.printa();// 4
		
		B b1 = new B();
		b1.printb();// 14 100
		b1.printa();// 14
		
		b1.setx(6);
		b1.printb();// 16 100
		b1.printa();// 16
		a1.printa();// 16
	
	}
}
