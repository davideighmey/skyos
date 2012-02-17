package nachos.threads;

import nachos.machine.*;
//import java.util.Queue; // for sleeping threads -- didn't need used condition variables

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

	Condition wordReady;
	Condition noWord;
	Condition noListener;
	Condition noSpeaker;
	private int toTransfer; // holds the word that needs to be transfered
	boolean speaker; // is there a speaker?
	boolean listener;// is there a listener?
	boolean wordThere; // is there a word there?

	public Communicator() {
		mutex = new Lock();
		listenerArrived = new Condition(mutex);
		speakArrived = new Condition(mutex);
		
		
		this.wordReady = new Condition(mutex);
		this.noWord = new Condition(mutex);
		this.noListener = new Condition(mutex);
		this.noSpeaker = new Condition(mutex);
		this.speaker = false; // start off with no speaker
		this.listener = false;// start off with no listener
		this.wordThere = false;// start off with no word there 
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
		this.mutex.acquire(); // get the lock
		
		while(speaker == true) // while there is no speaker
			this.noSpeaker.sleep(); // set condition to sleep until there is a speaker
		
		this.speaker = true; // there is now a speaker
		
		while(listener == false) // while there is no listener
			this.listenerArrived.sleep(); // sleep on this condition
		
		this.toTransfer = word; // save the word to a global variable
		this.wordThere = true; // there is now a word available
		
		this.wordReady.wake(); // wake the listeners
		
		while(wordThere == true)
			this.noWord.sleep(); // 
		
		this.speaker = false;
		this.noSpeaker.wake();
		
		this.mutex.release(); // release the lock before return ending
	}
	/*public void speak(int word)
	{
		this.mutex.acquire(); // get the lock
		this.speaker = true; // there is now a speaker
		
		while(listener == false) // while there is nothing to get
		{
			this.speakArrived.sleep(); // put the thread to sleep
		}
		this.listener = false; //
		this.toTransfer = word; // store the word to a global variable
		this.wordThere = true;
		this.listenerArrived.wake(); // wake all before releasing
		this.mutex.release(); // now release the lock before returning
	}
	*/
	/**
	 * Wait for a thread to speak through this communicator, and then return
	 * the <i>word</i> that thread passed to <tt>speak()</tt>.
	 *
	 * @return	the integer transferred.
	 */
	public int listen()
	{
		this.mutex.acquire(); // get the lock for this critical section
		
		while(listener == true)
			this.noListener.sleep();
		
		this.listener = true; // 
		this.listenerArrived.wake();//
		
		while(wordThere == false)
			this.wordReady.sleep();
		
		int toReturn = this.toTransfer; // word to return from global variable
		this.wordThere = false; // the word there is no longer valid
		
		this.noWord.wake();
		this.listener = false; // there will not be a listener
		this.noListener.wake();
		
		this.mutex.release(); // let go of the lock before returning
		
		return (toReturn);		
	}
	
	/*
	public int listen() 
	{
			this.mutex.acquire(); // get the lock
			this.listener = true; // there is now a listener
			
			while(speaker == false) // if no speaker wait for the speaker
			{
				this.listenerArrived.sleep();// put to sleep while waiting
			}
			
			this.speaker = false;
			int word = this.toTransfer;
			this.wordThere = false;
			this.speakArrived.wake(); // wake all or just one ?			
			this.mutex.release(); // release lock before returning the word
			
			return (word);
		}
	*/
}
