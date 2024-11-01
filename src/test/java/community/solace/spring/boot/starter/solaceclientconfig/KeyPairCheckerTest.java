package community.solace.spring.boot.starter.solaceclientconfig;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import static community.solace.spring.boot.starter.solaceclientconfig.KeyPairChecker.isKeyPair;
import static org.junit.jupiter.api.Assertions.*;

class KeyPairCheckerTest {

    private KeyPair createKeyPair(String algo, int size) throws NoSuchAlgorithmException {
        var kpg = KeyPairGenerator.getInstance(algo);
        kpg.initialize(size);
        return kpg.generateKeyPair();
    }

    @Test
    void dsa_match() throws Exception {
        var pair = createKeyPair("DSA", 1024);
        assertTrue(isKeyPair(pair.getPrivate(), pair.getPublic()));
    }

    @Test
    void dsa_no_match() throws Exception {
        var pair1 = createKeyPair("DSA", 1024);
        var pair2 = createKeyPair("DSA", 1024);
        assertFalse(isKeyPair(pair1.getPrivate(), pair2.getPublic()));
    }

    @Test
    void rsa_match() throws Exception {
        var pair = createKeyPair("RSA", 1024);
        assertTrue(isKeyPair(pair.getPrivate(), pair.getPublic()));
    }

    @Test
    void rsa_no_match() throws Exception {
        var pair1 = createKeyPair("RSA", 1024);
        var pair2 = createKeyPair("RSA", 1024);
        assertFalse(isKeyPair(pair1.getPrivate(), pair2.getPublic()));
    }

    @Test
    void ec_match() throws Exception {
        var pair = createKeyPair("EC", 256);
        assertTrue(isKeyPair(pair.getPrivate(), pair.getPublic()));
    }

    @Test
    void ec_no_match() throws Exception {
        var pair1 = createKeyPair("EC", 256);
        var pair2 = createKeyPair("EC", 256);
        assertFalse(isKeyPair(pair1.getPrivate(), pair2.getPublic()));
    }

    @Test
    void algo_no_match() throws Exception {
        var pair1 = createKeyPair("RSA", 1024);
        var pair2 = createKeyPair("EC", 256);
        assertFalse(isKeyPair(pair1.getPrivate(), pair2.getPublic()));
    }
}
