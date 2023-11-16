package community.solace.spring.boot.starter.solaceclientconfig;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("solace.java.ssl-cert-info")
public class SslCertInfoProperties {
    /**
     * Enable/Disable log messages for certificate expiration
     */
    private boolean enabled = true;

    /**
     * Log a warning message once a day (09:00 AM) if ssl client cert,
     * that is used to connect to the solace broker,
     * is valid for fewer than X days.
     */
    private int warnInDays = 30;

    /**
     * Log an error message once a day (09:00 AM) if ssl client cert,
     * that is used to connect to the solace broker,
     * is valid for fewer than X days.
     */
    private int errorInDays = 7;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getWarnInDays() {
        return warnInDays;
    }

    public void setWarnInDays(int warnInDays) {
        this.warnInDays = warnInDays;
    }

    public int getErrorInDays() {
        return errorInDays;
    }

    public void setErrorInDays(int errorInDays) {
        this.errorInDays = errorInDays;
    }
}
