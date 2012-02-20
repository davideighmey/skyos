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
	Condition listenerArrived; // condition for when there IS a listener
	Condition speakArrived; // condition for when there IS a speaker

	Condition wordReady; // condition for when there IS a word available
	Condition noWord;  // condition for when there is NO word available
	Condition noListener; // condition for when there is NO listener yet
	Condition noSpeaker; // condition for when there is NO speaker yet
	private int toTransfer; // holds the word that needs to be transfered
	boolean speaker; // is there a speaker?
	boolean listener;// is there a listener?
	boolean wordThere; // is there a word there?

	public Communicator() {
		mutex = new Lock();
		listenerArrived = new Condition(mutex);
		speakArrived = new Condition(mutex);
		
		// all condition variables use the same mutex(lock) resources
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
	
	/*
	 * In this speaker method.
	 * Get the lock. The speaker first make sure that there is not already another speaker 
	 * thread in its critical section by checking the speaker boolean, if another speaker is
	 * active then this speaker will go to sleep on the noSpeaker condition until the
	 * other speaker thread is done.	 If this is the only speaker thread it will set the
	 * speaker boolean to true so that others know that this speaker thread is active, so
	 * they wait for this speaker to finish. 	Now this speaker thread will wait till there
	 * is a listening thread active, and if there is not it will go to sleep until a listening
	 * thread arrives.	After the listening thread is there we store the word to transfer to 
	 * the global variable this.toTransfer so the other thread can access it, and set the 
	 * boolean flag this.wordThere to true so that the listening thread knows that there is a
	 * valid word in this.toTransfer that it can return. 	This speaker thread now waits till
	 * the listening thread has gotten the word in toTransfer and resets the wordThere flag to
	 * false. 	This thread now sets this.speaker to false so that other speaker threads know
	 * this speaker will be finished shortly, and we wake any thread that was waiting on the
	 * this.noSpeaker condition. 	Finally we make sure that the lock is released before the
	 * speaker method ends.
	 */
	
	public void speak(int word)
	{
		this.mutex.acquire(); // get the lock 
		
		while(speaker == true) // while there is a speaker
			this.noSpeaker.sleep(); // set condition to sleep until there is no speaker
		
		this.speaker = true; // there is now a speaker 
		
		while(listener == false) // while there is no listener
			this.listenerArrived.sleep(); // sleep on this condition (there is a listener condition)
		
		this.toTransfer = word; // save the word to a global variable
		this.wordThere = true; // there is now a word available
		
		this.wordReady.wake(); // wake the anyone waiting on the wordReady condition
		
		while(wordThere == true) // if there is a word there then
			this.noWord.sleep(); // sleep the no word condition cause there IS a word
		
		this.speaker = false; // speaker is about to exit so there will be no speaker
		this.noSpeaker.wake(); // wake anyone waiting on the no speaker condition, speaker about to leave
		
		this.mutex.release(); // release the lock before return ending
	}
	// This part words for the test cases 2a and 2b but not for 2c (with multiple words)
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
	
	/*
	 * In this listen method
	 * Firstly acquire the lock. Now make sure that there are no other listening
	 * threads in their critical section by checking that the listener boolean 
	 * flag is false if it is true then there is another listener thread in its
	 * critical section so this listening thread should go to sleep waiting for 
	 * the other listening thread to finish.	If there is not listener in its
	 * critical section then set let this listener thread enter and set the listener
	 * flag to true. And wake anyone waiting on the listenerArrived condition.
	 * Then this listening thread checks to see if there is a word available by 
	 * checking the wordThere boolean if it is false then sleep and wait for a 
	 * word to become available (wordThere == true). Once a word is available
	 * then store the word in a local variable that will be returned (toReturn)
	 * Make sure to set the wordThere to false because the word in the global 
	 * variable will no longer be available, already used.	Now wake anyone waiting
	 * on the noWord condition.		Release the lock before returning the word
	 * that was stored in the toReturn local variable.
	 */
	
	public int listen()
	{
		this.mutex.acquire(); // get the lock for this critical section
		
		while(listener == true) // if there is a listener
			this.noListener.sleep(); // then set the no listener condition to sleep, because there is a listener
		
		this.listener = true; // there is now a listener (true)
		this.listenerArrived.wake();// wake anyone waiting on the listener here condition
		
		while(wordThere == false) // if a word is not available 
			this.wordReady.sleep(); // then sleep on this condition until a word is available 
		
		int toReturn = this.toTransfer; // store word to return from global variable
		this.wordThere = false; // the word there is no longer valid
		
		this.noWord.wake(); // wake anyone waking on the no word available condition
		this.listener = false; // there will not be a listener
		this.noListener.wake(); // the is no longer going to be a listener
		
		this.mutex.release(); // let go of the lock before returning
		
		return (toReturn);		
	}
	//test communicator class with CommunicatorTest
	public static void test()
	{
		CommunicatorTest.runTest();
	}
	//test
	public static void selfTest()
	{
		System.out.println("Starting Test Case for Communicator!!");
		//Create threads
		KThread loud = new KThread(new Runnable(){
			public void run(){
				System.out.println("Loud guy thread created");
				
			}
		});
		KThread quite = new KThread(new Runnable(){
			public void run(){
				System.out.println("Quite guy thread created");
			}
		});
		
		// create communicator
		Communicator com = new Communicator();
		int sent = 4444;
		System.out.println("Sent " + sent);
		com.speak(sent);
		int recieved = com.listen();
		System.out.println("Recieved: " + recieved);		
	}
	
	// This part words with the test cases 2a and 2b but not with 2c (multiple words)
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

	/** to test communicator */
	/**
	static Runnable runSpk = new Runnable()
	{
		public void run()
		{
			System.out.println("Spk Thread created and about to call speak() with 4x4");
			speak(4444);
			System.out.println("Speak done");
		}
	};
	
	static Runnable runLst = new Runnable()
	{
		public void run()
		{
			System.out.println("lst thread created and going to call listen should return 4x4");
			int word1 = listen();
			System.out.println("listened returned: " + word1);
		}
	};
	
	public static void comTest()
	{
		//Lib.debug('t',"Entered Communicator Test");
		KThread spk = new KThread(runSpk);
		KThread lst = new KThread(runLst);
		//Lib.debug('t',"spk thread and lst thread created");
		
	}
	*/
}
