package nachos.threads;
//import java.util.LinkedList; no use for link list see if this works
import java.util.LinkedList;

import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */

/*
 * instead of using semaphores that was the foundation for condition variable,  
 * we have to use condition variable directly. In other words, we have to provide the same 
 * implementation as the original Condition class but without the use of semaphores. 
 */
public class Condition2 {
	/**
	 * Allocate a new condition variable.
	 *
	 * @param	conditionLock	the lock associated with this condition
	 *				variable. The current thread must hold this
	 *				lock whenever it uses <tt>sleep()</tt>,
	 *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
	 */
	public Condition2(Lock conditionLock) {
		this.conditionLock = conditionLock;
	}


	/**
	 * Atomically release the associated lock and go to sleep on this condition
	 * variable until another thread wakes it using <tt>wake()</tt>. The
	 * current thread must hold the associated lock. The thread will
	 * automatically reacquire the lock before <tt>sleep()</tt> returns.
	 */
	/* sleep(). first sleep must make sure the current thread has the lock. then we disable the interrupts
	 *  then we release the lock put the thread on waiting queue that is inside the current thread.
	 * the thread is then put to sleep. when it wakes up it acquires the lock, then we restore the status 
	 * and enable the interrupts.
	 */
	public void sleep() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread()); // make sure current thread has lock

		boolean intStatus = Machine.interrupt().disable();//the interrupt disable(), to deal with thread 
		conditionLock.release(); // releasing the lock

		waitQueue2.waitForAccess(KThread.currentThread()); //the thread queue inside current thread 
		KThread.sleep();//puts the thread to sleep
		conditionLock.acquire(); //gets the lock when it wakes up

		Machine.interrupt().restore(intStatus); //Interrupt enabled & restore to last status .

	}
	/**
	 * Wake up at most one thread sleeping on this condition variable. The
	 * current thread must hold the associated lock.
	 */
	/* we make sure the current thread has the lock. we then disable all the interrupts. we then make the kthread from the 
	 * queue we make sure there is a thread.. then we ready it. this causes it to wake up. then we enable intrupts and restores
	 * the status of the thread
	 */
	public void wake() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		boolean intStatus = Machine.interrupt().disable(); // the interrupted disable, to prevent the thread being changed
		//if (!waitQueue.isEmpty()){ //check and see if the thread queue is not empty.. dont need
			
			KThread thread = waitQueue2.nextThread(); // use the semaphore implementation readying the first thread on queue
													  // see semaphore.java v function
			if (thread != null) {  // make sure there is a thread
			    thread.ready(); // get it ready
			}
			
			//waitQueue.removeFirst(); //Grab the first sleeping thread on the list (FIFO Queue).. already woke it up with the ready()
			Machine.interrupt().restore(intStatus); //enable the interrupt & restores
			
		}
	
	/**
	 * Wake up all threads sleeping on this condition variable. The current
	 * thread must hold the associated lock.
	 */
	/* make sure the thread has the lock. then we disable all the interrupts. we then use a for loop to
	 * go through the queue and ready the threads to wake it up. we then restore the status and enable the
	 * interrupts
	 */
	public void wakeAll() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		boolean intStatus = Machine.interrupt().disable(); // disables the interrupts 
		//while (!waitQueue.isEmpty()){ // loop it .. didnt work
			//wake(); // pop each one}
		for (KThread Thread = waitQueue2.nextThread(); Thread!= null; waitQueue2.nextThread()){ //use for loop instead of while.. see if it works
			Thread.ready(); // use a for loop to go to each thread and wake it up.. instead of a while.. see if this works
		}
		Machine.interrupt().restore(intStatus);
	}
	
	
	
	
	
	
	/*
	 * testing condition2.
	 */
	
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
	
	public static LinkedList<KThread> createThread(int number, int special){
		final Lock lock = new Lock();
		final Condition2 condition = new Condition2(lock);
		LinkedList<KThread> list = new LinkedList<KThread>();
	
		if(special!=0){
			list.add(new KThread(new Runnable() {
				public void run() {
					lock.acquire();
					for(int i = 0; i<2; i++){
					System.out.println( KThread.currentThread().getName() + " IS GOING TO SLEEP" );
					condition.wake();}
					
				}
			}));
			list.add(new KThread(new Runnable() {
				public void run() {	
					lock.acquire();
					//System.out.println(KThread.currentThread().getName()+" IS NOT SUPPOSE TO RUN YET UNTIL A IS DONE");
					for(int i = 0; i<2; i++){

						System.out.println(KThread.currentThread().getName()+" said: waking up!");
						condition.sleep();
					}//when exited it is finished
					//System.out.println(KThread.currentThread().getName()+" said: I AM DONE. B RESUMES HERE ");
				}

			}));
			list.add(new KThread(new Runnable() {	 //when checking if thread c wakes all isnt called.. stuck =/
				public void run() {
					lock.acquire();
					System.out.println( KThread.currentThread().getName() + " waking all!!!" );
					condition.wakeAll();
					
				}
			}));


		}

		return list;
	}
	
	

	public static void condSelfTest(){
		Lib.debug(dbgThread, "Enter PriorityQueue.selfTest");
		//Created two threads with its runnable being printing things.
		LinkedList<KThread> threadList;
		//threadList = createThread(2,0); 	//creates two regular thread and run it
		//testMe(threadList,2,7,0);
		threadList= createThread(1,1);						//creates three thread-1 regular and 2 special
		testMe(threadList,7,3,2);
	}
	
	
	




	//private static KThread currentThread = null; .. no need for this
	//private LinkedList<KThread> waitQueue = new LinkedList<KThread>(); // only going to use one queue... see if this works
	private ThreadQueue waitQueue2 = ThreadedKernel.scheduler.newThreadQueue(false); // make a new wait queue so the thread doesn't loose all its data															
	private Lock conditionLock;
	
	
	
}
