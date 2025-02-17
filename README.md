# TP Compilation : Génération de code

L'objectif du TP est d'utiliser les outils JFlex et CUP pour générer du code machine abstraits correspondant à un sous ensemble du langage **λ-ada**.

## Fonctionnement de la génération :

Comme vu lors des précédents TP, nous utilisons dabord J-flex pour reconnaitre les caractères :

```
/* definitions regulieres */

chiffre     = [0-9]
espace      = \s
mod         = "%" | "mod"|"MOD"
let         = "let"|"LET" | "var" | "VAR"
while       = "while" | "WHILE"
do          = "do" | "DO"
if          = "if" | "IF"
then        = "then" | "THEN"
else        = "else" | "ELSE"
input       = "input" | "INPUT"
output      = "output" | "OUTPUT"
nil         = "nil" | "NIL" | "NULL"
not         = "not" | "NOT"
and         = "and" | "AND" | "&&"
or          = "or" | "OR" | "||"

```

pour ensuite les utiliser dans les règles lexical :

```
/* regles */

{let}       { return new Symbol(sym.LET, yyline, yycolumn) ;}
{while}     { return new Symbol(sym.WHILE, yyline, yycolumn) ;}
{do}        { return new Symbol(sym.DO, yyline, yycolumn) ;}
{if}        { return new Symbol(sym.IF, yyline, yycolumn) ;}
{then}      { return new Symbol(sym.THEN, yyline, yycolumn) ;}
{else}      { return new Symbol(sym.ELSE, yyline, yycolumn) ;}
{nil}       { return new Symbol(sym.NIL, yyline, yycolumn) ;}
{input}     { return new Symbol(sym.INPUT, yyline, yycolumn) ;}
{output}    { return new Symbol(sym.OUTPUT, yyline, yycolumn) ;}
{and}       { return new Symbol(sym.AND, yyline, yycolumn) ;}
{or}        { return new Symbol(sym.OR, yyline, yycolumn) ;}
{not}       { return new Symbol(sym.NOT, yyline, yycolumn) ;}
"="         { return new Symbol(sym.EGAL, yyline, yycolumn) ;}
"=="         { return new Symbol(sym.EGALTEST, yyline, yycolumn) ;}
"<"         { return new Symbol(sym.GT, yyline, yycolumn) ;}
"<="        { return new Symbol(sym.GTE, yyline, yycolumn) ;}
">"         { return new Symbol(sym.SI, yyline, yycolumn) ;}

```

Nous passons ensuite à notre fichier cup pour la structure du code :

Nous allons dabord nous interesser au symboles pour reconnaitre nos expressions :

```
terminal PLUS, MOINS, MOINS_UNAIRE, MUL, DIV, MOD, NOT, OR, AND, PAR_G, PAR_D, SEMI, POINT, LET, INPUT, OUTPUT, IF, THEN, ELSE, WHILE, DO, EGAL, GT, GTE, SI, SIE, NIL, ERROR, EGALTEST;
terminal Integer ENTIER;
terminal String IDENT;
/* non terminaux */
non terminal Arbre program, sequence, expression,expr;

```

puis nous avons les précédences :

```
precedence nonassoc OUTPUT;
precedence right OR;
precedence right AND;
precedence right NOT;
precedence nonassoc EGAL;
precedence nonassoc GT, GTE, SI, SIE, EGALTEST;
precedence left PLUS, MOINS;
precedence left MUL, DIV, MOD;
precedence left MOINS_UNAIRE;

```

Maintenant, l'architecture de notre code se fait grâce à 2 classes :

la classe Arbre et Feuille (feuille etant une sous classe de arbre) :
Arbre

```
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
    ...
    // getters et setters
    ...

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
```

Feuille

```
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
```

On peut voir dans arbre que l'affichage sous la forme (racine filsGauche filsDroit) est fait dans le toString.

Cela nous permet donc de structurer notre code en fonction des symboles reçus :

