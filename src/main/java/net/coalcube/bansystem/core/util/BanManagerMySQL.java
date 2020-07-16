package net.coalcube.bansystem.core.util;

import net.coalcube.bansystem.core.BanSystem;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BanManagerMySQL implements BanManager {

    private final MySQL mysql;

    public BanManagerMySQL(MySQL mysql) {
        this.mysql = mysql;
    }

    @Override
    public void log(String action, String creator, String target, String note) throws SQLException {
        mysql.update("INSERT INTO `logs` (`action`, `target`, `creator`, `note`, `creationdate`) " +
                "VALUES ('" + action + "', '" + target + "','" + creator + "', '" + note + "', NOW());");
    }

    @Override
    public void kick(UUID player, String creator) throws SQLException {
        kick(player, creator, "");
    }

    @Override
    public void kick(UUID player, UUID creator) throws SQLException {
        kick(player, creator.toString(), "");
    }

    @Override
    public void kick(UUID player, String creator, String reason) throws SQLException {
        mysql.update("INSERT INTO `kicks` (`player`, `creator`, `reason`, `creationdate`) " +
                "VALUES ('" + player + "', '" + creator + "', '" + reason + "', NOW());");

        log("Kicked Player", creator, UUIDFetcher.getName(player), (!reason.equals("") ? "reason: " + reason : ""));
    }

    @Override
    public void kick(UUID player, UUID creator, String reason) throws SQLException {
        kick(player, creator.toString(), "");
    }

    @Override
    public void ban(UUID player, long time, UUID creator, Type type, String reason, InetAddress v4adress) throws IOException, SQLException {
        ban(player, time, creator.toString(), type, reason, v4adress);
    }

    @Override
    public void ban(UUID player, long time, UUID creator, Type type, String reason) throws IOException, SQLException {
        ban(player, time, creator.toString(), type, reason);
    }

    @Override
    public void ban(UUID player, long time, String creator, Type type, String reason, InetAddress v4adress) throws IOException, SQLException {
        mysql.update("INSERT INTO `bans` (`player`, `duration`, `creationdate`, `creator`, `reason`, `ip`, `type`) " +
                "VALUES ('" + player + "', '" + time + "', NOW(), '" + creator + "', '" + reason + "', '" + v4adress.getHostName() + "', '" + type + "');");

        mysql.update("INSERT INTO `banhistories` (`player`, `duration`, `creator`, `reason`, `ip`, `type`, `creationdate`) " +
                "VALUES ('" + player + "', '" + time + "', '" + creator + "', '" + reason + "', '" + v4adress.getHostName() + "', '" + type + "', NOW());");

        log("Banned Player", creator, UUIDFetcher.getName(player),
                "reason: " + reason + ", " +
                        "duration: " + BanSystem.getInstance().getTimeFormatUtil().getFormattedRemainingTime(time) + ", " +
                        "Type: " + type);
    }

    @Override
    public void ban(UUID player, long time, String creator, Type type, String reason) throws IOException, SQLException {
        mysql.update("INSERT INTO `bans` (`player`, `duration`, `creationdate`, `creator`, `reason`, `ip`, `type`) " +
                "VALUES ('" + player + "', '" + time + "', NOW(), '" + creator + "', '" + reason + "', '','" + type + "');");

        mysql.update("INSERT INTO `banhistories` (`player`, `duration`, `creator`, `reason`, `type`, `ip`,`creationdate`) " +
                "VALUES ('" + player + "', '" + time + "', '" + creator + "', '" + reason + "', '" + type + "', '', NOW());");
    }

    @Override
    public void unBan(UUID player, UUID unBanner, String reason) throws IOException, SQLException {
        unBan(player, unBanner.toString(), reason);
    }

    @Override
    public void unBan(UUID player, String unBanner, String reason) throws IOException, SQLException {
        mysql.update("DELETE FROM `bans` WHERE player = '" + player + "' AND type = '" + Type.NETWORK + "'");
        mysql.update("INSERT INTO `unbans` (`player`, `unbanner`, `creationdate`, `reason`, `type`) " +
                "VALUES ('" + player + "', '" + unBanner + "', NOW(), '" + reason + "','" + Type.NETWORK +"');");

        log("Unbanned Player", unBanner, UUIDFetcher.getName(player),
                (!reason.equals("") ? "reason: " + reason : ""));
    }

    @Override
    public void unBan(UUID player, UUID unBanner) throws IOException, SQLException {
        unBan(player, unBanner);
    }

    @Override
    public void unBan(UUID player, String unBanner) throws IOException, SQLException {
        mysql.update("DELETE FROM `bans` WHERE player = '" + player + "' AND type = '" + Type.NETWORK + "'");
        mysql.update("INSERT INTO `unbans` (`player`, `unbanner`, `creationdate`, `type`) " +
                "VALUES ('" + player + "', '" + unBanner + "', NOW(), '" + Type.NETWORK +"');");

        log("Unbanned Player", unBanner, UUIDFetcher.getName(player),"");
    }

    @Override
    public void unMute(UUID player, UUID unBanner, String reason) throws IOException, SQLException {
        unMute(player, unBanner.toString(), reason);
    }

    @Override
    public void unMute(UUID player, String unBanner, String reason) throws IOException, SQLException {
        mysql.update("DELETE FROM `bans` WHERE player = '" + player + "' AND type = '" + Type.CHAT + "'");
        mysql.update("INSERT INTO `unbans` (`player`, `unbanner`, `creationdate`, `reason`, `type`) " +
                "VALUES ('" + player + "', '" + unBanner + "', NOW(), '" + reason + "','" + Type.CHAT +"');");

        log("Unmuted Player", unBanner, UUIDFetcher.getName(player),
                (!reason.equals("") ? "reason: " + reason : ""));
    }

    @Override
    public void unMute(UUID player, UUID unBanner) throws IOException, SQLException {
        unMute(player, unBanner.toString());
    }

    @Override
    public void unMute(UUID player, String unBanner) throws IOException, SQLException {
        mysql.update("DELETE FROM `bans` WHERE player = '" + player + "' AND type = '" + Type.CHAT + "'");
        mysql.update("INSERT INTO `unbans` (`player`, `unbanner`, `creationdate`, `type`) " +
                "VALUES ('" + player + "', '" + unBanner + "', NOW(),'" + Type.CHAT +"');");

        log("Unmuted Player", unBanner, UUIDFetcher.getName(player),"");
    }

    @Override
    public void deleteHistory(UUID player, String actor) throws SQLException {
        mysql.update("DELETE FROM `banhistories` WHERE player = '" + player + "';");

        log("Deleted History", actor, player.toString(), "");
    }

    @Override
    public void setIP(UUID player, InetAddress address) throws SQLException {
        mysql.update("UPDATE `bans` SET ip='" + address.getHostName() + "' WHERE ip IS NULL;");
    }

    @Override
    public String getBanReason(UUID player, Type type) throws SQLException {
        ResultSet resultSet = mysql.getResult("SELECT reason FROM `bans` WHERE player = '" + player + "' AND type = '" + type + "';");
        while (resultSet.next()) {
            return resultSet.getString("reason");
        }
        return null;
    }

    @Override
    public Long getEnd(UUID player, Type type) throws SQLException {
        ResultSet resultSet = mysql.getResult("SELECT duration FROM `bans` WHERE player = '" + player + "' AND type = '" + type + "';");
        while (resultSet.next()) {
            Long duration = resultSet.getLong("duration");

            return (duration == -1) ? duration : getCreationDate(player, type) + duration ;
        }
        return null;
    }

    @Override
    public String getBanner(UUID player, Type type) throws SQLException {
        ResultSet resultSet = mysql.getResult("SELECT creator FROM `bans` WHERE player = '" + player + "' AND type = '" + type + "';");
        while (resultSet.next()) {
            return resultSet.getString("creator");
        }
        return null;
    }

    @Override
    public Long getRemainingTime(UUID player, Type type) throws SQLException {
        return (getEnd(player, type) == -1) ? -1 : getEnd(player, type) - System.currentTimeMillis();
    }

    @Override
    public String getReason(UUID player, Type type) throws SQLException {
        ResultSet resultSet = mysql.getResult("SELECT reason FROM `bans` WHERE player = '" + player + "' AND type = '" + type + "';");
        while (resultSet.next()) {
            return resultSet.getString("reason");
        }
        return null;
    }

    @Override
    public int getLevel(UUID player, String reason) throws UnknownHostException, SQLException {
        ResultSet resultSet = mysql.getResult("SELECT * FROM `banhistories` WHERE player = '" + player + "' AND reason = '" + reason + "';");
        int lvl = 0;
        while (resultSet.next()) {
            lvl ++;
        }
        return lvl;
    }

    @Override
    public Long getCreationDate(UUID player, Type type) throws SQLException {
        ResultSet resultSet = mysql.getResult("SELECT creationdate FROM `bans` WHERE player = '" + player + "' AND type = '" + type + "';");
        while (resultSet.next()) {
            return resultSet.getTimestamp("creationdate").getTime();
        }
        return null;
    }

    @Override
    public List<History> getHistory(UUID player) throws UnknownHostException, SQLException {
        ResultSet resultSet = mysql.getResult("SELECT * FROM `banhistories` WHERE player = '" + player + "';");
        List<History> list = new ArrayList<>();
        while (resultSet.next()) {
            list.add(new History(UUID.fromString(
                    resultSet.getString("player")),
                    resultSet.getString("creator"),
                    resultSet.getString("reason"),
                    resultSet.getTimestamp("creationdate").getTime(),
                    resultSet.getLong("duration"),
                    Type.valueOf(resultSet.getString("type")),
                    InetAddress.getByName(resultSet.getString("ip"))));
        }
        return list;
    }

    @Override
    public List<UUID> getBannedPlayersWithSameIP(InetAddress address) throws SQLException {
        ResultSet resultSet = mysql.getResult("SELECT * FROM `banhistories` WHERE ip = '" + address.getHostName() + "';");
        List<UUID> list = new ArrayList<>();
        while (resultSet.next()) {
            list.add(UUID.fromString(resultSet.getString("player")));
        }
        return list;
    }

    @Override
    public boolean hasHistory(UUID player) throws UnknownHostException, SQLException {
        ResultSet resultSet = mysql.getResult("SELECT * FROM `banhistories` WHERE player = '" + player + "';");
        while (resultSet.next()) {
            return true;
        }
        return false;
    }

    @Override
    public boolean hasHistory(UUID player, String reason) throws UnknownHostException, SQLException {
        ResultSet resultSet = mysql.getResult("SELECT * FROM `banhistories` WHERE player='" + player + "' AND reason='" + reason + "';");
        while (resultSet.next()) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isBanned(UUID player, Type type) throws SQLException {
        ResultSet resultSet = mysql.getResult("SELECT * FROM `bans` WHERE player = '" + player + "' and type = '" + type.toString() + "';");
        while (resultSet.next()) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isSetIP(UUID player) {
        return false;
    }
}
