import java.io.File;
import java.io.FileWriter;
import java.util.*;

public class GSA {
    public static ArrayList<String> nonFinalChars = new ArrayList<>();
    public static ArrayList<String> finalChars = new ArrayList<>();
    public static ArrayList<String> syncChars = new ArrayList<>();
    public static Map<String, ArrayList<ArrayList<String>>> productions = new HashMap<>();
    public static ArrayList<String> productionsInOrder = new ArrayList<>();
    public static ArrayList<String> emptyChars = new ArrayList<>();
    public static Map<String, ArrayList<String>> startsWithTable = new HashMap<>();

    public static String FIRST_NONFINAL_CHAR = "<PocetnoStanjeMogAutomata>";
    public static String END_OF_LINE_CHAR = "#";

    public static ArrayList<String> states = new ArrayList<>(); // "<A> 0 0"
    public static HashSet<String> transitions = new HashSet<>(); // "0 A 0" index of current state in DKA_states, char, index od next state in DKA_states
    public static HashMap<String, ArrayList<String>> nextChars = new HashMap<>(); // oni znakici u viticastim zagradama

    public static ArrayList<HashSet<String>> DKA_states = new ArrayList<>();


    public static void main(String[] args) {
        //read the file from Stdin
        Scanner sc = new Scanner(System.in);
        nonFinalChars.add(FIRST_NONFINAL_CHAR);

        String line = sc.nextLine();
        line = line.replace("%V ", "");
        String[] split = line.split(" ");
        nonFinalChars.addAll(Arrays.asList(split));
        ArrayList<String> tmp = new ArrayList<>(Arrays.asList(nonFinalChars.get(1)));
        productions.put(FIRST_NONFINAL_CHAR, new ArrayList<>(Arrays.asList(tmp)));

        productionsInOrder.add(FIRST_NONFINAL_CHAR + "=" + nonFinalChars.get(1));

        line = sc.nextLine();
        line = line.replace("%T ", "");
        split = line.split(" ");
        for (String s : split) finalChars.add(s);


        line = sc.nextLine();
        line = line.replace("%Syn ", "");
        split = line.split(" ");
        syncChars.addAll(Arrays.asList(split));

        states.add(FIRST_NONFINAL_CHAR + " 0 0");
        states.add(FIRST_NONFINAL_CHAR + " 0 1");

        String key = sc.nextLine();
        while (sc.hasNextLine()) {
            line = sc.nextLine();
            do {
                line = line.trim();
                split = line.split(" ");

                if (!productions.containsKey(key)) productions.put(key, new ArrayList<>());
                productions.get(key).add(new ArrayList<>(Arrays.asList(split)));

                String tmp1 = line.replace(" ", ",");
                productionsInOrder.add(key + "=" + tmp1);

                for(int i = 0 ; i <= split.length; i++){
                    StringBuilder sb = new StringBuilder(key);

                    if(split[0].equals("$")) {
                        states.add(sb.append(" ").append(productions.get(key).size() - 1).append(" 0").toString());
                        break;
                    }
                    else {
                        sb.append(" ").append(productions.get(key).size() - 1).append(" ").append(i);
                        states.add(sb.toString());
                    }
                }

                // add all direct epsilon productions
                if (split[0].equals("$") && !emptyChars.contains(key)) {
                    emptyChars.add(key);
                }

                if (!sc.hasNextLine()) break;
                line = sc.nextLine();

            } while (line.startsWith(" "));
            key = line;
        }

        findEmptyChars();
        calculateStartsWithTable();
        createDKA();
        createTable();
    }

