package community.solace.spring.boot.starter.solaceclientconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

/**
 * This class creates certificates and private keys out of PEM strings.
 */
final class PemFormatTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(PemFormatTransformer.class);
    private static final Pattern PEM_FORMAT = Pattern.compile(
            "^\\s*-----BEGIN (RSA PRIVATE KEY|PRIVATE KEY)-----\\s*(" +
                    "([0-9a-zA-Z+/=]{64}\\s*)*" + "([0-9a-zA-Z+/=]{1,63}\\s*)?" +
                    ")-----END (RSA PRIVATE KEY|PRIVATE KEY)-----\\s*$"
    );

    Certificate[] getCertificates(final String pem, final String certificateType) throws CertificateException, IOException {
        final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        try (final ByteArrayInputStream is = new ByteArrayInputStream(cleanWhiteSpace(pem).getBytes(ISO_8859_1))) {
            return certificateFactory.generateCertificates(is).toArray(new Certificate[0]);
        } catch (Exception exception) {
            LOG.warn("Could not parse {} {} pem={}...",certificateType, exception.getMessage(), pem.substring(0, Math.min(10, pem.length() - 1)), exception);
            return null;
        }
    }

    /**
     * Normally pem allows any kind of space (\r\n|\n|\r|\t| ) between chunks, but java only accept \n.
     *
     * @param pem that is possible not java but standard compatible.
     * @return cleaned pem string
     */
    private static String cleanWhiteSpace(String pem) {
        String[] certs = pem.split("-----BEGIN CERTIFICATE-----");

        StringBuilder cleanedPem = new StringBuilder();
        for (String cert : certs) {
            cert = cert.replace("-----BEGIN CERTIFICATE-----", "").replace("-----END CERTIFICATE-----", "").trim();

            if (StringUtils.hasText(cert)) {
                String[] lines = cert.split("\\s+");

                cleanedPem.append("-----BEGIN CERTIFICATE-----\n");

                for (String line : lines) {
                    line = line.trim();

                    if (StringUtils.hasText(line)) {
                        cleanedPem.append(line);
                        cleanedPem.append("\n");
                    }
                }

                cleanedPem.append("-----END CERTIFICATE-----\n");
            }
        }

        return cleanedPem.toString();
    }

    PrivateKey getPrivateKey(final String pem) {
        try {
            byte[] encodedKey = decodePemKey(pem);
            if (encodedKey == null) {
                return null;
            }
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(encodedKey));
        } catch (Exception e) {
            LOG.warn("Could not parse SSL_PRIVATE_KEY {} pem={}...", e.getMessage(), StringUtils.truncate(pem, 10), e);
            return null;
        }
    }

    private byte[] decodePemKey(final String pem) {
        final Matcher matcher = PEM_FORMAT.matcher(pem);
        if (!matcher.matches()) {
            LOG.warn("Invalid PEM string SSL_PRIVATE_KEY pem={}...", StringUtils.truncate(pem, 10));
            return null;
        }
        return Base64.getMimeDecoder().decode(matcher.group(2));
    }
}
