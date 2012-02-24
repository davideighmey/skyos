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
		getThreadState(thread).needReorded = true;
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
		public void reOrdered(){
			PriorityQueue buffer = new PriorityQueue(this.transferPriority);
			ThreadState hold = null;								
			while(!this.waitPQueue.isEmpty()){										//for each element in the queue, check to make sure it has the highest priority 
				hold =this.waitPQueue.poll();
				buffer.waitPQueue.offer(hold);	
			}
			while(!buffer.waitPQueue.isEmpty()){										//for each element in the queue, check to make sure it has the highest priority 
				hold = buffer.waitPQueue.poll();
				this.waitPQueue.offer(hold);	
				}
		}
		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			PriorityQueue buffer = null;
			KThread returnThread = null;
			reOrdered();
			if(this.transferPriority&&this.resourceOwner!=null&&listDonate.size()!=0){			//Removes donation of the once running thread
				for(Donation k: listDonate){
					if(k.threadInQuestion.thread==this.resourceOwner.thread){
						this.resourceOwner.effective = this.resourceOwner.priority;
						k.removeDonation();
					} 
				}
				this.resourceOwner.effective = this.resourceOwner.priority;
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
			for(ThreadState k:waitPQueue){										//for each element in the queue, check to make sure it has the highest priority 
				if((hold.effective<k.effective))								//if the current hold has a lower effective than the checked effective set hold to the checked						
					hold = k;

			}
			for(ThreadState k:waitPQueue){	
				if((hold.effective==k.effective)&&
						(Machine.timer().getTime()-hold.timeINqueue)
						<(Machine.timer().getTime() - k.timeINqueue)&&
						k.thread.getName()!="main") //check if there is a longer waiting thread
					hold = k;
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
		/*	if(!listDonate.isEmpty()){
				int found = 0;
				for(int i = 0; i< listDonate.size();i++){
					if(this == listDonate.get(i).threadInQuestion){
						found = i;
					}
				}
				return listDonate.get(found).effective;
			}
			else*/
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
			if(this.needReorded)
				this.effective = priority;
			if(this.effective==0||this.effective<priority)
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
		public void waitForAccess(PriorityQueue waitQueue) {
			Lib.assertTrue(Machine.interrupt().disabled());
			if(waitQueue==null)													//in the case that it is null, do nothing
				return;
			this.timeINqueue = Machine.timer().getTime();						//this will keep track on how long it has been in the queue.
			this.waitingResource = waitQueue;
			waitQueue.waitPQueue.offer(this);
			if(waitQueue.transferPriority&&waitQueue.resourceOwner!=null&&waitQueue.resourceOwner!=this){		//if this is true we have to transfer priority and there is a lock in play

				//for(ThreadState k : waitQueue.waitPQueue)
				//if(this.effective<k.effective)								//look at queue of there is a lower priority on the list
				compute_donation(waitQueue,this);						//if there is one, priority inversion might be in play, so donate!
			}


			//addToQueue(waitQueue);									//add this to queue
		}		

		public void compute_donation(PriorityQueue waitQueue, ThreadState threadDonor){
			LinkedList<ThreadState> seenThreadState = new LinkedList<ThreadState>();
			if(waitQueue==null||threadDonor == null) return;
			while(!seenThreadState.contains(threadDonor)){					//checks if there is a same Donor on the list					
				seenThreadState.add(threadDonor);						
				//case where join is called
				if(threadDonor==null) break;
				if(threadDonor.thread!=null){
					if(waitQueue.resourceOwner != threadDonor&&threadDonor.thread.joinThread!=null){
						if(threadDonor.thread.joinThread==threadDonor.thread)
							break;
						Donation donor = new Donation(waitQueue, threadDonor, waitQueue.resourceOwner);
						if(threadDonor.effective>=waitQueue.resourceOwner.effective)
							listDonate.add(donor);
					}
				}
				if(threadDonor.waitingResource!=null){
					if(this.waitingResource.resourceOwner!=null){
						if(this.waitingResource!=null&&this.waitingResource.transferPriority&&threadDonor.waitingResource.resourceOwner!=null){
							if(threadDonor.waitingResource.resourceOwner==threadDonor)
								break;
							else if(threadDonor.effective>threadDonor.waitingResource.resourceOwner.effective){ //if(threadDonor.waitingResource.resourceOwner!=threadDonor&&threadDonor.waitingResource.resourceOwner.thread.getName()!="main"){
								Donation donor = new Donation(waitQueue, threadDonor, threadDonor.waitingResource.resourceOwner);
								if(threadDonor.effective>=threadDonor.waitingResource.resourceOwner.effective)
									listDonate.add(donor);
							}	

						}
					}
				}
				if(threadDonor.waitingResource!=null)
					threadDonor = threadDonor.waitingResource.resourceOwner;
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
	public static void testMe(LinkedList<KThread> list, LinkedList<KThread> special,int priority1, int priority2,int priority3){

		/***************************************************************************************************************************/
		/*
		 * Test case for priority scheduling. Depending on the number of threads created, we will set each thread priority randomly and
		 * allow them to run. The print out should be organize by their priority number with highest being first and the lowest being
		 * last.
		 */
		/***************************************************************************************************************************/
		if(list!=null&&special==null){
			Random r = new Random();
			int priority = 0;
			KThread thread = null;
			boolean int_state = Machine.interrupt().disable();
			//Setting Priorities
			for(int i =0; i< list.size();i++){
				priority = r.nextInt(4)+2;	//priority will be between 2 and 6
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
		if(special!=null&&special.size()>5){
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


	}

	/**
	 * Test if this module is working.
	 */
	public static LinkedList<KThread> createThread(int number, int special){
		final Lock lock1 = new Lock(); //creates a lock on one resource
		final Lock lock2 = new Lock(); //creates a different lock on a different resource
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
					System.out.println(KThread.currentThread().getName() + " taking the CPU and interupting Thread C");
					//lock1.acquire();
					System.out.println( KThread.currentThread().getName() + " is currently executing." );
					//lock1.release();
					System.out.println(KThread.currentThread().getName()+" has finished executing. Thread D shall attempt next.");
				}
			}));
			//This will be Thread B
			list.add(new KThread(new Runnable() {
				public void run() {
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
					System.out.println(KThread.currentThread().getName() + " is now attempting to Acquire Lock one. Should fail and let C resume");
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
					System.out.println(KThread.currentThread().getName()+" should come after Thread F!");
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
		return list;
	}
	public static void selfTest(){
		Lib.debug(dbgThread, "Enter PriorityQueue.selfTest");
		//Created two threads with its runnable being printing things.
		LinkedList<KThread> threadList = null, specialThreadList = null, specialThreadListPlus = null, doubleLockConfusion = null;
		threadList = createThread(5,0); 	//creates five regular thread and run it
		specialThreadList= createThread(0,1);	
		specialThreadListPlus = createThread(1,1);
		doubleLockConfusion = createThread(1,2);
		System.out.print("************************************************************************************************************\n**                                  ");
		System.out.println("Starting Basic Case: Random Madness!                                  **");
		System.out.println("************************************************************************************************************");
		testMe(threadList,null,0,0,0);
		System.out.println("FINISH TEST CASE!");
		System.out.print("************************************************************************************************************\n**                       ");
		System.out.println("Starting Special Test Case: Priority Inversion Confusion!                        **");
		System.out.println("************************************************************************************************************");
		testMe(null,specialThreadList,6,4,7);
		System.out.print("************************************************************************************************************\n**                 ");
		System.out.println("Starting Special Test Plus Case: Priority Inversion Confusion EXTREME!!                **");
		System.out.println("************************************************************************************************************");
		testMe(null,specialThreadListPlus,7,4,7);
		System.out.print("************************************************************************************************************\n**                 ");
		System.out.println("Starting Special Test Case 2: Double Lock Priority Inversion Confusion!                **");
		System.out.println("************************************************************************************************************");
		testMe(null,doubleLockConfusion,7,4,7);
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
