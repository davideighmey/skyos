package nachos.threads;
import java.util.LinkedList;

import nachos.ag.BoatGrader;
import nachos.machine.Machine;

public class Boat
{
	static BoatGrader bg;

	private static Lock lock = new Lock(); 	//declare lock
	private static boolean Loner = true;	//There will always be a loner, during school years. Kids...
	//private static Lock boatLock = new Lock(); //Boat Lock

	//private static Condition2 boat = new Condition2(boatLock); //The boat, will I need?
	private static Condition2 Ad = new Condition2(lock); 
	private static Condition2 Cd = new Condition2(lock);

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


	static Runnable runChild = new Runnable(){
		public void run(){
			ChildItinerary();
		}
	};
	static Runnable runAdult = new Runnable(){
		public void run(){
			AdultItinerary();
		}
	};




	public static void begin( int adults, int children, BoatGrader b )
	{
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		bg = b;
		// Instantiate global variables here
		//This is the boat of passings.
		//starts everything off at 0
		// Create threads here. See section 3.4 of the Nachos for Java
		// Walkthrough linked from the projects page.
		COnOahu = children;
		AOnOahu = adults;

		lock.acquire();		//get the lock
		for(int i = 0; i < children; i++){
			KThread child = new KThread(runChild);
			child.setName("Child " + i); 
			ChildrenOnOahu.add(child);
			child.fork();
		}
		for(int i = 0; i < adults; i++){
			KThread adult = new KThread(runAdult);
			adult.setName("Adult " + i);
			AdultsOnOahu.add(adult);
			adult.fork();
		}

		lock.release();
		/*
		while(COnOahu != 0){
			child[COnOahu].fork();
			child[COnOahu-1].fork();
			child[COnOahu].join();
			child[COnOahu-1].join();
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
			child[COnMolokai].fork();
			child[COnMolokai].join();
			COnOahu = COnOahu + 1;
			COnMolokai = COnMolokai - 1;
			child[COnOahu].fork();
			child[COnOahu-1].fork();
			child[COnOahu].join();
			child[COnOahu-1].join();
			COnOahu = COnOahu - 2;
			COnMolokai = COnMolokai + 2;
		}*/

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
		if(AOnOahu != AdultsOnOahu.size()-1){
			Machine.interrupt().disable();
			Ad.sleep();			//Assumed will put all adults to sleep as it is called.
			Machine.interrupt().enable();
		}
		else{
			while(!AdultsOnOahu.isEmpty()){
				//Adult to Molokai
				bg.AdultRideToMolokai();
				System.out.println("The Adult traveled to Molokai: " + AdultsOnOahu.getFirst().getName());
				AdultsOnMolokai.add(AdultsOnOahu.getFirst());
				AdultsOnOahu.removeFirst(); //Remove Adult from Oahu to Molokai

				//Child back to Oahu
				bg.ChildRowToOahu();	
				System.out.println("A Child traveling back to Oahu: " + ChildrenOnMolokai.getFirst().getName());
				ChildrenOnOahu.add(ChildrenOnMolokai.getFirst()); 
				ChildrenOnMolokai.removeFirst(); //Remove Child from Molokai to Oahu

				//Both Child back to Molokai
				bg.ChildRowToMolokai();
				bg.ChildRideToMolokai();
				System.out.print("Two Children traveling to Molokai: " + ChildrenOnOahu.getFirst().getName());
				ChildrenOnMolokai.add(ChildrenOnOahu.getFirst());
				ChildrenOnOahu.removeFirst();	//Remove First Child from Oahu to Molokai
				System.out.println(ChildrenOnOahu.getFirst().getName());
				ChildrenOnMolokai.add(ChildrenOnOahu.getFirst());
				ChildrenOnOahu.removeFirst(); //Remove Second Child from Oahu to Molokai

				//Child back to Oahu
				bg.ChildRowToOahu();	
				System.out.println("A Child traveling back to Oahu: " + ChildrenOnMolokai.getFirst().getName());
				ChildrenOnOahu.add(ChildrenOnMolokai.getFirst()); 
				ChildrenOnMolokai.removeFirst(); //Remove Child from Molokai to Oahu
			}
		}
	}

	static void ChildItinerary()
	{

		Machine.interrupt().disable();
		Cd.sleep();		//Assumed will put all children to sleep as it is called.
		Machine.interrupt().enable();
		if(COnOahu == ChildrenOnOahu.size()-1){
			lock.acquire();
			while(!ChildrenOnOahu.isEmpty()){
				if(ChildrenOnOahu.size()-1 == 1){
					Loner = true;
				}
				if(Loner == false){		//Check if there is a lonely kid
					//Both Child to Molokai
					bg.ChildRowToMolokai();
					bg.ChildRideToMolokai();
					System.out.print("Two Children traveling to Molokai: " + ChildrenOnOahu.getFirst().getName());
					ChildrenOnMolokai.add(ChildrenOnOahu.getFirst());
					ChildrenOnOahu.removeFirst();	//Remove First Child from Oahu to Molokai
					System.out.println(ChildrenOnOahu.getFirst().getName());
					ChildrenOnMolokai.add(ChildrenOnOahu.getFirst());
					ChildrenOnOahu.removeFirst(); //Remove Second Child from Oahu to Molokai

					//Child back to Oahu
					bg.ChildRowToOahu();	
					System.out.println("A Child traveling back to Oahu: " + ChildrenOnMolokai.getFirst().getName());
					ChildrenOnOahu.add(ChildrenOnMolokai.getFirst()); 
					ChildrenOnMolokai.removeFirst(); //Remove Child from Molokai to Oahu
				}
				else{
					System.out.println("Last kid on Oahu, became the loner");
					AdultItinerary();

					//Last child sent to Molokai
					System.out.println("Last kid on Oahu, was the loner. This child was so scared of a pedobear attack.");
					bg.ChildRowToMolokai();	
					System.out.println("A Child traveling back to Molokai: " + ChildrenOnOahu.getFirst().getName());
					ChildrenOnMolokai.add(ChildrenOnOahu.getFirst()); 
					ChildrenOnOahu.removeFirst(); //Remove Child from Oahu to Molokai

				}
			}
			lock.release();
		}




	}
	public static LinkedList<KThread> ChildrenOnOahu = new LinkedList<KThread>();
	public static LinkedList<KThread> ChildrenOnMolokai = new LinkedList<KThread>();

	public static LinkedList<KThread> AdultsOnOahu = new LinkedList<KThread>();
	public static LinkedList<KThread> AdultsOnMolokai = new LinkedList<KThread>();
	/*static void SampleItinerary()
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
	}*/

}
