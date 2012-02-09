package nachos.threads;

import nachos.machine.*;

import java.util.LinkedList;
import nachos.threads.ThreadQueue;
import nachos.machine.Timer;
/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
	protected class threadHold {
			public threadHold(KThread currentThread, Long sleepTime){
				KThread Thread = currentThread;
				Long TimeInList = sleepTime;
			}
	}
	
    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    
    public void timerInterrupt() {
	KThread.currentThread().yield();
    }
    
    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
    Machine.interrupt().disable();
    
	// for now, cheat just to get something working (busy waiting is bad)
	long wakeTime = Machine.timer().getTime() + x;
	// No more Cheating!
	//while (wakeTime > Machine.timer().getTime())
	 //   KThread.yield();
	if (x == 0){
		waitQueue.add(KThread.currentThread());
		timerInterrupt();
	}
	else{
		sleepTime = Timer.getTime();
		threadHold Hold = new threadHold(KThread.currentThread(), x);
		sleepList.add();
	}
	Machine.interrupt().enable();
    }
    
    
    public LinkedList<threadHold> sleepList = new LinkedList<threadHold>();
    public LinkedList<KThread> waitQueue  = new LinkedList<KThread>();
    private long sleepTime = 0;
    private Timer Timer = null;
}
