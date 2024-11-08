import java.io.File;
import java.io.FileWriter;
import java.util.*;

public class GSA {
    public static ArrayList<String> nonFinalChars = new ArrayList<>();
    public static ArrayList<String> finalChars = new ArrayList<>();
    public static ArrayList<String> syncChars = new ArrayList<>();
    public static Map<String, ArrayList<ArrayList<String>>> productions = new HashMap<>();
    public static HashMap<String, Integer> productionsInOrder = new HashMap<>();
    public static ArrayList<String> emptyChars = new ArrayList<>();
    public static Map<String, ArrayList<String>> startsWithTable = new HashMap<>();
    public static String FIRST_NONFINAL_CHAR = "<PocetnoStanjeMogAutomata>";
    public static String END_OF_LINE_CHAR = "#";
    public static ArrayList<String> states = new ArrayList<>(); // "<A> 0 0"
    public static HashMap<String, ArrayList<String>> nextChars = new HashMap<>(); // key : DKA_state substate without last part, value : all chars in {}
    public static HashMap<String, String> transitions = new HashMap<>(); // "0 A 0" index of current state in DKA_states, char, index od next state in DKA_states
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

        productionsInOrder.put(FIRST_NONFINAL_CHAR + "=" + nonFinalChars.get(1), 0);

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
        int prodInOrderIterator = 1;
        while (sc.hasNextLine()) {
            line = sc.nextLine();
            do {
                line = line.trim();
                split = line.split(" ");

                if (!productions.containsKey(key)) productions.put(key, new ArrayList<>());
                productions.get(key).add(new ArrayList<>(Arrays.asList(split)));

                String tmp1 = line.replace(" ", ",");
                productionsInOrder.put(key + "=" + tmp1, prodInOrderIterator);
                prodInOrderIterator++;

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
        long startTime = System.currentTimeMillis();


        findEmptyChars();
        calculateStartsWithTable();
        createDKA();

        System.out.println("Tmp END");

        createTable();


        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        System.out.println("Process ran for " + elapsedTime + " milliseconds.");
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
                    String[] split2 = split[2].split(",");
                    ArrayList<String> currentRightSide = productions.get(split[0]).get(Integer.parseInt(split[1]));
                    if((Integer.parseInt(split2[0]) == currentRightSide.size()) || currentRightSide.get(0).equals("$")){

                        if(split[0].equals(FIRST_NONFINAL_CHAR)){
                            //System.out.println(i + " " + END_OF_LINE_CHAR + " A|");
                            Actions.put(i + " " + END_OF_LINE_CHAR, " A/");
                        }
                        else{
                            String reductionKey = i + " " + split2[1];
                            if(Actions.containsKey(reductionKey)){
                                if(Actions.get(reductionKey).contains("A/") || Actions.get(reductionKey).contains("M/")) continue;
                                String prevRed = Actions.get(reductionKey).split("R/")[1];
                                StringBuilder sb = new StringBuilder(currentRightSide.get(0));
                                for(int j = 1; j < currentRightSide.size(); j++){
                                    sb.append(",").append(currentRightSide.get(j));
                                }
                                String currRed =  split[0] + "=" + sb.toString();
                                if(productionsInOrder.get(currRed) >= productionsInOrder.get(prevRed)) continue;
                                Actions.put(reductionKey, " R/" + currRed);
                            }
                            else{
                                StringBuilder sb = new StringBuilder(currentRightSide.get(0));
                                for(int j = 1; j < currentRightSide.size(); j++){
                                    sb.append(",").append(currentRightSide.get(j));
                                }
                                String currRed =  split[0] + "=" + sb.toString();
                                Actions.put(reductionKey, " R/" + currRed);
                            }
                        }
                    }
                    else if(finalChars.contains(currentRightSide.get(Integer.parseInt(split2[0])))){
                        String key = i + " " + currentRightSide.get(Integer.parseInt(split2[0]));
                        if(transitions.containsKey(key)){
                            String[] transSplit = transitions.get(key).split(" ");

                            if(Actions.containsKey(key) && Actions.get(key).contains("A/")) {
                                //System.err.println("Action: " + i + " " + currentRightSide.get(Integer.parseInt(split2[0])) + " M|" + transSplit[2] + " was removed because there was accept action: " + i + " " + currentRightSide.get(Integer.parseInt(split[2])) +  Actions.get(i + " " + currentRightSide.get(Integer.parseInt(split[2]))));
                                continue;
                            }
                            else if(Actions.containsKey(key)){
                                //System.err.println("Action: " + i + " " + currentRightSide.get(Integer.parseInt(split2[0])) + Actions.get(i + " " + currentRightSide.get(Integer.parseInt(split2[0]))) + " was removed because there is new move action: " + i + " " + currentRightSide.get(Integer.parseInt(split[2])) + " M|" + transSplit[2]);
                            }
                            Actions.put(key, " M/" + transitions.get(key));
                            //System.out.println(i + " " + currentRightSide.get(Integer.parseInt(split2[0])) + " M|" + transSplit[2]);
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
            //todo
            for(String transition : transitions.keySet()){
                String[] split = transition.split(" ");
                if(!nonFinalChars.contains(split[1])) continue;
                //System.out.println(transition);
                writer.write(transition + " " + transitions.get(transition) + "\n");
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
            //System.err.println();
            e.printStackTrace();
        }
        //

    }

        public static void createDKA(){
        nextChars.put((0 + " " + states.get(0) + "," + END_OF_LINE_CHAR), new ArrayList<>(Arrays.asList(END_OF_LINE_CHAR)));
        DKA_states.add(CLOSURE(new HashSet<>(Arrays.asList(states.get(0) + "," + END_OF_LINE_CHAR))));

        ArrayList<String> allChars = new ArrayList<>();
        allChars.addAll(nonFinalChars);
        allChars.addAll(finalChars);
        //boolean addedSomething = true;


        //while (addedSomething){
            //ArrayList<HashSet<String>> tmp = new ArrayList<>(DKA_states);
            //addedSomething = false;
            for(int i = 0; i < DKA_states.size(); i++){
                for(String Char : allChars){
                    HashSet<String> newStates = GOTO(DKA_states.get(i), Char);
                    if(newStates.isEmpty()) continue;
                    if(!DKA_states.contains(newStates)) {
                        DKA_states.add(new HashSet<>(newStates));
                        //addedSomething = true;
                    }
                    transitions.put(i + " " + Char , String.valueOf(DKA_states.indexOf(newStates)));
                }
            }
            //DKA_states.clear();
            //DKA_states.addAll(tmp);
        //}
    }
    public static HashSet<String> GOTO(HashSet<String> I, String X){
        HashSet<String> newSet = new HashSet<>();
        for(String state : I){
            String[] split = state.split(" ");
            String[] split2 = split[2].split(",");
            ArrayList<String> currentRightSide = productions.get(split[0]).get(Integer.parseInt(split[1]));
            if((Integer.parseInt(split2[0]) >= currentRightSide.size()) || currentRightSide.get(0).equals("$") || !currentRightSide.get(Integer.parseInt(split2[0])).equals(X)) continue;

            String nextState = split[0] + " " + split[1] + " " + (Integer.parseInt(split2[0]) + 1) + "," + split2[1];
            newSet.add(nextState);
            // todo nextChars.put(nextState, nextChars.get(state));
        }

        return CLOSURE(newSet);
    }
    public static HashSet<String> CLOSURE(HashSet<String> I){
        //boolean addedSomething = true;
        ArrayList<String> tmp = new ArrayList<>(I);
       // while (addedSomething){
            //addedSomething = false;
            for(int i = 0; i < tmp.size(); i++){
                String[] split = tmp.get(i).split(" ");
                String[] split2 = split[2].split(",");
                ArrayList<String> currentRightSide = productions.get(split[0]).get(Integer.parseInt(split[1]));

                if((Integer.parseInt(split2[0]) >= currentRightSide.size()) || currentRightSide.get(0).equals("$")) continue;

                String nextChar = currentRightSide.get(Integer.parseInt(split2[0]));
                if(!nonFinalChars.contains(nextChar)) continue;



                ArrayList<String> rightSidePlusNextStatesOfCurrentState = new ArrayList<>(currentRightSide.subList(Integer.parseInt(split2[0]) + 1, currentRightSide.size()));
                rightSidePlusNextStatesOfCurrentState.add(split2[1]);
                ArrayList<String> nextStates = startsWithProduction(rightSidePlusNextStatesOfCurrentState);

                //System.out.println(/*rightSidePlusNextStatesOfCurrentState + " : " +*/ nextStates);

                for(int j = 0; j < productions.get(nextChar).size(); j++){
                    for(String final_b : nextStates){
                        String newState = nextChar + " " + j + " 0," + final_b;
                        if(!tmp.contains(newState)) {
                            tmp.add(newState);
                        }
                        //addedSomething = true;
                    }
                }
            }
        //}
        return new HashSet<>(tmp);
    }
    public static ArrayList<String> startsWithProduction(ArrayList<String> keys) {
        ArrayList<String> valid = new ArrayList<>();
        for (String currChar: keys){
            if(finalChars.contains(currChar) || currChar.equals(END_OF_LINE_CHAR)) {
                if(valid.contains(currChar)) break;
                valid.add(currChar);
                break;
            }
            for(String s : startsWithChar(currChar)){
                if(!valid.contains(s)) valid.add(s);

            }
            if(!emptyChars.contains(currChar)) break;
        }

        return valid;
    }
    public static ArrayList<String> startsWithChar(String key){
        ArrayList<String> valid = new ArrayList<>();
        //if(!startsWithTable.containsKey(key)) return valid;
        for(String s : startsWithTable.get(key)){
            if((finalChars.contains(s) || s.equals("$")) && !valid.contains(s)) {
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
                if(character.equals("$")) break;
                else if(finalChars.contains(character)){
                    if(!validChars.contains(character)) validChars.add(character);
                    break;
                }
                else{
                    if (!validChars.contains(character)) validChars.add(character);
                    if (!emptyChars.contains(character)) {
                        break;
                    }
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
                    if(finalChars.contains(nextCharsFromFirst.get(i))) continue;
                    //System.out.println(nextCharsFromFirst.get(i));
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
