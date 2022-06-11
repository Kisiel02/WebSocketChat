package kisiel.jakub.websocketchat;

import kisiel.jakub.websocketchat.messages.ConfigMessage;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Getter
@Setter
@Component
public class SecurityManager {

    private final Logger logger = LogManager.getLogger(SecurityManager.class);

    private static String CIPHER = "RSA";

    private static String SESSION_CIPHER = "AES";

    private static String ECB = "AES/ECB/PKCS5Padding";

    private static String CBC = "AES/CBC/PKCS5Padding";

    private static String PUBLIC_KEY_STORAGE = "/storage/publicKey/";

    private static String PRIVATE_KEY_STORAGE = "AES/CBC/PKCS5Padding";

    private static int SYMMETRIC_KEY_SIZE = 128;

    private static int ASYMMETRIC_KEY_SIZE = 2048;

    private static int IV_SIZE = 16;

    private PrivateKey privateKey;

    private PublicKey publicKey;

    private PublicKey foreignKey;

    private SecretKey sessionKey;

    private IvParameterSpec iv;

    public enum BlockMode {
        CBC,
        ECB
    }

    public void generateSessionKey(String symmetricAlgorithm, int keySizeSym) throws NoSuchAlgorithmException {
        KeyGenerator symmetricGenerator = KeyGenerator.getInstance(symmetricAlgorithm);
        symmetricGenerator.init(keySizeSym);
        this.sessionKey = symmetricGenerator.generateKey();
        byte[] ivVector = new byte[IV_SIZE];
        new SecureRandom().nextBytes(ivVector);
        this.iv = new IvParameterSpec(ivVector);
        String sessionKeyString = Base64.getEncoder().encodeToString(sessionKey.getEncoded());
        logger.debug("Session key: {}", sessionKeyString);
        String ivVectorString = Base64.getEncoder().encodeToString(iv.getIV());
        logger.debug("IV vector: {}", ivVectorString);

    }

    @SneakyThrows
    public void generateSessionKey() {
        KeyGenerator symmetricGenerator = KeyGenerator.getInstance(SESSION_CIPHER);
        symmetricGenerator.init(SYMMETRIC_KEY_SIZE);
        this.sessionKey = symmetricGenerator.generateKey();
    }

    public void saveForeignKey(ConfigMessage configMessage) throws InvalidKeySpecException {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(configMessage.getPublicKey());
            this.setForeignKey(keyFactory.generatePublic(publicKeySpec));
            logger.debug("Foreign public key:\n {}", foreignKey);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Wrong algorithm name", e);
        }
    }

    public void initializeSecurity(String asymmetricAlgorithm, int keySizeAsym) throws NoSuchAlgorithmException {
        KeyPairGenerator asymmetricGenerator = KeyPairGenerator.getInstance(asymmetricAlgorithm);
        asymmetricGenerator.initialize(keySizeAsym);
        KeyPair keyPair = asymmetricGenerator.generateKeyPair();
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
        logger.info("App public key:\n {}", this.publicKey);
    }

    public void initializeSecurity(String asymmetricAlgorithm, int keySizeAsym, String symmetricAlgorithm, int keySizeSym) throws NoSuchAlgorithmException {
        KeyPairGenerator asymmetricGenerator = KeyPairGenerator.getInstance(asymmetricAlgorithm);
        asymmetricGenerator.initialize(keySizeAsym);
        KeyPair keyPair = asymmetricGenerator.generateKeyPair();
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
        generateSessionKey(symmetricAlgorithm, keySizeSym);
        logger.info("App public key:\n {}", getPublicKey());
    }

    @SneakyThrows
    public void setSessionKeyFromBytes(byte[] sessionKey) {
        this.sessionKey = new SecretKeySpec(sessionKey, 0, sessionKey.length, SESSION_CIPHER);
        String sessionKeyString = Base64.getEncoder().encodeToString(
            this.sessionKey.getEncoded());
        logger.info("Session key set:\n {}",
            sessionKeyString);
    }

    @SneakyThrows
    public byte[] encryptFileWithSessionKey(byte[] chunk, BlockMode blockMode) {
        Cipher cipher = getCipher(blockMode, Cipher.ENCRYPT_MODE);
        return cipher.doFinal(chunk);
    }

    @SneakyThrows
    public byte[] decryptFileWithSessionKey(byte[] chunk, BlockMode blockMode) {
        Cipher cipher = getCipher(blockMode, Cipher.DECRYPT_MODE);
        return cipher.doFinal(chunk);
    }

    @SneakyThrows
    public String encryptStringWithSessionKey(String message, BlockMode blockMode) {
        Cipher cipher = getCipher(blockMode, Cipher.ENCRYPT_MODE);

        byte[] encoded = cipher.doFinal(message.getBytes());
        String toReturn = Base64.getEncoder().encodeToString(encoded);
        String decrypted = decryptStringWithSessionKey(toReturn, blockMode);

        logger.info("Original message: {}\n string to be send: {}\n decoded string: {}", message, toReturn, decrypted);
        return toReturn;
    }


    @SneakyThrows
    public String decryptStringWithSessionKey(String message, BlockMode blockMode) {
        Cipher cipher = getCipher(blockMode, Cipher.DECRYPT_MODE);

        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(message));
        return new String(decrypted);
    }

    @SneakyThrows
    public byte[] encryptWithForeignKey(byte[] message) {
        Cipher encrypt = Cipher.getInstance(CIPHER);
        encrypt.init(Cipher.ENCRYPT_MODE, this.foreignKey);
        return encrypt.doFinal(message);
    }

    @SneakyThrows
    public String encryptWithForeignKey(String message) {
        Cipher encrypt = Cipher.getInstance(CIPHER);
        encrypt.init(Cipher.ENCRYPT_MODE, this.foreignKey);
        return Base64.getEncoder().encodeToString(encrypt.doFinal(message.getBytes(StandardCharsets.UTF_8)));
    }

    @SneakyThrows
    public byte[] decryptWithPrivateKey(byte[] message) {
        Cipher decrypt = Cipher.getInstance(CIPHER);
        decrypt.init(Cipher.DECRYPT_MODE, this.privateKey);
        return decrypt.doFinal(message);
    }

    @SneakyThrows
    public String decryptWithPrivateKey(String message) {
        Cipher decrypt = Cipher.getInstance(CIPHER);
        decrypt.init(Cipher.DECRYPT_MODE, this.privateKey);
        byte[] decrypted = decrypt.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private Cipher getCipher(BlockMode blockMode, int encryptMode) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        Cipher cipher;
        if (blockMode.equals(BlockMode.CBC)) {
            cipher = Cipher.getInstance(CBC);
            cipher.init(encryptMode, sessionKey, iv);
        } else if (blockMode.equals(BlockMode.ECB)) {
            cipher = Cipher.getInstance(ECB);
            cipher.init(encryptMode, sessionKey);
        } else {
            cipher = Cipher.getInstance(SESSION_CIPHER);
            cipher.init(encryptMode, sessionKey);
        }
        return cipher;
    }
}
