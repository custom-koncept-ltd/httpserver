package koncept.http.jce;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
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
	
	
	private static final int keysize = 1024;
    private static final String commonName = "localhost";
    private static final String organizationalUnit = "IT";
    private static final String organization = "test";
    private static final String city = "London";
    private static final String state = "none";
    private static final String country = "UK";
    private static final long validity = 365; //in days
    private static final String alias = "kncept";
    private static final char[] keyPass = "changeit".toCharArray();
	
	//https://www.mayrhofer.eu.org/create-x509-certs-in-java
	public static Object[] makeKeypair() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024, new SecureRandom());
        KeyPair keypair = keyGen.generateKeyPair();
        PrivateKey privKey = keypair.getPrivate();
        PublicKey pubKey = keypair.getPublic();
        return new Object[]{privKey, pubKey};
	}
	
//	//https://www.mayrhofer.eu.org/create-x509-certs-in-java
//	public void makeCert() {
//		 Calendar expiry = Calendar.getInstance();
//	        expiry.add(Calendar.DAY_OF_YEAR, validityDays);
//	 
//	        X509Name x509Name = new X509Name("CN=" + dn);
//
//	        V3TBSCertificateGenerator certGen = new V3TBSCertificateGenerator();
//	        certGen.setSerialNumber(new DERInteger(BigInteger.valueOf(System.currentTimeMillis())));
//	        certGen.setIssuer(PrincipalUtil.getSubjectX509Principal(caCert));
//	        certGen.setSubject(x509Name);
//	        DERObjectIdentifier sigOID = X509Util.getAlgorithmOID("SHA1WithRSAEncryption");
//	        AlgorithmIdentifier sigAlgId = new AlgorithmIdentifier(sigOID, new DERNull());
//	        certGen.setSignature(sigAlgId);
//	        certGen.setSubjectPublicKeyInfo(new SubjectPublicKeyInfo((ASN1Sequence)new ASN1InputStream(
//	                new ByteArrayInputStream(pubKey.getEncoded())).readObject()));
//	        certGen.setStartDate(new Time(new Date(System.currentTimeMillis())));
//	        certGen.setEndDate(new Time(expiry.getTime()));
//	        TBSCertificateStructure tbsCert = certGen.generateTBSCertificate();
//	}
	
//	//http://stackoverflow.com/questions/925377/generate-certificates-public-and-private-keys-with-java
//	//mmm uses bouncycastle!
//	public void makeCert() {
//		RSAPrivateKeySpec serPrivateSpec = new RSAPrivateKeySpec(
//				/*pub*/BigInteger.valueOf(7), BigInteger.valueOf(/*pri*/(int)5));
//			fact = KeyFactory.getInstance("RSA");
//			PrivateKey serverPrivateKey = fact.generatePrivate(serPrivateSpec);
//
//			RSAPublicKeySpec serPublicSpec = new RSAPublicKeySpec(
//			    new BigInteger(agentCL.getSerPubMod()), new BigInteger(agentCL.getSerPubExp()));
//			PublicKey serverPublicKey = fact.generatePublic(serPublicSpec);
//
//			keyStore = KeyStore.getInstance(IMXAgentCL.STORE_TYPE);
//			keyStore.load(null, SOMEPWD.toCharArray());
//
//			Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
//
//			X509Certificate[] serverChain = new X509Certificate[1];
//			X509V3CertificateGenerator serverCertGen = new X509V3CertificateGenerator();
//			X500Principal serverSubjectName = new X500Principal("CN=OrganizationName");
//			serverCertGen.setSerialNumber(new BigInteger("123456789"));
//			// X509Certificate caCert=null;
//			serverCertGen.setIssuerDN(somename);
//			serverCertGen.setNotBefore(new Date());
//			serverCertGen.setNotAfter(new Date());
//			serverCertGen.setSubjectDN(somename);
//			serverCertGen.setPublicKey(serverPublicKey);
//			serverCertGen.setSignatureAlgorithm("MD5WithRSA");
//			// certGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false,new
//			// AuthorityKeyIdentifierStructure(caCert));
//			serverCertGen.addExtension(X509Extensions.SubjectKeyIdentifier, false,
//			    new SubjectKeyIdentifierStructure(serverPublicKey));
//			serverChain[0] = serverCertGen.generateX509Certificate(serverPrivateKey, "BC"); // note: private key of CA
//
//			keyStore.setEntry("xyz",
//			    new KeyStore.PrivateKeyEntry(serverPrivateKey, serverChain),
//			    new KeyStore.PasswordProtection("".toCharArray()));
//	}
	
	//http://stackoverflow.com/questions/4634124/how-to-generate-sign-and-import-ssl-certificate-from-java
	public static KeyStore makeKeyStore() throws Exception {

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);

        CertAndKeyGen keypair = new CertAndKeyGen("RSA", "SHA1WithRSA", null);

        X500Name x500Name = new X500Name(commonName, organizationalUnit, organization, city, state, country);

        keypair.generate(keysize);
        PrivateKey privKey = keypair.getPrivateKey();

        X509Certificate[] chain = new X509Certificate[1];

        chain[0] = keypair.getSelfCertificate(x500Name, new Date(), (long) validity * 24 * 60 * 60);

        keyStore.setKeyEntry(alias, privKey, keyPass, chain);

//        keyStore.store(new FileOutputStream(".keystore"), keyPass);
        
        return keyStore;
    }
	
	public static SSLContext makeSSLContext() throws Exception {
		return makeSSLContext(makeKeyStore());
	}
	
	public static SSLContext makeSSLContext(KeyStore ks) throws Exception {
		
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(ks, SecurityUtil.keyPass);
		
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
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
