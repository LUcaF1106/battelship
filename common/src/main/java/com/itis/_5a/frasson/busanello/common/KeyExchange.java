package com.itis._5a.frasson.busanello.common;

import javax.crypto.KeyAgreement;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import java.security.KeyFactory;

public class KeyExchange {
    private KeyPair keyPair;
    private PublicKey OtherPublicKey;
    private final int KEY_SIZE= 2048;
    private final String ALGO="DH";

    public void generateDHKeys() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGO);
        keyGen.initialize(KEY_SIZE);
        keyPair = keyGen.generateKeyPair();
    }

    public byte[] getPublicKeyBytes() {
        return keyPair.getPublic().getEncoded();
    }

    public void setOtherPublicKey(byte[] publicKeyBytes) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance(ALGO);
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(publicKeyBytes);//Formato della chiave pubblica inviata
        OtherPublicKey = keyFactory.generatePublic(x509KeySpec);
    }


    /**
     * La combinazione delle chiavi (privata + pubblica dell'altro) garantisce che entrambi generino lo stesso segreto.
     **/
    public byte[] generateSecret() throws Exception {

        KeyAgreement keyAgreement = KeyAgreement.getInstance(ALGO);
        keyAgreement.init(keyPair.getPrivate());
        keyAgreement.doPhase(OtherPublicKey, true);
        return keyAgreement.generateSecret();
    }
}
