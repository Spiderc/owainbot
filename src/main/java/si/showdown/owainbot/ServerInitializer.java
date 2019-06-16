package si.showdown.owainbot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.vdurmont.emoji.EmojiParser;

@SpringBootApplication
public class ServerInitializer implements ApplicationRunner {
	public void run(ApplicationArguments args) throws FileNotFoundException, IOException {
		boolean prod = true;
		
		String token = "";
		if(prod) {
			Properties properties = new Properties();
			properties.load(new FileInputStream("webapps/owainbot/WEB-INF/classes/config.properties"));
			
			token = properties.getProperty("prod");
		} else {
			Properties properties = new Properties();
			properties.load(new FileInputStream("src/main/resources/config.properties"));
			
			token = properties.getProperty("test");
		}

		DiscordApi api = new DiscordApiBuilder().setToken(token).login().join();

		api.addMessageCreateListener(event -> {
			if (event.getMessageContent().equalsIgnoreCase("!help")) {
				event.getMessage().getUserAuthor().get().sendMessage(Constants.HELP);
			} else if (event.getMessageContent().equalsIgnoreCase("!bard")) {
				event.getChannel().sendMessage(Constants.BARD_LINK);
			} else if (event.getMessageContent().equalsIgnoreCase("!eb")) {
				event.getChannel().sendMessage(Constants.EB_LINK);
			} else if (event.getMessageContent().equalsIgnoreCase("!ben si")) {
				event.getChannel().sendMessage(Constants.BENSI_LINK);
			} else if (event.getMessageContent().equalsIgnoreCase("!ben si redux")) {
				event.getChannel().sendMessage(Constants.BENREDUX_LINK);
			} else if (event.getMessageContent().equalsIgnoreCase("!source")) {
				event.getChannel().sendMessage(Constants.SOURCE);
			} else if (event.getMessageContent().startsWith("!poll ")) {
				createPoll(event);
			}
		});

		api.addServerMemberJoinListener(join -> {
			String message = Constants.GREETINGS[BotUtils.randomNumber(0, Constants.GREETINGS.length) - 1].replaceAll("JOIN_USER", join.getUser().getDisplayName(join.getServer()));
			List<ServerTextChannel> channels = join.getServer().getTextChannels();
			for (ServerTextChannel channel : channels) {
				if (channel.getName().equalsIgnoreCase("general")) {
					channel.sendMessage(message);
				}
			}
		});
	}

	private static void createPoll(MessageCreateEvent event) {
		String pollContents = event.getMessageContent().substring("!poll ".length());
		String[] options = pollContents.split(";");
		String[] reacts = { ":one:", ":two:", ":three:", ":four:", ":five:", ":six:", ":seven:", ":eight:", ":nine:", ":ten:" };

		String pollMessage = Constants.POLL_INTRO.replace("POLL_USER", event.getMessageAuthor().getDisplayName());
		for (int i = 0; i < options.length; i++) {
			if (i == 0) {
				pollMessage = pollMessage + "\n" + options[i];
			} else {
				pollMessage = pollMessage + "\n" + i + ": " + options[i];
			}
		}
		try {
			Message poll = event.getChannel().sendMessage(pollMessage).get();
			for (int i = 0; i < options.length - 1; i++) {
				poll.addReaction(EmojiParser.parseToUnicode(reacts[i]));
			}
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}
}
