package modules.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import model.Process;
import utils.Logger;

 //Funcionando con MOdified y Referenced

public class NRU extends MemoryManager {

    public NRU(int totalFrames) {
        super(totalFrames);
        Logger.memLog("NRU MemoryManager inicializado con " + totalFrames + " marcos");
    }

    @Override //Clases 4 
    protected int selectVictimFrame(Process requestingProcess, int requestedPage) {
        List<Integer> class0 = new ArrayList<>();
        List<Integer> class1 = new ArrayList<>();
        List<Integer> class2 = new ArrayList<>();
        List<Integer> class3 = new ArrayList<>();

        for (int i = 0; i < totalFrames; i++) {
            Frame f = frames[i];
            if (f.isOccupied()) {
                boolean R = f.isReferenced();
                boolean M = f.isModified();

                if (!R && !M) class0.add(i);
                else if (!R && M) class1.add(i);
                else if (R && !M) class2.add(i);
                else class3.add(i);
            }
        }
        
        Random rand = new Random(); // Elegir victima de la clase más baja disponible
        String why = "";
        int victim = -1;
        if (!class0.isEmpty()) {
            victim = class0.get(rand.nextInt(class0.size()));
            why = "no ser referenciada recientemente, no modificada (ideal para reemplazo)";
        } else if (!class1.isEmpty()) {
            victim = class1.get(rand.nextInt(class1.size()));
            why = "solo ser modificado recientemente, no referenciado (clase 1)";
        } else if (!class2.isEmpty()) {
            victim = class2.get(rand.nextInt(class2.size()));
            why = "ser referenciado recienteenete pero no modificado no existencias en anteriores categorias (clase 2)";
        } else if (!class3.isEmpty()) {
            victim = class3.get(rand.nextInt(class3.size()));
            why = "ultimo, por no existencias en las anteriores categorias prioritarias...es elegido";
        }

        Logger.memLog("NRU selecciono marco " + victim + " como victima por " + why);
        return victim;
    }

    @Override
    public String getAlgorithmName() {
        return "NRU (Not Recently Used)";
    }


    public String getClassState() {
        StringBuilder sb = new StringBuilder("Estado de clases NRU:\n");
        for (int i = 0; i < totalFrames; i++) {
            Frame f = frames[i];
            if (f.isOccupied()) {
                int simulatedClass = new Random().nextInt(4); // 0..3
                sb.append("Marco ").append(i).append(": ").append(f.toString())
                  .append(" | Clase=").append(simulatedClass).append("\n");
            }
        }
        return sb.toString();
    }

   
    public void printClassState() {
        Logger.memLog(getClassState());
       
    }
}