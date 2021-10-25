package client;


import cd.group12.grpcchat.grpcchatContract.ChatMessage;
import io.grpc.stub.StreamObserver;

public class ChatClientObserver implements StreamObserver<ChatMessage> {
    @Override
    public void onNext(ChatMessage chatMessage) {
        System.out.println("[sender: " + chatMessage.getFromUser() + "] " + chatMessage.getTxtMsg());
    }

    @Override
    public void onError(Throwable throwable) {
        // TODO
        System.out.println("onError!!"+throwable.getMessage());
    }

    @Override
    public void onCompleted() {
        // TODO
    }
}
