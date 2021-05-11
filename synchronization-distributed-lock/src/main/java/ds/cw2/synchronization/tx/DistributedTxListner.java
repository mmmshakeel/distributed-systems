package ds.cw2.synchronization.tx;

public interface DistributedTxListner {

    void onGlobalCommit();
    void onGlobalAbort();

}
