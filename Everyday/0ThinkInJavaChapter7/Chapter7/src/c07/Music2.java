package c07;

class Note2{
	private int value;
	private Note2(int val) {value = val;}
	public static final Note2
		middleC = new Note2(0),
		cSharp = new Note2(1),
		cFlat = new Note2(2);
}

class Instrument2{
	public void play(Note2 n) {
		System.out.println("Instrument2.play()");
	}
}

class Wind2 extends Instrument2{
	public void play(Note2 n) {
		System.out.println("Wind2.play()");
	}
}

class Stringed2 extends Instrument2{
	public void play(Note2 n) {
		System.out.println("Stringed2.play()");
	}
}

class Brass2 extends Instrument2{
	public void play(Note2 n) {
		System.out.println("Brass2.play()");
	}
}


public class Music2 {

	public static void tune(Wind2 i) {
		i.play(Note2.middleC);
	}
	
	public static void tune(Stringed2 i) {
		i.play(Note2.middleC);
	}
	
	public static void tune(Brass2 i) {
		i.play(Note2.middleC);
	}
	
	public static void tuneDiff(Instrument2 i) {
		i.play(Note2.middleC);
	}
	
	public static void main(String[] args) {
		Wind2 flute  = new Wind2();
		Stringed2 violin = new Stringed2();
		Brass2 frenchHorn = new Brass2();
		//���ٴ�������
		Instrument2 aaaaa = new Wind2();
		Instrument2 bbbbb = new Stringed2();
		Instrument2 ccccc = new Brass2();
	
		tuneDiff(aaaaa);
		tuneDiff(bbbbb);
		tuneDiff(ccccc);
		System.out.println("***************");
		tune(flute);
		tune(violin);
		tune(frenchHorn);
	}
	
}
