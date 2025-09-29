import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Authors: Spencer Ford, Gail Kinder, Cameron Kadre
 * This is our implementation of the SIM_IL_Interface. It uses an
 * int[256] to represent the memory. a List<String> that hols the 
 * trace(i.e. what each count has done). The point is to simulate
 * a simple chip and play with assembly language and bit arithmetic.
 * A few functions were added for readablility such as bit arithmetic ones.
 */

public class S12_IL implements S12_IL_Interface {

    private int PC, ACC, COUNT;

    private final int[] mem = new int[256];
    private final List<String> trace = new ArrayList<>();
    private boolean halted = false;

    /**
     * S12_IL constructor initialize system variables with '0'
     * @return S12_IL
     */
    public S12_IL() {
        this.PC = 0;    //8-bit value
        this.ACC = 0;   //12-bit value
        this.COUNT = 0; 
        Arrays.fill(mem, 0);
    }

    /**
     * initializeMem reads in the plain text file and instantiates the memory array.
     * It does this by opening a BufferedReader uses nextDataLine() to grab one line
     * at a time. 
     * 
     * @param filename of memory to be read in
     * @return true if memory successfully parsed and instantiated
     */
    @Override
    public boolean intializeMem(String filename) {

        try(BufferedReader br = new BufferedReader(new FileReader(filename))) {
            
            Arrays.fill(mem, 0);    //make sure mem is empty
            trace.clear();              //make sure trace is empty
            halted = false;
            COUNT = 0;

            String header = nextDataLine(br);   //Grab first line that's not started with '//' - i.e. header
            if(header == null) throw new IllegalArgumentException("missing header line");
             String[] hp = header.split("\\s+"); // "\\s+" - this regex finds any whitespace
            if(hp.length !=2) throw new IllegalArgumentException("Header format wrong");

            PC = parseBin8(hp[0]);  //PC is and 8-bit value -> call parseBin8()
            ACC = parseBin12(hp[1]);//ACC is a 12-bit address->call parseBin12()


            String line;

            while ((line = nextDataLine(br)) != null) {     //Itterate while hasNextLine()
                String[] parts = line.split("\\s+");  //Split into [0] = address, [1] = instruction               
                if(parts.length != 2) {
                    throw new IllegalArgumentException("mem line format wrong" + line +"'");
                }
                int addr = parseHexByte(parts[0]);  //Change hex addr to int[0-256]
                int word = parseBin12(parts[1]);    //Mask 12-bit word value into an int

                mem[addr & 0xFF] = word & 0xFFF;    //addr & 0xFF -> Mask for 8-bit mem address [0-256] & word & 0xFFF store 12-bits of the instruction at that address
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
     }

     /**
     * Returns state of the registers in the machine
     * out[0] = PC, out[1] = ACC, out[2] == COUNT (cycle), out[3] = halted (String)
     * @return String array of register values
     */
    @Override
    public String[] getProcessorState() {
        String[] out = {
            "Cycles Executed: " + Integer.toString(COUNT), //Us in build function
            "PC: " + toHex8(PC), 
            "ACC: " + toBin12(ACC)
        }; 
        return out;
    }

    /**
     * Builds a string - address(hex), data at address(bin)
     * This was used to write debug information.
     * @return string representation of memory
     */
    @Override
    public String getMemState() {
        StringBuilder sb = new StringBuilder(256 * 6);  //Use build in StringBuilder to make list of all 256 addresses
        for(int addr = 0; addr < 256; addr++) {
            sb.append(toHex8(addr)).append(' ').append(toBin12(mem[addr])).append('\n');
        }
        return sb.toString();
    }

     /**
     * This changes the state of the machine based on the machine instruction.
     * It takes the all the part of the current position marked with PC,
     * and breaks it into pieces to be processed. It's then sent through
     * a big switch statement that maps each opcode and these opcodes
     * changes the machine state based on what opcode was found.
     * 
     * @return String representation of the instruction executed (binary)
     */
    @Override
    public String update() {
        if (halted) {

            return "000000000000";
        }

        int pcBefore = PC & 0xFF;   //PC masked to 8-bit
        int instr = mem[pcBefore] & 0xFFF;  //instr masked at mem[pc] for 12-bits (word siz)
        PC = (PC + 1) & 0xFF;   //Advance PC to next instruction -> mask for 12-bits

        int opcode = (instr >> 8) & 0xF;   //Parse opcode: take instr >>>(bit shift) 8bits in 12-8 = 4bit opbode -> maske for only 4-bits
        int X = instr & 0xFF;              //Parse X: mask instruction for last 8-bits
        int addr = X & 0xFF;               //Parse addr: X - instruction; masked for last 8-bits
        int mval = mem[addr] & 0xFFF;      //Grab value at mem[addr] and mask for 12-bit

        String mnem = null;                //This will hold + build a string for printing to files or stdout(debug)

        switch (opcode) {
            case  0x0: //JMP X
                PC = addr;
                mnem = String.format("JMP %02X", addr);
                break;

            case 0x1:  //JN X 
                if (isNeg12(ACC)) {
                    PC = addr;
                    mnem = String.format("JN %2X", addr);
                }
                break;

            case  0x2:  //JZ X
                if ((ACC & 0xFFF) == 0) {
                    PC = addr;
                }
                mnem = String.format("JZ %02X", addr);
                break;
            
            case  0x4:  //LOAD X
                ACC = mval & 0xFFF;
                mnem = String.format("LOAD %2X", addr);
                break;

            case  0x5:  //STORE X
                mem[addr] = ACC & 0xFFF;
                mnem = String.format("STORE %2X", addr);
                break;

            case  0x6://STORI 
                int indir = mem[addr] & 0xFF;
                mem[indir] = ACC & 0xFFF;
                mnem = String.format("STORI (%2X)->%02X", addr, indir);
                break;

            case  0x8://AND X
                ACC = (ACC & mval) & 0xFFF;
                mnem = String.format("AND %02X", addr);
                break;
            
            case  0x9://OR X
                ACC = (ACC | mval) & 0xFFF;
                mnem = String.format("OR %02X", addr);
                break;

            case  0xA://ADD X
                ACC = (ACC + mval) & 0xFFF;
                mnem = String.format("ADD %02X", addr);
                break;

            case  0xB://SUB X
                ACC = (ACC - mval) & 0xFFF;
                mnem = String.format("SUB %02X", addr);
                break;

            case 0xF://HALT
                halted = true;
                mnem = "HALT";
                break;

            default:
                mnem = String.format("NOP(?) opcode=%X", opcode);
                break;
        }

        COUNT++;
        String traceLine = String.format("%s %s -> PC=%s ACC=%s ; %s", 
            toHex8(pcBefore), toBin12(instr), toHex8(PC), toBin12(ACC), mnem
        );
        trace.add(traceLine);

        return toBin12(instr);
    }

     /**
     * Takes in af filename argument and writes the current state of every
     * mem address to the file. It is written as follows
     * addr(hex) data(bin)
     * @param filename - name of memFile to create
     * @return true if successful file creation, false otherwise
     */
    @Override
    public boolean writeMem(String filename) {
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {

            bw.write(toBin8(PC));
            bw.write(' ');
            bw.write(toBin12(ACC));
            bw.write('\n');

            for (int addr = 0; addr < 256; addr++) {
                bw.write(toHex8(addr));
                bw.write(' ');
                bw.write(toBin12(mem[addr]));
                bw.write('\n');
            }

            return true;
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Writes the entire trace stack line by line into 
     * the file provided. It's added to and updated in the update().
     * @param filename - name of trace file to create
     * @return true if successfully written, else false
     */
    @Override
    public boolean writeTrace(String filename) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {
            for (String line : trace) {
                bw.write(line);
                bw.write('\n');
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * @param br - bufferedreader
     * @return String - next line in the mem file
     */
    private static String nextDataLine(BufferedReader br) throws IOException {
        String line;
        while ((line = br.readLine()) != null) {
            String t = stripLineComment(line);
            if (t.isEmpty()) {
                continue;
            }
            return t;
        }
        return null;
    }

    /**
     * This is a helper function that allows for mem files with 
     * inline comments. It'll remove inline comments and give back just
     * the instructions
     * @param s - string that is to be stripped
     * @return String - trimmed string without comments
     */
    private static String stripLineComment(String s) {
        int i = s.indexOf("//");
        if (i >= 0) {
            s = s.substring(0, i);
        }
        return s.trim();
    }

    /**
     * This is a helper function turns string into 8-bit binary int
     * @param s - string that is to be converted to 8-bits binary
     * @return int - masked for 8-bits
     */
    private static int parseBin8(String s) {
        if (s.length() != 8) throw new IllegalArgumentException("PC must be 8-bit binary");
        return Integer.parseInt(s, 2) & 0xFF;
    }

    /**
     * This is a helper function turns string into 12-bit binary int
     * @param s - string that is to be converted to 12-bits binary
     * @return int - masked for 12-bits
     */
    private static int parseBin12(String s) {
        if (s.length() != 12) throw new IllegalArgumentException("Word must be 12-bit binary");
        return Integer.parseInt(s, 2) & 0xFFF;
    }

    /**
     * This is a helper function turns string hex values into 8-bit binary
     * @param s - string that is to be converted to 8-bits binary
     * @return int - masked for 8-bits
     */
    private static int parseHexByte(String s) {
        int v = Integer.parseInt(s, 16);
        if ((v & ~0xFF) != 0) throw new IllegalArgumentException("Address must be 8-bit hex");
        return v & 0xFF;
    }

    /**
     * This is a helper function 8-bit binary value and converts to a string of hex
     * @param s - string that is to be converted to 8-bits binary
     * @return String - to be printed and expressed as hex number
     */
    private static String toHex8(int x) {
        return String.format("%02X", x & 0xFF);
    }

    /**
     * This is a helper function 8-bit binary value and converts to a string of bin
     * @param s - string that is to be converted to 8-bits binary
     * @return String - to be printed and expressed as bin number
     */
    private static String toBin8(int x) {
        String s = Integer.toBinaryString(x & 0xFF);
        return "00000000".substring(s.length()) + s;
    }

    /**
     * This is a helper function 12-bit binary value and converts to a string of bin
     * @param s - string that is to be converted to 12-bits binary
     * @return String - to be printed and expressed as bin number
     */
    private static String toBin12(int x) {
        String s = Integer.toBinaryString(x & 0xFFF);
        return "000000000000".substring(s.length()) + s;
    }

    /**
     * Test to see if word 12-bit is negative or has overrun 12 bits
     * @param x - int to be tested
     * @return boolean - is wrd neg
     */
    private static boolean isNeg12(int x) {
        return (x & 0x800) != 0;
    }

    /**
     * Halted getter
     * @return halted
     */
    public boolean isHalted() { return halted; }

}
