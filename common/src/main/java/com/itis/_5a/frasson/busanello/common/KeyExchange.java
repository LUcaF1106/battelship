package com.itis._5a.frasson.busanello.common;

import javax.crypto.KeyAgreement;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class KeyExchange {
    private KeyPair keyPair;
    private PublicKey OtherPublicKey;
    private final int KEY_SIZE= 2048;
    private final String ALGO="DH";

    public void generateDHKeys() {
        try{
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGO);
            keyGen.initialize(KEY_SIZE);
            keyPair = keyGen.generateKeyPair();
        }catch (NoSuchAlgorithmException e ){
            throw new RuntimeException("Algoritmo inesitente", e);
        }
    }

    public byte[] getPublicKeyBytes() {
        return keyPair.getPublic().getEncoded();
    }

    public void setOtherPublicKey(byte[] publicKeyBytes)  {
        try{
            KeyFactory keyFactory = KeyFactory.getInstance(ALGO);
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(publicKeyBytes);
            OtherPublicKey = keyFactory.generatePublic(x509KeySpec);
        }catch(NoSuchAlgorithmException e){
            throw new RuntimeException("Algoritmo inesitente", e);
        }catch (NullPointerException e){
            throw new RuntimeException("Pubblic key null", e);
        }catch (InvalidKeySpecException e){
            throw new RuntimeException("Errore generazionr chiave pubblica", e);
        }
    }


    /**
     * La combinazione delle chiavi (privata + pubblica dell'altro) garantisce che entrambi generino lo stesso segreto.
     **/
    public byte[] generateSecret()  {
        try {
            KeyAgreement keyAgreement = KeyAgreement.getInstance(ALGO);
            keyAgreement.init(keyPair.getPrivate());
            keyAgreement.doPhase(OtherPublicKey, true);
            return keyAgreement.generateSecret();
        } catch (InvalidKeyException e) {

            throw  new RuntimeException("Chiave usata non valida", e );
        } catch (NoSuchAlgorithmException e) {

            throw  new RuntimeException("Nessun algoritmo trovato", e );
        }
    }
}
