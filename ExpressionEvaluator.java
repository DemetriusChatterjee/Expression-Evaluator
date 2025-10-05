import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Stack;
import java.util.Scanner;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.lang.IllegalArgumentException;

public class ExpressionEvaluator {
    private static class Node {
        String value;
        Node left;
        Node right;
        
        // constructor for the node
        Node(String value) {
            this.value = value;
            this.left = null;
            this.right = null;
        }
        
        // method to check if a node is an operator
        boolean isOperator() {
            return value.length() == 1 && "+-*/^".contains(value);
        }

        // method to check if a node is a unary operator
        boolean isUnaryOperator() {
            return value.equals("-u");
        }

        // method to check if a node is a variable
        boolean isVariable() {
            return value.matches("^[a-zA-Z]\\w*$");
        }

        // method to check if a node is a function
        boolean isFunction() {
            return value.matches("^(sin|cos|log)$");
        }

        // method to evaluate the expression tree
        double evaluate(String[] variableNames, double[] variableValues) {
            if (isVariable()) {
                for (int i = 0; i < variableNames.length; i++) {
                    if (variableNames[i].equals(value)) {
                        return variableValues[i];
                    }
                }
                throw new IllegalArgumentException("Undefined variable: " + value);
            }
            if (isFunction()) { // if the node is a function, evaluate the argument and apply the function
                double arg = left.evaluate(variableNames, variableValues);
                switch (value) {
                    case "sin": return Math.sin(arg);
                    case "cos": return Math.cos(arg);
                    case "log": return Math.log(arg);
                    default: throw new IllegalArgumentException("Unknown function: " + value);
                }
            }
            if (isUnaryOperator()) { // if the node is a unary operator, evaluate the argument and apply the operator
                return -left.evaluate(variableNames, variableValues);
            }
            if (!isOperator()) { // if the node is not an operator, evaluate the argument and return the value
                return Double.parseDouble(value);
            }
            double leftVal = left.evaluate(variableNames, variableValues); // evaluate the left and right arguments
            double rightVal = right.evaluate(variableNames, variableValues);
            switch (value) {
                case "+": return leftVal + rightVal;
                case "-": return leftVal - rightVal;
                case "*": return leftVal * rightVal;
                case "/":
                    if (rightVal == 0) throw new ArithmeticException("Division by zero");
                    return leftVal / rightVal;
                case "^": return Math.pow(leftVal, rightVal);
                default: throw new IllegalArgumentException("Unknown operator: " + value);
            }
        }

        // method to display the tree structure
        void displayTree(String prefix, boolean isLeft) {
            System.out.println(prefix + (isLeft ? "├── " : "└── ") + value);
            if (left != null) {
                left.displayTree(prefix + (isLeft ? "│   " : "    "), true);
            }
            if (right != null) {
                right.displayTree(prefix + (isLeft ? "│   " : "    "), false);
            }
        }
    }
    
