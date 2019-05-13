package controller;

import controller.message.*;

import java.io.IOException;
import java.util.HashMap;

public class NodeStatisticsController implements NodeMessageHandler {
    private Long nodeId;
    private SocketNodeStatistics controller;
    private volatile HashMap<Long, Object> lockList = new HashMap<>();
    private volatile Long lockID = 0L;

    NodeStatisticsController(Long nodeId, SocketNodeStatistics controller) {
        this.nodeId = nodeId;
        this.controller = controller;
    }

    /**
     * Creates the lock object
     * @return lockId corresponding to the created lock object
     */
    private synchronized Long createLock(){
        lockList.put(lockID, new Object());
        lockID = lockID + 1;
        return lockID - 1;
    }

    public void connected() throws IOException {
        Long lockId = createLock();
        synchronized (lockList.get(lockId)) {
            controller.sendMessage(new ConnectedMessage(nodeId, lockId));
            try {
                lockList.get(lockId).wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void stable() throws IOException {
        Long lockId = createLock();
        synchronized (lockList.get(lockId)) {
            controller.sendMessage(new StableMessage(lockId));
            try {
                lockList.get(lockId).wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void notStable() throws IOException {
        Long lockId = createLock();
        synchronized (lockList.get(lockId)) {
            controller.sendMessage(new NotStableMessage(lockId));
            try {
                lockList.get(lockId).wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void startLookup() throws IOException {
        Long lockId = createLock();
        synchronized (lockList.get(lockId)) {
            controller.sendMessage(new StartLookupMessage(lockId));
            try {
                lockList.get(lockId).wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void endOfLookup() throws IOException {
        Long lockId = createLock();
        synchronized (lockList.get(lockId)) {
            controller.sendMessage(new EndOfLookupMessage(lockId));
            try {
                lockList.get(lockId).wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void startInsertKey() throws IOException {
        Long lockId = createLock();
        synchronized (lockList.get(lockId)) {
            controller.sendMessage(new StartInsertKeyMessage(lockId));
            try {
                lockList.get(lockId).wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void endInsertKey() throws IOException {
        Long lockId = createLock();
        synchronized (lockList.get(lockId)) {
            controller.sendMessage(new EndInsertKeyMessage(lockId));
            try {
                lockList.get(lockId).wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void startFindKey() throws IOException {
        Long lockId = createLock();
        synchronized (lockList.get(lockId)) {
            controller.sendMessage(new StartFindKeyMessage(lockId));
            try {
                lockList.get(lockId).wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void endFindKey() throws IOException {
        Long lockId = createLock();
        synchronized (lockList.get(lockId)) {
            controller.sendMessage(new EndFindKeyMessage(lockId));
            try {
                lockList.get(lockId).wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    @Override
    public void handle(ReceivedMessage receivedMessage) throws IOException {
        synchronized (lockList.get(receivedMessage.getLockId())){
            lockList.get(receivedMessage.getLockId()).notifyAll();
        }
    }
}