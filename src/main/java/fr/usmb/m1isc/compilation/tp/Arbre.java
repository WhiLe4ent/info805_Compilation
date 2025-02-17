package fr.usmb.m1isc.compilation.tp;

public class Arbre {
    private SymbolType racine;
    private Arbre filsGauche;
    private Arbre filsDroit;

    public Arbre(SymbolType racine) {
        this.racine = racine;
        filsGauche = null;
        filsDroit = null;
    }

    public Arbre(SymbolType racine, Arbre filsGauche) {
        this.racine = racine;
        this.filsGauche = filsGauche;
    }

    public Arbre(SymbolType racine, Arbre filsGauche, Arbre filsDroit) {
        this.racine = racine;
        this.filsDroit = filsDroit;
        this.filsGauche = filsGauche;
    }

    public Arbre() {
    }

    public SymbolType getRacine() {
        return racine;
    }

    public void setRacine(SymbolType racine) {
        this.racine = racine;
    }

    public Arbre getFilsGauche() {
        return filsGauche;
    }

    public void setFilsGauche(Arbre filsGauche) {
        this.filsGauche = filsGauche;
    }

    public Arbre getFilsDroit() {
        return filsDroit;
    }

    public void setFilsDroit(Arbre filsDroit) {
        this.filsDroit = filsDroit;
    }

    public String toString() {
        if (racine != null) {
            if (filsDroit != null & filsGauche != null)
                return "( " + racine.toString() + " " + filsGauche.toString() + " " + filsDroit.toString() + ")";
            if (filsDroit == null && filsGauche != null)
                return "( " + racine.toString() + " " + filsGauche.toString() + " ())";
            if (filsDroit != null)
                return "( " + racine.toString() + " () " + filsDroit.toString() + ")";
            return racine.toString();
        }
        return "";
    }
}