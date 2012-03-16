package nachos.threads;

import nachos.machine.*;

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

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).acquire(this);
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			PriorityQueue buffer = null;
			KThread returnThread = null;
			ThreadState returnThreadState = null;
			if(waitPQueue!=null)	
				reOrdered();													//Re-order the queue according to the effective
			if(this.transferPriority&&this.resourceOwner!=null){			//Removes donation of the once running thread
				this.resourceOwner.effective = this.resourceOwner.priority;
				//System.out.println(this.resourceOwner.thread.getName() + " has its priority change to " + this.resourceOwner.effective);
				//updatePriorities(this.resourceOwner);
			}
			ThreadState peek = pickNextThread();					//peek at the nextThread and return a thread with a highest priority and longest wait time
			if(peek!=waitPQueue.peek()){							//if not the same, there is a thread that has been waiting longer
				buffer = new PriorityQueue(this.transferPriority);	//create a buffer to hold reorganize the queue
				while(peek!=waitPQueue.peek()){						//pop out the current queue and store it in the buffer until we see the one that has been waiting the longest
					buffer.waitPQueue.offer(waitPQueue.poll());
				}
				returnThreadState = waitPQueue.poll();
				returnThread = returnThreadState.thread;			//save the thread that we need
				while(buffer.waitPQueue.peek()!=null){				//store back the elements in the buffer back to the orginal queue
					waitPQueue.offer(buffer.waitPQueue.poll());
				}
			}
			else{													//nothing special happen so, just remove the highest priority
				returnThreadState = waitPQueue.poll();
				if(returnThreadState==null)
					return null;
				else
					returnThread = returnThreadState.thread;

			}
			returnThreadState.timeINqueue = Machine.timer().getTime();		//time in queue has been reseted	
			this.resourceOwner = returnThreadState;
			return returnThread;
		}
		/**********************************************************************
		 * Due to java implementation of the priority queue, changing the 	  *	
		 * priority of a thread while it is in queue will not be sorted again *
		 * even though the priority has changed.   							  *
		 *  																  *
		 *  Have to force the queue to reorder itself by removing it from the *
		 *  queue and putting it back it, which will aligned it self properly.*
		 *********************************************************************/
		
		public void reOrdered(){
			PriorityQueue buffer = new PriorityQueue(this.transferPriority);
			ThreadState hold = null;								
			while(!this.waitPQueue.isEmpty()){		
				hold =this.waitPQueue.poll();
				buffer.waitPQueue.offer(hold);	
			}
			while(!buffer.waitPQueue.isEmpty()){										 
				hold = buffer.waitPQueue.poll();
				this.waitPQueue.offer(hold);	
			}
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
			for(ThreadState k:waitPQueue){										//for each element in the queue, check to make sure it has the highest priority 
				if((hold.effective<k.effective))								//if the current hold has a lower effective than the checked effective set hold to the checked						
					hold = k;
			}
			for(ThreadState k:waitPQueue){	
				if((hold.effective==k.effective)&&
						(Machine.timer().getTime()-hold.timeINqueue)
						<(Machine.timer().getTime() - k.timeINqueue)) //check if there is a longer waiting thread
					hold = k;
			}
			for(ThreadState k:waitPQueue){				//aging implemented here
				if(Machine.timer().getTime() - k.timeINqueue>=500)		//if a thread has been waiting for more than 200 ticks increase priority
					if(k.effective!=7){
						//k.effective++;
						//System.out.println(k.thread.getName()+" has its priority increased due to aging! It is now: "+ k.effective);
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
		//Threads that holds a resource will be called resourceOwner and is also the owner of the ThreadQueue
		protected ThreadState resourceOwner; 					
		//This is the queue
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
			this.effective = priority;
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
			/************************************************************
			 * A thread that goes through here have been denied access  *
			 * the resource that this queue is waiting for.             *
			 ***********************************************************/
			//Store the current tick to keep track how long it has been in queue
			this.timeINqueue = Machine.timer().getTime();	
			//Add this thread to the queue
			waitQueue.waitPQueue.offer(this);
			//It is now waiting for the resouce that is owned by the queue owner
			this.waitingForResource = waitQueue;

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
			/************************************************************
			 * A thread that goes through here have acquire the rights  *
			 * for the resource that this queue is waiting for.         *
			 ***********************************************************/
			//In case if this waitQueue did not remove the thread properly
			if(waitQueue.waitPQueue.contains(this))		
				waitQueue.waitPQueue.remove(this);	
			//This thread is not waiting for any resource as it will be the owner
			waitingForResource = null;
			//Set the owner of this queue/resource to this thread
			waitQueue.resourceOwner = this;					
		}	
		/** The thread with which this object is associated. */	   
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;
		/** The effective priority of the associated thread */
		protected int effective = 0;
		/** The time in queue of the associated thread  */
		protected long timeINqueue = 0;
		/** The resource the associated thread is waiting on */
		//protected LinkedList<PriorityQueue> waitingForResource = new LinkedList<PriorityQueue>();
		protected PriorityQueue waitingForResource = null;
	}
	public static void testMe(LinkedList<KThread> list, LinkedList<KThread> special,int priority1, int priority2,int priority3){

		/***************************************************************************************************************************/
		/*
		 * Test case for priority scheduling. Depending on the number of threads created, we will set each thread priority randomly and
		 * allow them to run. The print out should be organize by their priority number with highest being first and the lowest being
		 * last.
		 */
		/***************************************************************************************************************************/
		if(list!=null&&special==null){
			//Random r = new Random();
			int priority = 0;
			KThread thread = null;
			boolean int_state = Machine.interrupt().disable();
			//Setting Priorities
			for(int i =0; i< list.size();i++){
				//priority = r.nextInt(4)+2;	//priority will be between 2 and 6
				priority = i +2;
				thread = list.get(i);
				thread.setName("Thread "+ (char)(i+72));
				ThreadedKernel.scheduler.setPriority(thread, priority);
				System.out.println(thread.getName()+" has been created with a priority of " + priority + ".");
			}
			Machine.interrupt().restore(int_state);
			for(int i =0; i< list.size();i++){
				thread = list.get(i);
				thread.fork();

			}
			for(int i =0; i< list.size();i++){
				thread = list.get(i);
				thread.join();
			}
		}

		/***************************************************************************************************************************/
		/*
		 * Test case for donation. There are three thread being created: A,B,C with priorities 6,4,7 in their respective order. 
		 * C starts running with a lock since it has a priority of 7. However since we are testing priority inversion, it will
		 * set its own priority to 2 so other threads that does not need to use Thread C resources can easily interrupt it. 
		 * Since Thread A does need to use a resource that Thread C is using and also the current highest priority, it does not 
		 * want to wait any longer than it should and proceeds to donate his priority to Thread C. This should allow Thread C 
		 * to continue running and eventually give up its lock on the resource. Thread A shall proceed normally and Thread B 
		 * following afterward.  
		 */
		/***************************************************************************************************************************/
		if(special!=null&&special.size()==3){
			boolean int_state2 = Machine.interrupt().disable();
			ThreadedKernel.scheduler.setPriority(special.get(0), priority1);
			ThreadedKernel.scheduler.setPriority(special.get(1), priority2);
			ThreadedKernel.scheduler.setPriority(special.get(2), priority3);
			Machine.interrupt().restore(int_state2);
			KThread spe1 = special.get(0);
			KThread spe2 = special.get(1);
			KThread spe3 = special.get(2);
			spe1.setName("Thread A").fork();
			spe2.setName("Thread B").fork();
			spe3.setName("Thread C").fork();
			spe1.join();
			spe2.join();
			spe3.join();
		}
		/***************************************************************************************************************************/
		/* Special Test Plus Case: Priority Inversion Confusion EXTREME!!
		 * Second test case for donation. There are five threads being created: A,B,C,D,E with priorities 7,4,7,6,5 in their 
		 * respective order. C starts running with a lock since it has a priority of 7. However since we are testing priority 
		 * inversion, it will set its own priority to 2 so other threads that does not need to use Thread C resources can easily interrupt it. 
		 * (Thread D will be the only thread to attempt to have the lock) 
		 * Thread A will take ownership of the queue and will execute, interrupting Thread C. After Thread A is finish, Thread D
		 * will try to attempt to execute but Thread C still have the lock. If Donation works, it will donate to Thread C and prevent Thread E 
		 * or Thread B from taking the CPU. After Thread C releases the lock, Thread D will run and to finish it off, Thread E and B will finish
		 * afterwards.
		 */
		/***************************************************************************************************************************/
		if(special!=null&&special.size()==5){
			boolean int_state2 = Machine.interrupt().disable();
			ThreadedKernel.scheduler.setPriority(special.get(0), priority1);
			ThreadedKernel.scheduler.setPriority(special.get(1), priority2);
			ThreadedKernel.scheduler.setPriority(special.get(2), priority3);
			ThreadedKernel.scheduler.setPriority(special.get(3), 6);
			ThreadedKernel.scheduler.setPriority(special.get(4), 5);
			Machine.interrupt().restore(int_state2);
			KThread spe1 = special.get(0);
			KThread spe2 = special.get(1);
			KThread spe3 = special.get(2);
			KThread spe4 = special.get(3);
			KThread spe5 = special.get(4);
			spe1.setName("Thread A").fork();
			spe2.setName("Thread B").fork();
			spe3.setName("Thread C").fork();
			spe4.setName("Thread D").fork();
			spe5.setName("Thread E").fork();
			spe1.join();
			spe2.join();
			spe3.join();
			spe4.join();
			spe5.join();
		}
		/***************************************************************************************************************************/
		/* Special Test Case 2: Double Lock Priority Inversion Confusion! --It can be confusing but has to be done. I need to find ways to break my code
		 * Third test case for donation. There are twelve threads being created: A,B,C,D,E,F,G,H,I,J,K,L with priorities 4,3,7,2,4,7,5,2,3,4,5,6 
		 * in their respective order. Thread C and Thread F starts running with a lock since it has a priority of 7. However since we are testing priority 
		 * inversion, it will set its own priority to 2(Thread F will set it self to 2 as well) so other threads that does not need to use Thread C  or F resources can 
		 * be easily interrupted. 
		 * 
		 * (Thread A(4) && Thread D(6)  will need Lock1 which Thread C(2) starts off with and Thread G(5) will need Lock2 which Thread F(1) starts with) 
		 * 
		 * Theoretical Procedure:
		 * 
		 * Thread L(6) will take ownership of the queue and will execute, interrupting Thread C(2). After Thread L(6) is finish, Thread G(5)
		 * will try to attempt to execute but Thread F(2) still have the lock. If Donation works, it will donate to Thread F and prevent any 
		 * thread(except Thread K(5)) from taking the CPU. Since Thread K has been waiting, it will run instead of Thread F. After Thread K
		 * is finished, Thread F should execute and release the lock. Thread G shall follow. Thread A(4) shall attempt to grab the lock, but 
		 * Thread C still has the lock. Thread A should donate and C should run. But might be interrupted by Thread J(4). Thread J will finish.
		 * After Thread C  runs and releases the lock, and Thread A should run and finish. Thread B will run and then Thread E. After Thread E 
		 * and I will run and the remainder of the priority 2 thread will finish.
		 */
		/***************************************************************************************************************************/
		if(special!=null&&special.size()>10){
			boolean int_state2 = Machine.interrupt().disable();
			ThreadedKernel.scheduler.setPriority(special.get(0), 4);
			ThreadedKernel.scheduler.setPriority(special.get(1), 3);
			ThreadedKernel.scheduler.setPriority(special.get(2), 7);
			ThreadedKernel.scheduler.setPriority(special.get(3), 2);
			ThreadedKernel.scheduler.setPriority(special.get(4), 3);
			ThreadedKernel.scheduler.setPriority(special.get(5), 7);
			ThreadedKernel.scheduler.setPriority(special.get(6), 5);
			ThreadedKernel.scheduler.setPriority(special.get(7), 2);
			ThreadedKernel.scheduler.setPriority(special.get(8), 3);
			ThreadedKernel.scheduler.setPriority(special.get(9), 4);
			ThreadedKernel.scheduler.setPriority(special.get(10), 5);
			ThreadedKernel.scheduler.setPriority(special.get(11), 6);
			Machine.interrupt().restore(int_state2);
			KThread spe1 = special.get(0);
			KThread spe2 = special.get(1);
			KThread spe3 = special.get(2);
			KThread spe4 = special.get(3);
			KThread spe5 = special.get(4);
			KThread spe6 = special.get(5);
			KThread spe7 = special.get(6);
			KThread spe8 = special.get(7);
			KThread spe9 = special.get(8);
			KThread spe10 = special.get(9);
			KThread spe11 = special.get(10);
			KThread spe12 = special.get(11);
			spe1.setName("Thread A").fork();
			spe2.setName("Thread B").fork();
			spe3.setName("Thread C").fork();
			spe4.setName("Thread D").fork();
			spe5.setName("Thread E").fork();
			spe6.setName("Thread F").fork();
			spe7.setName("Thread G").fork();
			spe8.setName("Thread H").fork();
			spe9.setName("Thread I").fork();
			spe10.setName("Thread J").fork();
			spe11.setName("Thread K").fork();
			spe12.setName("Thread L").fork();
			spe1.join();
			spe2.join();
			spe3.join();
			spe4.join();
			spe5.join();
			spe6.join();
			spe7.join();
			spe8.join();
			spe9.join();
			spe10.join();
			spe11.join();
			spe12.join();
		}
		if(special!=null&&special.size()==9){

			boolean int_state2 = Machine.interrupt().disable();
			ThreadedKernel.scheduler.setPriority(special.get(0), 7);
			ThreadedKernel.scheduler.setPriority(special.get(1), 7);
			ThreadedKernel.scheduler.setPriority(special.get(2), 7);
			ThreadedKernel.scheduler.setPriority(special.get(3), 7);
			ThreadedKernel.scheduler.setPriority(special.get(4), 7);
			ThreadedKernel.scheduler.setPriority(special.get(5), 6);
			ThreadedKernel.scheduler.setPriority(special.get(6), 5);
			ThreadedKernel.scheduler.setPriority(special.get(7), 4);
			ThreadedKernel.scheduler.setPriority(special.get(8), 3);
			//ThreadedKernel.scheduler.setPriority(special.get(9), 4);
			Machine.interrupt().restore(int_state2);
			KThread spe1 = special.get(0);
			KThread spe2 = special.get(1);
			KThread spe3 = special.get(2);
			KThread spe4 = special.get(3);
			KThread spe5 = special.get(4);
			KThread spe6 = special.get(5);
			KThread spe7 = special.get(6);
			KThread spe8 = special.get(7);
			KThread spe9 = special.get(8);
			//KThread spe10 = special.get(9);
			spe1.setName("Tier1 Thread A::P2::E6").fork();
			spe2.setName("Tier2 Thread B::P3::E6").fork();
			spe3.setName("Tier2 Thread C::P3::E5").fork();
			spe4.setName("Tier2 Thread D::P3::E4").fork();
			spe5.setName("Tier2 Thread E::P3::E3").fork();
			spe6.setName("Tier3 Thread F::P6").fork();
			spe7.setName("Tier3 Thread G::P5").fork();
			spe8.setName("Tier3 Thread H::P4").fork();
			spe9.setName("Tier3 Thread I::P3").fork();
			//spe10.setName("Block Thread").fork();
			System.out.println("Setting up test environment!");
			spe1.join();
			spe2.join();
			spe3.join();
			spe4.join();
			spe5.join();
			spe6.join();
			spe7.join();
			spe8.join();
			spe9.join();
	
		}
		if(special!=null&&special.size()==10){

			boolean int_state2 = Machine.interrupt().disable();
			ThreadedKernel.scheduler.setPriority(special.get(0), 7);
			ThreadedKernel.scheduler.setPriority(special.get(1), 7);
			ThreadedKernel.scheduler.setPriority(special.get(2), 7);
			ThreadedKernel.scheduler.setPriority(special.get(3), 7);
			ThreadedKernel.scheduler.setPriority(special.get(4), 7);
			ThreadedKernel.scheduler.setPriority(special.get(5), 6);
			ThreadedKernel.scheduler.setPriority(special.get(6), 5);
			ThreadedKernel.scheduler.setPriority(special.get(7), 4);
			ThreadedKernel.scheduler.setPriority(special.get(8), 3);
			ThreadedKernel.scheduler.setPriority(special.get(9), 4);
			Machine.interrupt().restore(int_state2);
			KThread spe1 = special.get(0);
			KThread spe2 = special.get(1);
			KThread spe3 = special.get(2);
			KThread spe4 = special.get(3);
			KThread spe5 = special.get(4);
			KThread spe6 = special.get(5);
			KThread spe7 = special.get(6);
			KThread spe8 = special.get(7);
			KThread spe9 = special.get(8);
			KThread spe10 = special.get(9);
			spe1.setName("Tier1 Thread A::P2::E6").fork();
			spe2.setName("Tier2 Thread B::P3::E6").fork();
			spe3.setName("Tier2 Thread C::P3::E5").fork();
			spe4.setName("Tier2 Thread D::P3::E4").fork();
			spe5.setName("Tier2 Thread E::P3::E3").fork();
			spe6.setName("Tier3 Thread F::P6").fork();
			spe7.setName("Tier3 Thread G::P5").fork();
			spe8.setName("Tier3 Thread H::P4").fork();
			spe9.setName("Tier3 Thread I::P3").fork();
			spe10.setName("Block Thread").fork();
			System.out.println("Setting up test environment!");
			spe1.join();
			spe2.join();
			spe3.join();
			spe4.join();
			spe5.join();
			spe6.join();
			spe7.join();
			spe8.join();
			spe9.join();
			spe10.join();

		}


	}

	/**
	 * Test if this module is working.
	 */
	public static LinkedList<KThread> createThread(int number, int special){
		final Lock lock1 = new Lock(); //creates a lock on one resource
		final Lock lock2 = new Lock(); //creates a different lock on a different resource
		final Lock lock3 = new Lock(); //creates a different lock on a different resource
		final Lock lock4 = new Lock(); //creates a different lock on a different resource
		final Lock lock5 = new Lock(); //creates a different lock on a different resource
		final Alarm alarm = new Alarm();
		LinkedList<KThread> list = new LinkedList<KThread>();
		/*
		 * This is a basic creation of thread. These threads created below will have a random priority assigned to them
		 */
		if(number>0&&special==0){
			for(int i = 0; i<number;i++){				//creates a specific number of test thread
				list.add(new KThread(new Runnable() {
					public void run() {
						//System.out.println(KThread.currentThread().getName() + " has a priority of "+ ThreadedKernel.scheduler.getPriority());
						System.out.println(KThread.currentThread().getName()+"  is a basic thread and is now starting!");
						for(int i = 1; i<3; i++){
							System.out.println(KThread.currentThread().getName()+" said: Running "+ i +" out of 2 times");
							KThread.yield();
						}
						System.out.println(KThread.currentThread().getName()+" has now finished! ");
					}
				}));
			}
		}
		/*
		 * Creation of Special threads with specific priority associated with them
		 */
		if(number==0&&special!=0){
			//This will be Thread A
			list.add(new KThread(new Runnable() {
				public void run() {
					System.out.println(KThread.currentThread().getName() + " is now attempting to Acquire Lock one. Should fail and let C resume");
					lock1.acquire();
					System.out.println( KThread.currentThread().getName() + " is currently running with the Lock one." );
					lock1.release();
					System.out.println(KThread.currentThread().getName()+" has release the lock and B shall run.");
				}
			}));
			//This will be Thread B
			list.add(new KThread(new Runnable() {
				public void run() {
					System.out.println(KThread.currentThread().getName()+" should be running after A is done.");
					for(int i = 1; i<4; i++){
						System.out.println(KThread.currentThread().getName()+" is now running " +i+" out of 3 times.");
						KThread.yield();
					}
					System.out.println(KThread.currentThread().getName()+" said: I AM DONE.");
				}
			}));
			//This will be Thread c
			list.add(new KThread(new Runnable() {
				public void run() {
					System.out.println(KThread.currentThread().getName() + " has started with priority 7. It aquire the lock one first and will proceed to change its own priority");
					lock1.acquire();
					boolean int_state = Machine.interrupt().disable();
					ThreadedKernel.scheduler.setPriority( 2 );
					Machine.interrupt().restore( int_state );
					System.out.println(KThread.currentThread().getName() + " now has priority 2...Priority Inversion chaos has started");
					System.out.println(KThread.currentThread().getName() + " has now given up its runtime in the cpu. Will attempt to grab the cpu again.");
					KThread.yield();
					System.out.println( KThread.currentThread().getName() + " has grab hold of the cpu again! But is it in the right order?" );
					System.out.println(KThread.currentThread().getName() + " is now going to release the lock. A should run after this.");
					lock1.release();
					KThread.yield();
					System.out.println( KThread.currentThread().getName() + " has the cpu again. This thread should run after Thread A and B is finished" );
					lock1.acquire();
					//System.out.println(" ");
					lock1.release();
					System.out.println( KThread.currentThread().getName() + " has finished");
					System.out.println("FINISH TEST CASE!");
				}
			}));

		}
		if(number>0&&special==1){
			//This will be Thread A
			list.add(new KThread(new Runnable() {
				public void run() {
					System.out.println(KThread.currentThread().getName() + " taking the CPU first");
					//lock1.acquire();
					System.out.println( KThread.currentThread().getName() + " is currently executing." );
					//lock1.release();
					System.out.println(KThread.currentThread().getName()+" has finished executing. Thread C shall attempt next.");
				}
			}));
			//This will be Thread B
			list.add(new KThread(new Runnable() {
				public void run() {
					System.out.println(KThread.currentThread().getName()+" said Did I stall C?");
					System.out.println(KThread.currentThread().getName()+" should be running after E is done.");
					for(int i = 1; i<4; i++){
						System.out.println(KThread.currentThread().getName()+" is now running " +i+" out of 3 times.");
						KThread.yield();
					}
					System.out.println(KThread.currentThread().getName()+" said: I AM DONE.");
				}
			}));
			//This will be Thread c
			list.add(new KThread(new Runnable() {
				public void run() {
					System.out.println(KThread.currentThread().getName() + " has started with priority 7. It aquire the lock one first and will proceed to change its own priority");
					lock1.acquire();
					boolean int_state = Machine.interrupt().disable();
					ThreadedKernel.scheduler.setPriority( 2 );
					Machine.interrupt().restore( int_state );
					System.out.println(KThread.currentThread().getName() + " now has priority 2...Priority Inversion chaos has started");
					System.out.println(KThread.currentThread().getName() + " has now given up its runtime in the cpu. Will attempt to grab the cpu again.");
					KThread.yield();
					System.out.println( KThread.currentThread().getName() + " has grab hold of the cpu again! But is it in the right order?" );
					System.out.println(KThread.currentThread().getName() + " is now going to release the lock. A should run after this.");
					lock1.release();
					KThread.yield();
					System.out.println( KThread.currentThread().getName() + " has the cpu again. This thread should run after all the other threads are finished" );
					lock1.acquire();
					System.out.println( KThread.currentThread().getName() + " is now running as a regular thread");
					lock1.release();
					System.out.println( KThread.currentThread().getName() + " has finished");
					System.out.println("FINISH TEST CASE!");
				}
			}));
			//This will be Thread D
			list.add(new KThread(new Runnable() {
				public void run() {
					System.out.println(KThread.currentThread().getName() + " is now attempting to Acquire Lock one. Should fail and donate to Thread C to prevent B and E from taking the CPU");
					lock1.acquire();
					System.out.println( KThread.currentThread().getName() + " is currently running with the Lock one." );
					lock1.release();
					System.out.println(KThread.currentThread().getName()+" has release the lock and E shall run.");
				}
			}));
			//This will be Thread E
			list.add(new KThread(new Runnable() {
				public void run() {
					System.out.println(KThread.currentThread().getName()+" should be running after D is done.");
					for(int i = 1; i<4; i++){
						System.out.println(KThread.currentThread().getName()+" is now running " +i+" out of 3 times.");
						KThread.yield();
					}
					System.out.println(KThread.currentThread().getName()+" said: I AM DONE.");
				}
			}));
		}
		if(number>0&&special==2){
			//This will be Thread A && priority 4
			list.add(new KThread(new Runnable() {
				public void run() {
					System.out.println(KThread.currentThread().getName() + " is trying to get Lock1 and failed. j?");
					lock1.acquire();
					System.out.println( KThread.currentThread().getName() + " is currently executing with Lock1." );
					lock1.release();
					System.out.println(KThread.currentThread().getName()+" has finished executing. Released Lock1");
				}
			}));
			//This will be Thread B && priority 3
			list.add(new KThread(new Runnable() {
				public void run() {
					System.out.println(KThread.currentThread().getName()+" should be running after A is done.");
					for(int i = 1; i<4; i++){
						System.out.println(KThread.currentThread().getName()+" is now running " +i+" out of 3 times.");
						KThread.yield();
					}
					System.out.println(KThread.currentThread().getName()+" said: I AM DONE.");
				}
			}));
			//This will be Thread c && priority 7 to 2
			list.add(new KThread(new Runnable() {
				public void run() {
					System.out.println(KThread.currentThread().getName() + " has started with priority 7. It aquire the lock one first and will proceed to change its own priority");
					lock1.acquire();
					boolean int_state = Machine.interrupt().disable();
					ThreadedKernel.scheduler.setPriority( 2 );
					Machine.interrupt().restore( int_state );
					System.out.println(KThread.currentThread().getName() + " now has priority 2.");
					System.out.println(KThread.currentThread().getName() + " has now given up its runtime in the cpu. Will attempt to grab the cpu again after Thread A.");
					KThread.yield();
					System.out.println( KThread.currentThread().getName() + " has grab hold of the cpu again! But is it in the right order?(J)" );
					System.out.println(KThread.currentThread().getName() + " is now going to release the lock. A should run after this.");
					lock1.release();
					KThread.yield();
					System.out.println( KThread.currentThread().getName() + " has the cpu again. This thread should run after all the other threads are finished" );
					lock1.acquire();
					System.out.println( KThread.currentThread().getName() + " is now running as a regular thread");
					lock1.release();
					System.out.println( KThread.currentThread().getName() + " has finished");
					System.out.println("FINISH TEST CASE!");
				}
			}));
			//This will be Thread D && priority 2
			list.add(new KThread(new Runnable() {
				public void run() {
					System.out.println(KThread.currentThread().getName() + " is now attempting to Acquire Lock one. ");
					lock1.acquire();
					System.out.println( KThread.currentThread().getName() + " is currently running with the Lock one." );
					lock1.release();
					System.out.println(KThread.currentThread().getName()+" has release the lock and H shall run.");
					//System.out.println("FINISH TEST CASE!");
				}
			}));
			//This will be Thread E
			list.add(new KThread(new Runnable() {
				public void run() {
					System.out.println(KThread.currentThread().getName()+" should be running with B.");
					for(int i = 1; i<4; i++){
						System.out.println(KThread.currentThread().getName()+" is now running " +i+" out of 3 times.");
						KThread.yield();
					}
					System.out.println(KThread.currentThread().getName()+" said: I AM DONE.");
				}

			}));
			//This will be Thread F with starting Priority of 7 and will be changed to 2
			list.add(new KThread(new Runnable() {
				public void run() {
					System.out.println(KThread.currentThread().getName() + " has started with priority 7. It aquire the lock two first and will proceed to change its own priority");
					lock2.acquire();
					boolean int_state = Machine.interrupt().disable();
					ThreadedKernel.scheduler.setPriority( 2 );
					Machine.interrupt().restore( int_state );
					System.out.println(KThread.currentThread().getName() + " now has priority 2...Double Lock Priority Inversion chaos has started");
					System.out.println(KThread.currentThread().getName() + " has now given up its runtime in the cpu. Will attempt to grab the cpu again after Thread K.");
					KThread.yield();
					System.out.println( KThread.currentThread().getName() + " has grab hold of the cpu again! But is it in the right order?(K)" );
					System.out.println(KThread.currentThread().getName() + " is now going to release the lock2. G should run after this.");
					lock2.release();
					KThread.yield();
					System.out.println( KThread.currentThread().getName() + " has the cpu again. This thread should run after all the other threads are finished" );
					lock2.acquire();
					System.out.println( KThread.currentThread().getName() + " is now running as a regular thread");
					lock2.release();
					System.out.println( KThread.currentThread().getName() + " has finished");
					//System.out.println("FINISH TEST CASE!");
				}
			}));
			//This will be Thread G && priority 5
			list.add(new KThread(new Runnable() {
				public void run() {
					System.out.println(KThread.currentThread().getName() + " is now attempting to Acquire Lock two. Comes after Thread L");
					lock2.acquire();
					System.out.println( KThread.currentThread().getName() + " is currently running with the Lock two." );
					lock2.release();
					System.out.println(KThread.currentThread().getName()+" has release the lock and A shall run.");
				}
			}));			
			//This will be Thread H && Priority 2
			list.add(new KThread(new Runnable() {
				public void run() {
					//System.out.println(KThread.currentThread().getName() + " has a priority of "+ ThreadedKernel.scheduler.getPriority());
					System.out.println(KThread.currentThread().getName()+" should come after Thread D!");
					System.out.println(KThread.currentThread().getName()+" has finished.");
					KThread.yield();
				}
			}));
			//This will be Thread I && Priority 3
			list.add(new KThread(new Runnable() {
				public void run() {
					//System.out.println(KThread.currentThread().getName() + " has a priority of "+ ThreadedKernel.scheduler.getPriority());
					System.out.println(KThread.currentThread().getName()+" should come after Thread F!");
					System.out.println(KThread.currentThread().getName()+" has finished.");
					KThread.yield();
				}
			}));
			//This will be Thread J && priority 4
			list.add(new KThread(new Runnable() {
				public void run() {
					//System.out.println(KThread.currentThread().getName() + " has a priority of "+ ThreadedKernel.scheduler.getPriority());
					System.out.println(KThread.currentThread().getName()+" is strolling by! Should come after Thread A!");
					System.out.println(KThread.currentThread().getName()+" has finished.");
					KThread.yield();
				}
			}));
			//This will be Thread K && priority 5
			list.add(new KThread(new Runnable() {
				public void run() {
					//System.out.println(KThread.currentThread().getName() + " has a priority of "+ ThreadedKernel.scheduler.getPriority());
					System.out.println(KThread.currentThread().getName()+" is strolling by! Should come after Thread G!");
					System.out.println(KThread.currentThread().getName()+" has finished.");
					KThread.yield();
				}
			}));
			//This will be Thread L && priority 6
			list.add(new KThread(new Runnable() {
				public void run() {
					//System.out.println(KThread.currentThread().getName() + " has a priority of "+ ThreadedKernel.scheduler.getPriority());
					System.out.println(KThread.currentThread().getName()+" is strolling by. Should come after Thread F!");
					System.out.println(KThread.currentThread().getName()+" has finished. Thread G, are you next?");
					KThread.yield();
				}
			}));
		}
		if(special==3){
			list.add(new KThread(new Runnable() {
				public void run() {
					//System.out.println(KThread.currentThread().getName() + " has started with priority 7 and owns ResourceTier1. Tier One-Everyone wants this resource. Changing its own priorties to 2.");
					lock1.acquire();
					boolean int_state = Machine.interrupt().disable();
					ThreadedKernel.scheduler.setPriority( 2 );
					Machine.interrupt().restore( int_state );
					System.out.println(KThread.currentThread().getName() + " owns ResourceTier1. Tier One--Everyone wants this resource!");		
					System.out.println(KThread.currentThread().getName() + " has now given up its runtime in the cpu....Fighting for this resource has commence!\n In the end, Tier2 Thread B should take it.");
					KThread.yield();
					System.out.println( KThread.currentThread().getName() + " has grab hold of the cpu again! " );
					System.out.println(KThread.currentThread().getName() + " is now going to give up ResourceTier1. Tier2 Thread B should run after this.");
					lock1.release();
					KThread.yield();
				}
			}));
			list.add(new KThread(new Runnable() {
				public void run() {
					lock2.acquire();
					boolean int_state = Machine.interrupt().disable();
					ThreadedKernel.scheduler.setPriority( 3 );
					Machine.interrupt().restore( int_state );
					KThread.yield();
					//System.out.println(KThread.currentThread().getName() + " owns ResourceTier2(a) is determined to grab ResourceTier1 but failed.");
					lock1.acquire();
					//KThread.yield();
					System.out.println( KThread.currentThread().getName() + " has grab hold of ResourceTier1! Is tired and giving up cpu..." );
					KThread.yield();
					System.out.println( KThread.currentThread().getName() + " got the cpu and now releasing the Legendary Tier1 Resource!");
					lock1.release();
					KThread.yield();
					System.out.println(KThread.currentThread().getName() + " is now going to give up ResourceTier2(a). Tier3 Thread F should run after this.");
					lock2.release();
					KThread.yield();
				}
			}));
			list.add(new KThread(new Runnable() {
				public void run() {
					lock3.acquire();
					boolean int_state = Machine.interrupt().disable();
					ThreadedKernel.scheduler.setPriority( 3 );
					Machine.interrupt().restore( int_state );
					KThread.yield();
					//System.out.println(KThread.currentThread().getName() + " owns ResourceTier2(b) is determined to grab ResourceTier1 but failed.");
					lock1.acquire();
					//KThread.yield();
					System.out.println( KThread.currentThread().getName() + " has grab hold of ResourceTier1! Is tired and giving up cpu..." );
					KThread.yield();
					System.out.println( KThread.currentThread().getName() + " got the cpu and now releasing the Legendary Tier1 Resource!");
					lock1.release();
					KThread.yield();
					System.out.println(KThread.currentThread().getName() + " is now going to give up ResourceTier2(b). Tier3 Thread G should run after this.");
					lock3.release();
					KThread.yield();
				}
			}));
			list.add(new KThread(new Runnable() {
				public void run() {
					//System.out.println(KThread.currentThread().getName() + " has spawn with Priority 3");

					lock4.acquire();
					boolean int_state = Machine.interrupt().disable();
					ThreadedKernel.scheduler.setPriority( 3 );
					Machine.interrupt().restore( int_state );
					KThread.yield();
					//System.out.println(KThread.currentThread().getName() + " owns ResourceTier2(c) is determined to grab ResourceTier1 but failed.");
					lock1.acquire();
					//KThread.yield();
					System.out.println( KThread.currentThread().getName() + " has grab hold of ResourceTier1! Is tired and giving up cpu..." );
					KThread.yield();
					System.out.println( KThread.currentThread().getName() + " got the cpu and now releasing the Legendary Tier1 Resource!");
					lock1.release();
					KThread.yield();
					System.out.println(KThread.currentThread().getName() + " is now going to give up ResourceTier2(c). Tier3 Thread H should run after this.");
					lock4.release();
					KThread.yield();
				}
			}));
			list.add(new KThread(new Runnable() {
				public void run() {
					//System.out.println(KThread.currentThread().getName() + " has spawn with Priority 3");

					lock5.acquire();
					boolean int_state = Machine.interrupt().disable();
					ThreadedKernel.scheduler.setPriority( 3 );
					Machine.interrupt().restore( int_state );
					KThread.yield();
					//System.out.println(KThread.currentThread().getName() + " owns ResourceTier2(d) is determined to grab ResourceTier1 but failed.");
					lock1.acquire();
					//KThread.yield();
					System.out.println( KThread.currentThread().getName() + " has grab hold of ResourceTier1! Is tired and giving up cpu..." );
					KThread.yield();
					System.out.println( KThread.currentThread().getName() + " got the cpu and now releasing the Legendary Tier1 Resource!");
					lock1.release();
					KThread.yield();
					System.out.println(KThread.currentThread().getName() + " is now going to give up ResourceTier2(d). Tier3 Thread I should run after this.");
					lock5.release();
					KThread.yield();
				}
			}));
			list.add(new KThread(new Runnable() {
				public void run() {
					//System.out.println(KThread.currentThread().getName() + " is now attempting to Acquire ResourceTier2(a), but failed.");
					for(int i = 0; i <9;i++){
						KThread.yield();
					}
					lock2.acquire();
					System.out.println( KThread.currentThread().getName() + " has grabbed ResourceTier2(a)!" );
					lock2.release();
					System.out.println(KThread.currentThread().getName()+" has release the ResourceTier2(a) and Thread C shall run.");
				}
			}));	
			list.add(new KThread(new Runnable() {
				public void run() {
					//System.out.println(KThread.currentThread().getName() + " is now attempting to Acquire ResourceTier2(b), but failed.");
					for(int i = 0; i <9;i++){
						KThread.yield();
					}
					lock3.acquire();
					System.out.println( KThread.currentThread().getName() + " has grabbed ResourceTier2(b)!" );
					lock3.release();
					System.out.println(KThread.currentThread().getName()+" has release the ResourceTier2(b) and Thread D shall run.");
				}
			}));	
			list.add(new KThread(new Runnable() {
				public void run() {
					//System.out.println(KThread.currentThread().getName() + " is now attempting to Acquire ResourceTier2(c), but failed.");
					for(int i = 0; i <9;i++){
						KThread.yield();
					}
					lock4.acquire();
					System.out.println( KThread.currentThread().getName() + " has grabbed ResourceTier2(c)!" );
					lock4.release();
					System.out.println(KThread.currentThread().getName()+" has release the ResourceTier2(c) and Thread E shall run.");
				}
			}));	
			list.add(new KThread(new Runnable() {
				public void run() {
					//System.out.println(KThread.currentThread().getName() + " is now attempting to Acquire ResourceTier2(d), but failed.");
					for(int i = 0; i <9;i++){
						KThread.yield();
					}
					lock5.acquire();
					System.out.println( KThread.currentThread().getName() + " has grabbed ResourceTier2(d)!" );
					lock5.release();
					System.out.println(KThread.currentThread().getName()+" has release the ResourceTier2(d).");
					System.out.println("Test case completed!");
				}
			}));
		}
		if(special==4){
			list.add(new KThread(new Runnable() {
				public void run() {
					//System.out.println(KThread.currentThread().getName() + " has started with priority 7 and owns ResourceTier1. Tier One-Everyone wants this resource. Changing its own priorties to 2.");
					lock1.acquire();
					boolean int_state = Machine.interrupt().disable();
					ThreadedKernel.scheduler.setPriority( 2 );
					Machine.interrupt().restore( int_state );
					System.out.println(KThread.currentThread().getName() + " owns ResourceTier1. Tier One--Everyone wants this resource!");		
					System.out.println(KThread.currentThread().getName() + " has now given up its runtime in the cpu....Fighting for this resource has commence!\n In the end, Tier2 Thread B should take it.");
					KThread.yield();
					System.out.println( KThread.currentThread().getName() + " has grab hold of the cpu again! " );
					System.out.println(KThread.currentThread().getName() + " is now going to give up ResourceTier1. Tier2 Thread B should run after this.");
					lock1.release();
					KThread.yield();
				}
			}));
			list.add(new KThread(new Runnable() {
				public void run() {
					lock2.acquire();
					boolean int_state = Machine.interrupt().disable();
					ThreadedKernel.scheduler.setPriority( 3 );
					Machine.interrupt().restore( int_state );
					KThread.yield();
					//System.out.println(KThread.currentThread().getName() + " owns ResourceTier2(a) is determined to grab ResourceTier1 but failed.");
					lock1.acquire();
					//KThread.yield();
					System.out.println( KThread.currentThread().getName() + " has grab hold of ResourceTier1! Is tired and giving up cpu..." );
					KThread.yield();
					System.out.println( KThread.currentThread().getName() + " got the cpu and now releasing the Legendary Tier1 Resource!");
					lock1.release();
					KThread.yield();
					System.out.println(KThread.currentThread().getName() + " is now going to give up ResourceTier2(a). Tier3 Thread F should run after this.");
					lock2.release();
					KThread.yield();
				}
			}));
			list.add(new KThread(new Runnable() {
				public void run() {
					lock3.acquire();
					boolean int_state = Machine.interrupt().disable();
					ThreadedKernel.scheduler.setPriority( 3 );
					Machine.interrupt().restore( int_state );
					KThread.yield();
					//System.out.println(KThread.currentThread().getName() + " owns ResourceTier2(b) is determined to grab ResourceTier1 but failed.");
					lock1.acquire();
					//KThread.yield();
					System.out.println( KThread.currentThread().getName() + " has grab hold of ResourceTier1! Is tired and giving up cpu..." );
					KThread.yield();
					System.out.println( KThread.currentThread().getName() + " got the cpu and now releasing the Legendary Tier1 Resource!");
					lock1.release();
					KThread.yield();
					System.out.println(KThread.currentThread().getName() + " is now going to give up ResourceTier2(b). Tier3 Thread G should run after this.");
					lock3.release();
					KThread.yield();
				}
			}));
			list.add(new KThread(new Runnable() {
				public void run() {
					//System.out.println(KThread.currentThread().getName() + " has spawn with Priority 3");

					lock4.acquire();
					boolean int_state = Machine.interrupt().disable();
					ThreadedKernel.scheduler.setPriority( 3 );
					Machine.interrupt().restore( int_state );
					KThread.yield();
					//System.out.println(KThread.currentThread().getName() + " owns ResourceTier2(c) is determined to grab ResourceTier1 but failed.");
					lock1.acquire();
					//KThread.yield();
					System.out.println( KThread.currentThread().getName() + " has grab hold of ResourceTier1! Is tired and giving up cpu..." );
					KThread.yield();
					System.out.println( KThread.currentThread().getName() + " got the cpu and now releasing the Legendary Tier1 Resource!");
					lock1.release();
					KThread.yield();
					System.out.println(KThread.currentThread().getName() + " is now going to give up ResourceTier2(c). Tier3 Thread H should run after this.");
					lock4.release();
					KThread.yield();
				}
			}));
			list.add(new KThread(new Runnable() {
				public void run() {
					//System.out.println(KThread.currentThread().getName() + " has spawn with Priority 3");

					lock5.acquire();
					boolean int_state = Machine.interrupt().disable();
					ThreadedKernel.scheduler.setPriority( 3 );
					Machine.interrupt().restore( int_state );
					KThread.yield();
					//System.out.println(KThread.currentThread().getName() + " owns ResourceTier2(d) is determined to grab ResourceTier1 but failed.");
					lock1.acquire();
					//KThread.yield();
					System.out.println( KThread.currentThread().getName() + " has grab hold of ResourceTier1! Is tired and giving up cpu..." );
					KThread.yield();
					System.out.println( KThread.currentThread().getName() + " got the cpu and now releasing the Legendary Tier1 Resource!");
					lock1.release();
					KThread.yield();
					System.out.println(KThread.currentThread().getName() + " is now going to give up ResourceTier2(d). Tier3 Thread I should run after this.");
					lock5.release();
					KThread.yield();
				}
			}));
			list.add(new KThread(new Runnable() {
				public void run() {
					//System.out.println(KThread.currentThread().getName() + " is now attempting to Acquire ResourceTier2(a), but failed.");
					for(int i = 0; i <9;i++){
						KThread.yield();
					}
					lock2.acquire();
					System.out.println( KThread.currentThread().getName() + " has grabbed ResourceTier2(a)!" );
					lock2.release();
					System.out.println(KThread.currentThread().getName()+" has release the ResourceTier2(a) and Thread C shall run.");
				}
			}));	
			list.add(new KThread(new Runnable() {
				public void run() {
					//System.out.println(KThread.currentThread().getName() + " is now attempting to Acquire ResourceTier2(b), but failed.");
					for(int i = 0; i <9;i++){
						KThread.yield();
					}
					lock3.acquire();
					System.out.println( KThread.currentThread().getName() + " has grabbed ResourceTier2(b)!" );
					lock3.release();
					System.out.println(KThread.currentThread().getName()+" has release the ResourceTier2(b) and Thread D shall run.");
				}
			}));	
			list.add(new KThread(new Runnable() {
				public void run() {
					//System.out.println(KThread.currentThread().getName() + " is now attempting to Acquire ResourceTier2(c), but failed.");
					for(int i = 0; i <9;i++){
						KThread.yield();
					}
					lock4.acquire();
					System.out.println( KThread.currentThread().getName() + " has grabbed ResourceTier2(c)!" );
					lock4.release();
					System.out.println(KThread.currentThread().getName()+" has release the ResourceTier2(c) and Thread E shall run.");
				}
			}));	
			list.add(new KThread(new Runnable() {
				public void run() {
					//System.out.println(KThread.currentThread().getName() + " is now attempting to Acquire ResourceTier2(d), but failed.");
					for(int i = 0; i <9;i++){
						KThread.yield();
					}
					lock5.acquire();
					System.out.println( KThread.currentThread().getName() + " has grabbed ResourceTier2(d)!" );
					lock5.release();
					System.out.println(KThread.currentThread().getName()+" has release the ResourceTier2(d).");
					System.out.println("Test case completed!");
				}
			}));
			list.add(new KThread(new Runnable() {
				public void run() {
					//System.out.println(KThread.currentThread().getName() + " is now attempting to Acquire ResourceTier2(d), but failed.");
					
					for(int i = 0;i<100;i++){
						System.out.println(KThread.currentThread().getName()+" is running! " + i + " times");
						KThread.yield();
					}
				}
			}));	
		}
		return list;
	}
	public static void selfTest(){
		//Created two threads with its runnable being printing things.
		LinkedList<KThread> threadList = null, specialThreadList = null, specialThreadListPlus = null, doubleLockConfusion = null, deepDonation = null,deepDonation2 = null;
		threadList = createThread(5,0); 	//creates five regular thread and run it
		specialThreadList= createThread(0,1);	
		specialThreadListPlus = createThread(1,1);
		doubleLockConfusion = createThread(1,2);
		deepDonation = createThread(1,3);
		deepDonation2 = createThread(1,4);
		System.out.print("************************************************************************************************************\n**                                  ");
		System.out.println("Starting Basic Case: Random Madness!                                  **");
		System.out.println("************************************************************************************************************");
		testMe(threadList,null,0,0,0);
		System.out.println("FINISH TEST CASE!");
		System.out.print("************************************************************************************************************\n**                       ");
		System.out.println("Starting Special Test Case: Priority Inversion Confusion!                        **");
		System.out.println("************************************************************************************************************");
		//testMe(null,specialThreadList,6,4,7);
		System.out.print("************************************************************************************************************\n**                 ");
		System.out.println("Starting Special Test Plus Case: Priority Inversion Confusion EXTREME!!                **");
		System.out.println("************************************************************************************************************");
		//testMe(null,specialThreadListPlus,7,4,7);
		System.out.print("************************************************************************************************************\n**                 ");
		System.out.println("Starting Special Test Case 2: Double Lock Priority Inversion Confusion!                **");
		System.out.println("************************************************************************************************************");
		//testMe(null,doubleLockConfusion,7,4,7);
		System.out.print("************************************************************************************************************\n**                              ");
		System.out.println("Starting Special Test Case 3: Deep Donation!                              **");
		System.out.println("************************************************************************************************************");
		//testMe(null,deepDonation,0,0,0);
		System.out.print("************************************************************************************************************\n**                         ");
		System.out.println("Starting Special Test Case 4: Deep Donation w/Blocking!                        **");
		System.out.println("************************************************************************************************************");
		//testMe(null,deepDonation2,0,0,0);
		System.out.print("************************************************************************************************************\n**                                ");
		System.out.println("Priority Scheduler Test Cases Completed!!                               **");
		System.out.println("************************************************************************************************************");

	}

}
