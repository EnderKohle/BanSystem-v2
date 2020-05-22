package net.coalcube.bansystem.core.command;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.util.*;

import java.net.UnknownHostException;
import java.util.UUID;

public class CMDcheck implements Command {

    private BanManager bm;
    private MySQL mysql;
    private Config messages;

    public CMDcheck(BanManager banmanager, MySQL mysql, Config messages) {
        this.bm = banmanager;
        this.mysql = mysql;
        this.messages = messages;
    }

    @Override
    public void execute(User user, String[] args) {
        if (user.hasPermission("bansys.check")) {
            if (mysql.isConnected()) {
                if (args.length == 1) {
                    UUID uuid = UUIDFetcher.getUUID(args[0]);
                    if (uuid == null) {
                        user.sendMessage(
                                messages.getString("Playerdoesnotexist").replaceAll("%P%", messages.getString("prefix")).replaceAll("&", "§"));
                        return;
                    }

                    try {
                        if (bm.isBanned(uuid, Type.CHAT) && bm.isBanned(uuid, Type.NETWORK)) {

                            String player = UUIDFetcher.getName(uuid);
                            String bannerchat = bm.getBanner(uuid, Type.CHAT);
                            String bannernetwork = bm.getBanner(uuid, Type.NETWORK);
                            String reasonchat = bm.getReason(uuid, Type.CHAT);
                            String reasonnetwork = bm.getReason(uuid, Type.NETWORK);
                            String reamingtimechat = BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(bm.getRemainingTime(uuid, Type.CHAT));
                            String reamingtimenetwork = BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(bm.getRemainingTime(uuid, Type.NETWORK));
                            String lvlchat = String.valueOf(bm.getLevel(uuid, bm.getReason(uuid, Type.CHAT)));
                            String lvlnetwork = String.valueOf(bm.getLevel(uuid, bm.getReason(uuid, Type.NETWORK)));

                            for(String m : messages.getStringList("Check.networkandchat")) {
                                user.sendMessage(m
                                        .replaceAll("%P%", messages.getString("prefix"))
                                        .replaceAll("%player%", player)
                                        .replaceAll("%bannerchat%", bannerchat)
                                        .replaceAll("%reasonchat%", reasonchat)
                                        .replaceAll("%reamingtimechat%", reamingtimechat)
                                        .replaceAll("%levelchat%", lvlchat)
                                        .replaceAll("%bannernetwork%", bannernetwork)
                                        .replaceAll("%reasonnetwork%", reasonnetwork)
                                        .replaceAll("%reamingtimenetwork%", reamingtimenetwork)
                                        .replaceAll("%levelnetwork%", lvlnetwork)
                                        .replaceAll("&", "§"));
                            }

                        } else if (bm.isBanned(uuid, Type.CHAT)) {

                            String player = UUIDFetcher.getName(uuid);
                            String banner = bm.getBanner(uuid, Type.CHAT);
                            String reason = bm.getReason(uuid, Type.CHAT);
                            String reamingtime = BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(bm.getRemainingTime(uuid, Type.CHAT));
                            String lvl = String.valueOf(bm.getLevel(uuid, bm.getReason(uuid, Type.CHAT)));

                            for(String m : messages.getStringList("Check.chat")) {
                                user.sendMessage(m
                                        .replaceAll("%P%", messages.getString("prefix"))
                                        .replaceAll("%player%", player)
                                        .replaceAll("%banner%", banner)
                                        .replaceAll("%reason%", reason)
                                        .replaceAll("%reamingtime%", reamingtime)
                                        .replaceAll("%level%", lvl)
                                        .replaceAll("&", "§"));
                            }

                        } else if (bm.isBanned(uuid, Type.NETWORK)) {

                            String player = UUIDFetcher.getName(uuid);
                            String banner = bm.getBanner(uuid, Type.NETWORK);
                            String reason = bm.getReason(uuid, Type.NETWORK);
                            String reamingtime = BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(bm.getRemainingTime(uuid, Type.NETWORK));
                            String lvl = String.valueOf(bm.getLevel(uuid, bm.getReason(uuid, Type.NETWORK)));

                            for(String m : messages.getStringList("Check.network")) {
                                user.sendMessage(m
                                        .replaceAll("%P%", messages.getString("prefix"))
                                        .replaceAll("%player%", player)
                                        .replaceAll("%banner%", banner)
                                        .replaceAll("%reason%", reason)
                                        .replaceAll("%reamingtime%", reamingtime)
                                        .replaceAll("%level%", lvl)
                                        .replaceAll("&", "§"));
                            }

                        } else {
                            user.sendMessage(messages.getString("Playernotbanned")
                                    .replaceAll("%P%", messages.getString("prefix"))
                                    .replaceAll("%player%", UUIDFetcher.getName(uuid))
                                    .replaceAll("&", "§"));
                        }
                    } catch (UnknownHostException e) {
                        user.sendMessage(messages.getString("Check.faild")
                                .replaceAll("%P%", messages.getString("prefix"))
                                .replaceAll("&", "§"));
                        e.printStackTrace();
                    }
                } else {
                    user.sendMessage(messages.getString("Check.usage")
                            .replaceAll("%P%", messages.getString("prefix"))
                            .replaceAll("&", "§"));
                }
            } else {
                user.sendMessage(messages.getString("NoDBConnection"));
            }
        } else {
            user.sendMessage(messages.getString("NoPermission"));
        }
    }
}