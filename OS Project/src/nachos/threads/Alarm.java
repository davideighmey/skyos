package nachos.threads;

import nachos.machine.*;
import java.util.LinkedList;
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
		KThread Thread = null;
		long timeInList = 0;
		long duration = 0;

		public threadHold(KThread currentThread, long sleepTime, long wakeTime){
			this.Thread = currentThread;
			this.timeInList = sleepTime;
			this.duration = wakeTime;
		}
		public long getime(){
			return timeInList;
		}
		public long getduration(){
			return duration;
		}
		public KThread getThread(){
			return Thread;
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
		//KThread.currentThread().yield();
		if(Machine.interrupt().disabled()){}
		else{
			Machine.interrupt().disable();
		}
		for(int i = 0; i < sleepList.size(); i++){
			if((sleepList.get(i).getduration()) <= Machine.timer().getTime()){
				waitQueue.add(sleepList.get(i).getThread());
				sleepList.remove(i);
			}
		}
		Machine.interrupt().enable();
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
	@SuppressWarnings("static-access") //Can Take out As not needed, just to rid of yellow line
	public void waitUntil(long x) {
		Machine.interrupt().disable();
		// for now, cheat just to get something working (busy waiting is bad)
		long wakeTime = Machine.timer().getTime() + x;
		// No more Cheating!
		//while (wakeTime > Machine.timer().getTime())
		//   KThread.yield();
		if (x == 0){
			waitQueue.add(KThread.currentThread()); //For testing purposes
			//KThread.currentThread().ready(); //Goes to the ready queue
			timerInterrupt();
		}
		else{
			sleepTime = Timer.getTime();//Time it goes into the sleep List
			KThread.currentThread().sleep();
			threadHold hold = new threadHold(KThread.currentThread(), sleepTime, wakeTime);
			sleepList.add(hold);
		}
		Machine.interrupt().enable();
	}


	public LinkedList<threadHold> sleepList = new LinkedList<threadHold>();
	public LinkedList<KThread> waitQueue  = new LinkedList<KThread>();
	private long sleepTime = 0;
	private Timer Timer = null;
}
