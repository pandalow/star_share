package com.star.share.auth.utils;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
    import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Pem READ Utils
 * supporting read PKCS#8 private key and X.509 public Key
 * Encoding by Base 64
 */
public class PemUtils {

    private static final String PRIVATE_BEGIN = "-----BEGIN PRIVATE KEY-----";
    private static final String PRIVATE_END = "-----END PRIVATE KEY-----";
    private static final String PUBLIC_BEGIN = "-----BEGIN PUBLIC KEY-----";
    private static final String PUBLIC_END = "-----END PUBLIC KEY-----";
    /**
     * Read specific Resource
     * @param resource resource
     * @return Using utf-8 decode content
     * @throws IOException throw I/O exception
     */
    private static String readResource(Resource resource) throws IOException {
        try(InputStream is = resource.getInputStream()){
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Reading private Key from PEM file
     * @param resource point to private PEM file
     * @return RSAPrivateKey
     */
    public static RSAPrivateKey readPrivateKey(Resource resource){
        try{
            String pem = readResource(resource);
            String keyData = pem.replace(PRIVATE_BEGIN, "")
                    .replace(PRIVATE_END, "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(keyData);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");

            return(RSAPrivateKey) kf.generatePrivate(spec);
        }catch (IOException | GeneralSecurityException e){
            throw new IllegalStateException("Failed to read RSA private key", e);
        }
    }

    /**
     * Reading public key from PEM file
     * @param resource point to private PEM file
     * @return RSAPublicKey
     */
    public static RSAPublicKey readPublicKey(Resource resource){
        try {
            String pem = readResource(resource);
            String keyData = pem.replace(PUBLIC_BEGIN, "")
                    .replace(PUBLIC_END,"")
                    .replaceAll("\\s","");

            byte[] keyBytes = Base64.getDecoder().decode(keyData);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");

            return (RSAPublicKey) kf.generatePublic(spec);
        }catch (IOException | GeneralSecurityException e){
            throw new IllegalStateException("Failed to read RSA public key", e);
        }
    }


}

