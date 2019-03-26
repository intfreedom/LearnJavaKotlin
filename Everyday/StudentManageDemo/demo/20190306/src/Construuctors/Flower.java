//:Flower.java
//Calling constructors with "this"
package Construuctors;

public class Flower {
	private int petalCount = 0;
	private String s = new String("null");
	Flower(int petals){
		petalCount = petals;
		System.out.println(
				"Coustructor w/ int arg only, petalCount = "
				+ petalCount);
	}
	Flower(String ss){
		System.out.println(
				"Constructor w/ String arg only, s="+ss);
		s = ss;
	}
	Flower(String s, int petals){
		this(petals);
		this.s;
		System.out.println("String & int args");
	}
	Flower(){
		this("hi", 47);
		System.out.println(
				"default constructor (no args)");
	}
	void print() {
		System.out.println(
				System.out.println(
						"petalCont = " + petalCount + " s = " + s));
		
	}
	
	public static void main(String[] args) {
		Flower x = new Flower();
		x.point();
	}

}