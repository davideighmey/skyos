package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
	/**
	 * Allocate a new lottery scheduler.
	 */
	public LotteryScheduler() {
	}

	/**
	 * Allocate a new lottery thread queue.
	 *
	 * @param	transferPriority	<tt>true</tt> if this queue should
	 *					transfer tickets from waiting threads
	 *					to the owning thread.
	 * @return	a new lottery thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority){
		return new LotteryQueue(transferPriority);   
	}

	public static final int priorityMaximum = Integer.MAX_VALUE;
	public static final int priorityMinimun = 1;
	
	protected class LotteryQueue extends ThreadQueue {

		LotteryQueue(boolean transferPriority){
			this.transferPriority = transferPriority;
		}

		public KThread nextThread(){
			//Picks the next thread here
			//Generate a random number
			//Pick the winner and return the thread   
			return null;
		}

		@Override
		public void waitForAccess(KThread thread) {
			// TODO Auto-generated method stub

		}

		@Override
		public void acquire(KThread thread) {
			// TODO Auto-generated method stub

		}

		@Override
		public void print() {
			// TODO Auto-generated method stub
		}
		public boolean transferPriority;
	}
	protected class ThreadState {
		public ThreadState(KThread thread){
			this.thread = thread;
			setPriority(priorityMinimun);
		}

		public void waitForAccess(LotteryQueue waitQueue){
			//add the thread the queue
		}

		public void compute_donation(LotteryQueue waitQueue){
			//gives tickets to the queue owner
		}

		public void acquire(LotteryQueue waitQueue){
			//set the resource owner to the current thread
		}

		protected KThread thread;
		protected int priority;
		protected int ticketCount;   

	}
}
