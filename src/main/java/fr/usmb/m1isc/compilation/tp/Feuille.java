package fr.usmb.m1isc.compilation.tp;

public class Feuille extends Arbre {
    private String val;

    public Feuille(String val) {
        super();
        this.val = val;
    }

    public String toString() {
        return val;
    }
}