    // method to check if a character is an operator
    private static boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '^';
    }
    
    // method to get precedence of operators
    private static int getPrecedence(String op) {
        switch (op) {
            case "+": return 1;
            case "-": return 1;
            case "*": return 2;
            case "/": return 2;
            case "^": return 3;
            case "-u": return 4;
            default: return 0;
        }
    }
    
    // method to convert infix expression to postfix expression
    public static ArrayList<Node> infixToPostfix(String expression) throws IllegalArgumentException {
        ArrayList<Node> postfix = new ArrayList<>();
        Stack<String> stack = new Stack<>();
        StringBuilder tokens = new StringBuilder();
        boolean expectOperand = true;
        
        for (char c : expression.toCharArray()) { // iterate through the expression
            if (Character.isLetterOrDigit(c) || c == '.') { // if the character is a letter or digit or a dot
                tokens.append(c); // add it to the token
            } else {
                if (tokens.length() > 0) {
                    postfix.add(new Node(tokens.toString())); // add the token to the postfix expression
                    tokens = new StringBuilder(); // reset the token
                    expectOperand = false;
                }
                if (c == '(') {
                    stack.push(String.valueOf(c)); // push the character to the stack
                    expectOperand = true; 
                } else if (c == ')') { // if the character is a closing parenthesis
                    while (!stack.isEmpty() && !stack.peek().equals("(")) { // pop the stack until we find the opening parenthesis
                        postfix.add(new Node(stack.pop()));
                    }
                    if (stack.isEmpty()) {
                        throw new IllegalArgumentException("Mismatched parentheses");
                    }
                    stack.pop(); // Remove '('
                    expectOperand = false;
                } else if (isOperator(c)) {
                    String op = String.valueOf(c);
                    if (expectOperand && c == '-') {
                        op = "-u"; // Unary minus
                    }
                    while (!stack.isEmpty() && getPrecedence(stack.peek()) >= getPrecedence(op)) { // pop the stack until we find an operator with lower precedence
                        postfix.add(new Node(stack.pop()));
                    }
                    stack.push(op); // push the operator to the stack
                    expectOperand = true;
                } else if (!Character.isWhitespace(c)) {
                    throw new IllegalArgumentException("Invalid character: " + c);
                }
            }
        }
        
        if (tokens.length() > 0) { // if there is a token left, add it to the postfix expression
            postfix.add(new Node(tokens.toString()));
        }
        
        while (!stack.isEmpty()) {
            if (stack.peek().equals("(")) { // if the stack is not empty and the top element is an opening parenthesis
                throw new IllegalArgumentException("Mismatched parentheses");
            }
            postfix.add(new Node(stack.pop()));
        }
        return postfix;
    }
    
    // method to build the expression tree
    public static Node buildExpressionTree(ArrayList<Node> postfix) {
        Stack<Node> stack = new Stack<>();
        
        for (Node node : postfix) { // iterate through the postfix expression
            if (node.isOperator() || node.isUnaryOperator() || node.isFunction()) { // if the node is an operator or a unary operator or a function
                if (node.isUnaryOperator() || node.isFunction()) { // if the node is a unary operator or a function
                    if (stack.isEmpty()) {
                        throw new IllegalArgumentException("Invalid expression: not enough operands");
                    }
                    node.left = stack.pop(); // pop the stack and set the left child of the node
                } else {
                    if (stack.size() < 2) { // if the stack has less than 2 elements, throw an error
                        throw new IllegalArgumentException("Invalid expression: not enough operands");
                    }
                    node.right = stack.pop(); // pop the stack and set the right child of the node
                    node.left = stack.pop(); // pop the stack and set the left child of the node
                }
            }
            stack.push(node);
        }
        
        if (stack.size() != 1) { // if the stack has more than 1 element, throw an error
            throw new IllegalArgumentException("Invalid expression: too many operands");
        }
        return stack.pop();
    }

    // method to save expression tree to file
    public static void saveExpressionTree(Node root, String filename) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("savedTree.txt"))) {
            writeNodeToFile(root, writer, 0);
        } catch (IOException e) {
            throw new IOException("Error saving expression tree to file: " + e.getMessage());
        }
    }

    // method to write the expression tree to a file
    private static void writeNodeToFile(Node node, BufferedWriter writer, int depth) throws IOException {
        if (node == null) {
            return;
        }
        
        String indent = "  ".repeat(depth);
        writer.write(indent + node.value + "\n");
        
        writeNodeToFile(node.left, writer, depth + 1);
        writeNodeToFile(node.right, writer, depth + 1);
    }

    // method to load expression tree from file
    public static Node loadExpressionTree(String filename) throws IOException {
        if (!filename.toLowerCase().endsWith(".txt")) {
            filename += ".txt";
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line = reader.readLine();
            if (line == null) {
                throw new IOException("File is empty");
            }
            return buildExpressionTree(infixToPostfix(line));
        } catch (IOException e) {
            throw new IOException("Error loading expression tree from file: " + e.getMessage());
        }
    }

    // method to evaluate the expression
    private static void evaluateExpression(Scanner scanner, ArrayList<String> variableNames, ArrayList<Double> variableValues) {
        System.out.print("Enter an expression: ");
        String input = scanner.nextLine().trim();
        
        try {
            ArrayList<Node> postfix = infixToPostfix(input);
            Node root = buildExpressionTree(postfix);
            System.out.println("Expression Tree:");
            root.displayTree("", true);
            
            // Update variable values
            variableNames.clear();
            variableValues.clear();
            for (Node node : postfix) {
                if (node.isVariable() && !variableNames.contains(node.value)) {
                    System.out.print("Enter value for " + node.value + ": ");
                    double value = scanner.nextDouble();
                    variableNames.add(node.value);
                    variableValues.add(value);
                    scanner.nextLine(); // Consume newline
                }
            }
            
            double result = root.evaluate(variableNames.toArray(new String[0]), variableValues.stream().mapToDouble(Double::doubleValue).toArray());
            System.out.printf("Result: %.2f\n", result);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (ArithmeticException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // method to batch process expressions from file
    private static void batchProcessExpressions(Scanner scanner) {
        System.out.print("Enter input file name: ");
        String inputFile = scanner.nextLine();
        if (!inputFile.toLowerCase().endsWith(".txt")) {
            inputFile += ".txt";
        }
        System.out.print("Enter output file name: ");
        String outputFile = scanner.nextLine();
        if (!outputFile.toLowerCase().endsWith(".txt")) {
            outputFile += ".txt";
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    ArrayList<Node> postfix = infixToPostfix(line);
                    Node root = buildExpressionTree(postfix);
                    double result = root.evaluate(new String[0], new double[0]);
                    writer.write(String.format("%s = %.2f\n", line, result));
                } catch (Exception e) {
                    writer.write(String.format("%s = Error: %s\n", line, e.getMessage()));
                }
            }
            System.out.println("Batch processing completed. Results written to " + outputFile);
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // method to save expression tree to file
    private static void saveExpressionTreeToFile(Scanner scanner) {
        System.out.print("Enter an expression to save: ");
        String input = scanner.nextLine().trim();
        
        try {
            ArrayList<Node> postfix = infixToPostfix(input);
            Node root = buildExpressionTree(postfix);
            saveExpressionTree(root, "savedTree.txt");
            System.out.println("Expression tree saved to savedTree.txt");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // method to load expression tree from file
    private static void loadExpressionTreeFromFile(Scanner scanner, ArrayList<String> variableNames, ArrayList<Double> variableValues) {
        System.out.print("Enter file name to load: ");
        String filename = scanner.nextLine();
        if (!filename.toLowerCase().endsWith(".txt")) {
            filename += ".txt";
        }
        
        try {
            Node root = loadExpressionTree(filename);
            System.out.println("Expression Tree loaded:");
            root.displayTree("", true);
            
            // Update variable values
            variableNames.clear();
            variableValues.clear();
            updateVariables(root, scanner, variableNames, variableValues);
            
            double result = root.evaluate(variableNames.toArray(new String[0]), variableValues.stream().mapToDouble(Double::doubleValue).toArray());
            System.out.printf("Result: %.2f\n", result);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // method to update variable values
    private static void updateVariables(Node node, Scanner scanner, ArrayList<String> variableNames, ArrayList<Double> variableValues) {
        if (node == null) return;
        if (node.isVariable() && !variableNames.contains(node.value)) { // if the node is a variable and not in the list of variables
            System.out.print("Enter value for " + node.value + ": ");
            double value = scanner.nextDouble();
            variableNames.add(node.value);
            variableValues.add(value);
            scanner.nextLine(); // Consume newline
        }
        updateVariables(node.left, scanner, variableNames, variableValues);
        updateVariables(node.right, scanner, variableNames, variableValues);
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ArrayList<String> variableNames = new ArrayList<>();
        ArrayList<Double> variableValues = new ArrayList<>();
        boolean isRunning = true;
        
        while (isRunning) {
            System.out.println("Choose an option:");
            System.out.println("1. Evaluate expression");
            System.out.println("2. Batch process expressions from file");
            System.out.println("3. Save expression tree to file");
            System.out.println("4. Load expression tree from file");
            System.out.println("5. Exit");
            System.out.print("Enter your choice: ");
            try{
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline
                
                switch (choice) {
                    case 1:
                        evaluateExpression(scanner, variableNames, variableValues);
                        break;
                    case 2:
                        batchProcessExpressions(scanner);
                        break;
                    case 3:
                        saveExpressionTreeToFile(scanner);
                        break;
                    case 4:
                        loadExpressionTreeFromFile(scanner, variableNames, variableValues);
                        break;
                    case 5:
                        scanner.close();
                        isRunning = false;
                        break;
                    default:
                        System.out.println("Invalid choice. Please enter a number between 1 and 5.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.next(); // Clear the invalid input
            }
        }
    }
}