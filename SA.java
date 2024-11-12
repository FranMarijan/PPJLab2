import java.util.*;

class TreeNode {
    String symbol;
    int lineNumber;
    String lexeme;
    List<TreeNode> children;

    public TreeNode(String symbol, int lineNumber, String lexeme) {
        this.symbol = symbol;
        this.lineNumber = lineNumber;
        this.lexeme = lexeme;
        this.children = new ArrayList<>();
    }

    public void addChild(TreeNode child) {
        children.add(child);
    }

    public void dfs() {
        dfsHelper(this, 0);
    }

    private void dfsHelper(TreeNode node, int depth) {
        if (node == null) return;

        if (node.children.isEmpty()) {
            if (node.symbol.equals("$")) {
                System.out.println(" ".repeat(depth) + "$");
            } else {
                System.out.println(" ".repeat(depth) + node.symbol + " " + node.lineNumber + " " + node.lexeme);
            }
        } else {
            System.out.println(" ".repeat(depth) + node.symbol);
        }


        for (int i = node.children.size() - 1; i >= 0; i--) {
            dfsHelper(node.children.get(i), depth + 1);
        }
    }
}

public class SA {
    private Map<AbstractMap.SimpleEntry<Integer, String>, String> actiontable = new HashMap<>();
    private Map<AbstractMap.SimpleEntry<Integer, String>, Integer> newstates = new HashMap<>();
    private String[] sync;
    private List<String[]> input;

    public SA(Map<AbstractMap.SimpleEntry<Integer, String>, String> actiontable,
              Map<AbstractMap.SimpleEntry<Integer, String>, Integer> newstates,
              String[] sync,
              List<String[]> input) {
        this.actiontable = actiontable;
        this.newstates = newstates;
        this.sync = sync;
        this.input = input;
    }

    static Stack<Integer> stateStack = new Stack<>();
    static Stack<String> symbolStack = new Stack<>();
    static Stack<TreeNode> nodeStack = new Stack<>();
    public void parse() {
        stateStack.push(0);

        int inputIndex = 0;
        while (!stateStack.empty()) {
            int state = stateStack.peek();
            String[] symbol = {"#", "-1", "#"};
            if(inputIndex < input.size()) {
                symbol = input.get(inputIndex);
            }
            int lineNumber = Integer.parseInt(symbol[1]);
            String action = actiontable.get(new AbstractMap.SimpleEntry<>(state, symbol[0]));

            if (action != null && action.startsWith("M")) {
                TreeNode node = new TreeNode(symbol[0], lineNumber, symbol[2]);
                int newState = Integer.parseInt(action.split("/")[1]);
                nodeStack.push(node);
                stateStack.push(newState);
                symbolStack.push(symbol[0]);
                inputIndex++;
                //System.out.println("Stvorio sam list " + symbol[2] +" "+ lineNumber +" "+ symbol[0] + ";");
            }else if (action != null && action.startsWith("R")) {
                String reduction = action.split("/")[1];
                String[] parts = reduction.split("=");
                String leftSide = parts[0].trim();
                String rightSide = parts[1].trim();

                TreeNode node = new TreeNode(leftSide, lineNumber, null);

                String[] rightSymbols = rightSide.split(",");
                for (String symbols : rightSymbols) {
                    symbolStack.pop();
                    stateStack.pop();
                    TreeNode child = nodeStack.pop();
                    node.addChild(child);
                    //System.out.println("Dodali smo dijete " + child + " u " + node);
                }
                //System.out.println("Pushamo leftside u symolsStack " + leftSide);
                symbolStack.push(leftSide);
                int currentState = stateStack.peek();
                //System.out.println("currentState je " + currentState);
                int newState = newstates.get(new AbstractMap.SimpleEntry<>(currentState, leftSide));
                //System.out.println("newstate je " + newState);
                stateStack.push(newState);
                nodeStack.push(node);

            } else if (action != null && action.startsWith("A")) {
                String rootSymbol = symbolStack.pop();
                TreeNode root = nodeStack.pop();

                //System.out.println("Stvorio sam korijen " + rootSymbol +" "+ symbol[0] + ";");
                root.dfs();
                break;
            }
        }
    }


    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Map<AbstractMap.SimpleEntry<Integer, String>, String> actiontable = new HashMap<>();
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if(line.contains("%X%X%X%X%X")) break;
            String[] s = line.split(" ");
            //System.out.println(s[0] + "---" + s[1] + "---" + s[2]);
            actiontable.put(new AbstractMap.SimpleEntry<>(Integer.parseInt(s[0]), s[1]), s[2]);

        }

        Map<AbstractMap.SimpleEntry<Integer, String>, Integer> newstates = new HashMap<>();
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if(line.contains("%X%X%X%X%X")) break;
            String[] s = line.split(" ");
            //System.out.println(s[0] + "---" + s[1] + "---" + s[2]);
            newstates.put(new AbstractMap.SimpleEntry<>(Integer.parseInt(s[0]), s[1]), Integer.parseInt(s[2]));

        }
        String syncline = sc.nextLine();
        String[] sync = syncline.split(" ");

        List<String[]> input = new ArrayList<>();
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if(line.isEmpty()) break;
            String[] s = line.split(" ");
            //System.out.println(s[0] + "---" + s[1] + "---" + s[2]);
            input.add(s);

        }

        SA LRparser = new SA(actiontable, newstates, sync, input);
        LRparser.parse();
    }
}
