package client;

import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import cd.group12.grpcchat.grpcchatContract.ChatGrpc;
import cd.group12.grpcchat.grpcchatContract.ChatMessage;
import cd.group12.grpcchat.grpcchatContract.UserID;
import cd.group12.grpcchat.grpcchatContract.Users;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;



public class ChatClient {	
    public static final Logger logger = Logger.getLogger(ChatClient.class.getName());

    private static String serverIP = "localhost";
    private static int serverPort = 9000;

    public static void main(String[] args) throws Exception {
        ManagedChannel channel=null;
        try {
            // Setup Channel to Server
            channel = ManagedChannelBuilder.forAddress(serverIP, serverPort)
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext()
                .build();
            ChatGrpc.ChatStub nonBlockingStub = ChatGrpc.newStub(channel);
            ChatGrpc.ChatBlockingStub blockingStub = ChatGrpc.newBlockingStub(channel);

            // Get available users
            Users users = blockingStub.getActiveUsers(Empty.newBuilder().build());
            System.out.println("List of active users in the server: ");
            users.getUsersList().stream().forEach((user) -> {
                System.out.println(user.getName());
            });

            Scanner input = new Scanner(System.in);
            System.out.println("\n");
            System.out.print("Enter your nickName: ");
            String clientName = input.nextLine();

            // register client in remote server
            UserID userID = UserID.newBuilder().setName(clientName).build();
            nonBlockingStub.register(userID, new ChatClientObserver());
            // send messages
            System.out.println("Enter lines or the word \"exit\"");
            while (true) {
                String line = input.nextLine();

                if (line.equals("exit")) {
                    blockingStub.exitChat(userID);
                    System.out.println("You have exited the chat successfully");
                };

                blockingStub.sendMessage(ChatMessage.newBuilder()
                    .setFromUser(clientName)
                    .setTxtMsg(line).build());
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error:" + ex.getMessage());
        }
        if (channel!=null) {
            logger.log(Level.INFO, "Shutdown channel to server ");
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}

