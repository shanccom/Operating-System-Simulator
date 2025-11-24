packege modules.sync;

import model.Burst;
import model.Process;
import model.ProcessState;
import utils.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class IOManager implements Runnable {
  private static class IORequest {
    final Process process;
    final Burst ioBuest;
    final int requestTime;

    IORequest (Process process, Burst ioBurst, int requestTime) {
      this.process = process;
      this.ioBurst = ioBurst;
      this.requestTime = requestTime;
    }

    @Override
    public String toString() {
      return String.format("IORequest[%s, duracion=%d]", process.getPid, ioBurst.getDuration());
    }
  }
}