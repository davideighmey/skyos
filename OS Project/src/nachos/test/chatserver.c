
//
//  chatserver.c - Creating a sever that a chat client can connect to.
//  


#include "stdio.h"
#include "syscall.h"
#include "stdlib.h"

int clients[14]; // initializing chat clients
char buffer[16][255];
int clientLoc[14];

void writeFunc(int sender,char* msg)
{
    int i;
	for(i = 0; i < 14; i++)
	{
		if(clients[i] != -1 && i != sender)
		{
			write(clients[i],msg,strlen(msg)+1);
		}
	}	
}

int main (int argc, char *argv[]){
    int i = 0;
    
    for(i = 0; i < 14; i++){
        clients[i] = -1; //making all spots in sever -1 initally.
    }
    
    while(1){
        int newSocket = accept(15) //accept port 15
        if (newSocket > 0)
        {
            //finding a new client opening
            for (i = 0; i<14; i++){ 
                if(clients[i] == -1){ //spot open
                    clients[i] = NewSocket;//spot in server found
                    clientLoc[i] = 0;
                    break;//break out of the for-loop
                }
            }
            
            for(i = 0; i<14; i++){
                if(clients != -1){
                    int message = read(clients[i], &(buffer[i][clientLoc[i]], 1)// read message 
                    clientBufLoc[i]+=b;
                    if(message >0){
                        if(buffer[i][clientLoc[i]-1] == 0){
                            writeFunc(i,clientMsgBuf[i]); //write the message
                            clientLoc[i] = 0; //make locaton
                        }
                    }
                    else{
                        clients[i] == -1;
                    }   
                }
            }
        }   
    }
    return 0;
}