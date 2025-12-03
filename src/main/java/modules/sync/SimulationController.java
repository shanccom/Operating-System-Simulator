package modules.sync;
//ACA SE ENCUENTRA LA ESPERA PARA LA FUNCION PASO A PASO
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SimulationController {
    private final Lock lock = new ReentrantLock();
    private final Condition stepCondition = lock.newCondition();
    
    private volatile boolean stepMode = false;
    private volatile boolean waitingForStep = false;
    private volatile boolean shouldContinue = false;
    
    public void setStepMode(boolean enabled) {
        lock.lock();
        try {
            this.stepMode = enabled;
            if (!enabled) {
                shouldContinue = true;
                stepCondition.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }
    
    public void waitForNextStep() throws InterruptedException {
        if (!stepMode) return;
        
        lock.lock();
        try {
            waitingForStep = true;
            while (stepMode && !shouldContinue) {
                stepCondition.await();
            }
            shouldContinue = false;
            waitingForStep = false;
        } finally {
            lock.unlock();
        }
    }
    
    public void advanceOneStep() {
        lock.lock();
        try {
            shouldContinue = true;
            stepCondition.signal();
        } finally {
            lock.unlock();
        }
    }
    
    public void continueExecution() {
        lock.lock();
        try {
            stepMode = false;
            shouldContinue = true;
            stepCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }
    
    public boolean isWaitingForStep() {
        lock.lock();
        try {
            return waitingForStep;
        } finally {
            lock.unlock();
        }
    }
    
    public boolean isStepMode() {
        return stepMode;
    }
}