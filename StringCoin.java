import blockchain.Blockchain;
import blockchain.HashPointer;
import public_key_crypto.PublicKeyDemo;

import java.io.File;
import java.io.IOException;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Scanner;

public class StringCoin {

    private static PublicKey pubKeyBill;
    private static HashMap<String, PublicKey> coins = new HashMap<>();

    public static void main(String[] args) throws Exception {
        // Confirm arguments are valid
        if (args.length == 0) {
            System.out.println(">>> ERROR: Please specify a valid blockchain file <<<");
            System.exit(0);
        }

        // Save Bill's pk
        pubKeyBill = PublicKeyDemo.loadPublicKey(HashPointer.convertBytesToHexString("3081f03081a806072a8648ce38040130819c024100fca682ce8e12caba26efccf7110e526db078b05edecbcd1eb4a208f3ae1617ae01f35b91a47e6df63413c5e12ed0899bcd132acd50d99151bdc43ee737592e17021500962eddcc369cba8ebb260ee6b6a126d9346e38c50240678471b27a9cf44ee91a49c5147db1a9aaf244f05a434d6486931d2d14271b9e35030b71fd73da179069b32e2935630e1c2062354d0da20a6c416e50be794ca403430002405b0656317dd257ec71982519d38b42c02621290656eba54c955704e9b5d606062ec663bdeef8b79daa2631287d854da77c05d3e178c101b2f0a1dbbe5c7d5e10".getBytes()));

        // Create Blockchain
        Blockchain b = populateBlockchain(readFileContents(args[0]));
        if (b == null) System.exit(0);
        else b.iterateAndVerify();
    }

    /**
     *
     * @param s the Scanner object containing block data
     * @return the Blockchain object
     */
    private static Blockchain populateBlockchain(Scanner s) throws Exception {
        Blockchain blockchain = new Blockchain();
        while (s.hasNext()) {
            String data = s.nextLine();
            String[] blockData = data.split(",");
            blockData[0] = HashPointer.convertBytesToHexString(blockData[0].getBytes());
            blockData[3] = HashPointer.convertBytesToHexString(blockData[3].getBytes());
            blockData[4] = HashPointer.convertBytesToHexString(blockData[4].getBytes());
            boolean valid = verifyBlock(blockData, blockchain);
            if (valid) {
                blockchain.addBlock(data);
                PublicKey pk = (blockData[1].equals("CREATE")) ? pubKeyBill : PublicKeyDemo.loadPublicKey(HashPointer.convertBytesToHexString(blockData[3].getBytes()));
                coins.put(blockData[2], pk);
            }
            else return null;
        }
        return blockchain;
    }

    /**
     *
     * @param filename the filename to read
     * @return the Scanner object to read file contents with
     * @throws IOException if the file could not be found
     */
    private static Scanner readFileContents(String filename) throws IOException {
        try {
            return new Scanner(new File(filename));
        } catch (IOException e) {
            System.out.println(">>> ERROR: " + filename + " not found <<<");
            System.exit(0);
            throw e;
        }
    }

    /**
     *
     * @param newBlockData the data and metadata for the block being created
     * @return true if the block is valid, false otherwise
     */
    private static boolean verifyBlock(String[] newBlockData, Blockchain blockchain) {
        if (!newBlockData[0].equals(blockchain.getCurHash())) {
            System.out.println(">>> ERROR: HashPointer hash for block " + blockchain.getSize()+1 + " is invalid <<<");
            return false;
        }

        switch (newBlockData[1]) {
            case "CREATE":
                return verifyCreationBlock(newBlockData, blockchain);
            case "TRANSFER":
                return verifyTransferBlock(newBlockData, blockchain);
            default:
                System.out.println(">>> ERROR: " + newBlockData[1] + " not a valid transaction type <<<");
                return false;
        }
    }

    /**
     *
     * @param newBlockData the data and metadata for the block being created
     * @return true if the block is valid, false otherwise
     */
    private static boolean verifyCreationBlock(String[] newBlockData, Blockchain blockchain) {
        String blockContents = newBlockData[0] + "," + newBlockData[1] + "," + newBlockData[2] + "," + newBlockData[3];
        if (!verifySignature(newBlockData[2], newBlockData[3], pubKeyBill)) {
            System.out.println(">>> ERROR: Coin signature for block " + blockchain.getSize()+1 + " is invalid <<<");
            return false;
        }
        else if (!verifySignature(blockContents, newBlockData[4], pubKeyBill)) {
            System.out.println(">>> ERROR: Block signature for block " + blockchain.getSize()+1 + " is invalid <<<");
            return false;
        }
        return true;
    }

    /**
     *
     * @param newBlockData the data and metadata for the block being created
     * @return true if the block is valid, false otherwise
     */
    private static boolean verifyTransferBlock(String[] newBlockData, Blockchain blockchain) {
        String blockContents = newBlockData[0] + "," + newBlockData[1] + "," + newBlockData[2] + "," + newBlockData[3];
        if (!coins.containsKey(newBlockData[2])) {
            System.out.println(">>> ERROR: Coin " + newBlockData[2] + " in block " + blockchain.getSize()+1 +
                    " was never created <<<");
            return false;
        }
        else if (!verifySignature(blockContents, newBlockData[4], coins.get(newBlockData[2]))) {
            System.out.println(">>> ERROR: Block signature for block " + blockchain.getSize()+1 + " is invalid <<<");
            return false;
        }
        return true;
    }

    /**
     *
     * @param data the original data
     * @param signedData the signed data
     * @param pubKey the public key to verify the signed data with
     * @return true if the signature was valid, false otherwise
     */
    private static boolean verifySignature(String data, String signedData, PublicKey pubKey) {
        try {
            return PublicKeyDemo.verify(data, signedData.getBytes(), pubKey);
        } catch (Exception e) {
            System.out.println(">>> ERROR: Could not verify signature for " + data + " <<<");
            return false;
        }
    }
}