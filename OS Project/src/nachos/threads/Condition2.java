package nachos.threads;
//import java.util.LinkedList; no use for link list see if this works
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
	public void wake() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		boolean intStatus = Machine.interrupt().disable(); // the interrupted disable, to prevent the thread being changed
		//if (!waitQueue.isEmpty()){ //check and see if the thread queue is not empty 
			
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
	public void wakeAll() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		boolean intStatus = Machine.interrupt().disable(); // disables the interrupts 
		//while (!waitQueue.isEmpty()){ // loop it 
			//wake(); // pop each one}
		for (KThread Thread = waitQueue2.nextThread(); Thread!= null; waitQueue2.nextThread()){ //use for loop instead of while.. see if it works
			Thread.ready(); // use a for loop to go to each thread and wake it up.. instead of a while.. see if this works
		}
		Machine.interrupt().restore(intStatus);
	}
	//private static KThread currentThread = null;
	//private LinkedList<KThread> waitQueue = new LinkedList<KThread>(); // only going to use one queue see if this works
	private ThreadQueue waitQueue2 = ThreadedKernel.scheduler.newThreadQueue(false); // make a new wait queue so the thread doesn't loose all its data
	//private ThreadQueue waitQueue = ThreadedKernel.scheduler.newThreadQueue(false);
																				
	private Lock conditionLock;
}
