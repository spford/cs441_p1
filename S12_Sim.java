
public class S12_Sim {
        public static void main(String[] args) {

        if(args.length < 1) {
            System.err.println("java S12_Sim <memFile> <optional: -o outputFileBasedName> <optional: -c cyclesToExecute>");
            System.exit(1);
        }

        String memFile = args[0];
        String baseName = null;
        int maxCycles = -1;

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "-o":
                    if (i + 1 >= args.length) {
                        System.err.println("-o requires a value");
                        System.exit(2);
                    } else {
                        baseName = args[++i];
                    }
                        break;
                case "-c":
                    if (i + 1 >= args.length) {
                        System.err.println("-c requires an integer");
                        System.exit(2);
                        maxCycles = Integer.parseInt(args[++i]);
                        break;
                    }
                default:
                    System.err.println("Unknown args: " + args[i]);
            }
        }

        S12_IL sim = new S12_IL();    
        if (!sim.intializeMem(memFile)) {
            System.err.println("Failed to load mem file: " + memFile);
            System.exit(2);
        }

        int steps = 0;
        while (!sim.isHalted() && (maxCycles < 0 || steps < maxCycles)) {
            sim.update();
            steps++;
        }

        for (String s : sim.getProcessorState()) {
            System.out.println(s);
        }

        if (baseName != null) {
            boolean m0k = sim.writeMem(baseName + ".mem");
            boolean t0k = sim.writeTrace(baseName + ".trace");
            System.out.println("writeMem ->" + (baseName + ".mem") + " : " + m0k);
            System.out.println("writeTrace ->" + (baseName + ".trace") + " : " + t0k);
        }
    }
}
