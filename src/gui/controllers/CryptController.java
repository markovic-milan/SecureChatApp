package gui.controllers;

import gui.main.Main;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class CryptController {

    public static void cryptFileSymmetric(File input, File output, SecretKey sesionKey, String cipherName) {
        long pocetak = System.currentTimeMillis();
        try {
            output.createNewFile();
            FileInputStream fis = new FileInputStream(input);
            FileOutputStream fos = new FileOutputStream(output);

            Cipher cipher = Cipher.getInstance(cipherName);
            byte[] iv;
            if (cipherName.equals("DESede/CBC/PKCS5Padding"))
                iv = new byte[8];
            else {
                iv = new byte[16];
            }
            cipher.init(Cipher.ENCRYPT_MODE, sesionKey, new IvParameterSpec(iv));
            CipherInputStream cis = new CipherInputStream(fis, cipher);
            int count;
            byte[] buffer = new byte[1024];
            while ((count = cis.read(buffer)) > 0) {
                fos.write(buffer, 0, count);
                fos.flush();
            }
            cis.close();
            fos.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Vrijeme enkripcije simetricnim algoritmom je " + (double) (System.currentTimeMillis() - pocetak) / 1000);
    }

    public static void decryptFileSymmetric(File input, File output, SecretKey sessionKey, String cipherName) {
        long pocetak = System.currentTimeMillis();
        try {
            if (!output.exists()) {
                output.createNewFile();
            }
            FileInputStream fis = new FileInputStream(input);
            FileOutputStream fos = new FileOutputStream(output);

            Cipher decipher = Cipher.getInstance(cipherName);
            byte[] iv;
            if (cipherName.equals("DESede/CBC/PKCS5Padding"))
                iv = new byte[8];
            else {
                iv = new byte[16];
            }
            decipher.init(Cipher.DECRYPT_MODE, sessionKey, new IvParameterSpec(iv));
            CipherOutputStream cos = new CipherOutputStream(fos, decipher);
            int count;
            byte[] buffer = new byte[1024];
            while ((count = fis.read(buffer)) > 0) {
                cos.write(buffer, 0, count);
                cos.flush();
            }
            cos.close();
            fos.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Vrijeme dekripcije simetricnim algoritmom je " + (double) (System.currentTimeMillis() - pocetak) / 1000);
    }

    public static SecretKey genSymetricKey(String algorithm) {
        long pocetak = System.currentTimeMillis();
        SecretKey key = null;
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(algorithm);
            key = keyGen.generateKey();
            System.out.println("Format i algoritam: " + key.getAlgorithm() + key.getFormat());
        } catch (NoSuchAlgorithmException exc) {
            Main.showAlert("Greška pri generisanju simetričnog algoritma.Takav simetrični algoritam ne postoji.", true);
        } finally {
            System.out.println("Vrijeme generisanja simetricnog kljuca je " + (double) (System.currentTimeMillis() - pocetak) / 1000);
            return key;
        }
    }

    public static String sign(File choosenFile, PrivateKey loggedUserPrivateKey, String hashAlgorithm) {
        long pocetak = System.currentTimeMillis();
        String encodedSignature = null;
        FileInputStream fis = null;
        try {
            Signature signature = Signature.getInstance(hashAlgorithm);
            signature.initSign(loggedUserPrivateKey);
            fis = new FileInputStream(choosenFile);
            byte[] buffer = new byte[1024];
            int count;
            while ((count = fis.read(buffer)) > 0) {
                signature.update(buffer, 0, count);
            }
            byte[] signatureBytes = signature.sign();
            encodedSignature = new String(Base64.getEncoder().encode(signatureBytes));
            fis.close();
        } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | SignatureException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        System.out.println("Vrijeme generisanja potpisa " + (double) (System.currentTimeMillis() - pocetak) / 1000);
        return encodedSignature;
    }

    public static X509Certificate getCertificate(File file) {
        long pocetak = System.currentTimeMillis();
        X509Certificate certificate = null;
        CertificateFactory factory;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            factory = CertificateFactory.getInstance("X.509");
            certificate = (X509Certificate) factory.generateCertificate(fis);
        } catch (Exception ex) {
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ex) {
                }
            }
        }
        System.out.println("getCertificate " + (double) (System.currentTimeMillis() - pocetak) / 1000);
        return certificate;
    }

    public static PrivateKey getPemPrivateKey(String privateKey) throws Exception {
        long pocetak = System.currentTimeMillis();
        Security.addProvider(new BouncyCastleProvider());
        File f = new File(privateKey);
        FileInputStream fis = new FileInputStream(f);
        DataInputStream dis = new DataInputStream(fis);
        byte[] keyBytes = new byte[(int) f.length()];
        dis.readFully(keyBytes);
        dis.close();

        String temp = new String(keyBytes);
        StringBuilder pkcs8Lines = new StringBuilder();
        BufferedReader rdr = new BufferedReader(new StringReader(temp));
        String line;
        while ((line = rdr.readLine()) != null) {
            pkcs8Lines.append(line);
        }

        // Remove the "BEGIN" and "END" lines, as well as any whitespace
        String pkcs8Pem = pkcs8Lines.toString();
        pkcs8Pem = pkcs8Pem.replace("-----BEGIN RSA PRIVATE KEY-----", "");
        pkcs8Pem = pkcs8Pem.replace("-----END RSA PRIVATE KEY-----", "");

        // Base64 decode the result
        byte[] base64DecodedData = Base64.getDecoder().decode(pkcs8Pem);

        // extract the private key
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(base64DecodedData);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey privKey = kf.generatePrivate(keySpec);
        System.out.println("getPemPrivateKey " + (double) (System.currentTimeMillis() - pocetak) / 1000);
        return privKey;
    }

    public static void decryptAsymmetric(PrivateKey privateKey, byte[] input, File output) {
        long pocetak = System.currentTimeMillis();
        try {
            if (!output.exists()) {
                output.createNewFile();
            }
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] toWrite = cipher.doFinal(input);
            FileOutputStream fos = new FileOutputStream(output);
            fos.write(toWrite);
            fos.flush();
            fos.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println("decryptAsymmetric " + (double) (System.currentTimeMillis() - pocetak) / 1000);
    }

    public static boolean verifySignature(File decryptedFile, PublicKey publicKey, File receivedSignature, String hashAlgorithmForSign) {
        long pocetak = System.currentTimeMillis();
        Signature sig;
        try {
            sig = Signature.getInstance(hashAlgorithmForSign);
            sig.initVerify(publicKey);
            BufferedInputStream bufin = new BufferedInputStream(new FileInputStream(decryptedFile));

            byte[] buffer = new byte[1024];
            int len;
            while (bufin.available() != 0) {
                len = bufin.read(buffer);
                sig.update(buffer, 0, len);
            }
            bufin.close();
            byte[] signedBytes = Files.readAllBytes(receivedSignature.toPath());
            byte signature[] = Base64.getMimeDecoder().decode(signedBytes);

            if (signature.length != 256) //provjera korektne velicine potpisa
                return false;
            boolean verifies = sig.verify(signature);
            System.out.println("verifySignature " + (double) (System.currentTimeMillis() - pocetak) / 1000);
            return verifies;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("verifySignature " + (double) (System.currentTimeMillis() - pocetak) / 1000);
            return false;
        }
    }

    public static byte[] getFileInBytes(File f) throws IOException {
        long pocetak = System.currentTimeMillis();
        FileInputStream fis = new FileInputStream(f);
        byte[] fbytes = new byte[(int) f.length()];
        fis.read(fbytes);
        fis.close();
        System.out.println("getFileInBytes " + (System.currentTimeMillis() - pocetak) / 1000);
        return fbytes;
    }

    public static byte[] encryptAsymmetric(PublicKey receiverPublicKey, byte[] input, File output) {
        long pocetak = System.currentTimeMillis();
        byte[] toWrite = null;
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, receiverPublicKey);
            FileOutputStream fos = new FileOutputStream(output);
            toWrite = cipher.doFinal(input);
            fos.write(toWrite);
            fos.flush();
            fos.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            System.out.println("encryptAsymmetric " + (double) (System.currentTimeMillis() - pocetak) / 1000);
            return toWrite;
        }

    }

    public static SecretKey convertSymmetricKeyFromB64(String encodedSessionKey, String algorithmName) {
        long pocetak = System.currentTimeMillis();
        SecretKey key = null;
        try {
            byte[] decodedKey = Base64.getDecoder().decode(encodedSessionKey);
            key = new SecretKeySpec(decodedKey, 0, decodedKey.length, algorithmName);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println("convertSymmetricKeyFromB64 " + (double) (System.currentTimeMillis() - pocetak) / 1000);
        return key;
    }

    public static String getSecurePassword(String algorithmName, String passwordToHash) {
        return hashWithSHA256(passwordToHash);
    }

    private static String hashWithSHA256(String textToHash) {
        String hashtext = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // digest() method called
            // to calculate message digest of an input
            // and return array of byte
            byte[] messageDigest = md.digest(textToHash.getBytes());

            // Convert byte array into signum representation
            BigInteger no = new BigInteger(1, messageDigest);

            // Convert message digest into hex value
            hashtext = no.toString(16);

            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
        }
        // For specifying wrong message digest algorithms
        catch (NoSuchAlgorithmException e) {
            Main.showAlert("Ne postoji takav algoritam za heširanje!", true);
        } finally {
            return hashtext;
        }
    }

    private static byte[] getSalt(String algName) throws NoSuchAlgorithmException {
        //Always use a SecureRandom generator
        // String algName = algorithmName.replaceAll("[-+.^:,]","");
        try {
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG", "SUN");
            //Create array for salt
            byte[] salt = new byte[16];
            //Get a random salt
            sr.nextBytes(salt);
            //return salt
            return salt;
        } catch (NoSuchProviderException exc) {
            exc.printStackTrace();
        }
        return null;
    }
}
