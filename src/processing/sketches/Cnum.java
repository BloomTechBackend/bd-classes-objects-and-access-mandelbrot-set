package processing.sketches;

// a + bi
public class Cnum {
    private double a;
    private double b;

    public Cnum(double a, double b) {
        this.a = a;
        this.b = b;
    }

    public Cnum() {
        this.a = 0.0;
        this.b = 0.0;
    }

    public double getA() {
        return a;
    }

    public void setA(double a) {
        this.a = a;
    }

    public double getB() {
        return b;
    }

    public void setB(double b) {
        this.b = b;
    }
}