    // M move
    // R reduce
    // A accept
    public static void createTable(){
        HashMap<String, String> Actions = new HashMap<>();
        try{
            String folderPath = "analizator";
            String filePath = folderPath + "/table.txt";
            File folder = new File(folderPath);
            if (!folder.exists()) {
                folder.mkdirs();  // Create the directory (analizator)
            }
            FileWriter writer = new FileWriter(filePath);
            for(int i = 0; i < DKA_states.size(); i++){
                for(String subState : DKA_states.get(i)){
                    String[] split = subState.split(" ");
                    ArrayList<String> currentRightSide = productions.get(split[0]).get(Integer.parseInt(split[1]));
                    if((Integer.parseInt(split[2]) == currentRightSide.size()) || currentRightSide.get(0).equals("$")){// todo epsilon
                        if(split[0].equals(FIRST_NONFINAL_CHAR)){
                            //System.out.println(i + " " + END_OF_LINE_CHAR + " A|");
                            Actions.put(i + " " + END_OF_LINE_CHAR, " A|");
                        }
                        else{
                            for(String character : nextChars.get(subState)){
                                StringBuilder sb = new StringBuilder(currentRightSide.get(0));
                                for(int j = 1; j < currentRightSide.size(); j++){
                                    sb.append(",").append(currentRightSide.get(j));
                                }
                                //System.out.println((i + " " + character + " R|" + split[0] + "=" + sb.toString()));
                                if(Actions.containsKey(i + " " + character)){
                                    if(Actions.get(i + " " + character).contains("M|")) {
                                        //System.err.println("Reduction: " + (i + " " + character + " R|" + split[0] + "=" + sb.toString()) + " was not accepted because there was move action: " + Actions.get(i + " " + character));
                                        continue;
                                    }
                                    for(String prodInOrder : productionsInOrder){
                                        if(prodInOrder.contains(Actions.get(i + " " + character))) {
                                            //System.err.println("Reduction: " + (i + " " + character + " R|" + split[0] + "=" + sb.toString()) + " was not accepted because there was reduction action: " + Actions.get(i + " " + character));
                                            break;
                                        }
                                        else if(prodInOrder.contains(Actions.get(split[0] + "=" + sb.toString()))){
                                            //System.err.println("Reduction: " + Actions.get(i + " " + character) + " was removed because there is new reduction action: " + (i + " " + character + " R|" + split[0] + "=" + sb.toString()));
                                            Actions.put(i + " " + character + " R|", split[0] + "=" + sb.toString());
                                            break;
                                        }
                                    }
                                }
                                else{
                                    //System.out.println((i + " " + character + " R|" + split[0] + "=" + sb.toString()));
                                    Actions.put(i + " " + character, " R|" + split[0] + "=" + sb.toString());
                                }
                            }
                        }
                    }
                    else if(finalChars.contains(currentRightSide.get(Integer.parseInt(split[2])))){
                        for(String transition : transitions){
                            String[] transSplit = transition.split(" ");
                            if(transition.startsWith(String.valueOf(i)) && transSplit[1].equals(currentRightSide.get(Integer.parseInt(split[2])))){
                                if(Actions.containsKey(i + " " + currentRightSide.get(Integer.parseInt(split[2]))) && Actions.get(i + " " + currentRightSide.get(Integer.parseInt(split[2]))).contains("A|")) {
                                    //System.err.println("Action: " + i + " " + currentRightSide.get(Integer.parseInt(split[2])) + " M|" + transSplit[2] + " was removed because there was accept action: " + i + " " + currentRightSide.get(Integer.parseInt(split[2])) +  Actions.get(i + " " + currentRightSide.get(Integer.parseInt(split[2]))));
                                    continue;
                                }
                                else if(Actions.containsKey(i + " " + currentRightSide.get(Integer.parseInt(split[2])))){
                                    //System.err.println("Action: " + i + " " + currentRightSide.get(Integer.parseInt(split[2])) + Actions.get(i + " " + currentRightSide.get(Integer.parseInt(split[2]))) + " was removed because there is new move action: " + i + " " + currentRightSide.get(Integer.parseInt(split[2])) + " M|" + transSplit[2]);
                                }
                                Actions.put(i + " " + currentRightSide.get(Integer.parseInt(split[2])), " M|" + transSplit[2]);
                                //System.out.println(i + " " + currentRightSide.get(Integer.parseInt(split[2])) + " M|" + transSplit[2]);
                            }
                        }
                    }
                }
            }
            for(String key : Actions.keySet()){
                writer.write(key + Actions.get(key) + "\n");
            }
            //New states
            //System.out.println("%X%X%X%X%X");
            writer.write("%X%X%X%X%X\n");
            for(String transition : transitions){
                String[] split = transition.split(" ");
                if(!nonFinalChars.contains(split[1])) continue;
                if(!nonFinalChars.contains(split[1])) continue;
                //System.out.println(transition);
                writer.write(transition + "\n");
            }
            //Sync chars
            //System.out.println("%X%X%X%X%X");
            writer.write("%X%X%X%X%X\n");
            StringBuilder sb = new StringBuilder(syncChars.get(0));
            for (int i = 1; i < syncChars.size(); i++){
                sb.append(" ").append(syncChars.get(i));
            }
            //System.out.println(sb.toString());
            writer.write(sb.toString());
            writer.close();
        }catch(Exception e){
            System.err.println(e);
        }
        //

    }

