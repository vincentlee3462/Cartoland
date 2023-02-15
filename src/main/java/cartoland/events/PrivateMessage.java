package cartoland.events;

import cartoland.utilities.FileHandle;
import cartoland.utilities.IDAndEntities;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

/**
 * {@code PrivateMessage} is a listener that triggers when a user types anything in the direct message to the bot. This class was registered in
 * {@link cartoland.Cartoland#main}, with the build of JDA.
 *
 * @since 1.0
 * @author Alex Cai
 */
public class PrivateMessage extends ListenerAdapter
{
	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event)
	{
		if (!event.isFromType(ChannelType.PRIVATE)) //不是私訊
			return;
		User author = event.getAuthor();
		if (author.isBot() || author.isSystem()) //是機器人或系統
			return;

		Message message = event.getMessage();
		String rawMessage = message.getContentRaw();
		String attachments = message.getAttachments().stream().map(Message.Attachment::getUrl).collect(Collectors.joining("\n"));
		if (attachments.length() != 0)
			rawMessage += "\n" + attachments;
		IDAndEntities.undergroundChannel.sendMessage(rawMessage).queue(); //私訊轉到地下聊天室

		FileHandle.log(author.getName() + "(" + author.getId() + ") typed \"" + rawMessage + "\" in direct message.");
	}
}