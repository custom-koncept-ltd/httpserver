package koncept.http.jce;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import sun.security.x509.CertAndKeyGen;
import sun.security.x509.X500Name;

public class SecurityUtil {
	private SecurityUtil(){}
	
//	//https://www.mayrhofer.eu.org/create-x509-certs-in-java
//	public static Object[] makeKeypair() throws Exception {
//		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
//        keyGen.initialize(1024, new SecureRandom());
//        KeyPair keypair = keyGen.generateKeyPair();
//        PrivateKey privKey = keypair.getPrivate();
//        PublicKey pubKey = keypair.getPublic();
//        return new Object[]{privKey, pubKey};
//	}
	
	//http://stackoverflow.com/questions/4634124/how-to-generate-sign-and-import-ssl-certificate-from-java
	public static KeyStoreDetails makeKeyStore() throws Exception {
		return makeKeyStore(new CertificateDetails());
	}
	
	public static KeyStoreDetails makeKeyStore(CertificateDetails certificateDetails) throws Exception {
		if (certificateDetails == null)
			certificateDetails = new CertificateDetails();
		else
			certificateDetails = certificateDetails.clone();
		
		KeyStoreDetails ksd = new KeyStoreDetails();
		ksd.certificateDetails = certificateDetails;

		ksd.keyStore = KeyStore.getInstance("JKS");
		ksd.keyStore.load(null, null);

        CertAndKeyGen keypair = new CertAndKeyGen("RSA", "SHA1WithRSA", null);

        X500Name x500Name = new X500Name(
        		certificateDetails.commonName,
        		certificateDetails.organizationalUnit,
        		certificateDetails.organization,
        		certificateDetails.city,
        		certificateDetails.state,
        		certificateDetails.country);

        keypair.generate(certificateDetails.keysize);
        ksd.privateKey = keypair.getPrivateKey();
        ksd.publicKey = keypair.getPublicKey();

        X509Certificate[] chain = new X509Certificate[1];

        chain[0] = keypair.getSelfCertificate(x500Name, new Date(), (long) certificateDetails.validityDays * 24 * 60 * 60);

        ksd.keyStore.setKeyEntry(certificateDetails.alias, ksd.privateKey, certificateDetails.keyPass, chain);

//        keyStore.store(new FileOutputStream(".keystore"), keyPass);
        return ksd;
    }
	
	public static SSLContext makeSSLContext() throws Exception {
		return makeSSLContext(makeKeyStore());
	}
	
	public static SSLContext makeSSLContext(KeyStoreDetails ksd) throws Exception {
		KeyStore ks = ksd.keyStore;
		
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(ks, ksd.certificateDetails.keyPass);
		
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(ks);
		
		SSLContext sslContext = SSLContext.getInstance("TLS");
		
		SecureRandom random = new SecureRandom();
		sslContext.init(
				kmf.getKeyManagers(),
				tmf.getTrustManagers(),
				random);
		return sslContext;
	}
	
	
	
	
	
	
	
	
	public static class CertificateDetails implements Cloneable {
		public int keysize = 1024;
		public String commonName = "localhost";
	    public String organizationalUnit = "IT";
	    public String organization = "test";
	    public String city = "London";
	    public String state = "none";
	    public String country = "UK";
	    public long validityDays = 365; //in days
	    public String alias = "kncept";
	    public char[] keyPass = "changeit".toCharArray();
	    
	    @Override
	    public CertificateDetails clone() throws CloneNotSupportedException {
	    	CertificateDetails details = (CertificateDetails)super.clone();
	    	//deep copy array
	    	details.keyPass = new char[keyPass.length];
	    	System.arraycopy(keyPass, 0, details.keyPass, 0, keyPass.length);
	    	return details;
	    }
	}
	
	public static class KeyStoreDetails {
		public CertificateDetails certificateDetails;
		public PrivateKey privateKey;
		public PublicKey publicKey;
		public KeyStore keyStore;
		
	}
	
}
