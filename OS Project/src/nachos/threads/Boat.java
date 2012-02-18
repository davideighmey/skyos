package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
	static BoatGrader bg;

	private static Lock lock; 	//declare lock
	private static int COnOahu; //total children on Oahu
	private static int AOnOahu; //total adults on Oahu
	private static int COnMolokai; //total children on Molokai
	private static int AOnMolokai; //total adults on Molokai

	public static void selfTest()
	{
		BoatGrader b = new BoatGrader();

		System.out.println("\n ***Testing Boats with only 2 children***");
		begin(0, 2, b);

		//	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
		//  	begin(1, 2, b);

		//  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
		//  	begin(3, 3, b);
	}

	public static void begin( int adults, int children, BoatGrader b )
	{
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		bg = b;
		// Instantiate global variables here
		lock = new Lock(); //This is the boat of passings.
		//starts everything off at 0
		// Create threads here. See section 3.4 of the Nachos for Java
		// Walkthrough linked from the projects page.
		lock.acquire();		//get the lock
		
		COnOahu = children;
		AOnOahu = adults;
		COnMolokai = 0;
		AOnMolokai = 0;

		KThread child[] = new KThread[children];
		KThread adult[] = new KThread[adults];

		Runnable runChild = new Runnable(){
			public void run(){
				ChildItinerary();
			}
		};
		Runnable runAdult = new Runnable(){
			public void run(){
				AdultItinerary();
			}
		};
		
		for(int i = 0; i < adults; i++){
			adult[i] = new KThread(runAdult);
		}

		for(int i = 0; i < children; i++){
			child[i] = new KThread(runChild);
		}
		
		while(COnOahu != 0){
			child[COnOahu].fork();
			child[COnOahu].fork();
			child[COnOahu].join();
			child[COnOahu].join();
			COnOahu = COnOahu - 2;
			COnMolokai = COnMolokai + 2;
			child[COnMolokai].fork();
			child[COnMolokai].join();
			COnOahu = COnOahu + 1;
			COnMolokai = COnMolokai - 1;
		}
		while(AOnOahu !=0){
			adult[AOnOahu].fork();
			adult[AOnOahu].join();
			AOnOahu = AOnOahu - 1;
			
		}

		/*
		 * 
		KThread childrenRide = new KThread(new Runnable() {
			public void run() {
				System.out.println(KThread.currentThread().getName()+" has started their long journey!");
				lock.acquire();
				bg.ChildRowToMolokai();
				bg.ChildRideToMolokai();
				System.out.println(KThread.currentThread().getName()+" has finished their long journey!");
				lock.release();//releasing the lock
				KThread.yield();
			}
		});
		KThread childRide0 = new KThread(new Runnable() {
			public void run() {
				System.out.println(KThread.currentThread().getName()+" has started his long journey!");
				lock.acquire();
				bg.ChildRowToMolokai();
				System.out.println(KThread.currentThread().getName()+" has finished his long journey!");
				lock.release();//releasing the lock
				KThread.yield();
			}
		});
		KThread childRide = new KThread(new Runnable() {
			public void run() {
				System.out.println(KThread.currentThread().getName()+" has started his long journey!");
				lock.acquire();
				bg.ChildRowToOahu();
				System.out.println(KThread.currentThread().getName()+" has finished his long journey!");
				lock.release();//releasing the lock
				KThread.yield();
			}
		});
		KThread adultRow = new KThread(new Runnable() {
			public void run() {
				System.out.println(KThread.currentThread().getName()+" has started his long journey!");
				lock.acquire();
				bg.AdultRowToMolokai();
				System.out.println(KThread.currentThread().getName()+" has finished his long journey!");
				lock.release();//releasing the lock
				KThread.yield();
			}
		});
		childrenRide.setName("A Child rowing and a Child Riding to Molokai");
		childRide0.setName("Child Rowing to Molokai");
		childRide.setName("Child Rowing to Oahu");
		adultRow.setName("Adult Rowing to Molokai");
		 */

		//All of these are put onto the readyQueue running first come first serve.
		/*childRide0.fork();
		childrenRide.fork();
		childRide.fork();
		adultRow.fork();*/

		//childRide.fork();
		//childRide.join();
		//children++;
		//System.out.println("Amount of children left: "+children+" We purposely left a child on an island by himself with a bunch of pedobears!~");
	}
	/*
	Runnable r = new Runnable() {
	    public void run() {
                SampleItinerary();
            }
        };
        KThread t = new KThread(r);
        t.setName("Sample Boat Thread");
        t.fork();
	 */

	static void AdultItinerary()
	{
		/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
		 */
		lock.acquire(); //get the lock 
		bg.AdultRowToMolokai();
		lock.release();//releasing the lock
	}

	static void ChildItinerary()
	{
		lock.acquire(); // get the lock 

		if(COnOahu > 1){
			bg.ChildRowToMolokai();
			bg.ChildRideToMolokai();
			COnOahu = COnOahu - 2;
			bg.ChildRowToOahu();
			COnOahu = COnOahu - 1;
		}
		else{
			bg.ChildRideToMolokai();
			COnOahu = COnOahu - 1;
		}
		lock.release(); // release the lock
	}

	static void SampleItinerary()
	{
		// Please note that this isn't a valid solution (you can't fit
		// all of them on the boat). Please also note that you may not
		// have a single thread calculate a solution and then just play
		// it back at the autograder -- you will be caught.
		System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
		bg.AdultRowToMolokai();
		bg.ChildRideToMolokai();
		bg.AdultRideToMolokai();
		bg.ChildRideToMolokai();
	}

}