```
sequence ::= expression:e1 SEMI sequence:e2 {: RESULT = new Arbre(SymbolType.SEMI, e1, e2); :}
            | expression:e {: RESULT = e; :}
            |
            ;

// une expession est soit une affectation ,une boucle tant que ou un si / sinon
expression ::= expr:e {: RESULT = e; :}
             | LET IDENT:nom EGAL expr:e        {: RESULT = new Arbre(SymbolType.LET, new Feuille(nom), e); :}
             | WHILE expr:cond DO expression:e  {: RESULT = new Arbre(SymbolType.WHILE, cond, e); :}
             | IF expr:cond THEN expression:a   {: RESULT = new Arbre(SymbolType.IF, cond, a); :}
             //| IF expr:cond THEN expression:a1 ELSE expression:a2 {: RESULT = new Arbre(SymbolType.IF, a1, a2); :}
             | error // reprise d'erreurs
             ;

// expression arithmetiques et logiques
expr ::= NOT:op expr:e          {: RESULT = new Arbre(SymbolType.NOT, e); :}
       | expr:e1 AND expr:e2    {: RESULT = new Arbre(SymbolType.AND, e1, e2); :}
       | expr:e1 OR expr:e2     {: RESULT = new Arbre(SymbolType.OR, e1, e2); :}
       | expr:e1 EGAL expr:e2   {: RESULT = new Arbre(SymbolType.EGAL, e1, e2); :}
       | expr:e1 GT expr:e2     {: RESULT = new Arbre(SymbolType.GT, e1, e2); :}
       | expr:e1 GTE expr:e2    {: RESULT = new Arbre(SymbolType.GTE, e1, e2); :}
       | expr:e1 SI expr:e2     {: RESULT = new Arbre(SymbolType.SI, e1, e2); :}
       | expr:e1 SIE expr:e2    {: RESULT = new Arbre(SymbolType.SIE, e1, e2); :}
       | expr:e1 PLUS expr:e2   {: RESULT = new Arbre(SymbolType.PLUS, e1, e2); :}
       | expr:e1 MOINS expr:e2  {: RESULT = new Arbre(SymbolType.MOINS, e1, e2); :}
       | expr:e1 MUL expr:e2    {: RESULT = new Arbre(SymbolType.MUL, e1, e2); :}
       | expr:e1 DIV expr:e2    {: RESULT = new Arbre(SymbolType.DIV, e1, e2); :}
       | expr:e1 MOD expr:e2    {: RESULT = new Arbre(SymbolType.MOD, e1, e2); :}
       | expr:e1 EGALTEST expr:e2    {: RESULT = new Arbre(SymbolType.EGALTEST, e1, e2); :}
       | MOINS expr:e           {: RESULT = new Arbre(SymbolType.MOINS_UNAIRE, e); :}   %prec MOINS_UNAIRE
       | OUTPUT expr:e          {: RESULT = new Arbre(SymbolType.OUTPUT,e); :}
       | INPUT                  {: RESULT = new Arbre(SymbolType.INPUT); :}
       | NIL                    {: RESULT = new Arbre(SymbolType.NIL); :}
       | ENTIER:n               {: RESULT = new Feuille(n.toString()); :}
       | IDENT:id               {: RESULT = new Feuille(id.toString()); :}
       | PAR_G sequence:e PAR_D {: RESULT = e; :}
       ;
```

l'énumeration SymbolType permet de limiter le risque d'erreur pour la classe AssemblerGenerator qui permettra l'affichage de notre code machine.
Cette appel à la classe ce fait lors de la définition de programme :

```
program ::=
    sequence:s POINT {:
        System.out.println(s.toString());
        AssemblerGenerator ag = new AssemblerGenerator(s);
    :}
    | sequence:s {:
        System.out.println(s.toString());
        AssemblerGenerator ag = new AssemblerGenerator(s);
    :}
    ;
```

pour l'affichage, le code va ce déplacer dans l'arbre séquence pour afficher le code :

```
public class AssemblerGenerator {
    private Arbre arbre;
    private String data_segment;
    private String code_segment;

    public AssemblerGenerator(Arbre arbre) throws IOException {
        this.arbre = arbre;
        generate();
    }
    ...
    //plusieurs fonctions pour générer les parties du code machine dans generate()
    ...

    public void generate() throws IOException {
        String dataSegment = getDataSegment(arbre);
        this.setData_segment(dataSegment);
        printDataSegment(dataSegment);
        String codeSegment = getCodeSegment(arbre);
        this.setCode_segment(codeSegment);
        printCodeSegment(codeSegment);
        this.saveAsFile();
    }
```

avec des exemples de la "génération" du code marchine en fonction de la racine de l'arbre :

```
public String getCodeSegment(Arbre data) {
        if (data == null)
            return "";

        // Création de la String résultat
        String tmp = "";

        // S'il s'agit d'une condition "IF"

        // S'il s'agit d'une boucle while
        if (data.getRacine() == SymbolType.WHILE) {
            tmp += "debut_while_1:\n"; // TODO : voir si le 1 doit s'incrémenter à chaque while
            tmp += getCodeSegment(data.getFilsGauche()); // Condition du while
            tmp += "\tjz sortie_while_1\n";
            tmp += getCodeSegment(data.getFilsDroit()); // Contenu de la boucle
            tmp += "\tjmp debut_while_1\n";
            tmp += "sortie_while_1:\n";
        } else if (data.getRacine() == SymbolType.IF) {
            tmp += "debut_if_1:\n";
            tmp += "\tjz si_1\n";
            tmp += getCodeSegment(data.getFilsGauche()); // Contenu de la condition
            tmp += "\tjmp alors_1\n";
            tmp += getCodeSegment(data.getFilsDroit());; // Contenu du IF
            tmp += "fin_if_1:\n";
        } else {
            tmp = getCodeSegment(data.getFilsGauche()) + getCodeSegment(data.getFilsDroit());
        }



        // S'il s'agit d'un input
        if (data.getRacine() == SymbolType.INPUT) tmp += "\tin eax\n";

        // S'il s'agit d'une affectation
        if (data.getRacine() == SymbolType.LET) {
            // Le contenu du fils droit est récupéré s'il s'agit d'une feuille
            if (data.getFilsDroit().getClass().getSimpleName().equals("Feuille")) {
                tmp += String.format("\tmov eax, %s\n", data.getFilsDroit());
            } else {
                // Sinon si le fils n'est pas une feuille, on s'assure que le résultat n'a pas été push pour
                // pouvoir l'utiliser dans l'affectation à venir
                String[] lines = tmp.split("\n");
                boolean isPush = lines[lines.length - 1].contains("push");

                // On recompose la string sans le "push"
                if (isPush) {
                    tmp = "";
                    for (int i = 0; i < lines.length - 1; i++) {
                        tmp += (lines[i] + "\n");
                    }
                }
            }
            ...
            // le reste
```

