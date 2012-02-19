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

		if(Machine.interrupt().disabled()){} //disable interrupt, if already disabled do nothing
		else{
			Machine.interrupt().disable(); //disable interrupt
		}

		/*Loop that checks the sleepList for any thread that has passed their duration to be asleep
		 *wakes up the thread and puts it onto the ready queue.
		 */

		/*
		 * Need to search from the bottom of the list not the top of the list. If you search from the top of the list
		 * your only able to actually search through the most recent threads put on the list, which would increase the
		 * time that the threads from the bottom of the list will be sleeping, as they were put on the list first. So
		 * what you want to do is start from the bottom of the list, as it would be the most accurate way to wake up
		 * a sleeping thread.
		 * 
		 */
		// for(int i = 0; i < sleepList.size(); i++){
		int i = 0;
		for(i = sleepList.size()-1; i >= 0; i--){ 
			/*
			 * Compares the time that it went into the sleepList + how long its supposed to be there
			 * and if it is less than or equal to the current time, than put the thread into the ready 
			 * queue.
			 */
			if((sleepList.get(i).getduration()) <= Machine.timer().getTime()){
				sleepList.get(i).getThread().ready(); //put the thread into the ready queue
				//waitQueue.add(sleepList.get(i).getThread()); //Testing purposes
				if(testcase == true){
					System.out.println("Waking up thread : " + sleepList.get(i).getThread().getName());
				}
				sleepList.remove(i); //Removing the object that held the thread and times
			}
		}
		Machine.interrupt().enable();
		KThread.currentThread().yield(); 
		/*Deals with context switching, you would want to yield the current thread that is running so that it allows you
		to run timerInterrupt() and check through the list;
		You want yield the current thread(the one running timerInterrupt()), after the current thread has done what ever it has to do, which in this case
		has finished checking the sleepList(), so now it will yield for another thread to run.
		 */
	}

	/**
	 * Creates an Object that will hold both the thread and time that it is in the SleepList
	 * includes the time that it went into the queue.
	 * @author jle33
	 *
	 */
	protected class threadHold {
		KThread Thread = null;
		long duration = 0;

		public threadHold(KThread currentThread, long wakeTime){
			this.Thread = currentThread;
			this.duration = wakeTime;
		}
		public long getduration(){
			return duration;
		}
		public KThread getThread(){
			return Thread;
		}
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
		long wakeTime = Machine.timer().getTime() + x;
		threadHold hold = new threadHold(KThread.currentThread(), wakeTime); //Add both the current thread and time to list
		sleepList.add(hold); //Add the thread and time to the list.
		/*
		 * reversed from putting thread to sleep than adding to list, to put thread onto sleepList than put thread to sleep.
		 */
		KThread.currentThread().sleep(); //Put thread to sleep
		Machine.interrupt().enable();
	}

	public void selftest(){
		testcase = true;
		System.out.println("Starting alarm test");
		KThread thread1 = new KThread(new Runnable(){	
			public void run(){
				System.out.println("Creating Thread1");
				long time = 1500;
				waitUntil(time);
			}
		});
		KThread thread2 = new KThread(new Runnable(){	
			public void run(){
				System.out.println("Creating Thread2");
				long time = 2500;
				waitUntil(time);
			}

		});
		KThread thread3 = new KThread(new Runnable(){	
			public void run(){
				System.out.println("Creating Thread3");
				long time = 3500;
				waitUntil(time);
			}
		});
		KThread thread4 = new KThread(new Runnable(){	
			public void run(){
				System.out.println("Creating Thread4");
				long time = 4500;
				waitUntil(time);
			}
		});
		KThread thread5 = new KThread(new Runnable(){	
			public void run(){
				System.out.println("Creating Thread5");
				long time = 0;
				waitUntil(time);
			}
		});
		thread1.setName("Thread 1");
		thread2.setName("Thread 2");
		thread3.setName("Thread 3");
		thread4.setName("Thread 4");
		thread5.setName("Thread 5");
		System.out.println("Running Threads");
		Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(thread1, 5);
		ThreadedKernel.scheduler.setPriority(thread2, 7);
		ThreadedKernel.scheduler.setPriority(thread3, 4);
		ThreadedKernel.scheduler.setPriority(thread4, 6);
		ThreadedKernel.scheduler.setPriority(thread5, 5);
		Machine.interrupt().enable();
		thread1.fork();
		thread2.fork();
		thread3.fork();
		thread4.fork();
		thread5.fork();
		System.out.println("Thread1 should wake up first, than Thread4, Thread3, Thread2, Thread5");
		System.out.println("Running Threads");
		Machine.interrupt().disable();
		long timebefore = Machine.timer().getTime();

		while((timebefore+30000) > Machine.timer().getTime()){
			timerInterrupt();
		}
		Machine.interrupt().enable();
		KThread.currentThread().yield();
		System.out.println("Ending");
	}
	public LinkedList<threadHold> sleepList = new LinkedList<threadHold>();
	boolean testcase = false;
}
