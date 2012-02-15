package nachos.threads;

import nachos.machine.*;
import java.util.Queue; // for sleeping threads

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
	private Lock mutex;
	Condition listenerArrived;
	Condition speakArrived;
	
	// I created these 
	//Queue<KThread> speakers; // 
	//Queue<Integer> toTransfer; // global variable to transfer word from speak to listen
	//Queue<KThread> asleep; // queue to hold the sleeping threads
	Condition speaking;	// is there a speaker speaking??
	Condition listening;// is there a listener to listening??
	int toTransfer; // to store the word that should be transfered	
	boolean lockSet = false;// has the lock been set by speak?
	
	
    public Communicator() {
    	mutex = new Lock();
    	listenerArrived = new Condition(mutex);
    	speakArrived = new Condition(mutex);
    	// initialize condition variables that were created
    	this.speaking = new Condition(this.mutex);
    	this.listening = new Condition(this.mutex);
    	
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word)
    {
    	this.mutex.acquire(); // have the current thread get the lock
    	while(lockSet == false)
    	{
    		this.speaking.sleep(); // set the current thread to sleep
    	}
    	this.toTransfer = word; // store the word to transfer
    	this.lockSet = false; // 
    	this.listening.wake(); // wake or wakeAll ?
    	this.mutex.release(); // release the lock 
    	
    	/*
    	//!!!! make a queue that holds multiple speakers
    	speakers.add(mutex.lockHolder); // add the thread to the speakers queue
    	
    	if(!mutex.isHeldByCurrentThread()) // if thread does not already have the lock
    		mutex.acquire(); // get the lock
    	
    	this.lockSet = true; // signal that speak has been called
    	this.toTransfer.add(word); // set global variable = word
    	
    	this.speakArrived.notify();// send signal to wake listening thread if it is asleep
    	mutex.release(); //release lock
    	 */
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() 
    {
    	//int word; // the transfered word that will be returned
    	this.mutex.acquire(); // have this thread get the lock
    	while(lockSet == true) // 
    	{
    		this.listening.sleep(); // have it wait for a speaker --sleep
    	}
    	//word = this.toTransfer; // transfered word
    	this.lockSet = false;
    	this.speaking.wake(); // wake or wakeAll??
    	this.mutex.release(); // release the lock before returning word
    	return (this.toTransfer);
    	/*
    	// check the queue that has the speakers and take from the top of the queue
    	
    	mutex.acquire(); //get lock    	
    	while(!this.lockSet)
    	{
    		this.listenerArrived.sleep(); //put thread to sleep
    	}
    	mutex.acquire(); // get lock when thread wakes up
    	this.lockSet = false; // reset lock for next threads
    	speakers.poll();
    	return (this.toTransfer.poll());
    	*/
    	//return (this.transferWords.poll());
    	/*
    	if(this.lockSet) // thread has lock so speak has been called
    	{
    		mutex.release(); // release the lock before we return the word
    		this.lockSet = false; // reset for next threads 
    		//System.out.print("listen called");
    		return (this.toTransfer);    		
    	}
    	else	// lock not set yet, have to wait for speak to be called
    	{
    			this.listenerArrived.sleep();// add thread to asleep queue
    		
    	} */
	//return 0;
    }

}
