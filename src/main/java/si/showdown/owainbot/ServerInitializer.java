package si.showdown.owainbot;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.vdurmont.emoji.EmojiParser;

import si.showdown.owainbot.bean.Crit;
import si.showdown.owainbot.controller.CritController;

@SpringBootApplication
public class ServerInitializer implements ApplicationRunner {
	public void run(ApplicationArguments args) throws FileNotFoundException, IOException {
		boolean prod = true;

		String token = "";
		if (prod) {
			Properties properties = new Properties();
			properties.load(new FileInputStream("webapps/owainbot/WEB-INF/classes/config.properties"));

			token = properties.getProperty("prod");
		} else {
			Properties properties = new Properties();
			properties.load(new FileInputStream("src/main/resources/config.properties"));

			token = properties.getProperty("test");
		}

		DiscordApi api = new DiscordApiBuilder().setToken(token).login().join();

		api.updateActivity("!help for commands");

		api.addServerTextChannelChangeTopicListener(event -> {
			try {
				User user = api.getUserById(116331879635877894L).get();
				user.sendMessage("The topic in #" + event.getServerTextChannel().get().getName() + " changed from '" + event.getOldTopic() + "' to '" + event.getNewTopic() + "'");
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		});

		api.addMessageCreateListener(event -> {
			if (event.getMessageContent().equalsIgnoreCase("!help")) {
				event.getMessage().getUserAuthor().get().sendMessage(Constants.HELP);
			} else if (event.getMessageContent().equalsIgnoreCase("!bard")) {
				event.getChannel().sendMessage(Constants.BARD_LINK);
			} else if (event.getMessageContent().equalsIgnoreCase("!eb")) {
				event.getChannel().sendMessage(Constants.EB_LINK);
			} else if (event.getMessageContent().equalsIgnoreCase("!aoa")) {
				event.getChannel().sendMessage(Constants.AOA_LINK);
			} else if (event.getMessageContent().equalsIgnoreCase("!source")) {
				event.getChannel().sendMessage(Constants.SOURCE);
			} else if (event.getMessageContent().equalsIgnoreCase("!roles")) {
				roles(event);
			} else if (event.getMessageContent().equalsIgnoreCase("!attack")) {
				attack(event);
			} else if (event.getMessageContent().startsWith("!poll ")) {
				createPoll(event);
			} else if (event.getMessageContent().startsWith("!roll ")) {
				rollDice(event);
			} else if (event.getMessageContent().startsWith("!play ")) {
				play(event);
			} else if (event.getMessageContent().startsWith("!addrole ")) {
				addRole(event);
			} else if (event.getMessageContent().startsWith("!removerole ")) {
				removeRole(event);
			} else if (event.getMessageContent().equals("!census")) {
				CensusThread thread = new CensusThread(event);
				thread.run();
			} else if (event.getMessageContent().startsWith("!seteth ")) {
				seteth(event);
			} else if (event.getMessageContent().startsWith("!crit")) {
				crit(event);
			}
		});

		api.addServerMemberJoinListener(join -> {
			String message = Constants.GREETINGS[BotUtils.randomNumber(0, Constants.GREETINGS.length - 1)].replaceAll("JOIN_USER", join.getUser().getDisplayName(join.getServer()));
			List<ServerTextChannel> channels = join.getServer().getTextChannels();
			for (ServerTextChannel channel : channels) {
				if (channel.getName().equalsIgnoreCase("general")) {
					channel.sendMessage(message);
				}
			}
		});
	}

	private static void roles(MessageCreateEvent event) {
		Server server = event.getMessage().getServer().get();
		Role botRole = null;

		for (Role role : event.getApi().getYourself().getRoles(server)) {
			if (role.isManaged() && !role.isEveryoneRole()) {
				botRole = role;
			}
		}

		String roles = "```";
		for (Role role : server.getRoles()) {
			if (!role.isManaged() && !role.isEveryoneRole()) {
				roles = roles + "\n" + role.getName() + ": " + role.getUsers().size();
			} else if (botRole != null && role == botRole) {
				break;
			}
		}

		event.getChannel().sendMessage(roles + "```");
	}

	private static void attack(MessageCreateEvent event) {
		User user = event.getMessageAuthor().asUser().get();
		Server server = event.getMessage().getServer().get();

		boolean foundRole = false;
		for (Role userRole : user.getRoles(server)) {
			if (userRole.getName().equals("Black Eagles")) {
				event.getChannel().sendMessage("Black Eagle Bash!");
				foundRole = true;
				break;
			} else if (userRole.getName().equals("Blue Lions")) {
				event.getChannel().sendMessage("Blue Lion Lacerate!");
				foundRole = true;
				break;
			} else if (userRole.getName().equals("Golden Deer")) {
				event.getChannel().sendMessage("Golden Deer Gore!");
				foundRole = true;
				break;
			}
		}

		if (!foundRole) {
			event.getChannel().sendMessage("My sword hand twitches, but I must know your allegiance before I can show you my latest attack!");
		}
	}

	private static void createPoll(MessageCreateEvent event) {
		String pollContents = event.getMessageContent().substring("!poll ".length());
		String[] options = pollContents.split(";");
		String[] reacts = { ":one:", ":two:", ":three:", ":four:", ":five:", ":six:", ":seven:", ":eight:", ":nine:", ":keycap_ten:" };

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

	private static void rollDice(MessageCreateEvent event) {
		String message = "```";
		int total = 0;

		String dice = event.getMessageContent().substring("!roll ".length());
		String[] rolls = dice.replaceAll("\\-", "\\+-").split("\\+");

		for (String roll : rolls) {
			if (!roll.trim().equals("")) {
				boolean positive = true;
				String[] rollParts = roll.trim().replace(" ", "").split("d");

				if (rollParts[0].startsWith("-") && rollParts.length != 1) {
					message = message + " - ";
					positive = false;
				} else if (!message.equals("```")) {
					message = message + " + ";
				}

				if (rollParts.length == 1) {
					message = message + rollParts[0];
					total = total + Integer.parseInt(rollParts[0]);
				} else {
					message = message + "( ";
					for (int i = 0; i < Math.abs(Integer.parseInt(rollParts[0])); i++) {
						int rollValue = BotUtils.randomNumber(1, Integer.parseInt(rollParts[1]));
						message = message + rollValue + " + ";
						if (positive) {
							total = total + rollValue;
						} else {
							total = total - rollValue;
						}
					}
					if (!message.equals("( ")) {
						message = message.substring(0, message.length() - 3);
					}
					message = message + " )";
				}
			}
		}

		message = message + " = " + total + "```";

		event.getChannel().sendMessage(message);
	}

	private static void play(MessageCreateEvent event) {
		String link = event.getMessageContent().substring("!play ".length());
		System.out.println(link);

		User user = event.getMessageAuthor().asUser().get();
		Server server = event.getMessage().getServer().get();

		if (server.getConnectedVoiceChannel(user).isPresent()) {
			ServerVoiceChannel voiceChannel = event.getMessage().getServer().get().getConnectedVoiceChannel(user).get();
		}
	}

	private static void addRole(MessageCreateEvent event) {
		User user = event.getMessageAuthor().asUser().get();
		Server server = event.getMessage().getServer().get();

		String role = event.getMessageContent().substring("!addrole ".length());
		Role targetRole = null;

		for (Role serverRole : server.getRoles()) {
			if (serverRole.getName().toLowerCase().equals(role.toLowerCase())) {
				targetRole = serverRole;
				break;
			}
		}

		if (targetRole != null) {
			user.addRole(targetRole);
		}
	}

	private static void removeRole(MessageCreateEvent event) {
		User user = event.getMessageAuthor().asUser().get();
		Server server = event.getMessage().getServer().get();

		String role = event.getMessageContent().substring("!removerole ".length());
		Role targetRole = null;

		for (Role userRole : user.getRoles(server)) {
			if (userRole.getName().toLowerCase().equals(role.toLowerCase())) {
				targetRole = userRole;
				break;
			}
		}

		if (targetRole != null) {
			user.removeRole(targetRole);
		}
	}

	private static void seteth(MessageCreateEvent event) {
		String message = event.getMessageContent().substring("!seteth ".length());
		
		String[] splitMessage = message.split(" ");
		List<String> lines = new ArrayList<>();
		
		int line = 0;
		int charCount = 0;
		for(String word:splitMessage) {
			if(charCount == 0) {
				lines.add(word);
				charCount = word.length();
			} else if(charCount + word.length() > 45) {
				line = line + 1;
				lines.add(word);
				charCount = word.length();
			} else {
				lines.set(line, lines.get(line) + " " + word);
				charCount = charCount + 1 + word.length();
			}
		}

		File file = new File("seteth.png");
		try {
			final BufferedImage image = ImageIO.read(new URL("https://i.imgur.com/6JTlReh.png"));

			Graphics g = image.getGraphics();
			g.setFont(new Font("Athelas Regular", Font.PLAIN, 18));
			g.setColor(new Color(80, 74, 50));
			
			int height = 210;
			for(String text:lines) {
				g.drawString(text, 20, height);
				height = height + 20;
			}
			
			g.dispose();

			ImageIO.write(image, "png", file);

			event.getChannel().sendMessage(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void crit(MessageCreateEvent event) {
		String param = "";
		if(!event.getMessageContent().equals("!crit")) {
			param = event.getMessageContent().substring("!crit ".length());
		}
		
		CritController quoteController = new CritController();
		Crit quote = quoteController.getRandomQuote(param);
		
		String tag = "";
		if(quote.getTag() != null && !quote.getTag().equals("")) {
			tag = " [" + quote.getTag() + "]";
		}

		event.getChannel().sendMessage("\"" + quote.getQuote() + "\" -" + quote.getCharacter() + tag + " (" + quote.getGame() + ")");
	}
}
