package community.solace.spring.boot.starter.solaceclientconfig;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Map;
import java.util.Objects;

public class KeyPairChecker {
    private static final Map<String, String> SIGNATURE_ALGORITHMS = Map.of(
            "DSA", "SHA256WithDSA",
            "RSA", "SHA256WithRSA",
            "EC", "SHA256WithECDSA"
    );

    public static boolean isKeyPair(PrivateKey privateKey, PublicKey publicKey) {
        if (privateKey == null || publicKey == null) {
            return false;
        }

        if (!Objects.equals(privateKey.getAlgorithm(), publicKey.getAlgorithm())) {
            return false;
        }

        try {
            var signature = Signature.getInstance(SIGNATURE_ALGORITHMS.get(privateKey.getAlgorithm()));
            signature.initSign(privateKey);
            var signed = signature.sign();
            signature.initVerify(publicKey);
            return signature.verify(signed);
        } catch (Exception ignored) {
            return false;
        }
    }
}
