import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class Assembler {
    public static void main(String[] args) {
    	assembleFile("pong.asm");
    }
    
    public static void assembleFile(String input) {
        String inputFile = input;
        String outputFile = input.replace(".asm", ".hack");

        // Create and initialize the symbol table
        SymbolTable symbolTable = new SymbolTable();

        // Read the file
    	try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
    	    int currentAddress = 0;

    	    // First pass: Populate the symbol table
    	    String line;
    	    while ((line = reader.readLine()) != null) {
    	        // Remove comments and trim whitespace
    	        line = line.split("//")[0].trim();

    	        if (line.isEmpty()) {
    	            continue; // Skip empty lines
    	        }

    	        if (line.startsWith("(")) {
    	            // Found a label declaration
    	            String label = line.substring(1, line.length() - 1);
    	            symbolTable.addEntry(label, currentAddress);
    	        } else {
    	            currentAddress++; // Increment address for A and C instructions
    	        }
    	    }
    	} catch (IOException e) {
    	    e.printStackTrace();
    	}
    	
    	//Output file
    	try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
    		     BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

    		    int currentROMAddress = 16; // Start addresses for user-defined variables

    		    String line;
    		    while ((line = reader.readLine()) != null) {
    		        line = line.split("//")[0].trim();

    		        if (line.isEmpty() || line.startsWith("(")) {
    		            continue; // Skip labels and empty lines
    		        }

    		        if (line.startsWith("@")) {
    		            // A-instruction
    		            String symbol = line.substring(1);

    		            int address;
    		            if (Character.isDigit(symbol.charAt(0))) {
    		                // Numeric constant
    		                address = Integer.parseInt(symbol);
    		            } else {
    		                // Symbol or label
    		                if (!symbolTable.contains(symbol)) {
    		                    symbolTable.addEntry(symbol, currentROMAddress);
    		                    currentROMAddress++;
    		                }
    		                address = symbolTable.getAddress(symbol);
    		            }

    		            String binaryInstruction = String.format("%16s", Integer.toBinaryString(address))
    		                    .replace(' ', '0');
    		            writer.write(binaryInstruction);
    		            writer.newLine();
    		        } else {
                        // C-instruction
                        String binaryInstruction = parseAndTranslateCInstruction(line);
                        writer.write(binaryInstruction);
                        writer.newLine();
    		        }
    		    }
    		} catch (IOException e) {
    		    e.printStackTrace();
    		}    	
    }
    
    private static String parseAndTranslateCInstruction(String line) {
        String[] parts = line.split("=");
        String dest = parts.length > 1 ? parts[0] : "";
        String comp = parts[parts.length - 1];
        String jump = "";

        if (comp.contains(";")) {
            String[] compParts = comp.split(";");
            comp = compParts[0];
            jump = compParts[1];
        }

        // Translate dest to binary
        String destBinary = "000";
        if (dest.contains("M")) destBinary = "001";
        if (dest.contains("D")) destBinary = "010";
        if (dest.contains("A")) destBinary = "100";

        // Translate comp to binary
        String compBinary = "0000000";
        switch (comp) {
            case "0": compBinary = "0101010"; break;
            case "1": compBinary = "0111111"; break;
            case "-1": compBinary = "0111010"; break;
            case "D": compBinary = "0001100"; break;
            case "A": compBinary = "0110000"; break;
            case "!D": compBinary = "0001101"; break;
            case "!A": compBinary = "0110001"; break;
            case "-D": compBinary = "0001111"; break;
            case "-A": compBinary = "0110011"; break;
            case "D+1": compBinary = "0011111"; break;
            case "A+1": compBinary = "0110111"; break;
            // Add more cases for other comp values
        }

        // Translate jump to binary
        String jumpBinary = "000";
        if (jump.equals("JGT")) jumpBinary = "001";
        if (jump.equals("JEQ")) jumpBinary = "010";
        if (jump.equals("JGE")) jumpBinary = "011";
        if (jump.equals("JLT")) jumpBinary = "100";
        if (jump.equals("JNE")) jumpBinary = "101";
        if (jump.equals("JLE")) jumpBinary = "110";
        if (jump.equals("JMP")) jumpBinary = "111";

        return "111" + compBinary + destBinary + jumpBinary;
    }
}

class SymbolTable {
    private HashMap<String, Integer> table;

    public SymbolTable() {
        table = new HashMap<>();

        table.put("SP", 0);
        table.put("LCL", 1);
        table.put("ARG", 2);
    }

    public void addEntry(String symbol, int address) {
        table.put(symbol, address);
    }

    public boolean contains(String symbol) {
        return table.containsKey(symbol);
    }

    public int getAddress(String symbol) {
        return table.get(symbol);
    }
}
