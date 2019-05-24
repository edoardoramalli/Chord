package node;

import exceptions.UnexpectedBehaviourException;

public class Hash {
    private static Hash hashInstance;
    private int dimFingerTable;

    private Hash(int dimFingerTable) {
        this.dimFingerTable = dimFingerTable;
    }

    static void initializeHash(int dimFingerTable) {
        if (hashInstance == null)
            hashInstance = new Hash(dimFingerTable);
        else
            throw new UnexpectedBehaviourException();
    }

    public static Hash getHash() {
        if (hashInstance == null)
            throw new UnexpectedBehaviourException();
        return hashInstance;
    }

    /**
     * Applies the hash function to calculate the nodeId starting from ipAddress and socketPort
     *
     * @param ipAddress  ipAddress of node
     * @param socketPort socketPort of node
     * @return the calculated hash correspondent to nodeId
     */
    public Long calculateHash(String ipAddress, int socketPort) {
        Long ipNumber = ipToLong(ipAddress) + socketPort;
        Long numberNodes = (long) Math.pow(2, dimFingerTable);
        return ipNumber % numberNodes;
    }

    /**
     * An IPv4 Address is composed by four blocks of number from 0 to 255. Each block is transformed into a long number
     * through the Hex notation.
     * and then
     *
     * @param ipAddress ipAddress of node
     * @return is returned the translation of the IPv4 notation into a long number.
     */
    private long ipToLong(String ipAddress) {
        String[] ipAddressInArray = ipAddress.split("\\.");
        long result = 0;
        for (int i = 0; i < ipAddressInArray.length; i++) {
            int power = 3 - i;
            int ip = Integer.parseInt(ipAddressInArray[i]);
            result += ip * Math.pow(256, power);
        }
        return result;
    }
}
