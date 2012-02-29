package nachos.threads;

import nachos.machine.*;
import java.util.*;


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
	//protected ThreadState getThreadState(KThread thread) {
		//if (thread.schedulingState == null)
			//thread.schedulingState = new ThreadState(thread);
//
	//	return (ThreadState)thread.schedulingState;
	//}
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
		protected ThreadState pickNextThread() {
			ThreadState hold = waitPQueue.peek();								//original peek
			for(ThreadState k:waitPQueue){										//for each element in the queue, check to make sure it has the highest priority 
				if((hold.effective<k.effective))								//if the current hold has a lower effective than the checked effective set hold to the checked						
					hold = k;
			}
			return hold;
		}
		@Override
		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			//getThreadState(thread).waitForAccess(this);

		}

		@Override
		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			//getThreadState(thread).acquire(this);
		}
		

		@Override
		public void print() {
			// TODO Auto-generated method stub
		}
		
		public boolean transferPriority;
		protected int ticketCount; 
		protected ThreadState resourceOwner; 	
		public Queue<ThreadState> waitPQueue = new java.util.PriorityQueue<ThreadState>(1, new PriorityComparator());
		
		public class PriorityComparator implements Comparator<ThreadState>
		{	@Override
			//Allow automatic sorting of the Queue
			public int compare(ThreadState o1, ThreadState o2) {
			if(o1.getEffectivePriority()>o2.getEffectivePriority())						
				return -1;
			if(o1.getEffectivePriority()<o2.getEffectivePriority())
				return 1;
			return 0;
		}
		
	}
		
	}
	protected class ThreadState {
		public ThreadState(KThread thread){
			this.thread = thread;
			setPriority(priorityMinimun);
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

			return this.effective;
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 *
		 * @param	priority	the new priority.
		 */
		public void setPriority(int priority) {
			if (this.priority == priority)
				return;
			this.effective = priority;
			this.priority = priority;
			//System.out.println(this.thread.getName() + " has its priority set to " + this.effective);
			updatePriorities(this);
			// implement me
		}
		
		public void waitForAccess(LotteryQueue waitQueue){
			waitQueue.waitPQueue.offer(this);
			this.waitingForResource = waitQueue;
			if(waitQueue.transferPriority){								//if this is true we have to transfer priority and there is a lock in play
				compute_donation(waitQueue,this);						//if there is one, priority inversion might be in play, so donate!
			}	
		}

		public void compute_donation(LotteryQueue waitQueue, ThreadState threadDonor){
			int i = 0;
			LinkedList<LotteryQueue> seenQueueState = new LinkedList<LotteryQueue>();
			while(i<queueList.size()){
				if(waitQueue==null||threadDonor == null) return;
				while(!seenQueueState.contains(waitQueue)){								//checks if there is a same Donor on the list					
					seenQueueState.add(waitQueue);						
					if(threadDonor == null||threadDonor.thread==null||waitQueue.resourceOwner==null/*||waitQueue.resourceOwner.thread.getName()=="main" */) break;
					if(waitQueue.resourceOwner != threadDonor && waitQueue.waitPQueue.contains(threadDonor) ){							//Don't want it to donate to itself and do not donate to the resource owner you are not part of
						if(threadDonor.effective > waitQueue.resourceOwner.effective){	//only donate if the resource owner has a lower priority
							waitQueue.resourceOwner.donatedFrom = threadDonor;
							waitQueue.resourceOwner.effective = threadDonor.effective;	
							//System.out.println(threadDonor.thread.getName() + " with effective " + threadDonor.effective +" has donated to " + waitQueue.resourceOwner.thread.getName());
							//System.out.println(waitQueue.resourceOwner.thread.getName() + " got donated and has its effective change to " + waitQueue.resourceOwner.effective);
							updatePriorities(waitQueue.resourceOwner);		//Since priorities has change, update everywhere else
						}
					}
				}
				waitQueue = queueList.get(i);
				i++;
			}
		}

		public void acquire(LotteryQueue waitQueue){
			if(waitQueue.waitPQueue.equals(this))						//check if the thread is removed, if not remove it
				waitQueue.waitPQueue.remove(this);	
			if(this.waitingForResource == waitQueue)					//if this thread is waiting for the current resource, remove it
				this.waitingForResource = null;
			waitQueue.resourceOwner = this;								//notify this queue that a thread has received access
			queueList.add(waitQueue);
		}
		LinkedList<LotteryQueue> queueList = new LinkedList<LotteryQueue>();

		public void updatePriorities(ThreadState threadInQuestion){
			for(LotteryQueue queue: queueList){
				if (queue.resourceOwner == null)								//if there is no owner of the queue...assume that queue is dead
					queueList.remove(queue);	
				if(queue.resourceOwner.thread==threadInQuestion.thread)						//if the queue resource owner matches up, set it, no need to search
					queue.resourceOwner.effective = threadInQuestion.effective;
				else{
					for(ThreadState k:queue.waitPQueue){
						if(k.thread == threadInQuestion.thread){
							k.effective = threadInQuestion.effective;
							break;
						}
					}
				}
			}
		}
		/** The thread with which this object is associated. */	   
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;
		/** The effective priority of the associated thread */
		protected int effective = 0;
		/** The number of tickets of the associated thread */
		protected ThreadState donatedFrom = null;
		/** The resource the associated thread is waiting on */
		protected LotteryQueue waitingForResource = null;

	}
}
