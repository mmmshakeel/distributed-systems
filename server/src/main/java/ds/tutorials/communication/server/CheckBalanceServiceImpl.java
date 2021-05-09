package ds.tutorials.communication.server;

import ds.tutorial.communication.grpc.generated.CheckBalanceRequest;
import ds.tutorial.communication.grpc.generated.CheckBalanceServiceGrpc;
import ds.tutorial.communication.grpc.generated.CheckBalanceResponse;
import io.grpc.stub.StreamObserver;

import java.util.Random;

public class CheckBalanceServiceImpl extends CheckBalanceServiceGrpc.CheckBalanceServiceImplBase {

    private BankServer server;

    public CheckBalanceServiceImpl(BankServer server) {
        this.server = server;
    }

    private double getAccountBalance(String accountId) {
        return server.getAccountBalance(accountId);
    }

    @Override
    public void checkBalance(CheckBalanceRequest request, StreamObserver<CheckBalanceResponse> responseObserver) {
        String accountId = request.getAccountId();
        System.out.println("Request received..");
        double balance = getAccountBalance(accountId);
        CheckBalanceResponse response = CheckBalanceResponse
                .newBuilder()
                .setBalance(balance)
                .build();

        System.out.println("Responding, balance for account " + accountId + " is " + balance);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }


}
