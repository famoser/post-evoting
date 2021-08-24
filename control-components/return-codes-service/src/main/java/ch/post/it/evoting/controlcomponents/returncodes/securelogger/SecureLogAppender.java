/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.securelogger;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.MGF1ParameterSpec;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

import ch.post.it.evoting.controlcomponents.commons.keymanagement.KeysManager;
import ch.post.it.evoting.controlcomponents.returncodes.securelogger.events.RegularSecureLogEvent;
import ch.post.it.evoting.controlcomponents.returncodes.securelogger.events.SecureLogEventFactory;

@Plugin(name = "SecureLogAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE)
public class SecureLogAppender extends AbstractAppender {
	private final AtomicInteger lineNumber = new AtomicInteger();
	private final SecureLogLayout layout;
	private final RollingFileAppender delegate;
	private final int linesBetweenCheckPoints;
	private int lastCheckpointLine = 0;
	private byte[] previousHmac = new byte[0];
	private byte[] currentHmacKey;
	private KeysManager keysManager;

	protected SecureLogAppender(String name, Filter filter, SecureLogLayout layout, String fileName, String filePattern, String sizePolicy,
			int linesBetweenCheckPoints) {
		super(name, filter, layout, false, new Property[0]);

		delegate = RollingFileAppender.newBuilder().setName("RollingFile").setLayout(layout).withFileName(fileName).withFilePattern(filePattern)
				.withPolicy(TimeBasedTriggeringPolicy.newBuilder().build()).withPolicy(SizeBasedTriggeringPolicy.createPolicy(sizePolicy)).build();

		this.layout = layout;
		this.linesBetweenCheckPoints = linesBetweenCheckPoints;
	}

	@PluginBuilderFactory
	public static <B extends Builder<B>> B newBuilder() {
		return new Builder<B>().asBuilder();
	}

	public static SecureLogAppender getAppender() {
		Logger logger = (Logger) LogManager.getLogger("SecureLog");
		final Map<String, Appender> appenders = logger.getAppenders();
		return (SecureLogAppender) appenders.get("SecureLogAppender");
	}

	@Override
	public void start() {
		delegate.start();
		this.setStarting();
		if (getFilter() != null) {
			getFilter().start();
		}

		this.setStarted();
	}

	@Override
	public boolean stop(long timeout, TimeUnit timeUnit) {
		if (delegate.isStopped()) {
			getHandler().error("delegate stopped before main");
		}

		if (keysManager != null && keysManager.hasNodeKeysActivated()) {
			try {
				delegate.append(createCheckpointLogEvent());
			} catch (GeneralSecurityException e) {
				getStatusLogger().error(e);
				getHandler().error("failed to log final checkpoint", e);
			}
		}

		boolean stopped = super.stop(timeout, timeUnit);
		stopped &= delegate.stop(timeout, timeUnit);
		return stopped;
	}

	@Override
	public synchronized void append(LogEvent event) {
		if (!isStarted() || keysManager == null) {
			throw new IllegalStateException("SecureLogAppender " + getName() + " is not active");
		}

		delegate.append(createRegularLogEvent(event));
		if (lineNumber.get() - lastCheckpointLine >= linesBetweenCheckPoints) {
			try {
				delegate.append(createCheckpointLogEvent());
			} catch (GeneralSecurityException e) {
				getHandler().error("Failed to log", event, e);
			}
			lastCheckpointLine = lineNumber.get();
		}
	}

	public void setKeysManager(KeysManager keysManager) {
		this.keysManager = keysManager;
	}

	public void logInitialCheckpoint() {
		try {
			if (keysManager != null) {
				delegate.append(createFirstLineLogEvent());
			}
		} catch (GeneralSecurityException e) {
			getHandler().error("Failed to create checkpoint", e);
		}
	}

	private RegularSecureLogEvent createRegularLogEvent(LogEvent event) {
		final String message = layout.innerSerializable(event);
		byte[] currentHmac = computeHmacForRegularEvent(event, message);
		// Prepare for the next event
		previousHmac = currentHmac.clone();
		return SecureLogEventFactory.regularEvent(event, currentHmac, lineNumber.getAndIncrement());
	}

	private byte[] computeHmacForRegularEvent(LogEvent event, String message) {
		HMac hmacFunction = getHmacFunction();
		final byte[] timestampBytes = getTimestampBytes(event);
		final byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
		hmacFunction.update(previousHmac, 0, previousHmac.length);
		hmacFunction.update(timestampBytes, 0, timestampBytes.length);
		hmacFunction.update(messageBytes, 0, messageBytes.length);

		final byte[] hmac = new byte[hmacFunction.getUnderlyingDigest().getDigestSize()];
		hmacFunction.doFinal(hmac, 0);
		return hmac;
	}

	private LogEvent createFirstLineLogEvent() throws GeneralSecurityException {
		LogEvent baseEvent = Log4jLogEvent.newBuilder().setMessage(new SimpleMessage("Initial checkpoint")).setLevel(Level.INFO)
				.setLoggerName(this.getClass().getSimpleName()).setNanoTime(System.nanoTime()).build();

		updateHmacKey();
		final byte[] encryptedHmacKey = encryptHmacKey();

		final String message = layout.innerSerializable(baseEvent);

		byte[] currentHmac = computeHmacForCheckpoint(baseEvent, new byte[0], encryptedHmacKey, message);

		final byte[] sig = signCheckpoint(baseEvent, new byte[0], encryptedHmacKey, message, currentHmac);

		// Prepare for the next event
		previousHmac = currentHmac.clone();
		return SecureLogEventFactory.firstLineEvent(baseEvent, encryptedHmacKey, linesBetweenCheckPoints, currentHmac, sig);
	}

	private LogEvent createCheckpointLogEvent() throws GeneralSecurityException {

		LogEvent baseEvent = Log4jLogEvent.newBuilder().setMessage(new SimpleMessage("Checkpoint event")).setLevel(Level.INFO)
				.setLoggerName(this.getClass().getSimpleName()).setNanoTime(System.nanoTime()).build();

		final byte[] previousHmacKey;
		if (currentHmacKey != null) {
			previousHmacKey = currentHmacKey.clone();
		} else {
			previousHmacKey = new byte[0];
		}
		updateHmacKey();
		final byte[] encryptedHmacKey = encryptHmacKey();

		final String message = layout.innerSerializable(baseEvent);

		byte[] currentHmac = computeHmacForCheckpoint(baseEvent, previousHmacKey, encryptedHmacKey, message);

		final byte[] signature = signCheckpoint(baseEvent, previousHmacKey, encryptedHmacKey, message, currentHmac);

		// Prepare for the next event
		previousHmac = currentHmac.clone();
		return SecureLogEventFactory
				.checkpointEvent(baseEvent, previousHmac, previousHmacKey, encryptedHmacKey, linesBetweenCheckPoints, currentHmac, signature);
	}

	private void updateHmacKey() throws NoSuchAlgorithmException {
		final SecureRandom csprng = SecureRandom.getInstance("SHA1PRNG");
		if (currentHmacKey == null) {
			currentHmacKey = new byte[32];
		}
		csprng.nextBytes(currentHmacKey);
	}

	private byte[] computeHmacForCheckpoint(LogEvent baseEvent, byte[] previousHmacKey, byte[] encryptedHmacKey, String message) {
		HMac hmacFunction = getHmacFunction();
		final ByteBuffer linesBuffer = ByteBuffer.allocate(Integer.BYTES);
		linesBuffer.putInt(linesBetweenCheckPoints);
		final byte[] linesBytes = linesBuffer.array();
		final byte[] timestampBytes = getTimestampBytes(baseEvent);
		final byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

		hmacFunction.update(previousHmac, 0, previousHmac.length);
		hmacFunction.update(previousHmacKey, 0, previousHmacKey.length);
		hmacFunction.update(encryptedHmacKey, 0, encryptedHmacKey.length);
		hmacFunction.update(linesBytes, 0, linesBytes.length);

		hmacFunction.update(timestampBytes, 0, timestampBytes.length);
		hmacFunction.update(messageBytes, 0, messageBytes.length);

		final byte[] hmac = new byte[hmacFunction.getUnderlyingDigest().getDigestSize()];
		hmacFunction.doFinal(hmac, 0);
		return hmac;
	}

	private byte[] signCheckpoint(LogEvent baseEvent, byte[] previousHmacKey, byte[] encryptedHmacKey, String message, byte[] currentHmac)
			throws GeneralSecurityException {
		final Signature signature = Signature.getInstance("SHA256withRSA");

		final PrivateKey privateKey = keysManager.nodeLogSigningPrivateKey();
		signature.initSign(privateKey);

		final ByteBuffer linesBytes = ByteBuffer.allocate(Integer.BYTES);
		linesBytes.putInt(linesBetweenCheckPoints);
		final byte[] timestampBytes = getTimestampBytes(baseEvent);

		signature.update(previousHmac);
		signature.update(previousHmacKey);
		signature.update(encryptedHmacKey);
		signature.update(linesBytes.array());
		signature.update(timestampBytes);
		signature.update(message.getBytes(StandardCharsets.UTF_8));
		signature.update(currentHmac);

		return signature.sign();
	}

	private byte[] encryptHmacKey() throws GeneralSecurityException {
		Cipher c = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding", "BC");
		OAEPParameterSpec oaepSpec = new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
		final PublicKey publicKey = keysManager.nodeLogEncryptionPublicKey();
		c.init(Cipher.ENCRYPT_MODE, publicKey, oaepSpec);
		return c.doFinal(currentHmacKey);
	}

	private HMac getHmacFunction() {
		final HMac hMac = new HMac(new SHA256Digest());
		final KeyParameter keyParameter = new KeyParameter(currentHmacKey);
		hMac.init(keyParameter);
		return hMac;
	}

	private byte[] getTimestampBytes(LogEvent event) {
		ByteBuffer timestampBuff = ByteBuffer.allocate(Long.BYTES);
		timestampBuff.putLong(event.getTimeMillis());
		return timestampBuff.array();
	}

	public static class Builder<B extends Builder<B>> extends AbstractAppender.Builder<B>
			implements org.apache.logging.log4j.core.util.Builder<SecureLogAppender> {
		@PluginAttribute("fileName")
		String fileName;

		@PluginAttribute("filePattern")
		String filePattern;

		@PluginAttribute("sizePolicy")
		String sizePolicy;

		@PluginAttribute("linesBetweenCheckpoints")
		@Required
		int linesBetweenCheckpoints;

		@Override
		public SecureLogAppender build() {
			return new SecureLogAppender(getName(), getFilter(), (SecureLogLayout) getLayout(), fileName, filePattern, sizePolicy,
					linesBetweenCheckpoints);
		}
	}
}
