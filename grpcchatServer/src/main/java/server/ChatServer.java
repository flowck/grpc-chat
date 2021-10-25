package server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.google.protobuf.Empty;

import cd.group12.grpcchat.grpcchatContract.ChatGrpc.ChatImplBase;
import cd.group12.grpcchat.grpcchatContract.ChatMessage;
import cd.group12.grpcchat.grpcchatContract.UserID;
import cd.group12.grpcchat.grpcchatContract.Users;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;

public class ChatServer extends ChatImplBase {

    private static final Logger logger = Logger.getLogger(ChatServer.class.getName());
    static int serverPort = 9000;

    public static void main(String[] args) {
        try {
            if (args.length == 1) serverPort = Integer.parseInt(args[0]);
            final Server svc = ServerBuilder.forPort(serverPort)
                    .addService(new ChatServer())
                    .build()
                    .start();
            logger.info("Server started, listening on " + serverPort);

            System.err.println("*** server await termination");
            svc.awaitTermination();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final ConcurrentHashMap<UserID, StreamObserver<ChatMessage>> clients;


    public ChatServer() {
        clients = new ConcurrentHashMap<UserID, StreamObserver<ChatMessage>>();
    }
    
    

	@Override
	public void getActiveUsers(Empty request, StreamObserver<Users> responseObserver) {
      // Add all users to an array list
      List<UserID> users = new ArrayList<>(clients.keySet());

      // Build a new Users object
      Users usersReply = Users.newBuilder().addAllUsers(users).build();

      // Send a response to the client
      responseObserver.onNext(usersReply);
      responseObserver.onCompleted();
	}

    @Override
    public void exitChat(UserID userID, StreamObserver<Empty> responseObserver) {
        clients.remove(userID);
        System.out.println("User " + userID.getName() + " has been removed");
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void sendMessage(ChatMessage inMessage, StreamObserver<Empty> responseObserver) {
        for (UserID clientDest : clients.keySet()) {
            try {
                ChatMessage outMessage = ChatMessage.newBuilder()
                    .setFromUser(inMessage.getFromUser())
                    .setTxtMsg(inMessage.getTxtMsg()).build();
                clients.get(clientDest).onNext(outMessage);
            } catch (Throwable ex) {
                // error calling remote client, remove client name and callback
                System.out.println("Client " + clientDest.getName() + " removed");
                clients.remove(clientDest);
            }
        }
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void register(UserID clientID, StreamObserver<ChatMessage> responseObserver) {
        synchronized (clients) {
            if (!clients.containsKey(clientID))
                clients.put(clientID, responseObserver);
            else {
                System.out.println("Client " + clientID.getName() + " already taken");
                Throwable t = new StatusException(
                    Status.INVALID_ARGUMENT.withDescription("Client nickname already taken")
                );
                responseObserver.onError(t);
            }
        }
    }
}
