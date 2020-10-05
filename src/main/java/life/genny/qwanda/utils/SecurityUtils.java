package life.genny.qwanda.utils;


import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.InvalidKeyException;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.jboss.logging.Logger;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.security.Key;
import java.util.Date;
import java.util.Map;

@RegisterForReflection
public class SecurityUtils {
	private static final Logger log = Logger.getLogger(SecurityUtils.class);

	public static String createJwt(String id, String issuer, String subject, long ttlMillis, String apiSecret,
								   Map<String, Object> claims) {

		// The JWT signature algorithm we will be using to sign the token
		SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
		String aud = issuer;
		if (claims.containsKey("aud")) {
			aud = (String) claims.get("aud");
			claims.remove("aud");
		}
		long nowMillis = System.currentTimeMillis();
		Date now = new Date(nowMillis);

		// We will sign our JWT with our ApiKey secret
		byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(apiSecret);

		// Let's set the JWT Claims
		JwtBuilder builder = Jwts.builder().setId(id).setIssuedAt(now).setSubject(subject).setIssuer(issuer)
				.setAudience(aud).setClaims(claims);
		// .signWith(signatureAlgorithm, signingKey);

		Key key;

		try {
			key = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());
			builder.signWith(key, SignatureAlgorithm.HS256);
		} catch (Exception e) {
			try {
				key = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());
				builder.signWith(key, SignatureAlgorithm.HS256);
			} catch (Exception e1) {
				try {
					Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());
					builder.signWith(signingKey, signatureAlgorithm);
				} catch (InvalidKeyException e2) {
					log.error("Cannot creating key foor JWT");
				}
			}
		}

		// if it has been specified, let's add the expiration
		if (ttlMillis >= 0) {
			long expMillis = nowMillis + ttlMillis;
			Date exp = new Date(expMillis);
			builder.setExpiration(exp);
		}

		// Builds the JWT and serializes it to a compact, URL-safe string
		return builder.compact();
	}
}