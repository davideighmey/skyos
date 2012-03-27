package nachos.threads;

import nachos.machine.*;

import java.util.LinkedList;
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
public class LotteryScheduler extends Scheduler {
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
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new LotteryScheduler.LotteryQueue(transferPriority);
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

		Lib.assertTrue(priority >= priorityMinimum
				&& priority <= priorityMaximum);

		getThreadState(thread).setPriority(priority);
	}

	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			return false;

		setPriority(thread, priority + 1);

		Machine.interrupt().restore(intStatus);
		return true;
	}

	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			return false;

		setPriority(thread, priority - 1);

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
	public static final int priorityMinimum = 1;
	/**
	 * The maximum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMaximum = Integer.MAX_VALUE;

	/**
	 * Return the scheduling state of the specified thread.
	 * 
	 * @param thread
	 *            the thread whose scheduling state to return.
	 * @return the scheduling state of the specified thread.
	 */
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}

	/**
	 * A <tt>ThreadQueue</tt> that sorts threads by priority.
	 */
	protected class LotteryQueue extends ThreadQueue {
		LotteryQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
			resourceOwner = null;
			waitPQueue = new LinkedList<LotteryScheduler.ThreadState>();
		}

		 public void waitForAccess(KThread thread) {
	            Lib.assertTrue(Machine.interrupt().disabled());
	            getThreadState(thread).waitForAccess(this);
	        }

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread	).acquire(this);
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			ThreadState next = this.pickNextThread();
			if (next != null) {
				next.acquire(this);
				return next.thread;
			}
			return null;
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 * 
		 * @return the next thread that <tt>nextThread()</tt> would return.
		 */
		protected ThreadState pickNextThread() {
			if(waitPQueue.size()<1 )//if there is only 1 thread, we will return it
				return null;
			if(waitPQueue.size()==1 )//if there is only 1 thread, we will return it
				return waitPQueue.getFirst();
			long totalTickets, winningTicket, ticketsSoFar;
			totalTickets = 0; //total tickets is the number of tickets entered into the lottery ( sum of all threads effective priorities)
			for (ThreadState thread : waitPQueue) {
				totalTickets += thread.getEffectivePriority();
				if( totalTickets >= priorityMaximum)//no need to go bigger than  the limit
					totalTickets = priorityMaximum; 
			}
			java.util.Random lotto = new java.util.Random();
			winningTicket=0;//winningTickety will be the number of the ticket that won the lotto
			winningTicket = lotto.nextInt((int) totalTickets) + 1; //  random number from 1 to totalTickets
			ticketsSoFar = 0; //the counter that will be used to find the winner. 
			if (winningTicket==0)
				return waitPQueue.getFirst();
			ThreadState rval = null; //the thread to be returned
			for (ThreadState thread : waitPQueue) {
				ticketsSoFar+=thread.getEffectivePriority();
				if(winningTicket <= ticketsSoFar){// if this thread holds the ticket range that includes winning ticket, it wins. 
					rval = thread; 
					break; 
				}
			}
		

			
			return rval; 

		}

		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			Iterator<ThreadState> nextIt = waitPQueue.iterator();
			System.out.println("Queue SnapShot: ");
			while(nextIt.hasNext()){
				ThreadState next = (ThreadState) nextIt.next();
				System.out.print(" "+next.thread.getName() +" with effective priority of " + next.getEffectivePriority()+ ",");
			}
			System.out.println(". end");

		}
		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		public boolean transferPriority;
		// The priority queue that will hold the waiting threads
		LinkedList<LotteryScheduler.ThreadState> waitPQueue;
		public ThreadState resourceOwner; 
		//The amount of tickets in the system
		//private int totalTickets = 0;
	}


	/**
	 * The scheduling state of a thread. This should include the thread's
	 * priority, its effective priority, any objects it owns, and the queue it's
	 * waiting for, if any.
	 * 
	 * @see nachos.threads.KThread#schedulingState
	 */
	protected class ThreadState {
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 * 
		 * @param thread
		 *            the thread this state belongs to.
		 */
		public ThreadState(KThread thread) {
			this.thread = thread;
			setPriority(priorityDefault);
		}

		/**
		 * Return the priority of the associated thread.
		 * 
		 * @return the priority of the associated thread.
		 */
		public int getPriority() {
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 * 
		 * @return the effective priority of the associated thread.
		 */
		public int getEffectivePriority() {
			if (donationAllowed) {

				effective = calculateDonated();
				donationAllowed = false;
			}
			try{
				return effective + priority; 
			}
			catch(Exception e){
				return priorityMaximum; 
			}

		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 * 
		 * @param priority
		 *            the new priority.
		 */
		public void setPriority(int priority) {
			if (this.priority == priority)
				return;

			this.priority = priority;
			this.effective = priority;
		}

		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the resource
		 * guarded by <tt>waitQueue</tt>. This method is only called if the
		 * associated thread cannot immediately obtain access.
		 * 
		 * @param waitQueue
		 *            the queue that the associated thread is now waiting on.
		 * 
		 * @see nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(LotteryQueue waitQueue) {
			/************************************************************
			 * A thread that goes through here have acquire the rights  *
			 * for the resource that this queue is waiting for.         *
			 ***********************************************************/
			//Store the current tick to keep track how long it has been in queue
			this.timeINqueue = Machine.timer().getTime();   
			//add any threads that goes through here to the queue of the wanted resource
			waitQueue.waitPQueue.add(this);
			//Donation is allowed for this thread only if transferPriority is true
			if (waitQueue.transferPriority) {
				donationAllowed = true;
			}

		}

		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 * 
		 * @see nachos.threads.ThreadQueue#acquire
		 * @see nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(LotteryQueue waitQueue) {
			/************************************************************
			 * A thread that goes through here have acquire the rights  *
			 * for the resource that this queue is waiting for.         *
			 ***********************************************************/
			//In case if this waitQueue did not remove the thread properly
			if(waitQueue.waitPQueue.contains(this))         
				waitQueue.waitPQueue.remove(this);  
			//This thread is now the current resource owner
			waitQueue.resourceOwner = this;
			//add the current waitQueue to list
			resourceQueue.add(waitQueue);
			//Donation is allowed for this thread only if transferPriority is true
			if (waitQueue.transferPriority) 
				donationAllowed = true;

		}

		public int calculateDonated() {
			// get the max donated priorities from all possible donations ( all
			// queues this thread is waiting for)
			if (priority == priorityMaximum)
				return priority;
			long ticket = 0;
			for (LotteryScheduler.LotteryQueue pq : resourceQueue) {
				long maxPriority = 0;
				for (ThreadState thread : pq.waitPQueue) {
					if (thread != this) {
							maxPriority += thread.getEffectivePriority();
						if (maxPriority >= priorityMaximum)
							return priorityMaximum;
					}
				}
				ticket += maxPriority;
				if(ticket>=Integer.MAX_VALUE)
					return priorityMaximum; 
			}
			return (int)ticket;
		}

		/** The thread with which this object is associated. */    
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;// = priorityDefault;
		/** The effective priority of the associated thread */
		protected int effective;// = priorityDefault;
		/** Boolean Check if donation is allowed for the current thread**/
		protected boolean donationAllowed = true;
		/** The time in queue of the associated thread  */
		protected long timeINqueue = 0;
		protected LinkedList<LotteryScheduler.LotteryQueue> resourceQueue = new LinkedList<LotteryScheduler.LotteryQueue>();
		
	}



}