Voila comment est crée notre code machine.

Enfin, le code généré est enregistrer sur un fichier res.asm :

```
    public void saveAsFile() throws IOException {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("res.asm"));
            String tmp = writeDataSegment() + writeCodeSegment();
            writer.write(tmp);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
```

## Exercice 1 :

Dans la première partie du tp on pourra se limiter à la génération de code pour les expressions arithmétiques sur les nombres entiers.

Ainsi, l'expression

```
let prixHt = 200;
let prixTtc =  prixHt * 119 / 100 .
```

correspondant, par exemple, à l'arbre ci-dessous pourrait amener à la production du code suivant :

```
( SEMI ( LET prixHt 200) ( LET prixTtc ( DIV ( MUL prixHt 119) 100)))
DATA SEGMENT
	prixHt DD
	prixTtc DD
DATA ENDS
CODE SEGMENT
	mov eax, 200
	mov prixHt, eax
	mov eax, prixHt
	push eax
	mov eax, 119
	pop ebx
	mul eax, ebx
	push eax
	mov eax, 100
	pop ebx
	div ebx, eax
	mov eax, ebx
	mov prixTtc, eax
CODE ENDS
```

## Exercice 2 :

Étendre la génération de code aux opérateurs booléens, de comparaison, aux boucles et aux conditionnelles, correspondant au sous-ensemble du langage λ-ada utilisé pour le TP précédent.

Exemple de code source pour le compilateur : calcul de PGCD.

```
let a = input;
let b = input;
while (0 < b)
do (let aux=(a mod b); let a=b; let b=aux );
output a
.
```

Et un exemple de code qui pourrait être produit :

```
( SEMI ( LET a INPUT) ( SEMI ( LET b INPUT) ( SEMI ( WHILE ( GT 0 b) ( SEMI ( LET aux ( MOD a b)) ( SEMI ( LET a b) ( LET b aux)))) ( OUTPUT a ()))))
DATA SEGMENT
	b DD
	a DD
	aux DD
DATA ENDS
CODE SEGMENT
	in eax
	mov a, eax
	in eax
	mov b, eax
debut_while_1:
	mov eax, 0
	push eax
	mov eax, b
	pop ebx
	sub eax,ebx
	jle faux_gt_1
	mov eax,1
	jmp sortie_gt_1
faux_gt_1:
	mov eax,0
sortie_gt_1:
	jz sortie_while_1
	mov eax, b
	push eax
	mov eax, a
	pop ebx
	mov ecx,eax
	div ecx,ebx
	mul ecx,ebx
	sub eax,ecx
	mov aux, eax
	mov eax, b
	mov a, eax
	mov eax, aux
	mov b, eax
	jmp debut_while_1
sortie_while_1:
	mov eax, a
	out eax
CODE ENDS
```

## Exemple 3 :

Plusieurs rajouts on était fait qui permettent de faire plus que les exercices demandé comme
le si ... alors ...

Exemple de code source :

```
let a = 3 ;
let b = 2 ;
if a == 3 then let c = a + b ;
let d = a + c
.
```

va nous donner :

```
( SEMI ( LET a 3) ( SEMI ( LET b 2) ( SEMI ( IF ( EGALTEST a 3) ( LET c ( PLUS a b))) ( LET d ( PLUS a c)))))

DATA SEGMENT
	a DD
	b DD
	c DD
	d DD
DATA ENDS
CODE SEGMENT
	mov eax, 3
	mov a, eax
	mov eax, 2
	mov b, eax
debut_if_1:
	jz si_1
	jne faux_egal_1
		mov eax, 1
	jmp sortie_egal_1
	faux_egal_1:
		mov eax, 0
	sortie_egal_1:
	jmp alors_1
	mov eax, a
	push eax
	mov eax, b
	pop ebx
	add eax, ebx
	mov c, eax
fin_if_1:
	mov eax, a
	push eax
	mov eax, c
	pop ebx
	add eax, ebx
	mov d, eax
CODE ENDS

```
