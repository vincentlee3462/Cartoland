package cartoland.commands;

import cartoland.utilities.CommonFunctions;
import cartoland.utilities.IDs;
import cartoland.utilities.JsonHandle;
import cartoland.utilities.RegularExpressions;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code QuoteCommand} is an execution when a user uses /transfer command. This class implements {@link ICommand}
 * interface, which is for the commands HashMap in {@link cartoland.events.CommandUsage}.
 *
 * @since 1.6
 * @author Alex Cai
 */
public class QuoteCommand implements ICommand
{
	private static final int SUB_STRING_START = ("https://discord.com/channels/" + IDs.CARTOLAND_SERVER_ID + "/").length();

	@Override
	public void commandProcess(SlashCommandInteractionEvent event)
	{
		User user = event.getUser();
		long userID = user.getIdLong();
		String link = event.getOption("link", "", CommonFunctions.getAsString);

		if (!RegularExpressions.CARTOLAND_MESSAGE_LINK_REGEX.matcher(link).matches()) //不是一個有效的訊息連結 或不在創聯
		{
			event.reply(JsonHandle.getString(userID, "quote.invalid_link")).setEphemeral(true).queue();
			return;
		}

		String[] numbersInLink = link.substring(SUB_STRING_START).split("/"); //從字串中取得數字
		//舉例 https://discord.com/channels/886936474723950603/886936474723950611/891666028986253322
		//numbersInLink[0] = "886936474723950611";
		//numbersInLink[1] = "891666028986253322";

		//從創聯中取得頻道
		Guild cartoland, eventGuild = event.getGuild(); //先假設指令在創聯中執行 這樣可以省去一次getGuildById
		if (eventGuild == null || eventGuild.getIdLong() != IDs.CARTOLAND_SERVER_ID) //結果不是在創聯
		{
			cartoland = event.getJDA().getGuildById(IDs.CARTOLAND_SERVER_ID); //定位創聯
			if (cartoland == null) //找不到創聯
			{
				event.reply("Can't get Cartoland server").setEphemeral(true).queue();
				return; //結束
			}
		}
		else
			cartoland = eventGuild;

		//獲取訊息內的頻道 注意ID是String 與慣例的long不同
		GuildMessageChannel linkChannel = cartoland.getChannelById(GuildMessageChannel.class, numbersInLink[0]);
		if (linkChannel == null)
		{
			event.reply(JsonHandle.getString(userID, "quote.no_channel")).setEphemeral(true).queue();
			return;
		}

		//從頻道中取得訊息 注意ID是String 與慣例的long不同
		linkChannel.retrieveMessageById(numbersInLink[1]).queue(linkMessage -> quoteMessage(event, linkChannel, linkMessage),
			new ErrorHandler().handle(ErrorResponse.UNKNOWN_MESSAGE, e -> event.reply(JsonHandle.getString(userID, "quote.no_message")).setEphemeral(true).queue()));
	}

	public static void quoteMessage(IReplyCallback event, MessageChannel channel, Message message)
	{
		User user = event.getUser();
		long userID = user.getIdLong();
		User author = message.getAuthor(); //連結訊息的發送者
		String url = message.getJumpUrl();
		List<MessageEmbed> embeds = new ArrayList<>();
		EmbedBuilder messageEmbed = new EmbedBuilder()
				.setTitle(JsonHandle.getString(userID, "quote.jump_message"), url)
				.setAuthor(author.getEffectiveName(), null, author.getEffectiveAvatarUrl())
				.appendDescription(message.getContentRaw()) //訊息的內容
				.setTimestamp(message.getTimeCreated()) //連結訊息的發送時間
				.setFooter(channel != null ? channel.getName() : author.getName(), null); //訊息的發送頻道

		List<Message.Attachment> attachments = message.getAttachments(); //訊息的附件
		if (attachments.isEmpty()) //沒有任何附件
			embeds.add(messageEmbed.build());
		else //有附件
			addImageAttachments(messageEmbed, embeds, attachments.stream().filter(Message.Attachment::isImage).toList());

		//提及訊息作者 vs 不提及訊息作者
		if (event instanceof SlashCommandInteractionEvent commandEvent && commandEvent.getOption("mention_author", Boolean.FALSE, CommonFunctions.getAsBoolean))
			event.reply(JsonHandle.getString(userID, "quote.mention", user.getEffectiveName(), author.getAsMention())).addEmbeds(embeds).queue();
		else
			event.replyEmbeds(embeds).queue();
	}

	private static void addImageAttachments(EmbedBuilder messageEmbed, List<MessageEmbed> embeds, List<Message.Attachment> images)
	{
		if (images.isEmpty()) //沒有圖片
		{
			embeds.add(messageEmbed.build()); //直接放上訊息embed
			return; //結束
		}

		MessageEmbed embed = messageEmbed.setImage(images.getFirst().getUrl()).build(); //建立訊息embed
		embeds.add(embed); //第一個要放訊息embed

		String title = embed.getTitle(); //訊息embed的title
		String url = embed.getUrl(); //訊息embed的url
		for (int i = 1, size = Math.min(images.size(), Message.MAX_EMBED_COUNT); i < size; i++) //剩下的要開新embed, 注意總數不能超過10個
			embeds.add(new EmbedBuilder()
					.setTitle(title, url) //title必須和訊息embed一樣
					.setImage(images.get(i).getUrl())
					.build());
	}
}