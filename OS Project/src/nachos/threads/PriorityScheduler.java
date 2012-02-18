package nachos.threads;

import nachos.machine.*;
import nachos.threads.PriorityScheduler.Donation;
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
		}
		/**
		 * The specified thread has received exclusive access, without using
		 * <tt>waitForAccess()</tt> or <tt>nextThread()</tt>. Assert that no
		 * threads are waiting for access.
		 */
		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).acquire(this);

		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			PriorityQueue buffer = null;
			KThread returnThread = null;
			if(this.transferPriority&&this.resourceOwner!=null){			//Removes donation of the once running thread
				for(Donation k: listDonate){
					if(k.donateFrom.thread==this.resourceOwner.thread){
						k.removeDonation();
					} 
				}
			}
			ThreadState peek = pickNextThread();					//peek at the nextThread and return a thread with a highest priority and longest wait time
			if(peek!=waitPQueue.peek()){							//if not the same, there is a thread that has been waiting longer
				buffer = new PriorityQueue(this.transferPriority);	//create a buffer to hold reorganize the queue
				while(peek!=waitPQueue.peek()){						//pop out the current queue and store it in the buffer until we see the one that has been waiting the longest
					buffer.waitPQueue.offer(waitPQueue.poll());
				}
				returnThread = waitPQueue.poll().thread;			//save the thread that we need
				while(buffer.waitPQueue.peek()!=null){				//store back the elements in the buffer back to the orginal queue
					waitPQueue.offer(buffer.waitPQueue.poll());
				}
			}
			else{													//nothing special happen so, just remove the highest priority
				ThreadState re = waitPQueue.poll();
				if(re==null)
					return null;
				else
					returnThread = re.thread;

			}
			getThreadState(returnThread).timeINqueue = Machine.timer().getTime();		//time in queue has been reseted	
			//this.resourceOwner = getThreadState(returnThread);
			return returnThread;
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 *
		 * @return	the next thread that <tt>nextThread()</tt> would
		 *		return.
		 */
		protected ThreadState pickNextThread() {
			ThreadState hold = waitPQueue.peek();								//original peek
			for(ThreadState k:waitPQueue){										//for each element in the queue, check if there is a same priority
				if((hold.priority==k.priority)						
						&&((Machine.timer().getTime()-hold.timeINqueue) 
								>(Machine.timer().getTime() - k.timeINqueue))){ //If there is one, compare the time in queue
					hold = k;													//the longest time in queue have higher priority
				}	
			}
			return hold;
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
		protected ThreadState resourceOwner; 
		private Queue<ThreadState> waitPQueue = new java.util.PriorityQueue<ThreadState>(1, new PriorityComparator());
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

	/**
	 * The scheduling state of a thread. This should include the thread's
	 * priority, its effective priority, any objects it owns, and the queue
	 * it's waiting for, if any.
	 *
	 * @see	nachos.threads.KThread#schedulingState
	 */
	protected class ThreadState {
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
			if(!listDonate.isEmpty()){
				int found = 0;
				for(int i = 0; i< listDonate.size();i++){
					if(this == listDonate.get(i).threadInQuestion){
						found = i;
					}
				}
				return listDonate.get(found).effective;
			}
			else
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

			this.priority = priority;
			this.effective = priority;
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
		public void addToQueue(){
			addToQueue(this.waitingResource);
		}

		public void addToQueue(PriorityQueue waitQueue){	//using the current queue, add thread
			if(waitQueue==null) return;
			if(this.needReorded){
				reSort(waitQueue, this);
			}
			if(waitQueue.resourceOwner!=null)
				if(waitQueue.resourceOwner.needReorded)
					reSort(waitQueue, waitQueue.resourceOwner);


				else
					waitQueue.waitPQueue.add(this);
		}
		public void reSort(PriorityQueue waitQueue,ThreadState sort){
			waitQueue.waitPQueue.remove(sort);
			waitQueue.waitPQueue.add(sort);
			this.needReorded = false;
		}
		public void waitForAccess(PriorityQueue waitQueue) {
			Lib.assertTrue(Machine.interrupt().disabled());
			if(waitQueue==null)													//in the case that it is null, do nothing
				return;
			this.timeINqueue = Machine.timer().getTime();						//this will keep track on how long it has been in the queue.
			if(waitQueue.transferPriority&&waitQueue.resourceOwner!=null&&waitQueue.resourceOwner!=this){		//if this is true we have to transfer priority and there is a lock in play

				//for(ThreadState k : waitQueue.waitPQueue)
				//if(this.effective<k.effective)								//look at queue of there is a lower priority on the list
				compute_donation(waitQueue,this);						//if there is one, priority inversion might be in play, so donate!
			}
			this.waitingResource = waitQueue;
			waitQueue.waitPQueue.offer(this);
			//addToQueue(waitQueue);									//add this to queue
		}

		public void compute_donation(PriorityQueue waitQueue, ThreadState threadDonor){
			if(!listDonate.contains(threadDonor) && waitingResource!=null){							//checks if there is a same Donor on the list		
				if(threadDonor.thread.joinThread!=null){											//case where join is called
					if(getThreadState(threadDonor.thread.joinThread) != threadDonor){
						Donation donor = new Donation(waitQueue, threadDonor, waitQueue.resourceOwner);
						listDonate.add(donor);
					}
				}
				//Donation holder =listDonate.get(listDonate.indexOf(donor));
				//holder.setDonation();
			}
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
			if(waitQueue.waitPQueue.equals(waitQueue))					//check if the thread is removed, if not remove it
				waitQueue.waitPQueue.remove(waitQueue);				

			waitQueue.resourceOwner = this;								//This thread is now the owner of the resource 
			//if(waitQueue==waitingResource)							//if the current queue is waiting on a resource
			//	waitingResource=null;									//Stop
		}	
		/** The thread with which this object is associated. */	
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;
		/** The effective priority of the associated thread. */
		protected int effective;
		/** The time the thread was in Queue;  */
		protected long timeINqueue;
		public PriorityQueue waitingResource;
		boolean needReorded = false;
	}
	/** The queue where threads are waiting on  */

	public LinkedList<Donation> listDonate = new LinkedList<Donation>();
	//private Queue<KThread> waitPQueue = new PriorityQueue<KThread>(1, new PriorityComparator());

	private static final char dbgThread = 't';
	public static void testMe(LinkedList<KThread> list,int priority1, int priority2,int priority3){

		boolean int_state = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(list.get(0), priority1);
		ThreadedKernel.scheduler.setPriority(list.get(1), priority2);
		if(priority3!=0)
			ThreadedKernel.scheduler.setPriority(list.get(2), priority3);	
		Machine.interrupt().restore(int_state);

		list.get(0).setName("Thread A").fork();
		list.get(1).setName("Thread B").fork();
		if(priority3 != 0)
			list.get(2).setName("Thread C").fork();
		list.get(0).join();
		list.get(1).join();
		if(priority3 != 0)
			list.get(2).join();
	}

	/**
	 * Test if this module is working.
	 */
	public static LinkedList<KThread> createThread(int number, int special){
		final Lock lock = new Lock();
		LinkedList<KThread> list = new LinkedList<KThread>();
		/*for(int i = 0; i<number;i++){				//creates a specific number of test thread
			list.add(new KThread(new Runnable() {
				public void run() {
					System.out.println(KThread.currentThread().getName()+" IS NOT SUPPOSE TO RUN YET UNTIL A IS DONE");
					for(int i = 0; i<5; i++){
						System.out.println(KThread.currentThread().getName()+" said: IM RUNNING!");
						KThread.yield();
					}//when exited it is finished
					System.out.println(KThread.currentThread().getName()+" said: I AM DONE. B RESUMES HERE ");
				}

			}));
		}*/
		if(special!=0){
			list.add(new KThread(new Runnable() {
				public void run() {
					System.out.println(KThread.currentThread().getName() + " HAS to run after C! C has to finish before A RUNS");
					lock.acquire();
					System.out.println( KThread.currentThread().getName() + " IS RUNNING" );
					lock.release();
				}
			}));
			list.add(new KThread(new Runnable() {
				public void run() {
					System.out.println(KThread.currentThread().getName()+" IS NOT SUPPOSE TO RUN YET UNTIL A IS DONE");
					for(int i = 0; i<5; i++){
						System.out.println(KThread.currentThread().getName()+" said: IM RUNNING!");
						KThread.yield();
					}//when exited it is finished
					System.out.println(KThread.currentThread().getName()+" said: I AM DONE.");
				}

			}));
			list.add(new KThread(new Runnable() {
				public void run() {
					//System.out.println(KThread.currentThread().getName() + " has started with priority: "+PriorityScheduler.ThreadStateKThread.currentThread());
					lock.acquire();
					boolean int_state = Machine.interrupt().disable();
					ThreadedKernel.scheduler.setPriority( 2 );
					Machine.interrupt().restore( int_state );
					System.out.println(KThread.currentThread().getName() + " now has priority 2...Priority Inversion chaos has started");
					KThread.yield();
					// t1.acquire() will now have to realise that t3 owns the lock it wants to obtain
					// so program execution will continue here.
					System.out.println( KThread.currentThread().getName() + " active ('a' wants its lock back so we are here)" );
					lock.release();
					KThread.yield();
					lock.acquire();
					System.out.println( KThread.currentThread().getName() + " active-again (should be after 'a' and 'b' done)" );
					lock.release();
				}
			}));


		}

		return list;
	}
	public static void selfTest(){
		Lib.debug(dbgThread, "Enter PriorityQueue.selfTest");
		//Created two threads with its runnable being printing things.
		LinkedList<KThread> threadList;
		//threadList = createThread(2,0); 	//creates two regular thread and run it
		//testMe(threadList,2,7,0);
		threadList= createThread(1,1);						//creates three thread-1 regular and 2 special
		testMe(threadList,6,4,7);
	}
	public class Donation{
		public int effective;
		public ThreadState donateFrom;
		public int orginalPriority;
		public KThread test;
		public ThreadState threadInQuestion;
		PriorityQueue queueHolder;
		public Donation(PriorityQueue waitQueue, ThreadState donor, ThreadState donee){
			//this.queueHolder = waitQueue;
			this.orginalPriority = donee.priority;	//just in case keep the orginal
			this.threadInQuestion = donee;			//keep track of who is the donee
			this.donateFrom = donor;				//keep track of who is the donor
			this.effective = donor.effective;		//set the priority of the donee 
			this.test = donee.thread;
			getThreadState(test).effective = donor.effective;	
			//priority has been change force resort

			//waitQueue.resourceOwner.waitingResource.waitPQueue.remove(getThreadState(test));
			//waitQueue.resourceOwner.waitingResource.waitPQueue.add(getThreadState(test));
			//waitQueue.waitPQueue.remove(getThreadState(test));
			//waitQueue.waitPQueue.add(getThreadState(test));

		}
		public void setDonation(){
			this.orginalPriority = threadInQuestion.priority;	//save the original for whatever reason
			if(threadInQuestion.effective <= donateFrom.effective)//only change if the donor has a higher priority than the donee
				threadInQuestion.effective = donateFrom.effective;	//set the donation
		}
		public void removeDonation(){
			threadInQuestion.effective = orginalPriority;	//set the priority to its original
		}	
	}
}
