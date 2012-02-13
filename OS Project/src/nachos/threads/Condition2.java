package nachos.threads;
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
    waitQueue = new LinkedList();

    }
    

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());

	conditionLock.release(); // releasing the lock
	boolean intStatus = Machine.interrupt().disable();//the interrupt disable(), to deal with thread
    waitQueue2.waitForAccess(KThread.currentThread()); //the thread queue inside current thread mention 

	currentThread.sleep();//puts current thread to sleep
    Machine.interrupt().restore(intStatus); //Interrupt enabled & restore to last status
    

	conditionLock.acquire();
    }
    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
	   if (!waitQueue.isEmpty()){ //check and see if the thread queue is not empty 
	        boolean intStatus = Machine.interrupt().disable(); // the interrupted disable, to prevent the thread is changed
	        currentThread.ready(); //ready the thread
	  
	        waitQueue.removeFirst(); //Grab the first sleeping thread on the list (FIFO Queue)
	        Machine.interrupt().restore(intStatus); //enable the interrupt
	    }
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
	  while (!waitQueue.isEmpty()){ // loop it 
		    wake();} // pop each one
      

    }
    private static KThread currentThread = null;
    private LinkedList waitQueue;
    private ThreadQueue waitQueue2 = ThreadedKernel.scheduler.newThreadQueue(false);
    private Lock conditionLock;
}
