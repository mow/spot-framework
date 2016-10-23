package at.spot.mail.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.subethamail.smtp.AuthenticationHandler;
import org.subethamail.smtp.AuthenticationHandlerFactory;
import org.subethamail.smtp.TooMuchDataException;
import org.subethamail.smtp.helper.SimpleMessageListener;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

import at.spot.core.infrastructure.annotation.logging.Log;
import at.spot.core.infrastructure.exception.ModelSaveException;
import at.spot.core.infrastructure.service.ModelService;
import at.spot.core.infrastructure.service.impl.AbstractService;
import at.spot.core.infrastructure.type.LogLevel;
import at.spot.core.management.exception.RemoteServiceInitException;
import at.spot.mail.model.Mail;
import at.spot.mail.service.SmtpService;

@Service
public class DefaultSmtpService extends AbstractService implements SmtpService, SimpleMessageListener {

	protected static final String CONFIG_KEY_PORT = "service.mail.smtp.port";
	protected static final String CONFIG_KEY_BIND_ADDRESS = "service.mail.smtp.bindaddress";
	protected static final int DEFAULT_PORT = 8025;
	protected static final String DEFAULT_BIND_ADDRESS = "localhost";

	protected final SMTPServer smtpServer = new SMTPServer(new SimpleMessageListenerAdapter(this),
			new SMTPAuthHandlerFactory());

	@Autowired
	protected ModelService modelService;

	@Log(logLevel = LogLevel.INFO, message = "Initiating SMTP service ...")
	@PostConstruct
	@Override
	public void init() throws RemoteServiceInitException {
		this.smtpServer.setBindAddress(getBindAddress());
		this.smtpServer.setPort(getPort());
		this.smtpServer.start();
	}

	@Override
	public int getPort() {
		return configurationService.getInteger(CONFIG_KEY_PORT, DEFAULT_PORT);
	}

	public InetAddress getBindAddress() {
		InetAddress ret = null;

		try {
			String address = configurationService.getString(CONFIG_KEY_BIND_ADDRESS, DEFAULT_BIND_ADDRESS);
			ret = InetAddress.getByName(address);
		} catch (UnknownHostException e) {
			loggingService.exception(e.getMessage());
		}

		return ret;
	}

	@Override
	public boolean accept(String paramString1, String paramString2) {
		return true;
	}

	@Override
	public void deliver(String from, String recipient, InputStream data) throws TooMuchDataException, IOException {
		saveMail(from, recipient, data);
	}

	protected void saveMail(String from, String recipient, InputStream data) {
		Mail mail = modelService.create(Mail.class);

		mail.sender = from;
		mail.toRecipients.add(recipient);
		mail.content = data.toString();

		try {
			modelService.save(mail);
		} catch (ModelSaveException e) {
			loggingService.exception("Can't save received mail.");
		}
	}

	final class SMTPAuthHandlerFactory implements AuthenticationHandlerFactory {
		private static final String LOGIN_MECHANISM = "LOGIN";

		public AuthenticationHandler create() {
			return new SMTPAuthHandler();
		}

		public List<String> getAuthenticationMechanisms() {
			List result = new ArrayList();
			result.add("LOGIN");
			return result;
		}
	}

	final class SMTPAuthHandler implements AuthenticationHandler {
		private static final String USER_IDENTITY = "User";
		private static final String PROMPT_USERNAME = "334 VXNlcm5hbWU6";
		private static final String PROMPT_PASSWORD = "334 UGFzc3dvcmQ6";
		private int pass;

		SMTPAuthHandler() {
			this.pass = 0;
		}

		public String auth(String clientInput) {
			String prompt;

			if (++this.pass == 1) {
				prompt = "334 VXNlcm5hbWU6";
			} else {
				if (this.pass == 2) {
					prompt = "334 UGFzc3dvcmQ6";
				} else {
					this.pass = 0;
					prompt = null;
				}
			}
			return prompt;
		}

		public Object getIdentity() {
			return "User";
		}
	}
}