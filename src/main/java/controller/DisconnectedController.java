package controller;

/**
 * Class used to substitute the real NodeControllerCommunicator, it has the same method but empty
 * in order to allow the correct behaviour of Node even if the Controller has been disconnected
 */
public class DisconnectedController implements ControllerInterface {
    @Override
    public void connected() {
        //do nothing
    }

    @Override
    public void stable() {
        //do nothing
    }

    @Override
    public void notStable() {
        //do nothing
    }

    @Override
    public void startLookup() {
        //do nothing
    }

    @Override
    public void endOfLookup() {
        //do nothing
    }

    @Override
    public void startInsertKey() {
        //do nothing
    }

    @Override
    public void endInsertKey() {
        //do nothing
    }

    @Override
    public void startFindKey() {
        //do nothing
    }

    @Override
    public void endFindKey() {
        //do nothing
    }
}
