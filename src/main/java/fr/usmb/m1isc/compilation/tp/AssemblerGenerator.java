package fr.usmb.m1isc.compilation.tp;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class AssemblerGenerator {
    private Arbre arbre;
    private String data_segment;
    private String code_segment;

    public AssemblerGenerator(Arbre arbre) throws IOException {
        this.arbre = arbre;
        generate();
    }

    public String cleanDataSegment(String data) {
        String[] lines = data.split("\n");
        String finalData = "";

        for (int i = 0; i < lines.length; i++) {
            if (!finalData.contains(lines[i])) finalData += (lines[i] + "\n");
        }

        return finalData;
    }

    public String getDataSegment(Arbre arbre) {
        String tmp = "";
        if (arbre == null)
            return "";
        if (arbre.getRacine() == SymbolType.LET) {
            tmp = ("\t" + arbre.getFilsGauche().toString() + " DD\n");
        }

        tmp += getDataSegment(arbre.getFilsGauche()) + getDataSegment(arbre.getFilsDroit());
        return cleanDataSegment(tmp);
    }

    public void printDataSegment(String data) {
        System.out.println("DATA SEGMENT");
        System.out.print(data);
        System.out.println("DATA ENDS");
    }

    public String getCodeSegment(Arbre data) {
        if (data == null)
            return "";

        // Création de la String résultat
        String tmp = "";

        // S'il s'agit d'une condition "IF"

        // S'il s'agit d'une boucle while
        if (data.getRacine() == SymbolType.WHILE) {
            tmp += "debut_while_1:\n"; 
            tmp += getCodeSegment(data.getFilsGauche()); // Condition du while
            tmp += "\tjz sortie_while_1\n";
            tmp += getCodeSegment(data.getFilsDroit()); // Contenu de la boucle
            tmp += "\tjmp debut_while_1\n";
            tmp += "sortie_while_1:\n";
        } else if (data.getRacine() == SymbolType.IF) {
            tmp += "debut_if_1:\n";
            tmp += "\tcmp eax, 0\n";  // Comparaison avec zéro (équivalent à jz si_1)
            tmp += "\tjz si_1\n";     // Jump si la condition est fausse
            tmp += getCodeSegment(data.getFilsGauche()); // Contenu de la condition
            tmp += "\tjmp alors_1\n"; // Jump vers la fin du if après l'exécution du contenu de la condition
            tmp += "si_1:\n";         // Étiquette si la condition est fausse
            tmp += getCodeSegment(data.getFilsDroit()); // Contenu du IF
            tmp += "alors_1:\n";      // Étiquette à la fin du bloc du IF
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
            // Le contenu du fils gauche est récupéré s'il s'agit d'une feuille
            if (data.getFilsGauche().getClass().getSimpleName().equals("Feuille")) {
                tmp += String.format("\tmov %s, eax\n", data.getFilsGauche());
            }
        }

        // S'il s'agit d'une des opérations suivantes : "+", "-", "/", "*"
        if (data.getRacine() == SymbolType.PLUS || data.getRacine() == SymbolType.MOINS ||
                data.getRacine() == SymbolType.MUL || data.getRacine() == SymbolType.DIV) {

            // Check la derniere commande
            String[] lines = tmp.split("\n");
            boolean isPush = lines[lines.length - 1].contains("push");

            // Le contenu du fils gauche est récupéré s'il s'agit d'une feuille ou s'il n'a pas déjà été push sur la pile
            if (data.getFilsGauche().getClass().getSimpleName().equals("Feuille") && !isPush) {
                tmp += String.format("\tmov eax, %s\n", data.getFilsGauche());
                tmp += "\tpush eax\n";
            }
            // Le contenu du fils droit est récupéré s'il s'agit d'une feuille
            if (data.getFilsDroit().getClass().getSimpleName().equals("Feuille")) {
                tmp += String.format("\tmov eax, %s\n", data.getFilsDroit());
            }

            // Pour chaque opération, on récupère sur la pile la valeur ebx pour effectuer le calcul.
            // Le calcul est stocké dans eax
            if (data.getRacine() == SymbolType.PLUS) {
                tmp += "\tpop ebx\n";
                tmp += "\tadd eax, ebx\n";
            } else if (data.getRacine() == SymbolType.MOINS) {
                tmp += "\tpop ebx\n";
                tmp += "\tsub eax, ebx\n";
            } else if (data.getRacine() == SymbolType.MUL) {
                tmp += "\tpop ebx\n";
                tmp += "\tmul eax, ebx\n";
            } else if (data.getRacine() == SymbolType.DIV) {
                tmp += "\tpop ebx\n";
                tmp += "\tdiv ebx, eax\n";
                tmp += "\tmov eax, ebx\n";
            }
            tmp += "\tpush eax\n";
        }

        if (data.getRacine() == SymbolType.MOD) {
            // Check la derniere commande
            String[] lines = tmp.split("\n");
            boolean isPush = lines[lines.length - 1].contains("push");

            // Le contenu du fils gauche est récupéré s'il s'agit d'une feuille ou s'il n'a pas déjà été push sur la pile
            if (data.getFilsDroit().getClass().getSimpleName().equals("Feuille") && !isPush) {
                tmp += String.format("\tmov eax, %s\n", data.getFilsDroit());
                tmp += "\tpush eax\n";
            }
            // Le contenu du fils droit est récupéré s'il s'agit d'une feuille
            if (data.getFilsGauche().getClass().getSimpleName().equals("Feuille")) {
                tmp += String.format("\tmov eax, %s\n", data.getFilsGauche());
            }

            tmp += "\tpop ebx\n";
            tmp += "\tmov ecx, eax\n";
            tmp += "\tdiv ecx, ebx\n";
            tmp += "\tmul ecx, ebx\n";
            tmp += "\tsub eax, ecx\n";
        }

        // S'il s'agit d'une comparaison : "<" "<="
        if (data.getRacine() == SymbolType.LT || data.getRacine() == SymbolType.LTE) {
            tmp += String.format("\tmov eax, %s\n", data.getFilsGauche());
            tmp += "\tpush eax\n";
            tmp += String.format("\tmov eax, %s\n", data.getFilsDroit());
            tmp += "\tpop ebx\n";
            tmp += "\tsub eax, ebx\n";

            if (data.getRacine() == SymbolType.LT) {
                tmp += "\tjle faux_gt_1\n"; 
                tmp += "\tmov eax, 1\n";
                tmp += "\tjmp sortie_gt_1\n";
                tmp += "faux_gt_1:\n";
                tmp += "\tmov eax, 0\n";
                tmp += "sortie_gt_1:\n";
            } else {
                tmp += "\tjle faux_gte_1\n"; 
                tmp += "\tmov eax, 1\n";
                tmp += "\tjmp sortie_gte_1\n";
                tmp += "faux_gte_1:\n";
                tmp += "\tmov eax, 0\n";
                tmp += "sortie_gte_1:\n";
            }
        }
        // S'il s'agit d'une comparaison : ">" ">="
        if (data.getRacine() == SymbolType.GT || data.getRacine() == SymbolType.GTE) {
            tmp += String.format("\tmov eax, %s\n", data.getFilsGauche());
            tmp += "\tpush eax\n";
            tmp += String.format("\tmov eax, %s\n", data.getFilsDroit());
            tmp += "\tpop ebx\n";
            tmp += "\tsub eax, ebx\n";

            if (data.getRacine() == SymbolType.GT) {
                tmp += "\tjg vrai_si_1\n";
                tmp += "faux_si_1:\n";
                tmp += "\tmov eax, 0\n";
                tmp += "\tjmp sortie_si_1\n";
                tmp += "vrai_si_1:\n";
                tmp += "\tmov eax, 1\n";
                tmp += "sortie_si_1:\n";
            } else {
                tmp += "\tjge vrai_sie_1\n";
                tmp += "faux_sie_1:\n";
                tmp += "\tmov eax, 0\n";
                tmp += "\tjmp sortie_sie_1\n";
                tmp += "vrai_sie_1:\n";
                tmp += "\tmov eax, 1\n";
                tmp += "sortie_sie_1:\n";
            }
        }

        // S'il s'agit d'un output
        if (data.getRacine() == SymbolType.OUTPUT) {
            tmp += String.format("\tmov eax, %s\n", data.getFilsGauche());
            tmp += "\tout eax\n";
        }

        if (data.getRacine() == SymbolType.EGALTEST) {
            tmp += "\tjne faux_egal_1\n"; // Jump si non égal
            tmp += "\t\tmov eax, 1\n";      // Valeur 1 si égal
            tmp += "\tjmp sortie_egal_1\n"; // Jump à la fin
            tmp += "\tfaux_egal_1:\n";       // Étiquette en cas de non égal
            tmp += "\t\tmov eax, 0\n";       // Valeur 0 si non égal
            tmp += "\tsortie_egal_1:\n";     // Étiquette de sortie
        }

        return tmp;
    }

    public void printCodeSegment(String data) {
        System.out.println("CODE SEGMENT");
        System.out.print(data);
        System.out.println("CODE ENDS");
    }

    public void generate() throws IOException {
        String dataSegment = getDataSegment(arbre);
        this.setData_segment(dataSegment);
        printDataSegment(dataSegment);
        String codeSegment = getCodeSegment(arbre);
        this.setCode_segment(codeSegment);
        printCodeSegment(codeSegment);
        this.saveAsFile();
    }

    public String writeCodeSegment() {
        return "CODE SEGMENT\n" + code_segment + "CODE ENDS\n";
    }

    public String writeDataSegment() {
        return "DATA SEGMENT\n" + data_segment + "DATA ENDS\n";
    }

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

    public String getData_segment() {
        return data_segment;
    }

    public void setData_segment(String data_segment) {
        this.data_segment = data_segment;
    }

    public String getCode_segment() {
        return code_segment;
    }

    public void setCode_segment(String code_segment) {
        this.code_segment = code_segment;
    }
}

