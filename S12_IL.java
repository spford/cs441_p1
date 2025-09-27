import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
     * It does this by opening a BufferedReader
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

            PC = parseBin8(hp[0]);
            ACC = parseBin12(hp[1]);


            String line;
            int linesRead = 0;

            while ((line = nextDataLine(br)) != null) {
                String[] parts = line.split("\\s+");                
                if(parts.length != 2) {
                    throw new IllegalArgumentException("mem line format wrong" + line +"'");
                }
                int addr = parseHexByte(parts[0]);
                int word = parseBin12(parts[1]);

                mem[addr & 0xFF] = word & 0xFFF;
                linesRead++;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
     }

     /**
     * Returns state of the registers in the machine
     * This is one place to add a javadoc in your implementation.
     * Specifying in what order the register values are returned
     * 
     * @return String array of register values
     */
    @Override
    public String[] getProcessorState() {
        String[] out = {
            "PC=" + toHex8(PC),
            "ACC=" + toBin12(ACC),
            "COUNT=" + Integer.toString(COUNT),
            "HALTED=" + halted
        };
        return out;
    }

    /**
     * String form of the current state of the memory (e.g. each line is hex
     * address, space, binary word)
     * 
     * @return string representation of memory
     */
    @Override
    public String getMemState() {
        StringBuilder sb = new StringBuilder(256 * 6);
        for(int addr = 0; addr < 256; addr++) {
            sb.append(toHex8(addr)).append(' ').append(toBin12(mem[addr])).append('\n');
        }
        return sb.toString();
    }

     /**
     * execute one cycle of the machine
     * 
     * @return String representation of the instruction executed (binary)
     */
    @Override
    public String update() {
        if (halted) return "000000000000";

        int pcBefore = PC & 0xFF;
        int instr = mem[pcBefore] & 0xFFF;
        PC = (PC + 1) & 0xFF;

        int opcode = (instr >>> 8) & 0xF;
        int X = instr & 0xFF;
        int addr = X & 0xFF;
        int mval = mem[addr] & 0xFFF;

        String mnem = null;

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
     * Write out the memFile associated with the current state of the simulation
     * 
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
     * 
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

    private static String stripLineComment(String s) {
        int i = s.indexOf("//");
        if (i >= 0) {
            s = s.substring(0, i);
        }
        return s.trim();
    }

    private static int parseBin8(String s) {
        if (s.length() != 8) throw new IllegalArgumentException("PC must be 8-bit binary");
        return Integer.parseInt(s, 2) & 0xFF;
    }

    private static int parseBin12(String s) {
        if (s.length() != 12) throw new IllegalArgumentException("Word must be 12-bit binary");
        return Integer.parseInt(s, 2) & 0xFFF;
    }

    private static int parseHexByte(String s) {
        int v = Integer.parseInt(s, 16);
        if ((v & ~0xFF) != 0) throw new IllegalArgumentException("Address must be 8-bit hex");
        return v & 0xFF;
    }

    private static String toHex8(int x) {
        return String.format("%02X", x & 0xFF);
    }

    private static String toHex12(int x) {
        return String.format("%03X", x & 0xFFF);
    }

    private static String toBin8(int x) {
        String s = Integer.toBinaryString(x & 0xFF);
        return "00000000".substring(s.length()) + s;
    }

    private static String toBin12(int x) {
        String s = Integer.toBinaryString(x & 0xFFF);
        return "000000000000".substring(s.length()) + s;
    }

    private static boolean isNeg12(int x) {
        return (x & 0x800) != 0;
    }

    public boolean isHalted() { return halted; }

}
