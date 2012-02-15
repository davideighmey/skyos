package nachos.threads;

import nachos.machine.*;
import nachos.threads.PriorityScheduler.PriorityQueue;


import java.util.*;


/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
	/**
	 * Allocate a new priority scheduler.
	 */
	public PriorityScheduler() {
	}

	/**
	 * Allocate a new priority thread queue.
	 *
	 * @param	transferPriority	<tt>true</tt> if this queue should
	 *					transfer priority from waiting threads
	 *					to the owning thread.
	 * @return	a new priority thread queue.
	 */

	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
	}

	public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getPriority();
	}

	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getEffectivePriority();
	}

	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());

		Lib.assertTrue(priority >= priorityMinimum &&
				priority <= priorityMaximum);

		getThreadState(thread).setPriority(priority);
	}

	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			return false;

		setPriority(thread, priority+1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			return false;

		setPriority(thread, priority-1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	/**
	 * The default priority for a new thread. Do not change this value.
	 */
	public static final int priorityDefault = 1;
	/**
	 * The minimum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMinimum = 0;
	/**
	 * The maximum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMaximum = 7;    

	/**
	 * Return the scheduling state of the specified thread.
	 *
	 * @param	thread	the thread whose scheduling state to return.
	 * @return	the scheduling state of the specified thread.
	 */
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}

	/**
	 * A <tt>ThreadQueue</tt> that sorts threads by priority.
	 */
	protected class PriorityQueue extends ThreadQueue {
		PriorityQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
		}

		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this);
			//waitPQueue.add(thread);
		}
		/**
		 * The specified thread has received exclusive access, without using
		 * <tt>waitForAccess()</tt> or <tt>nextThread()</tt>. Assert that no
		 * threads are waiting for access.
		 */
		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			//Lib.assertTrue(waitPQueue.isEmpty());
			getThreadState(thread).acquire(this);

		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			PriorityQueue buffer = null;
			ThreadState peek = pickNextThread();				//peek at the nextThread and return a thread with a highest priority and longest wait time
			if(peek!=getThreadState(waitPQueue.peek())){		//not the same
				buffer = new PriorityQueue(this.transferPriority);
				int count = 0;									//headache saver
				while(peek!=getThreadState(waitPQueue.peek())){
					buffer.waitPQueue.add(waitPQueue.poll());
					count++;
				}
				System.out.println("Count(Started): "+count);
				assert(peek==getThreadState(waitPQueue.peek()));
				KThread returnThread = waitPQueue.poll();
				count--;
				while(buffer.waitPQueue.peek()!=null){
					waitPQueue.add(buffer.waitPQueue.poll());
					count--;
				}
				System.out.println("Count(Done): "+count);
				getThreadState(returnThread).timeINqueue = 0;
				return returnThread;
			}
			// return the highest priority and removes it, else return null
			else{
				getThreadState(waitPQueue.peek()).timeINqueue = 0;
				return waitPQueue.poll();}
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 *
		 * @return	the next thread that <tt>nextThread()</tt> would
		 *		return.
		 */
		protected ThreadState pickNextThread() {
			// implement me

			KThread hold = waitPQueue.peek();
			for(KThread k:waitPQueue){
				if((getThreadState(hold).effective==getThreadState(k).effective)//Check of there is other same priority thread
						&&(getThreadState(hold).timeINqueue < getThreadState(k).timeINqueue)){ //If there is one, switch to the longest waiting thread
					hold = k;
				}
			}


			return getThreadState(hold);
		}

		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me (if you want)
		}

		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		public boolean transferPriority;
		/**
		 *<tt>LockHolder-</tt> The thread with which this lock is associated   
		 */
		protected ThreadState LockHolder;

		private Queue<KThread> waitPQueue = new java.util.PriorityQueue<KThread>(1, new PriorityComparator());
		public class PriorityComparator implements Comparator<KThread>
		{	@Override
			//Allow automatic sorting of the Queue
			public int compare(KThread o1, KThread o2) {
			if(getThreadState(o1).priority<getThreadState(o2).priority)
				return -1;
			if(getThreadState(o1).priority>getThreadState(o2).priority)
				return 1;
			return 0;
		}
		}
	}

	/**
	 * The scheduling state of a thread. This should include the thread's
	 * priority, its effective priority, any objects it owns, and the queue
	 * it's waiting for, if any.
	 *
	 * @see	nachos.threads.KThread#schedulingState
	 */
	protected class ThreadState {
		private PriorityQueue wait;
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 *
		 * @param	thread	the thread this state belongs to.
		 */
		public ThreadState(KThread thread) {
			this.thread = thread;

			setPriority(priorityDefault);
		}

		/**
		 * Return the priority of the associated thread.
		 *
		 * @return	the priority of the associated thread.
		 */
		public int getPriority() {
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 *
		 * @return	the effective priority of the associated thread.
		 */
		public int getEffectivePriority() {
			// implement me
			return effective;
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 *
		 * @param	priority	the new priority.
		 */
		public void setPriority(int priority) {
			if (this.priority == priority)
				return;

			this.priority = priority;

			// implement me
		}

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the
		 * resource guarded by <tt>waitQueue</tt>. This method is only called
		 * if the associated thread cannot immediately obtain access.
		 *
		 * @param	waitQueue	the queue that the associated thread is
		 *				now waiting on.
		 *
		 * @see	nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(PriorityQueue waitQueue) {
			// implement me
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(this.thread).timeINqueue = Machine.timer().getTime();	//store the time since nachos has started into the thread. This will keep track of how long it has been in the queue.
			//waitPQueue.add(this.thread);
			ready = waitQueue;
			waitQueue.waitPQueue.add(thread);
			//waitPQueue.add(this);
			if(waitQueue.transferPriority){}//if this is true we have to transfer priority
		}

		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 *
		 * @see	nachos.threads.ThreadQueue#acquire
		 * @see	nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(PriorityQueue waitQueue) {
			Lib.assertTrue(Machine.interrupt().disabled());
			assert(waitQueue!=null);
			//waitPQueue.equals(this);
			if(waitQueue.waitPQueue.equals(thread)){//check if the thread is remove, if it is not, remove it
				waitQueue.waitPQueue.remove(thread);
			}
			//The thread now has a lock
			waitQueue.LockHolder = this;
			//Lib.assertTrue(waitPQueue.isEmpty());
		}	
		/** The thread with which this object is associated. */	
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;
		/** The effective priority of the associated thread. */
		protected int effective;
		/** The time the thread was in Queue;  */
		protected long timeINqueue;
	}
	/** The queue where threads are waiting on  */
	private PriorityQueue ready;
	//private Queue<KThread> waitPQueue = new PriorityQueue<KThread>(1, new PriorityComparator());

	private static final char dbgThread = 't';
	public static void testMe(KThread testThread1, KThread testThread2, int priority1, int priority2){

		boolean int_state = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(testThread1, priority1);
		ThreadedKernel.scheduler.setPriority(testThread2, priority2);
		Machine.interrupt().restore(int_state);

		testThread1.setName("test1").fork();
		testThread2.setName("test2").fork();
		testThread1.join();
		testThread2.join();
	}

	/**
	 * Test if this module is working.
	 */

	public static void selfTest(){
		Lib.debug(dbgThread, "Enter PriorityQueue.selfTest");
		//Created two threads with its runnable being printing things.
		KThread t1 = new KThread(new Runnable() {
			public void run() {
				System.out.println(KThread.currentThread().getName()+" has started");
				for(int i = 0; i<5; i++){
					System.out.println(KThread.currentThread().getName()+" said: IM RUNNING!");
					KThread.yield();
				}//when exited it is finished
				System.out.println(KThread.currentThread().getName()+"said: hi im finished ");
			}

		});
		KThread  t2 = new KThread(new Runnable() {
			public void run() {
				System.out.println(KThread.currentThread().getName()+" has started");
				for(int i = 0; i<5; i++){
					System.out.println(KThread.currentThread().getName()+" said: IM RUNNING!");
					KThread.yield();
				}//when exited it is finished
				System.out.println(KThread.currentThread().getName()+"said: hi im finished ");
			}

		});
		testMe(t1,t2,3,3);

	}

}