    public static void createDKA(){
        nextChars.put(states.get(0), new ArrayList<>(Arrays.asList(END_OF_LINE_CHAR)));
        DKA_states.add(CLOSURE(new HashSet<>(Arrays.asList(states.get(0)))));

        ArrayList<String> allChars = new ArrayList<>();
        allChars.addAll(nonFinalChars);
        allChars.addAll(finalChars);
        boolean addedSomething = true;


        while (addedSomething){
            addedSomething = false;
            for(int i = 0; i < DKA_states.size(); i++){
                for(String Char : allChars){
                    HashSet<String> newStates = GOTO(DKA_states.get(i), Char);
                    if(newStates.isEmpty()) continue;
                    if(!DKA_states.contains(newStates)) {
                        DKA_states.add(newStates);
                        addedSomething = true;
                    }
                    transitions.add(i + " " + Char + " " + DKA_states.indexOf(newStates));
                }
            }
        }
    }
    public static HashSet<String> GOTO(HashSet<String> I, String X){
        HashSet<String> newSet = new HashSet<>();
        for(String state : I){
            String[] split = state.split(" ");
            ArrayList<String> currentRightSide = productions.get(split[0]).get(Integer.parseInt(split[1]));
            if((Integer.parseInt(split[2]) >= currentRightSide.size()) || currentRightSide.get(0).equals("$") || !currentRightSide.get(Integer.parseInt(split[2])).equals(X)) continue;

            String nextState = split[0] + " " + split[1] + " " + (Integer.parseInt(split[2]) + 1);
            newSet.add(nextState);
            nextChars.put(nextState, nextChars.get(state));
        }

        return CLOSURE(newSet);
    }
    public static HashSet<String> CLOSURE(HashSet<String> I){
        boolean addedSomething = true;
        ArrayList<String> tmp = new ArrayList<>(I);
        while (addedSomething){
            addedSomething = false;
            for(int i = 0; i < tmp.size(); i++){
                String[] split = tmp.get(i).split(" ");
                ArrayList<String> currentRightSide = productions.get(split[0]).get(Integer.parseInt(split[1]));
                if((Integer.parseInt(split[2]) >= currentRightSide.size()) || currentRightSide.get(0).equals("$")) continue;
                String nextChar = currentRightSide.get(Integer.parseInt(split[2]));
                if(!nonFinalChars.contains(nextChar)) continue;
                for(int j = 0; j < productions.get(nextChar).size(); j++){
                    String newState = nextChar + " " + j + " 0";
                    if(tmp.contains(newState)) continue;
                    tmp.add(newState);
                    addedSomething = true;

                    var nextStates = startsWithProduction(new ArrayList<>(currentRightSide.subList(Integer.parseInt(split[2]) + 1, currentRightSide.size())));

                    if(nextStates.getValue() || (nextStates.getKey().size() == 0)) {
                        for(var a : nextChars.get(tmp.get(i))){
                            if(!nextStates.getKey().contains(a)) nextStates.getKey().add(a);
                        }
                    }
                    nextChars.put(newState, nextStates.getKey());
                }
            }
        }

        return new HashSet<>(tmp);
    }
    public static Map.Entry<ArrayList<String>, Boolean> startsWithProduction(ArrayList<String> keys) {
        ArrayList<String> valid = new ArrayList<>();
        for (String currChar: keys){
            if(finalChars.contains(currChar)) {
                if(valid.contains(currChar)) break;
                valid.add(currChar);
                break;
            }
            for(String s : startsWithChar(currChar)){
                if(!valid.contains(s)) valid.add(s);
            }
        }
        boolean foundEmpty = valid.contains("$");
        if(foundEmpty) {
            valid.remove("$");
        }
        return Map.entry(valid, foundEmpty);
    }
    public static ArrayList<String> startsWithChar(String key){
        ArrayList<String> valid = new ArrayList<>();
        for(String s : startsWithTable.get(key)){
            if(finalChars.contains(s) || s.equals("$")) {
                valid.add(s);
            }
        }
        return valid;
    }
    public static void findEmptyChars() {
        boolean addedChar = true;
        while (addedChar) {
            addedChar = false;
            for (String key : productions.keySet()) {
                if (emptyChars.contains(key)) continue;
                for (ArrayList<String> rightSide : productions.get(key)) {
                    boolean isKeyEmpty = true;
                    for (String character : rightSide) {
                        if (!emptyChars.contains(character)) {
                            isKeyEmpty = false;
                            break;
                        }
                    }
                    if (isKeyEmpty) {
                        emptyChars.add(key);
                        addedChar = true;
                        break;
                    }
                }
            }
        }
    }
    public static void startsImmediatelyWith(String key) {
        ArrayList<String> validChars = new ArrayList<>();
        for (ArrayList<String> rightSide : productions.get(key)) {
            for (String character : rightSide) {
                if (!validChars.contains(character)) validChars.add(character);
                if (!emptyChars.contains(character)) {
                    break;
                }
            }
        }
        startsWithTable.put(key, validChars);
    }
    public static void calculateStartsWithTable(){
        for(String nonFinalChar : nonFinalChars){
            startsImmediatelyWith(nonFinalChar);
        }
        boolean addedSomething = true;
        while (addedSomething){
            addedSomething = false;
            for(String nonFinalChar : nonFinalChars) {
                ArrayList<String> nextCharsFromFirst = startsWithTable.get(nonFinalChar);
                for (int i = 0; i < nextCharsFromFirst.size(); i++) {
                    if(startsWithTable.get(nextCharsFromFirst.get(i)) == null) continue;
                    ArrayList<String> nextsNextChars = startsWithTable.get(nextCharsFromFirst.get(i));
                    for (int j = 0; j < nextsNextChars.size(); j++) {
                        if (startsWithTable.get(nonFinalChar).contains(nextsNextChars.get(j))) continue;
                        startsWithTable.get(nonFinalChar).add(nextsNextChars.get(j));
                        addedSomething = true;
                    }
                }
            }
        }
    }
}