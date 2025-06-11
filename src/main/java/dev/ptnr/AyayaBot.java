package dev.ptnr;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.FileUpload;

import java.time.Instant;
import java.util.List;

public class AyayaBot {
    public static void main(String[] args) {
        String token = Dotenv.load().get("DISCORD_TOKEN");

        JDA jda = JDABuilder.createDefault(token)
                .addEventListeners(new BotListener())
                .build();

        try {
            jda.awaitReady();
        } catch (InterruptedException e) {
            System.err.println("[ERR > AyayaBot.main()] Failed to Init\n" + e.getMessage());
        }
    }
}

class BotListener extends ListenerAdapter {
    @Override
    public void onReady(ReadyEvent event) {
        JDA jda = event.getJDA();

        CommandListUpdateAction commands = jda.updateCommands();

        commands.addCommands(
                Commands.slash("hitomi", "Get Image from ID (Hitomi)")
                        .addOptions(new OptionData(OptionType.INTEGER, "id", "gallery id")
                                .setRequiredRange(0, 9999999).setRequired(true))
                        .setContexts(InteractionContextType.GUILD)
        );

        commands.queue();

        List<Guild> guildList = jda.getGuilds();
        for (Guild guild : guildList) {
            System.out.println("Contain Guild : " + guild.getName());
        }

        System.out.println(jda.getSelfUser().getName() + " is Ready.");
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) return;

        switch (event.getName()) {
            case "hitomi":
                hitomi(event, event.getOption("id").getAsInt());
                break;
        }
    }

    public void hitomi(SlashCommandInteractionEvent event, Integer id) {
        event.reply("작품을 로드합니다. 조금 기다려주세요...").setEphemeral(true).queue();

        HitomiDTO hitomiData = Hitomi.GetHitomiData(id);

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(hitomiData.getTitle());
        eb.appendDescription("`품번: " + hitomiData.getId() + "`\n");
        eb.appendDescription("`작가: " + hitomiData.getArtistsAsString() + "`\n");
        eb.appendDescription("`종류: " + hitomiData.getType() + "`\n");
        eb.appendDescription("`태그: " + hitomiData.getTagsAsString() + "`");

        String coverUrl = Hitomi.getImageUrl(hitomiData.getImageHashList().get(0));
        byte[] webpData = AyayaUtils.GetFileFromUrl(coverUrl);
        if (webpData == null) {
            event.getHook().editOriginal("URL에서 이미지를 가져오지 못했습니다. URL이나 서버 상태를 확인해주세요.").queue();
            return;
        }

        byte[] pngData = AyayaUtils.ConvertWebpToPng(webpData);
        if (pngData == null) {
            event.getHook().editOriginal("이미지를 PNG로 변환하는 데 실패했습니다. 파일이 올바른 WebP 형식이 아닐 수 있습니다.").queue();
            return;
        }

        eb.setImage("attachment://" + "1.png");
        eb.setFooter("Uploader : " + event.getUser().getName());
        eb.setTimestamp(Instant.now());

        FileUpload fileUpload = FileUpload.fromData(pngData, "1.png");
        event.getChannel().sendFiles(fileUpload).setEmbeds(eb.build()).queue();
        event.getHook().editOriginal("작품을 받아오는 데에 성공하였습니다!").queue();
    }
}
