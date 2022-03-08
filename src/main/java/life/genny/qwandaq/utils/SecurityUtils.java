package life.genny.qwandaq.utils;

import org.jboss.logging.Logger;

import java.security.Key;
import java.util.Date;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.InvalidKeyException;

import life.genny.qwandaq.models.GennyToken;

/**
 * A utility class dedicated to security related operations.
 * 
 * @author Jasper Robison
 */
public class SecurityUtils {

	static final Logger log = Logger.getLogger(SecurityUtils.class);

	/**
	* Function to validate the authority for a given {@link GennyToken}.
	*
	* @param gennyToken the gennyToken to check
	* @return Boolean
	 */
	public static Boolean isAuthorisedGennyToken(GennyToken gennyToken) {

		if (gennyToken.hasRole("admin") || gennyToken.hasRole("service") || gennyToken.hasRole("dev")) {
			log.error(gennyToken.getUserCode() + " has no authority to schedule");
			return true;
		}

		return false;
	}
	
	/** 
	 * Create a JWT
	 *
	 * @param id the id to set
	 * @param issuer the issuer to set
	 * @param subject the subject to set
	 * @param ttlMillis the ttlMillis to set
	 * @param apiSecret the apiSecret to set
	 * @param claims the claims to set
	 * @return String
	 */
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

		Key key = null;

		try {
			key = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());
			builder.signWith(SignatureAlgorithm.HS256, key);

		} catch (Exception e) {
			try {
				key = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());
				builder.signWith(SignatureAlgorithm.HS256, key);

			} catch (Exception e1) {
				try {
					Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());
					builder.signWith(signatureAlgorithm, signingKey);

				} catch (InvalidKeyException e2) {
					log.error(e2);
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
