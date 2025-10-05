/**
 * Authors: Spencer Ford, Gail Kinder, Cameron Kadre
 * About: S12_Sim is the main driver to test our implementation of S12_IL.
 * It takes one mandatory argument that is the memory file. The mem file is 
 * used to set the state of the machine to be simulated. Users can use the 
 * -o flag to modify the name of their desired output file. Users can also
 * use the -c to specify the number of cycles you wish to execute. Otherwise 
 * the program will run until it hits a halt command.
 */


public class S12_Sim {

    public static void main(String[] args) {

        if(args.length < 1) {
            System.err.println("java S12_Sim <memFile> <optional: -o outputFileBasedName> <optional: -c cyclesToExecute>");
            System.exit(1);
        }

        String memFile = null;
        String baseName = null;
        Integer maxCycles = null;

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (!a.startsWith("-") && memFile == null) {
                memFile = a;
                continue;
            }
            switch (args[i]) {
                case "-o":
                    if (i + 1 >= args.length) { //deal with -o flag (output file name)
                        System.err.println("-o requires a value");
                        System.exit(2);
                    } else {
                        baseName = args[++i];
                    }
                        break;
                case "-c":
                    if (i + 1 >= args.length) { //deal with -c flag (# of cycles)
                        System.err.println("-c requires an integer");
                        System.exit(2);
                    } else {
                        maxCycles = Integer.parseInt(args[++i]);
                    }
                        break;
                    }
            
        }

        if (memFile == null) { 
            System.err.println("Failed to id mem file: ");
            System.exit(2);
        }

        S12_IL sim = new S12_IL();    //create new S12 object

        if (!sim.intializeMem(memFile)) {
            System.err.println("Failed to load mem file: " + memFile);
            System.exit(2);
        }

        if (maxCycles == null) { maxCycles = 1000000; } //set max to 10000000 unless specified - arbitrary
        int steps = 0;
        while (!sim.isHalted() && (maxCycles < 0 || steps < maxCycles)) {
            sim.update();
            steps++;
        }

        for (String s : sim.getProcessorState()) {  //print out helpful info
            System.out.println(s);
        }

        if (baseName == null) {
            baseName = memFile;
            boolean m0k = sim.writeMem(baseName + ".mem");
            boolean t0k = sim.writeTrace(baseName + ".trace");
            System.out.println("writeMem ->" + (baseName + "_mem") + " : " + m0k);
            System.out.println("writeTrace ->" + (baseName + "_trace") + " : " + t0k);
        }
    }
}
